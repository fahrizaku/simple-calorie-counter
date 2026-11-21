package com.fahriza.caloriecounter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(entry: FoodEntry)

    @Insert
    suspend fun insertAll(entries: List<FoodEntry>)

    @Query("SELECT * FROM food_entries ORDER BY id")
    suspend fun getAll(): List<FoodEntry>

    @Query("DELETE FROM food_entries")
    suspend fun deleteAll()

    @Update
    suspend fun update(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY createdAt DESC")
    fun getEntriesByDate(date: String): Flow<List<FoodEntry>>

    @Query(
        "SELECT date, " +
            "SUM(CASE WHEN type = 'CONSUMED' THEN calories ELSE 0 END) as consumed, " +
            "SUM(CASE WHEN type = 'BURNED' THEN calories ELSE 0 END) as burned " +
            "FROM food_entries GROUP BY date ORDER BY date DESC LIMIT 30"
    )
    fun getDailyTotals(): Flow<List<DailyTotal>>
}

data class DailyTotal(
    val date: String,
    val consumed: Int,
    val burned: Int
) {
    val total: Int get() = consumed - burned
}
