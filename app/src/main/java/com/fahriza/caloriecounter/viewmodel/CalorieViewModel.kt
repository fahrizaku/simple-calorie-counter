package com.fahriza.caloriecounter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.data.EntryType
import com.fahriza.caloriecounter.data.FoodEntry
import com.fahriza.caloriecounter.repository.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class CalorieViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FoodRepository(app)

    private fun todayString(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** Tanggal yang sedang dilihat/diisi (format yyyy-MM-dd). Default: hari ini. */
    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate

    val entries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { repo.getEntriesByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyGoal: StateFlow<Int> = repo.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val dailyTotals: StateFlow<List<DailyTotal>> = repo.getDailyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        // Tidak boleh mencatat untuk tanggal di masa depan.
        val target = if (date.isAfter(LocalDate.now())) LocalDate.now() else date
        _selectedDate.value = target.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun selectDate(date: String) = selectDate(LocalDate.parse(date))

    fun addEntry(calories: Int, note: String, type: EntryType) {
        viewModelScope.launch {
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            repo.addEntry(
                FoodEntry(calories = calories, note = note, type = type, date = _selectedDate.value, time = time)
            )
        }
    }

    fun updateEntry(entry: FoodEntry, calories: Int, note: String, type: EntryType) {
        viewModelScope.launch {
            repo.updateEntry(entry.copy(calories = calories, note = note, type = type))
        }
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { repo.setDailyGoal(goal) }
    }
}
