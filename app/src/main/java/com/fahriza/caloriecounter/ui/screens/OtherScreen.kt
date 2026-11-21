package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.fahriza.caloriecounter.data.Workout
import com.fahriza.caloriecounter.data.estimateKcal
import com.fahriza.caloriecounter.data.highImpactWarnings
import com.fahriza.caloriecounter.data.workouts
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel

@Composable
fun OtherScreen(vm: CalorieViewModel) {
    val session by vm.session.collectAsState()
    val weight by vm.weightKg.collectAsState()
    val current = session

    if (current != null) {
        WorkoutSessionScreen(vm, current, weight ?: DEFAULT_WEIGHT_KG, weight == null)
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
        item { SectionHeader("Low-impact (aman untuk sendi)") }
        items(items = workouts.filter { !it.highImpact }, key = { it.name }) { w ->
            WorkoutCard(
                workout = w,
                weightKg = effectiveWeight,
                expanded = expanded == w.name,
                onToggle = { expanded = if (expanded == w.name) null else w.name },
                onStart = { setupFor = w }
            )
        }
        item { SectionHeader("High-impact (ada lompatan, hati-hati)") }
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
            onStart = { targetSeconds ->
                vm.startSession(w, targetSeconds)
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
                if (workout.highImpact) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Peringatan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            highImpactWarnings.forEach {
                                Text(
                                    "• $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
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

@Composable
private fun SetupDialog(workout: Workout, onDismiss: () -> Unit, onStart: (Int?) -> Unit) {
    var targetMode by remember { mutableStateOf(true) }
    var minutes by remember { mutableIntStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(workout.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetMode,
                        onClick = { targetMode = true },
                        label = { Text("Target waktu") }
                    )
                    FilterChip(
                        selected = !targetMode,
                        onClick = { targetMode = false },
                        label = { Text("Bebas") }
                    )
                }
                if (targetMode) {
                    Text("Durasi (menit)", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 20, 30).forEach { m ->
                            FilterChip(
                                selected = minutes == m,
                                onClick = { minutes = m },
                                label = { Text("$m") }
                            )
                        }
                    }
                    Text(
                        "Bisa dihentikan kapan saja. Kalori dihitung dari waktu yang benar-benar dijalani.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Stopwatch tanpa batas waktu. Tekan Selesai kapan saja kalau sudah capek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(if (targetMode) minutes * 60 else null) }) { Text("Mulai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun formatWeight(w: Float): String =
    if (w % 1f == 0f) w.toInt().toString() else String.format(java.util.Locale.US, "%.1f", w)
