package com.fahriza.caloriecounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class EntryTypeConverter {
    @TypeConverter fun fromType(type: EntryType): String = type.name
    @TypeConverter fun toType(value: String): EntryType = EntryType.valueOf(value)
}

@Database(entities = [FoodEntry::class], version = 1)
@TypeConverters(EntryTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorie_counter.db"
                ).build().also { INSTANCE = it }
            }
    }
}
