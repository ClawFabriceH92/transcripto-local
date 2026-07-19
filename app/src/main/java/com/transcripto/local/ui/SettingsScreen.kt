package com.transcripto.local.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcripto.local.data.AppLogger
import com.transcripto.local.models.ModelManager
import com.transcripto.local.models.ModelProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val scope = rememberCoroutineScope()

    // Profil fixe : on embarque le profil ULTRA_LIGHT (Whisper Tiny + Qwen2 0.5B)
    val profile = ModelProfiles.ULTRA_LIGHT.profile

    // États d'extraction
    var isExtracting by remember { mutableStateOf(false) }
    var extractProgress by remember { mutableFloatStateOf(0f) }
    var extractStatus by remember { mutableStateOf("") }
    var extractError by remember { mutableStateOf<String?>(null) }

    val modelsReady by remember {
        derivedStateOf { modelManager.areModelsReady(profile) }
    }

    var lockEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Param\u00e8tres",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // === Mod\u00e8les embarqu\u00e9s ===
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mod\u00e8les",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = profile.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "STT: ${profile.sttModel} \u00b7 LLM: ${profile.llmModel}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Statut
                    if (modelsReady) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\u2705 Mod\u00e8les pr\u00eats",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch {
                                    modelManager.unloadModels(profile)
                                }
                            }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (isExtracting) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = extractStatus,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { extractProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${(extractProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    } else if (extractError != null) {
                        Text(
                            text = "\u274c $extractError",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Mod\u00e8les embarqu\u00e9s dans l'APK \u2014 extraction n\u00e9cessaire au premier lancement.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!modelsReady && !isExtracting) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isExtracting = true
                                    extractError = null
                                    extractProgress = 0f
                                    extractStatus = "Pr\u00e9paration..."
                                    AppLogger.i("Extraction des mod\u00e8les...")

                                    val files = listOf(
                                        profile.sttFile to profile.sttModel,
                                        profile.llmFile to profile.llmModel
                                    )

                                    val totalBytes = (profile.sttSize.toLong() + profile.llmSize.toLong()) * 1024L * 1024L
                                    var totalExtracted = 0L

                                    for ((fileName, displayName) in files) {
                                        extractStatus = "$displayName..."
                                        val outputFile = File(modelManager.modelsDir, fileName)

                                        val result = withContext(Dispatchers.IO) {
                                            modelManager.extractModel(
                                                assetPath = fileName,
                                                outputFile = outputFile,
                                                progress = { copied, total ->
                                                    val fileFraction = copied.toFloat() / total.toFloat()
                                                    extractProgress = (totalExtracted + fileFraction * totalBytes) / totalBytes
                                                    extractStatus = "$displayName : ${copied / (1024 * 1024)}/${total / (1024 * 1024)} Mo"
                                                }
                                            )
                                        }

                                        if (result == null) {
                                            extractError = "\u00c9chec d'extraction de $displayName"
                                            isExtracting = false
                                            return@launch
                                        }
                                        totalExtracted += totalBytes / files.size
                                    }

                                    extractProgress = 1f
                                    extractStatus = "Mod\u00e8les pr\u00eats !"
                                    isExtracting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Extraire les mod\u00e8les")
                        }
                    }
                }
            }
        }

        // === S\u00e9curit\u00e9 ===
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "S\u00e9curit\u00e9",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Verrouiller l'application", modifier = Modifier.weight(1f))
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = { lockEnabled = it }
                        )
                    }

                    if (lockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Utiliser la biom\u00e9trie", modifier = Modifier.weight(1f))
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { biometricEnabled = it }
                            )
                        }
                    }
                }
            }
        }

        // === Stockage ===
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Stockage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Mod\u00e8les, enregistrements et transcriptions.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* tout effacer */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Tout effacer")
                    }
                }
            }
        }

        // === Diagnostic ===
        item {
            var showLogs by remember { mutableStateOf(false) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diagnostic",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showLogs = !showLogs }) {
                            Text(if (showLogs) "Masquer" else "Voir les logs")
                        }
                    }

                    if (showLogs) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        val logText = remember { AppLogger.getText() }
                        val context = LocalContext.current

                        Text(
                            text = logText.ifBlank { "Aucun log pour l'instant." },
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("logs", AppLogger.getText())
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Logs copi\u00e9s", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copier les logs", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Transcripto Local v0.1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "100% local \u2014 aucune donn\u00e9e ne quitte l'appareil",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
