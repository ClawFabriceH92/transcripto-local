package com.transcripto.local.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Paramètres WAV : 16 kHz, 16-bit, mono
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

@Composable
fun RecordScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var soundLevel by remember { mutableFloatStateOf(0f) }

    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var recordingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

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
                    // Démarrer enregistrement PCM WAV
                    try {
                        val dir = File(context.filesDir, "recordings")
                        dir.mkdirs()
                        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRENCH).format(Date())
                        val file = File(dir, "rec_$ts.wav")
                        outputFile = file

                        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                        val record = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE,
                            CHANNEL_CONFIG,
                            AUDIO_FORMAT,
                            bufferSize * 2
                        )

                        if (record.state != AudioRecord.STATE_INITIALIZED) {
                            AppLogger.e("AudioRecord non initialisé")
                            return@Button
                        }

                        record.startRecording()
                        audioRecord = record
                        isRecording = true
                        AppLogger.i("Enregistrement WAV démarré: ${file.absolutePath}")

                        // Thread d'écriture
                        recordingJob = scope.launch(Dispatchers.IO) {
                            val buffer = ShortArray(bufferSize / 2)
                            val tempFile = File(context.cacheDir, "rec_temp_$ts.pcm")
                            val fos = FileOutputStream(tempFile)

                            while (isActive && isRecording) {
                                val read = record.read(buffer, 0, buffer.size)
                                if (read > 0) {
                                    val byteBuffer = ByteArray(read * 2)
                                    var idx = 0
                                    for (s in buffer) {
                                        byteBuffer[idx++] = (s.toInt() and 0xFF).toByte()
                                        byteBuffer[idx++] = ((s.toInt() shr 8) and 0xFF).toByte()
                                    }
                                    fos.write(byteBuffer, 0, read * 2)
                                    // Niveau sonore approximatif
                                    val max = buffer.take(read).maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                                    soundLevel = max.toFloat() / Short.MAX_VALUE.toFloat()
                                }
                            }

                            fos.close()

                            // Créer le WAV avec en-tête
                            if (tempFile.exists() && tempFile.length() > 44) {
                                val dataSize = tempFile.length().toInt()
                                val wavFos = FileOutputStream(file)
                                writeWavHeader(wavFos, dataSize, SAMPLE_RATE, 16, 1)
                                tempFile.inputStream().use { input ->
                                    input.copyTo(wavFos)
                                }
                                wavFos.close()
                                tempFile.delete()
                                AppLogger.i("Fichier WAV créé: ${file.absolutePath} (${file.length()} bytes)")
                            } else {
                                AppLogger.e("Enregistrement trop court: ${tempFile.length()} bytes")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Erreur démarrage enregistrement: ${e.message}")
                    }
                } else {
                    // Arrêter
                    try {
                        isRecording = false
                        recordingJob?.cancel()
                        kotlinx.coroutines.runBlocking { recordingJob?.join() }
                        audioRecord?.apply {
                            stop()
                            release()
                        }
                        audioRecord = null

                        if (outputFile?.exists() == true && outputFile?.length() ?: 0L > 1000) {
                            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
                            val now = Date()
                            appState.addRecording(
                                date = dateFormat.format(now),
                                time = timeFormat.format(now),
                                duration = formatDuration(elapsedSeconds),
                                audioPath = outputFile?.absolutePath ?: "",
                            )
                            AppLogger.i("Enregistrement terminé: ${outputFile?.absolutePath}")
                            appState.onNavigateToScreen(1)
                        } else {
                            AppLogger.w("Enregistrement annulé (fichier trop petit ou inexistant)")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Erreur arrêt enregistrement: ${e.message}")
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

private fun writeWavHeader(out: java.io.OutputStream, dataSize: Int, sampleRate: Int, bitsPerSample: Int, channels: Int) {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val totalSize = 36 + dataSize

    out.write("RIFF".toByteArray())
    out.write(intToLES(totalSize))
    out.write("WAVE".toByteArray())
    out.write("fmt ".toByteArray())
    out.write(intToLES(16)) // chunk size
    out.write(shortToLES(1)) // PCM
    out.write(shortToLES(channels))
    out.write(intToLES(sampleRate))
    out.write(intToLES(byteRate))
    out.write(shortToLES(blockAlign))
    out.write(shortToLES(bitsPerSample))
    out.write("data".toByteArray())
    out.write(intToLES(dataSize))
}

private fun intToLES(value: Int): ByteArray = byteArrayOf(
    (value and 0xFF).toByte(),
    ((value shr 8) and 0xFF).toByte(),
    ((value shr 16) and 0xFF).toByte(),
    ((value shr 24) and 0xFF).toByte()
)

private fun shortToLES(value: Int): ByteArray = byteArrayOf(
    (value and 0xFF).toByte(),
    ((value shr 8) and 0xFF).toByte()
)

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
