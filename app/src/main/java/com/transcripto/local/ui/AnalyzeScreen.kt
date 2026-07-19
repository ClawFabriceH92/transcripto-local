package com.transcripto.local.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcripto.local.data.LocalAppState
import com.transcripto.local.data.Recording

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("R\u00e9sum\u00e9", "Points cl\u00e9s", "Actions", "Question")

    val recordings = appState.recordings.filter { it.isTranscribed }
    val selectedRecording = appState.getSelectedRecording()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Analyse",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Sélection de l'enregistrement
        if (recordings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Aucune transcription disponible. Enregistrez et transcrivez d'abord un audio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        // Dropdown pour choisir l'enregistrement
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedRecording?.let { "${it.date} (${it.duration})" } ?: "S\u00e9lectionner un enregistrement",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                recordings.forEach { rec ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("${rec.date} (${rec.duration})", fontSize = 14.sp)
                                Text(
                                    rec.fullText.take(60) + "...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        onClick = {
                            appState.selectedRecordingId = rec.id
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedRecording == null) return

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            when (selectedTab) {
                0 -> AnalysisCard(title = "R\u00e9sum\u00e9", text = selectedRecording.summary.ifBlank { "Le r\u00e9sum\u00e9 sera g\u00e9n\u00e9r\u00e9 par le LLM." })
                1 -> AnalysisCard(title = "Points cl\u00e9s", text = selectedRecording.keyPoints.ifBlank { "Les points cl\u00e9s seront extraits par le LLM." })
                2 -> AnalysisCard(title = "Actions", text = selectedRecording.actions.ifBlank { "Les actions seront identifi\u00e9es par le LLM." })
                3 -> QaSection(fullText = selectedRecording.fullText)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = { /* exporter */ }) {
                Text("Exporter", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AnalysisCard(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun QaSection(fullText: String) {
    var question by remember { mutableStateOf("") }

    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Poser une question",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                if (fullText.isBlank()) {
                    Text(
                        text = "Aucune transcription disponible.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("Posez une question sur le contenu\u2026", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { /* envoyer la question au LLM */ },
                        enabled = question.isNotBlank()
                    ) {
                        Text("Questionner", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
