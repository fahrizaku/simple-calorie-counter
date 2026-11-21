package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.data.DEFAULT_WEIGHT_KG
import com.fahriza.caloriecounter.data.IntervalConfig
import com.fahriza.caloriecounter.data.Workout
import com.fahriza.caloriecounter.data.estimateKcal
import com.fahriza.caloriecounter.data.comboTemplates
import com.fahriza.caloriecounter.data.workouts
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel

@Composable
fun OtherScreen(vm: CalorieViewModel) {
    val session by vm.session.collectAsState()
    val draft by vm.draft.collectAsState()
    val weight by vm.weightKg.collectAsState()
    val current = session
    val editing = draft

    if (current != null) {
        WorkoutSessionScreen(vm, current, weight ?: DEFAULT_WEIGHT_KG, weight == null)
    } else if (editing != null) {
        ComboEditorScreen(vm, editing, weight ?: DEFAULT_WEIGHT_KG)
    } else {
        WorkoutListScreen(vm, weight)
    }
}

@Composable
private fun WorkoutListScreen(vm: CalorieViewModel, weight: Float?) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var setupFor by remember { mutableStateOf<Workout?>(null) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveWeight = weight ?: DEFAULT_WEIGHT_KG
    val userCombos by vm.userCombos.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Panduan Bakar Kalori di Rumah",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            WeightCard(weight = weight, onEdit = { showWeightDialog = true })
        }
        item {
            Text(
                "Estimasi kalori dihitung dari berat badan dan lama latihan. Hanya perkiraan, bukan angka pasti.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { SectionHeader("Kombinasi Latihan") }
        item {
            Button(onClick = { vm.newDraft() }, modifier = Modifier.fillMaxWidth()) {
                Text("Susun kombinasi sendiri")
            }
        }
        if (userCombos.isNotEmpty()) {
            item { Text("Kombinasi saya", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            items(items = userCombos, key = { it.id }) { c ->
                ComboCard(
                    combo = c,
                    weightKg = effectiveWeight,
                    expanded = expanded == c.id,
                    onToggle = { expanded = if (expanded == c.id) null else c.id },
                    onStart = { vm.startComboSession(c) },
                    onEdit = { vm.editDraft(c) },
                    onDelete = { vm.deleteCombo(c.id) }
                )
            }
        }
        item { Text("Template", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        items(items = comboTemplates, key = { it.id }) { c ->
            ComboCard(
                combo = c,
                weightKg = effectiveWeight,
                expanded = expanded == c.id,
                onToggle = { expanded = if (expanded == c.id) null else c.id },
                onStart = { vm.startComboSession(c) },
                onEdit = { vm.editDraft(c) },
                onDelete = null
            )
        }
        item { SectionHeader("Latihan Satuan") }
        item { Text("Low-impact (aman untuk sendi)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        items(items = workouts.filter { !it.highImpact }, key = { it.name }) { w ->
            WorkoutCard(
                workout = w,
                weightKg = effectiveWeight,
                expanded = expanded == w.name,
                onToggle = { expanded = if (expanded == w.name) null else w.name },
                onStart = { setupFor = w }
            )
        }
        item { Text("High-impact (ada lompatan, hati-hati)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        items(items = workouts.filter { it.highImpact }, key = { it.name }) { w ->
            WorkoutCard(
                workout = w,
                weightKg = effectiveWeight,
                expanded = expanded == w.name,
                onToggle = { expanded = if (expanded == w.name) null else w.name },
                onStart = { setupFor = w }
            )
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            currentWeight = weight,
            onDismiss = { showWeightDialog = false },
            onConfirm = {
                vm.setWeightKg(it)
                showWeightDialog = false
            }
        )
    }

    setupFor?.let { w ->
        SetupDialog(
            workout = w,
            onDismiss = { setupFor = null },
            onStart = { targetSeconds, interval ->
                vm.startSession(w, targetSeconds, interval)
                setupFor = null
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun WeightCard(weight: Float?, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Berat badan", style = MaterialTheme.typography.labelMedium)
                if (weight != null) {
                    Text(
                        "${formatWeight(weight)} kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Belum diisi, memakai perkiraan ${formatWeight(DEFAULT_WEIGHT_KG)} kg",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            TextButton(onClick = onEdit) { Text(if (weight == null) "Isi" else "Ubah") }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    weightKg: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        workout.focus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (workout.highImpact) {
                        Text(
                            "High-impact",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    "≈ ${estimateKcal(workout.met, weightKg, 600)} kkal\n/ 10 menit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (expanded) {
                if (workout.highImpact) HighImpactWarningCard()
                Text("Langkah", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                workout.steps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Tips", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                workout.tips.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Mulai") }
            }
        }
    }
}

@Composable
private fun WeightDialog(currentWeight: Float?, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var text by remember { mutableStateOf(currentWeight?.let { formatWeight(it) } ?: "") }
    val value = text.toFloatOrNull()
    val valid = value != null && value in 20f..300f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Berat Badan") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Berat (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onConfirm(it) } },
                enabled = valid
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private enum class SessionMode { TARGET, FREE, INTERVAL }

@Composable
private fun SetupDialog(workout: Workout, onDismiss: () -> Unit, onStart: (Int?, IntervalConfig?) -> Unit) {
    var mode by remember { mutableStateOf(SessionMode.TARGET) }
    var minutes by remember { mutableIntStateOf(10) }
    var workSec by remember { mutableIntStateOf(60) }
    var restSec by remember { mutableIntStateOf(30) }
    var rounds by remember { mutableIntStateOf(5) }
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(workout.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == SessionMode.TARGET,
                        onClick = { mode = SessionMode.TARGET },
                        label = { Text("Target") }
                    )
                    FilterChip(
                        selected = mode == SessionMode.FREE,
                        onClick = { mode = SessionMode.FREE },
                        label = { Text("Bebas") }
                    )
                    FilterChip(
                        selected = mode == SessionMode.INTERVAL,
                        onClick = { mode = SessionMode.INTERVAL },
                        label = { Text("Interval") }
                    )
                }
                when (mode) {
                    SessionMode.TARGET -> {
                        ChipRow("Durasi (menit)", listOf(5, 10, 20, 30), minutes) { minutes = it }
                        Text(
                            "Hitung mundur. Bisa dihentikan kapan saja, kalori dihitung dari waktu yang benar-benar dijalani.",
                            style = MaterialTheme.typography.bodySmall,
                            color = hintColor
                        )
                    }
                    SessionMode.FREE -> Text(
                        "Stopwatch tanpa batas waktu. Tekan Selesai kapan saja kalau sudah capek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = hintColor
                    )
                    SessionMode.INTERVAL -> {
                        ChipRow("Kerja (detik)", listOf(30, 45, 60, 90), workSec) { workSec = it }
                        ChipRow("Istirahat (detik)", listOf(15, 30, 45, 60), restSec) { restSec = it }
                        ChipRow("Jumlah ronde", listOf(3, 5, 8, 10), rounds) { rounds = it }
                        Text(
                            "Pindah fase otomatis dengan getar singkat. Waktu istirahat tidak dihitung sebagai kalori.",
                            style = MaterialTheme.typography.bodySmall,
                            color = hintColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (mode) {
                        SessionMode.TARGET -> onStart(minutes * 60, null)
                        SessionMode.FREE -> onStart(null, null)
                        SessionMode.INTERVAL -> onStart(null, IntervalConfig(workSec, restSec, rounds))
                    }
                }
            ) { Text("Mulai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ChipRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text("$value") }
                )
            }
        }
    }
}

private fun formatWeight(w: Float): String =
    if (w % 1f == 0f) w.toInt().toString() else String.format(java.util.Locale.US, "%.1f", w)
