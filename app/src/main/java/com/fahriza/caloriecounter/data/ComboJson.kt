package com.fahriza.caloriecounter.data

import org.json.JSONArray
import org.json.JSONObject

fun combosToJson(combos: List<Combo>): String {
    val arr = JSONArray()
    combos.forEach { c ->
        val steps = JSONArray()
        c.steps.forEach { s ->
            steps.put(JSONObject().put("w", s.workoutName).put("work", s.workSec).put("rest", s.restSec))
        }
        arr.put(
            JSONObject()
                .put("id", c.id)
                .put("name", c.name)
                .put("desc", c.description)
                .put("rounds", c.rounds)
                .put("steps", steps)
        )
    }
    return arr.toString()
}

fun combosFromJson(json: String?): List<Combo> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val stepsArr = o.getJSONArray("steps")
            Combo(
                id = o.getString("id"),
                name = o.getString("name"),
                description = o.optString("desc", ""),
                steps = List(stepsArr.length()) { j ->
                    val s = stepsArr.getJSONObject(j)
                    ComboStep(s.getString("w"), s.getInt("work"), s.getInt("rest"))
                },
                rounds = o.getInt("rounds"),
                builtIn = false
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
