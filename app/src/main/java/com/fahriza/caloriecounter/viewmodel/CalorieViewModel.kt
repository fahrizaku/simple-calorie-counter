package com.fahriza.caloriecounter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.data.EntryType
import com.fahriza.caloriecounter.data.FoodEntry
import com.fahriza.caloriecounter.repository.FoodRepository
import com.fahriza.caloriecounter.sync.TursoException
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

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}

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

    val tursoUrl: StateFlow<String> = repo.tursoUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val tursoToken: StateFlow<String> = repo.tursoToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val lastBackup: StateFlow<Long> = repo.lastBackup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun clearSyncState() { _syncState.value = SyncState.Idle }

    fun backup(url: String, token: String) = runSync {
        val n = repo.backupToCloud(url, token)
        SyncState.Success("Backup berhasil ($n entri)")
    }

    fun restore(url: String, token: String) = runSync {
        val n = repo.restoreFromCloud(url, token)
        SyncState.Success("Restore berhasil ($n entri)")
    }

    private fun runSync(block: suspend () -> SyncState) {
        if (_syncState.value == SyncState.Loading) return
        _syncState.value = SyncState.Loading
        viewModelScope.launch {
            _syncState.value = try {
                block()
            } catch (e: TursoException) {
                SyncState.Error(e.message ?: "Gagal")
            } catch (e: Exception) {
                SyncState.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

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
