package com.fahriza.caloriecounter.sync

import com.fahriza.caloriecounter.data.EntryType
import com.fahriza.caloriecounter.data.FoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TursoException(message: String) : Exception(message)

data class CloudBackup(val entries: List<FoodEntry>, val dailyGoal: Int?, val weightKg: Float?, val combosJson: String?)

/**
 * Klien minimal untuk Turso lewat HTTP API (Hrana over HTTP, /v2/pipeline).
 * Tanpa SDK native: hanya HttpURLConnection + org.json bawaan Android.
 */
class TursoClient(url: String, private val token: String) {
    private val baseUrl: String = url.trim()
        .replaceFirst("libsql://", "https://")
        .trimEnd('/')

    init {
        if (!baseUrl.startsWith("https://")) {
            throw TursoException("URL harus diawali libsql:// atau https://")
        }
        if (token.isBlank()) throw TursoException("Token belum diisi")
    }

    /** Snapshot penuh: isi tabel remote diganti dengan data lokal, atomik (gagal = rollback). */
    suspend fun replaceBackup(entries: List<FoodEntry>, dailyGoal: Int, weightKg: Float?, combosJson: String?) {
        val steps = JSONArray()
        var last = -1
        fun add(sql: String, args: List<JSONObject> = emptyList()) {
            val step = JSONObject().put("stmt", stmt(sql, args))
            if (last >= 0) step.put("condition", JSONObject().put("type", "ok").put("step", last))
            steps.put(step)
            last = steps.length() - 1
        }

        add("BEGIN")
        add(
            "CREATE TABLE IF NOT EXISTS food_entries (" +
                "id INTEGER PRIMARY KEY, calories INTEGER NOT NULL, note TEXT NOT NULL, " +
                "type TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, createdAt INTEGER NOT NULL)"
        )
        add("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        add("DELETE FROM food_entries")
        add("DELETE FROM app_settings")
        for (e in entries) {
            add(
                "INSERT INTO food_entries (id, calories, note, type, date, time, createdAt) VALUES (?,?,?,?,?,?,?)",
                listOf(int(e.id.toLong()), int(e.calories.toLong()), text(e.note), text(e.type.name),
                    text(e.date), text(e.time), int(e.createdAt))
            )
        }
        add("INSERT INTO app_settings (key, value) VALUES (?, ?)", listOf(text("daily_goal"), text(dailyGoal.toString())))
        if (weightKg != null) {
            add("INSERT INTO app_settings (key, value) VALUES (?, ?)", listOf(text("weight_kg"), text(weightKg.toString())))
        }
        if (!combosJson.isNullOrBlank()) {
            add("INSERT INTO app_settings (key, value) VALUES (?, ?)", listOf(text("user_combos"), text(combosJson)))
        }
        add("COMMIT")
        val commitStep = last
        // Jika salah satu langkah gagal, COMMIT tidak jalan dan transaksi dibatalkan.
        steps.put(
            JSONObject()
                .put("stmt", stmt("ROLLBACK", emptyList()))
                .put(
                    "condition",
                    JSONObject().put("type", "not").put(
                        "cond", JSONObject().put("type", "ok").put("step", commitStep)
                    )
                )
        )

        val request = JSONObject().put("type", "batch").put("batch", JSONObject().put("steps", steps))
        val response = send(listOf(request))
        val batch = response.getJSONObject(0).getJSONObject("response").getJSONObject("result")
        val results = batch.getJSONArray("step_results")
        if (results.isNull(commitStep)) {
            val errors = batch.getJSONArray("step_errors")
            val msg = (0 until errors.length())
                .firstNotNullOfOrNull { errors.optJSONObject(it)?.optString("message") }
            throw TursoException("Backup gagal: ${msg ?: "transaksi dibatalkan"}")
        }
    }

    suspend fun fetchBackup(): CloudBackup {
        val requests = listOf(
            execute("SELECT id, calories, note, type, date, time, createdAt FROM food_entries ORDER BY id"),
            execute("SELECT key, value FROM app_settings")
        )
        val results = try {
            send(requests)
        } catch (e: TursoException) {
            if (e.message?.contains("no such table", ignoreCase = true) == true) {
                throw TursoException("Belum ada backup di cloud")
            }
            throw e
        }
        val entries = rows(results.getJSONObject(0)).map { r ->
            FoodEntry(
                id = r[0].toInt(),
                calories = r[1].toInt(),
                note = r[2],
                type = EntryType.valueOf(r[3]),
                date = r[4],
                time = r[5],
                createdAt = r[6].toLong()
            )
        }
        val settings = rows(results.getJSONObject(1)).associate { it[0] to it[1] }
        return CloudBackup(entries, settings["daily_goal"]?.toIntOrNull(), settings["weight_kg"]?.toFloatOrNull(), settings["user_combos"])
    }

    // --- helpers ---

    private fun rows(result: JSONObject): List<List<String>> {
        val rows = result.getJSONObject("response").getJSONObject("result").getJSONArray("rows")
        return (0 until rows.length()).map { i ->
            val row = rows.getJSONArray(i)
            (0 until row.length()).map { j -> row.getJSONObject(j).optString("value", "") }
        }
    }

    private fun text(v: String) = JSONObject().put("type", "text").put("value", v)
    private fun int(v: Long) = JSONObject().put("type", "integer").put("value", v.toString())

    private fun stmt(sql: String, args: List<JSONObject>) =
        JSONObject().put("sql", sql).put("args", JSONArray(args))

    private fun execute(sql: String) =
        JSONObject().put("type", "execute").put("stmt", stmt(sql, emptyList()))

    /** Kirim pipeline; mengembalikan daftar hasil (tanpa 'close'), melempar TursoException jika ada error. */
    private suspend fun send(requests: List<JSONObject>): JSONArray = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("requests", JSONArray(requests + JSONObject().put("type", "close")))
            .toString()
        val conn = (URL("$baseUrl/v2/pipeline").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code != 200) {
                val detail = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                throw TursoException(
                    when (code) {
                        401, 403 -> "Token tidak valid atau tidak punya akses"
                        404 -> "Database tidak ditemukan, periksa URL"
                        else -> "Server menolak (HTTP $code) ${detail.take(120)}"
                    }
                )
            }
            val results = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONArray("results")
            val out = JSONArray()
            for (i in 0 until results.length() - 1) { // elemen terakhir = hasil 'close'
                val r = results.getJSONObject(i)
                if (r.getString("type") == "error") {
                    throw TursoException(r.getJSONObject("error").optString("message", "Error tidak diketahui"))
                }
                out.put(r)
            }
            out
        } catch (e: IOException) {
            throw TursoException("Tidak bisa terhubung ke Turso, periksa koneksi internet")
        } finally {
            conn.disconnect()
        }
    }
}
