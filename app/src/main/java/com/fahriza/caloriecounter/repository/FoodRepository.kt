package com.fahriza.caloriecounter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import androidx.datastore.preferences.preferencesDataStore
import com.fahriza.caloriecounter.data.AppDatabase
import com.fahriza.caloriecounter.data.Combo
import com.fahriza.caloriecounter.data.combosFromJson
import com.fahriza.caloriecounter.data.combosToJson
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.data.FoodEntry
import com.fahriza.caloriecounter.sync.TursoClient
import com.fahriza.caloriecounter.sync.TursoException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class FoodRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.foodDao()
    private val dailyGoalKey = intPreferencesKey("daily_goal")

    val dailyGoal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[dailyGoalKey] ?: 2000
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[dailyGoalKey] = goal }
    }

    private val weightKey = floatPreferencesKey("weight_kg")

    val weightKg: Flow<Float?> = context.dataStore.data.map { it[weightKey] }

    suspend fun setWeightKg(weight: Float) {
        context.dataStore.edit { it[weightKey] = weight }
    }

    private val combosKey = stringPreferencesKey("user_combos")

    val userCombos: Flow<List<Combo>> = context.dataStore.data.map { combosFromJson(it[combosKey]) }

    suspend fun saveUserCombos(combos: List<Combo>) {
        context.dataStore.edit { it[combosKey] = combosToJson(combos) }
    }

    // --- Cloud backup (Turso) ---
    private val tursoUrlKey = stringPreferencesKey("turso_url")
    private val tursoTokenKey = stringPreferencesKey("turso_token")
    private val lastBackupKey = longPreferencesKey("last_backup")

    val tursoUrl: Flow<String> = context.dataStore.data.map { it[tursoUrlKey] ?: "" }
    val tursoToken: Flow<String> = context.dataStore.data.map { it[tursoTokenKey] ?: "" }
    val lastBackup: Flow<Long> = context.dataStore.data.map { it[lastBackupKey] ?: 0L }

    suspend fun saveTursoConfig(url: String, token: String) {
        context.dataStore.edit {
            it[tursoUrlKey] = url.trim()
            it[tursoTokenKey] = token.trim()
        }
    }

    /** Mengembalikan jumlah entri yang di-backup. */
    suspend fun backupToCloud(url: String, token: String): Int {
        val client = TursoClient(url, token)
        saveTursoConfig(url, token)
        val entries = dao.getAll()
        val goal = dailyGoal.first()
        client.replaceBackup(entries, goal, weightKg.first(), context.dataStore.data.first()[combosKey])
        context.dataStore.edit { it[lastBackupKey] = System.currentTimeMillis() }
        return entries.size
    }

    /** Mengganti seluruh data lokal dengan backup cloud. Mengembalikan jumlah entri yang dipulihkan. */
    suspend fun restoreFromCloud(url: String, token: String): Int {
        val client = TursoClient(url, token)
        saveTursoConfig(url, token)
        val backup = client.fetchBackup()
        // Backup kosong ditolak supaya tidak menghapus semua data lokal secara tidak sengaja.
        if (backup.entries.isEmpty()) throw TursoException("Backup di cloud kosong, restore dibatalkan")
        db.withTransaction {
            dao.deleteAll()
            dao.insertAll(backup.entries)
        }
        backup.dailyGoal?.takeIf { it > 0 }?.let { setDailyGoal(it) }
        backup.weightKg?.takeIf { it > 0f }?.let { setWeightKg(it) }
        backup.combosJson?.let { json ->
            // Hanya dipulihkan jika JSON valid (tidak kosong setelah di-parse).
            if (combosFromJson(json).isNotEmpty()) context.dataStore.edit { it[combosKey] = json }
        }
        return backup.entries.size
    }

    fun getEntriesByDate(date: String): Flow<List<FoodEntry>> =
        dao.getEntriesByDate(date)

    fun getDailyTotals(): Flow<List<DailyTotal>> =
        dao.getDailyTotals()

    suspend fun addEntry(entry: FoodEntry) = dao.insert(entry)

    suspend fun updateEntry(entry: FoodEntry) = dao.update(entry)

    suspend fun deleteEntry(id: Int) = dao.delete(id)
}
