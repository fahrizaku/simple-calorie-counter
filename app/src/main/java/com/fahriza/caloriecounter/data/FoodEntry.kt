package com.fahriza.caloriecounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType { CONSUMED, BURNED }

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val calories: Int,
    val note: String = "",
    val type: EntryType = EntryType.CONSUMED,
    val date: String,
    val time: String,
    val createdAt: Long = System.currentTimeMillis()
)
