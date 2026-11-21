package com.fahriza.caloriecounter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY createdAt DESC")
    fun getEntriesByDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT date, SUM(CASE WHEN type = 'BURNED' THEN -calories ELSE calories END) as total FROM food_entries GROUP BY date ORDER BY date DESC LIMIT 30")
    fun getDailyTotals(): Flow<List<DailyTotal>>
}

data class DailyTotal(
    val date: String,
    val total: Int
)
