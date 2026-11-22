package com.fahriza.caloriecounter.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fahriza.caloriecounter.data.WorkoutSession
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
        var lastPhase = session.phaseKey(session.elapsedMs(SystemClock.elapsedRealtime()) / 1000)
        while (session.isRunning) {
            now = SystemClock.elapsedRealtime()
            val elapsed = session.elapsedMs(now)
            val total = session.totalSeconds
            if (total != null && elapsed >= total * 1000L) {
                vibrateDone(context)
                vm.finishSession()
                break
            }
            val phase = session.phaseKey(elapsed / 1000)
            if (phase != lastPhase) {
                lastPhase = phase
                vibrateShort(context)
            }
            delay(200)
        }
    }

    val elapsedMs = session.elapsedMs(now)
    val elapsedSec = elapsedMs / 1000
    val activeSec = session.activeSeconds(elapsedSec)
    val kcal = session.kcal(weightKg, elapsedSec)
    val target = session.targetSeconds
    val iv = session.interval
    val phases = session.phases
    val activeOnly = iv != null || phases != null

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            session.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (session.finished) {
            val minutes = Math.round(activeSec / 60.0).coerceAtLeast(1)
            var kcalText by rememberSaveable { mutableStateOf(kcal.toString()) }
            val kcalValue = kcalText.toIntOrNull()

            Text(
                "Latihan selesai",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (activeOnly) "Waktu aktif" else "Durasi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                formatTime(activeSec),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Perkiraan app: $kcal kkal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (weightEstimated) EstimatedWeightNote(weightKg)
            OutlinedTextField(
                value = kcalText,
                onValueChange = { kcalText = it.filter { c -> c.isDigit() } },
                label = { Text("Kalori yang dicatat (kkal)") },
                supportingText = { Text("Bisa diganti, misalnya dengan angka dari jam tangan.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !added,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )
            Button(
                onClick = {
                    kcalValue?.let {
                        vm.addBurnedToday(it, "Olahraga: ${session.title} $minutes menit")
                        added = true
                    }
                },
                enabled = kcalValue != null && kcalValue > 0 && !added,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(if (added) "Sudah ditambahkan ke catatan" else "Tambah ke catatan")
            }
            TextButton(
                onClick = { vm.clearSession() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tutup") }
        } else {
            if (phases != null) {
                val idx = session.phaseIndexAt(elapsedSec)
                val phase = phases[idx]
                val into = (elapsedSec - session.phaseStartSec(idx)).coerceAtLeast(0)
                val left = (phase.seconds - into).coerceAtLeast(0)
                val next = phases.drop(idx + 1).firstOrNull { it.isWork }
                val totalRounds = phases.last().round

                Text(
                    if (phase.isWork) phase.workoutName else "ISTIRAHAT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (phase.isWork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Ronde ${phase.round} dari $totalRounds",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTime(left),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { (into / phase.seconds.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
                Text(
                    if (next != null) "Berikutnya: ${next.workoutName}" else "Ini gerakan terakhir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (iv != null) {
                val t = elapsedSec.coerceAtMost(iv.totalSec.toLong())
                val within = t % iv.cycleSec
                val isWork = within < iv.workSec
                val round = (t / iv.cycleSec).toInt() + 1
                val phaseLen = if (isWork) iv.workSec else iv.restSec
                val phaseLeft = if (isWork) iv.workSec - within else iv.cycleSec - within

                Text(
                    if (isWork) "KERJA" else "ISTIRAHAT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Ronde $round dari ${iv.rounds}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTime(phaseLeft),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { ((phaseLen - phaseLeft) / phaseLen.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
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
    playTone(ToneGenerator.TONE_PROP_ACK, 700)
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (vibrator.hasVibrator()) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }
}

private fun vibrateShort(context: Context) {
    playTone(ToneGenerator.TONE_PROP_BEEP, 250)
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (vibrator.hasVibrator()) {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Bunyi beep lewat volume media (tetap terdengar walau ponsel mode getar). */
private fun playTone(tone: Int, durationMs: Int) {
    try {
        val generator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        generator.startTone(tone, durationMs)
        Handler(Looper.getMainLooper()).postDelayed({ generator.release() }, durationMs + 200L)
    } catch (_: RuntimeException) {
        // Perangkat tanpa audio / gagal inisialisasi: cukup lewati, getar tetap jalan.
    }
}
