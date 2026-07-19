package com.transcripto.local.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.transcripto.local.data.AppLogger
import com.transcripto.local.data.LocalAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var soundLevel by remember { mutableFloatStateOf(0f) }

    // Référence à l'enregistrement en cours
    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    // Timer
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val permissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enregistrement",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!permissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Permission micro requise",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = formatDuration(elapsedSeconds),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRecording) {
            Canvas(modifier = Modifier.height(80.dp).fillMaxWidth()) {
                val barCount = 40
                val barWidth = size.width / barCount
                for (i in 0 until barCount) {
                    val height = (soundLevel * (0.2f + 0.8f * i.toFloat() / barCount)) * size.height
                    drawRect(
                        color = if (i < barCount * 0.6) Color(0xFF00C853) else Color(0xFFD32F2F),
                        topLeft = androidx.compose.ui.geometry.Offset(i * barWidth, size.height - height),
                        size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, height)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (!isRecording) {
                    // Démarrer l'enregistrement
                    try {
                        val dir = File(context.filesDir, "recordings")
                        dir.mkdirs()
                        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRENCH).format(Date())
                        val file = File(dir, "rec_$ts.wav")
                        outputFile = file

                        val recorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }
                        audioRecorder = recorder
                        isRecording = true
                        AppLogger.i("Enregistrement démarré : ${file.absolutePath}")
                    } catch (e: Exception) {
                        AppLogger.e("Erreur démarrage enregistrement : ${e.message}")
                    }
                } else {
                    // Arrêter l'enregistrement
                    try {
                        audioRecorder?.apply {
                            stop()
                            release()
                        }
                        audioRecorder = null
                        isRecording = false

                        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
                        val now = Date()
                        appState.addRecording(
                            date = dateFormat.format(now),
                            time = timeFormat.format(now),
                            duration = formatDuration(elapsedSeconds),
                            audioPath = outputFile?.absolutePath ?: "",
                        )
                        AppLogger.i("Enregistrement termin\u00e9 : ${outputFile?.absolutePath}")
                        appState.onNavigateToScreen(1)  // bascule vers Transcriptions
                    } catch (e: Exception) {
                        AppLogger.e("Erreur arr\u00eat enregistrement : ${e.message}")
                    }
                    isPaused = false
                    elapsedSeconds = 0
                }
            },
            modifier = Modifier.size(96.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isRecording) "\u25A0" else "\u25CF",
                fontSize = 36.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecording) {
            OutlinedButton(
                onClick = { isPaused = !isPaused }
            ) {
                Text(if (isPaused) "Reprendre" else "Pause")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Aucune donn\u00e9e ne quitte l'appareil",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
