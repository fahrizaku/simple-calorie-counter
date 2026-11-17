package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahriza.caloriecounter.data.EntryType
import com.fahriza.caloriecounter.data.FoodEntry
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: CalorieViewModel) {
    val entries by vm.todayEntries.collectAsState()
    val dailyGoal by vm.dailyGoal.collectAsState()

    val consumed = entries.filter { it.type == EntryType.CONSUMED }.sumOf { it.calories }
    val burned = entries.filter { it.type == EntryType.BURNED }.sumOf { it.calories }
    val net = consumed - burned
    val progress = (net.toFloat() / dailyGoal).coerceIn(0f, 1f)
    val remaining = dailyGoal - net

    var showAddSheet by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID")))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Target", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                CalorieSummaryCard(
                    consumed = consumed,
                    burned = burned,
                    net = net,
                    goal = dailyGoal,
                    remaining = remaining,
                    progress = progress
                )
            }

            if (entries.isNotEmpty()) {
                item {
                    Text(
                        "Hari ini",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(entries, key = { it.id }) { entry ->
                    FoodEntryItem(entry = entry, onDelete = { vm.deleteEntry(entry.id) })
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada data hari ini.\nTap + untuk tambah kalori.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddSheet) {
        AddCalorieSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { calories, note, type ->
                vm.addEntry(calories, note, type)
                showAddSheet = false
            }
        )
    }

    if (showGoalDialog) {
        GoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { goal ->
                vm.setDailyGoal(goal)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun CalorieSummaryCard(
    consumed: Int, burned: Int, net: Int, goal: Int, remaining: Int, progress: Float
) {
    val overGoal = remaining < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Netto hari ini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "$net kkal",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Text("/ $goal kkal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (!overGoal) "Sisa $remaining kkal" else "Melebihi ${-remaining} kkal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Masuk $consumed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Terbakar $burned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun FoodEntryItem(entry: FoodEntry, onDelete: () -> Unit) {
    val isBurned = entry.type == EntryType.BURNED
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${if (isBurned) "−" else "+"}${entry.calories} kkal",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isBurned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isBurned) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Olahraga", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(entry.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Hapus entri?") },
            text = {
                Text(
                    if (entry.note.isNotBlank()) "${entry.calories} kkal — ${entry.note}"
                    else "${entry.calories} kkal (${entry.time})"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCalorieSheet(onDismiss: () -> Unit, onConfirm: (Int, String, EntryType) -> Unit) {
    var caloriesText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EntryType.CONSUMED) }
    val calories = caloriesText.toIntOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tambah Kalori", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Toggle Masuk / Terbakar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == EntryType.CONSUMED,
                    onClick = { selectedType = EntryType.CONSUMED },
                    label = { Text("Masuk") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == EntryType.BURNED,
                    onClick = { selectedType = EntryType.BURNED },
                    label = { Text("Terbakar") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it.filter { c -> c.isDigit() } },
                label = { Text("Jumlah kalori (kkal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = caloriesText.isNotEmpty() && calories == null
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(if (selectedType == EntryType.BURNED) "cth: lari 30 menit..." else "cth: nasi goreng...")
                }
            )

            Button(
                onClick = { calories?.let { onConfirm(it, note.trim(), selectedType) } },
                enabled = calories != null && calories > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}

@Composable
private fun GoalDialog(currentGoal: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentGoal.toString()) }
    val value = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Target Kalori Harian") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label = { Text("Target (kkal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onConfirm(it) } },
                enabled = value != null && value > 0
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
