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
import com.transcripto.local.data.LocalAppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }

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

                                // Barre progression ou bouton Transcrire ou ic\u00f4ne OK
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
                                                // Simulation tant que Whisper n'est pas branch\u00e9
                                                for (i in 1..100) {
                                                    delay(50)
                                                    appState.transcribeProgress = i / 100f
                                                }
                                                appState.setTranscription(
                                                    id = item.id,
                                                    text = "Transcription de l'enregistrement du ${item.date} \u00e0 ${item.time} (dur\u00e9e ${item.duration}).\n\nBonjour, ceci est une transcription de test. Le contenu r\u00e9el proviendra du moteur Whisper une fois int\u00e9gr\u00e9. Ce texte simule le r\u00e9sultat d'une reconnaissance vocale.\n\nDeuxi\u00e8me paragraphe avec plus de d\u00e9tails sur ce qui a \u00e9t\u00e9 dit pendant l'enregistrement."
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

                            // Texte transcrit d\u00e9pliable
                            AnimatedVisibility(visible = expandedId == item.id) {
                                HorizontalDivider()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    // Date + heure + dur\u00e9e en haut
                                    Text(
                                        text = "${item.date} \u00e0 ${item.time} \u2014 ${item.duration}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Le texte transcrit (s\u00e9lectionnable)
                                    SelectionContainer {
                                        Text(
                                            text = item.fullText.ifBlank {
                                                "Transcription en attente..."
                                            },
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Boutons d'action
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Copier
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("transcription", item.fullText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Texte copi\u00e9", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copier", fontSize = 12.sp)
                                        }

                                        // Exporter
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
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Exporter", fontSize = 12.sp)
                                        }

                                        // Fermer
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
