package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.data.Combo
import com.fahriza.caloriecounter.data.ComboStep
import com.fahriza.caloriecounter.data.comboTotalSeconds
import com.fahriza.caloriecounter.data.estimateComboKcal
import com.fahriza.caloriecounter.data.highImpactWarnings
import com.fahriza.caloriecounter.data.isHighImpact
import com.fahriza.caloriecounter.data.workouts
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel
import java.util.Collections

@Composable
internal fun HighImpactWarningCard() {
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

private fun minutesLabel(totalSec: Int): String = "${Math.round(totalSec / 60.0).coerceAtLeast(1)} menit"

@Composable
internal fun ComboCard(
    combo: Combo,
    weightKg: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val totalSec = comboTotalSeconds(combo)
    val kcal = estimateComboKcal(combo, weightKg)
    val highImpact = combo.isHighImpact()
    var confirmDelete by remember { mutableStateOf(false) }

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
                    Text(combo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (combo.description.isNotBlank()) {
                        Text(
                            combo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${minutesLabel(totalSec)} • ${combo.steps.size} gerakan • ${combo.rounds} ronde",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (highImpact) {
                        Text(
                            "High-impact",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    "≈ $kcal kkal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (expanded) {
                if (highImpact) HighImpactWarningCard()
                Text("Susunan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                combo.steps.forEachIndexed { i, s ->
                    val rest = if (s.restSec > 0) ", istirahat ${s.restSec} dtk" else ""
                    Text("${i + 1}. ${s.workoutName}: kerja ${s.workSec} dtk$rest", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Diulang ${combo.rounds} ronde. Kalori dihitung hanya saat fase kerja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Mulai") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text(if (combo.builtIn) "Salin & ubah" else "Ubah")
                    }
                    if (onDelete != null) {
                        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                            Text("Hapus")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus kombinasi?") },
            text = { Text("\"${combo.name}\" akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
internal fun ComboEditorScreen(vm: CalorieViewModel, draft: Combo, weightKg: Float) {
    var showPicker by remember { mutableStateOf(false) }
    val totalSec = comboTotalSeconds(draft)
    val kcal = estimateComboKcal(draft, weightKg)
    val canSave = draft.name.isNotBlank() && draft.steps.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Susun Kombinasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { vm.updateDraft(draft.copy(name = it)) },
                label = { Text("Nama kombinasi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Stepper("Jumlah ronde", draft.rounds, step = 1, min = 1, max = 20) {
                        vm.updateDraft(draft.copy(rounds = it))
                    }
                }
            }
        }
        itemsIndexed(draft.steps) { i, step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}. ${step.workoutName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val m = draft.steps.toMutableList()
                                Collections.swap(m, i, i - 1)
                                vm.updateDraft(draft.copy(steps = m))
                            },
                            enabled = i > 0
                        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Naikkan") }
                        IconButton(
                            onClick = {
                                val m = draft.steps.toMutableList()
                                Collections.swap(m, i, i + 1)
                                vm.updateDraft(draft.copy(steps = m))
                            },
                            enabled = i < draft.steps.lastIndex
                        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Turunkan") }
                        IconButton(
                            onClick = {
                                vm.updateDraft(draft.copy(steps = draft.steps.filterIndexed { j, _ -> j != i }))
                            }
                        ) { Icon(Icons.Default.Delete, contentDescription = "Hapus gerakan") }
                    }
                    Stepper("Kerja (detik)", step.workSec, step = 5, min = 5, max = 600) { v ->
                        vm.updateDraft(
                            draft.copy(steps = draft.steps.mapIndexed { j, s -> if (j == i) s.copy(workSec = v) else s })
                        )
                    }
                    Stepper("Istirahat (detik)", step.restSec, step = 5, min = 0, max = 300) { v ->
                        vm.updateDraft(
                            draft.copy(steps = draft.steps.mapIndexed { j, s -> if (j == i) s.copy(restSec = v) else s })
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tambah gerakan")
            }
        }
        if (draft.steps.isNotEmpty()) {
            item {
                Text(
                    "Total ${minutesLabel(totalSec)}, perkiraan ≈ $kcal kkal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (draft.isHighImpact()) {
            item { HighImpactWarningCard() }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { vm.discardDraft() }, modifier = Modifier.weight(1f)) { Text("Batal") }
                Button(onClick = { vm.saveDraft() }, enabled = canSave, modifier = Modifier.weight(1f)) {
                    Text("Simpan")
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pilih gerakan") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(items = workouts, key = { it.name }) { w ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.updateDraft(draft.copy(steps = draft.steps + ComboStep(w.name, 45, 15)))
                                    showPicker = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(w.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (w.highImpact) "${w.focus} • High-impact" else w.focus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (w.highImpact) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, step: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) {
            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) {
            Icon(Icons.Default.Add, contentDescription = "Tambah")
        }
    }
}
