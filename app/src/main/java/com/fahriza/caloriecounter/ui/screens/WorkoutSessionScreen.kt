package com.fahriza.caloriecounter.ui.screens

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.data.WorkoutSession
import com.fahriza.caloriecounter.data.estimateKcal
import com.fahriza.caloriecounter.viewmodel.CalorieViewModel
import kotlinx.coroutines.delay

@Composable
fun WorkoutSessionScreen(
    vm: CalorieViewModel,
    session: WorkoutSession,
    weightKg: Float,
    weightEstimated: Boolean
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var added by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(session.isRunning) {
        val window = (context as? Activity)?.window
        if (session.isRunning) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(session.runningSince) {
        while (session.isRunning) {
            now = SystemClock.elapsedRealtime()
            val target = session.targetSeconds
            if (target != null && session.elapsedMs(now) >= target * 1000L) {
                vibrateDone(context)
                vm.finishSession()
                break
            }
            delay(200)
        }
    }

    val elapsedMs = session.elapsedMs(now)
    val elapsedSec = elapsedMs / 1000
    val kcal = estimateKcal(session.workout.met, weightKg, elapsedSec)
    val target = session.targetSeconds

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            session.workout.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (session.finished) {
            val minutes = Math.round(elapsedSec / 60.0).coerceAtLeast(1)
            Text(
                "Latihan selesai",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                formatTime(elapsedSec),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "≈ $kcal kkal terbakar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (weightEstimated) EstimatedWeightNote(weightKg)
            Button(
                onClick = {
                    vm.addBurnedToday(kcal, "Olahraga: ${session.workout.name} $minutes menit")
                    added = true
                },
                enabled = kcal > 0 && !added,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            ) {
                Text(if (added) "Sudah ditambahkan ke catatan" else "Tambah ke catatan")
            }
            TextButton(
                onClick = { vm.clearSession() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tutup") }
        } else {
            val displaySec = if (target != null) (target - elapsedSec).coerceAtLeast(0) else elapsedSec
            Text(
                if (target != null) "Sisa waktu" else "Waktu berjalan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                formatTime(displaySec),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            if (target != null) {
                LinearProgressIndicator(
                    progress = { (elapsedMs / (target * 1000f)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
            Text(
                "≈ $kcal kkal",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (!session.isRunning) {
                Text(
                    "Dijeda",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (weightEstimated) EstimatedWeightNote(weightKg)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (session.isRunning) vm.pauseSession() else vm.resumeSession() },
                    modifier = Modifier.weight(1f)
                ) { Text(if (session.isRunning) "Jeda" else "Lanjut") }
                Button(
                    onClick = { vm.finishSession() },
                    modifier = Modifier.weight(1f)
                ) { Text("Selesai") }
            }
        }
    }
}

@Composable
private fun EstimatedWeightNote(weightKg: Float) {
    Text(
        "Memakai berat badan perkiraan ${weightKg.toInt()} kg. Isi berat badan di daftar latihan agar lebih akurat.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp)
    )
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun vibrateDone(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (vibrator.hasVibrator()) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }
}
