package com.fahriza.caloriecounter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahriza.caloriecounter.data.AppDatabase
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.data.FoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class FoodRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).foodDao()
    private val dailyGoalKey = intPreferencesKey("daily_goal")

    val dailyGoal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[dailyGoalKey] ?: 2000
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[dailyGoalKey] = goal }
    }

    fun getEntriesByDate(date: String): Flow<List<FoodEntry>> =
        dao.getEntriesByDate(date)

    fun getDailyTotals(): Flow<List<DailyTotal>> =
        dao.getDailyTotals()

    suspend fun addEntry(entry: FoodEntry) = dao.insert(entry)

    suspend fun deleteEntry(id: Int) = dao.delete(id)
}
