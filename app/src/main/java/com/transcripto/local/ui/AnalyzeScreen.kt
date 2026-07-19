package com.transcripto.local.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcripto.local.data.AppLogger
import com.transcripto.local.data.LocalAppState
import com.transcripto.local.llm.LlamaLlmEngine
import com.transcripto.local.llm.LlmQuery
import com.transcripto.local.llm.LlmResult
import com.transcripto.local.llm.PromptType
import com.transcripto.local.models.ModelManager
import com.transcripto.local.models.ModelProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    val modelManager = remember { ModelManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("R\u00e9sum\u00e9", "Points cl\u00e9s", "Actions", "Question")
    var analyzing by remember { mutableStateOf(false) }

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

        var menuExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedRecording?.let { "${it.date} (${it.duration})" }
                    ?: "S\u00e9lectionner un enregistrement",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true,
            )

            ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
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
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val recording = selectedRecording ?: return

        // Bouton g\u00e9n\u00e9rer l'analyse avec le vrai LLM
        if (recording.summary.isBlank() && !analyzing) {
            Button(
                onClick = {
                    analyzing = true
                    scope.launch {
                        AppLogger.i("Analyse LLM lanc\u00e9e #${recording.id}")

                        val profile = ModelProfiles.ULTRA_LIGHT.profile
                        val llmModel = modelManager.getLlmModelFile(profile)

                        if (!llmModel.exists()) {
                            AppLogger.e("Mod\u00e8le LLM introuvable : ${llmModel.absolutePath}")
                            analyzing = false
                            return@launch
                        }

                        val llm = LlamaLlmEngine()

                        // Charger le mod\u00e8le
                        val loadResult = withContext(Dispatchers.IO) {
                            llm.loadModel(llmModel.absolutePath)
                        }
                        if (loadResult.isFailure) {
                            AppLogger.e("LLM: \u00e9chec chargement mod\u00e8le")
                            analyzing = false
                            return@launch
                        }

                        AppLogger.i("LLM: mod\u00e8le charg\u00e9, lancement des analyses")

                        // R\u00e9sum\u00e9
                        val summaryResult = withContext(Dispatchers.IO) {
                            llm.analyze(LlmQuery(recording.fullText, PromptType.SUMMARY))
                        }
                        val summary = when (summaryResult) {
                            is LlmResult.Success -> summaryResult.text
                            else -> "Erreur: ${(summaryResult as? LlmResult.Error)?.message}"
                        }

                        // Points cl\u00e9s
                        val kpResult = withContext(Dispatchers.IO) {
                            llm.analyze(LlmQuery(recording.fullText, PromptType.KEY_POINTS))
                        }
                        val keyPoints = when (kpResult) {
                            is LlmResult.Success -> kpResult.text
                            else -> "Erreur: ${(kpResult as? LlmResult.Error)?.message}"
                        }

                        // Actions
                        val actionsResult = withContext(Dispatchers.IO) {
                            llm.analyze(LlmQuery(recording.fullText, PromptType.ACTIONS))
                        }
                        val actions = when (actionsResult) {
                            is LlmResult.Success -> actionsResult.text
                            else -> "Erreur: ${(actionsResult as? LlmResult.Error)?.message}"
                        }

                        appState.setAnalysis(recording.id, summary, keyPoints, actions)

                        // Lib\u00e9rer la m\u00e9moire
                        withContext(Dispatchers.IO) { llm.unloadModel() }

                        AppLogger.i("Analyse LLM termin\u00e9e #${recording.id}")
                        analyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("G\u00e9n\u00e9rer l'analyse", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (analyzing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Analyse en cours...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

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
                0 -> AnalysisCard(title = "R\u00e9sum\u00e9", text = recording.summary.ifBlank { "Cliquez sur 'G\u00e9n\u00e9rer l'analyse'." })
                1 -> AnalysisCard(title = "Points cl\u00e9s", text = recording.keyPoints.ifBlank { "Cliquez sur 'G\u00e9n\u00e9rer l'analyse'." })
                2 -> AnalysisCard(title = "Actions", text = recording.actions.ifBlank { "Cliquez sur 'G\u00e9n\u00e9rer l'analyse'." })
                3 -> QaSection(fullText = recording.fullText, llmEngine = LlamaLlmEngine(), llmModel = modelManager.getLlmModelFile(ModelProfiles.ULTRA_LIGHT.profile))
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
private fun QaSection(fullText: String, llmEngine: com.transcripto.local.llm.LlamaLlmEngine, llmModel: java.io.File) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var answering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                        onClick = {
                            answering = true
                            answer = null
                            scope.launch {
                                val loadResult = withContext(Dispatchers.IO) { llmEngine.loadModel(llmModel.absolutePath) }
                                if (loadResult.isSuccess) {
                                    val result = withContext(Dispatchers.IO) {
                                        llmEngine.analyze(LlmQuery(fullText, PromptType.FREE_QUERY, freeQuery = question))
                                    }
                                    answer = when (result) {
                                        is com.transcripto.local.llm.LlmResult.Success -> result.text
                                        else -> "Erreur: ${(result as? com.transcripto.local.llm.LlmResult.Error)?.message}"
                                    }
                                    withContext(Dispatchers.IO) { llmEngine.unloadModel() }
                                } else {
                                    answer = "Erreur de chargement du mod\u00e8le"
                                }
                                answering = false
                            }
                        },
                        enabled = question.isNotBlank() && !answering
                    ) {
                        Text("Questionner", fontSize = 13.sp)
                    }

                    if (answering) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (answer != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = answer!!,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
