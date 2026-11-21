package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: CalorieViewModel) {
    val entries by vm.entries.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val dailyGoal by vm.dailyGoal.collectAsState()

    val consumed = entries.filter { it.type == EntryType.CONSUMED }.sumOf { it.calories }
    val burned = entries.filter { it.type == EntryType.BURNED }.sumOf { it.calories }
    val net = consumed - burned
    val progress = (net.toFloat() / dailyGoal).coerceIn(0f, 1f)
    val remaining = dailyGoal - net

    var showAddSheet by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }

    val date = remember(selectedDate) { LocalDate.parse(selectedDate) }
    val isToday = date == LocalDate.now()
    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID")))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.selectDate(date.minusDays(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Hari sebelumnya")
                        }
                        Column(
                            modifier = Modifier.clickable { showDatePicker = true }.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                dateLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!isToday) {
                                Text(
                                    "Ketuk untuk ganti tanggal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { vm.selectDate(date.plusDays(1)) }, enabled = !isToday) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Hari berikutnya")
                        }
                    }
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
                    progress = progress,
                    title = if (isToday) "Netto hari ini" else "Netto"
                )
            }

            if (entries.isNotEmpty()) {
                item {
                    Text(
                        if (isToday) "Hari ini" else "Entri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(entries, key = { it.id }) { entry ->
                    FoodEntryItem(
                        entry = entry,
                        onEdit = { editingEntry = entry },
                        onDelete = { vm.deleteEntry(entry.id) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isToday) "Belum ada data hari ini.\nTap + untuk tambah kalori." else "Belum ada data di tanggal ini.\nTap + untuk tambah kalori.",
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

    editingEntry?.let { entry ->
        AddCalorieSheet(
            initial = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { calories, note, type ->
                vm.updateEntry(entry, calories, note, type)
                editingEntry = null
            }
        )
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            vm.selectDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
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
    consumed: Int, burned: Int, net: Int, goal: Int, remaining: Int, progress: Float, title: String
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
                    Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
private fun FoodEntryItem(entry: FoodEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isBurned = entry.type == EntryType.BURNED
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
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
private fun AddCalorieSheet(
    initial: FoodEntry? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, EntryType) -> Unit
) {
    var caloriesText by remember { mutableStateOf(initial?.calories?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var selectedType by remember { mutableStateOf(initial?.type ?: EntryType.CONSUMED) }
    val calories = caloriesText.toIntOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (initial == null) "Tambah Kalori" else "Edit Entri", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
