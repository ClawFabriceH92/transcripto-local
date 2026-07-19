package com.transcripto.local.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcripto.local.data.AppLogger
import com.transcripto.local.data.LocalAppState
import com.transcripto.local.models.ModelManager
import com.transcripto.local.models.ModelProfiles
import com.transcripto.local.stt.SttResult
import com.transcripto.local.stt.WhisperSttEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val modelManager = remember { ModelManager(context) }

    val recordings = appState.recordings.toList()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Transcriptions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun enregistrement",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (item.isTranscribed) {
                                            expandedId = if (expandedId == item.id) null else item.id
                                        }
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.date} \u00e0 ${item.time}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Dur\u00e9e: ${item.duration}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (appState.transcribingId == item.id) {
                                    Column(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .padding(end = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { appState.transcribeProgress },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${(appState.transcribeProgress * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (!item.isTranscribed) {
                                    FilledTonalButton(
                                        onClick = {
                                            AppLogger.i("Transcription lanc\u00e9e #${item.id}")

                                            val audioPath = item.audioPath
                                            if (audioPath.isBlank()) {
                                                AppLogger.w("Pas de fichier audio pour #${item.id}")
                                                return@FilledTonalButton
                                            }

                                            val audioFile = File(audioPath)
                                            if (!audioFile.exists()) {
                                                AppLogger.e("Fichier audio introuvable : $audioPath")
                                                return@FilledTonalButton
                                            }

                                            appState.transcribingId = item.id
                                            appState.transcribeProgress = 0f

                                            scope.launch {
                                                // 1. Charger Whisper
                                                val profile = ModelProfiles.ULTRA_LIGHT.profile
                                                val sttModel = modelManager.getSttModelFile(profile)
                                                val whisper = WhisperSttEngine()

                                                if (!whisper.isLoaded) {
                                                    val loadResult = withContext(Dispatchers.IO) {
                                                        whisper.loadModel(sttModel.absolutePath)
                                                    }
                                                    if (loadResult.isFailure) {
                                                        AppLogger.e("Whisper: \u00e9chec chargement")
                                                        appState.transcribingId = null
                                                        return@launch
                                                    }
                                                }

                                                // 2. Lancer la transcription
                                                val result = withContext(Dispatchers.IO) {
                                                    whisper.transcribe(audioFile, "fr")
                                                }

                                                when (result) {
                                                    is SttResult.Success -> {
                                                        AppLogger.i("Transcription r\u00e9ussie : ${result.transcription.fullText.take(100)}...")
                                                        appState.setTranscription(
                                                            id = item.id,
                                                            text = result.transcription.fullText
                                                        )
                                                    }
                                                    is SttResult.Error -> {
                                                        AppLogger.e("Transcription \u00e9chou\u00e9e : ${result.message}")
                                                        appState.setTranscription(
                                                            id = item.id,
                                                            text = "Erreur de transcription : ${result.message}"
                                                        )
                                                    }
                                                }

                                                // 3. Lib\u00e9rer la m\u00e9moire
                                                withContext(Dispatchers.IO) {
                                                    whisper.unloadModel()
                                                }
                                                appState.transcribingId = null
                                                appState.transcribeProgress = 1f
                                                AppLogger.i("Transcription termin\u00e9e #${item.id}")
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        enabled = appState.transcribingId == null
                                    ) {
                                        Text("Transcrire", fontSize = 13.sp)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Transcrit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (expandedId == item.id) Icons.Default.ExpandLess
                                                           else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    appState.recordings.removeAll { it.id == item.id }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            AnimatedVisibility(visible = expandedId == item.id) {
                                HorizontalDivider()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "${item.date} \u00e0 ${item.time} \u2014 ${item.duration}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    SelectionContainer {
                                        Text(
                                            text = item.fullText.ifBlank { "Transcription en attente..." },
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("transcription", item.fullText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Texte copi\u00e9", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copier", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Transcription ${item.date} ${item.time}")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, item.fullText)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Exporter la transcription"))
                                            },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Exporter", fontSize = 12.sp)
                                        }

                                        TextButton(
                                            onClick = { expandedId = null },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Fermer", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
