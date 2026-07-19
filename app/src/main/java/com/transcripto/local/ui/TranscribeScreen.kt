package com.transcripto.local.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TranscriptionItem(
    val id: Long,
    val date: String,
    val duration: String,
    val summary: String = "",
    val fullText: String = "",
    val isTranscribed: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var recordings by remember {
        mutableStateOf(
            listOf(
                TranscriptionItem(1, "19 juil. 2026", "32 min", isTranscribed = false),
            )
        )
    }

    var transcribingId by remember { mutableStateOf<Long?>(null) }
    var transcribeProgress by remember { mutableFloatStateOf(0f) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

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
                            // Ligne principale
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

                                // Barre de progression si en cours de transcription
                                if (transcribingId == item.id) {
                                    Column(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .padding(end = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { transcribeProgress },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${(transcribeProgress * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (!item.isTranscribed) {
                                    FilledTonalButton(
                                        onClick = {
                                            transcribingId = item.id
                                            transcribeProgress = 0f
                                            scope.launch {
                                                // Simulation de transcription progressive
                                                for (i in 1..100) {
                                                    delay(60)
                                                    transcribeProgress = i / 100f
                                                }
                                                recordings = recordings.map { r ->
                                                    if (r.id == item.id) r.copy(
                                                        isTranscribed = true,
                                                        fullText = "Ceci est le texte transcrit de l'enregistrement du ${item.date}. Il s'agit d'une transcription complète avec tout le contenu de l'audio. Les prochains paragraphes détaillent les différents sujets abordés pendant la réunion ou l'entretien. Le texte complet est disponible ci-dessous en cliquant sur la carte."
                                                    )
                                                    else r
                                                }
                                                transcribingId = null
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        enabled = transcribingId == null
                                    ) {
                                        Text("Transcrire", fontSize = 13.sp)
                                    }
                                } else {
                                    // Icône pour indiquer qu'on peut développer
                                    Icon(
                                        imageVector = if (expandedId == item.id) Icons.Default.ExpandLess
                                                       else Icons.Default.ExpandMore,
                                        contentDescription = if (expandedId == item.id) "Réduire"
                                                             else "Voir le texte",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }

                                IconButton(onClick = { /* supprimer */ }) {
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
                                    Text(
                                        text = item.fullText.ifBlank { "Texte transcrit indisponible." },
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedId = if (expandedId == item.id) null else item.id
                                            }
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
