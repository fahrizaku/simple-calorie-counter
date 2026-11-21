package com.fahriza.caloriecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel
import com.fahriza.caloriecounter.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudDialog(vm: CalorieViewModel, onDismiss: () -> Unit) {
    val savedUrl by vm.tursoUrl.collectAsState()
    val savedToken by vm.tursoToken.collectAsState()
    val lastBackup by vm.lastBackup.collectAsState()
    val syncState by vm.syncState.collectAsState()

    // Isi awal dari DataStore; setelah itu dikontrol oleh input pengguna.
    var url by remember(savedUrl) { mutableStateOf(savedUrl) }
    var token by remember(savedToken) { mutableStateOf(savedToken) }
    var confirmRestore by remember { mutableStateOf(false) }

    val loading = syncState == SyncState.Loading
    val canSubmit = !loading && url.isNotBlank() && token.isNotBlank()
    val lastBackupText = remember(lastBackup) {
        if (lastBackup == 0L) "Belum pernah"
        else SimpleDateFormat("d MMM yyyy HH:mm", Locale("id", "ID")).format(Date(lastBackup))
    }

    AlertDialog(
        onDismissRequest = { if (!loading) { vm.clearSyncState(); onDismiss() } },
        title = { Text("Cloud Backup (Turso)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL database") },
                    placeholder = { Text("libsql://nama-org.turso.io") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Auth token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Backup terakhir: $lastBackupText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (val s = syncState) {
                    SyncState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Memproses...", style = MaterialTheme.typography.bodySmall)
                    }
                    is SyncState.Success -> Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    is SyncState.Error -> Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    SyncState.Idle -> {}
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.backup(url, token) },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f)
                    ) { Text("Backup") }
                    OutlinedButton(
                        onClick = { confirmRestore = true },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f)
                    ) { Text("Restore") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.clearSyncState(); onDismiss() }, enabled = !loading) { Text("Tutup") }
        }
    )

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore dari cloud?") },
            text = { Text("Semua data di HP akan diganti dengan data dari backup cloud. Data lokal yang belum di-backup akan hilang.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmRestore = false; vm.restore(url, token) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Batal") } }
        )
    }
}
