package com.fahriza.caloriecounter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.data.FoodEntry
import com.fahriza.caloriecounter.repository.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CalorieViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FoodRepository(app)

    val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todayEntries: StateFlow<List<FoodEntry>> = repo
        .getEntriesByDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyGoal: StateFlow<Int> = repo.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val dailyTotals: StateFlow<List<DailyTotal>> = repo.getDailyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(calories: Int, note: String, type: com.fahriza.caloriecounter.data.EntryType) {
        viewModelScope.launch {
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            repo.addEntry(FoodEntry(calories = calories, note = note, type = type, date = today, time = time))
        }
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { repo.setDailyGoal(goal) }
    }
}
