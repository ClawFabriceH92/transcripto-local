package com.transcripto.local.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TranscriptionItem(
    val id: Long,
    val date: String,
    val duration: String,
    val summary: String = "",
    val isTranscribed: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(modifier: Modifier = Modifier) {
    var recordings by remember {
        mutableStateOf(
            listOf(
                TranscriptionItem(1, "19 juil. 2026", "32 min", isTranscribed = false),
            )
        )
    }

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
                items(recordings) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.date,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Durée: ${item.duration}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!item.isTranscribed) {
                                FilledTonalButton(
                                    onClick = { /* lancer transcription */ },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Transcrire")
                                }
                            }

                            IconButton(onClick = { /* supprimer */ }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }
}
