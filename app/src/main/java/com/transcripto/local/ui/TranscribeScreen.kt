package com.transcripto.local.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcripto.local.data.LocalAppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }

    // Copie locale pour la réactivité
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
                                        text = item.date,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Dur\u00e9e: ${item.duration}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Progression
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
                                            appState.transcribingId = item.id
                                            appState.transcribeProgress = 0f
                                            scope.launch {
                                                // Simulation : remplacer par appel STT réel
                                                for (i in 1..100) {
                                                    delay(50)
                                                    appState.transcribeProgress = i / 100f
                                                }
                                                appState.setTranscription(
                                                    id = item.id,
                                                    text = "Transcription de l'enregistrement du ${item.date} (durée ${item.duration}). Ce texte sera remplacé par la sortie de Whisper."
                                                )
                                                appState.transcribingId = null
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        enabled = appState.transcribingId == null
                                    ) {
                                        Text("Transcrire", fontSize = 13.sp)
                                    }
                                } else {
                                    // Transcrit : icône statut
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

                            // Texte transcrit (dépliable)
                            AnimatedVisibility(visible = expandedId == item.id) {
                                HorizontalDivider()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    val text = item.fullText.ifBlank {
                                        "Transcription en attente..."
                                    }
                                    Text(
                                        text = text,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { expandedId = null }) {
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
