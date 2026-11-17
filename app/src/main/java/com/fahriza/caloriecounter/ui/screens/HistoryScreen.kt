package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.data.DailyTotal
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(vm: CalorieViewModel) {
    val dailyTotals by vm.dailyTotals.collectAsState()
    val dailyGoal by vm.dailyGoal.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Text(
                "Riwayat 30 Hari",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
        }

        if (dailyTotals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada riwayat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(dailyTotals, key = { it.date }) { daily ->
                HistoryItem(daily = daily, goal = dailyGoal)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryItem(daily: DailyTotal, goal: Int) {
    val progress = (daily.total.toFloat() / goal).coerceIn(0f, 1f)
    val overGoal = daily.total > goal
    val dateLabel = remember(daily.date) {
        try {
            LocalDate.parse(daily.date)
                .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("id", "ID")))
        } catch (e: Exception) {
            daily.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${daily.total} kkal",
                    fontWeight = FontWeight.Bold,
                    color = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
