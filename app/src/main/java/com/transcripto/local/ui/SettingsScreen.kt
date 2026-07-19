package com.transcripto.local.ui

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
import com.transcripto.local.models.ModelManager
import com.transcripto.local.models.ModelProfile
import com.transcripto.local.models.ModelProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelProfileInfo(
    val name: String,
    val sttModel: String,
    val llmModel: String,
    val isActive: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val scope = rememberCoroutineScope()

    // Profil sélectionné (par défaut : détection automatique)
    var selectedProfile by remember { mutableStateOf<ModelProfile?>(null) }
    var detectedProfile by remember { mutableStateOf(modelManager.detectProfile()) }

    // États de téléchargement
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }

    // Modèles prêts ?
    val currentProfile = selectedProfile ?: detectedProfile
    val modelsReady by remember {
        derivedStateOf { modelManager.areModelsReady(currentProfile) }
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

        // === S\u00e9lection du profil mat\u00e9riel ===
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profil mat\u00e9riel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "D\u00e9tect\u00e9 : ${detectedProfile.label}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ModelProfiles.entries.forEach { entry ->
                        val profile = entry.profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile == currentProfile,
                                onClick = {
                                    selectedProfile = profile
                                    downloadError = null
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.label,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "STT: ${profile.sttModel} \u00b7 LLM: ${profile.llmModel}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Badge taille
                            val totalMb = profile.sttSize + profile.llmSize
                            Text(
                                text = "${totalMb} Mo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Statut des mod\u00e8les
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
                                    modelManager.unloadModels(currentProfile)
                                }
                            }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (isDownloading) {
                        // Barre de progression
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = downloadStatus,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    } else if (downloadError != null) {
                        Text(
                            text = "\u274c $downloadError",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Mod\u00e8les non t\u00e9l\u00e9charg\u00e9s (${currentProfile.sttModel} + ${currentProfile.llmModel})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bouton t\u00e9l\u00e9charger
                    if (!modelsReady && !isDownloading) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isDownloading = true
                                    downloadError = null
                                    downloadProgress = 0f
                                    downloadStatus = "Pr\u00e9paration..."

                                    // STT puis LLM avec leurs bons repos HuggingFace
                                    data class ModelDownload(
                                        val name: String,
                                        val file: String,
                                        val repo: String,
                                        val sizeMb: Int,
                                    )

                                    val downloads = listOf(
                                        ModelDownload(
                                            name = currentProfile.sttModel,
                                            file = currentProfile.sttFile,
                                            repo = currentProfile.sttRepo,
                                            sizeMb = currentProfile.sttSize,
                                        ),
                                        ModelDownload(
                                            name = currentProfile.llmModel,
                                            file = currentProfile.llmFile,
                                            repo = currentProfile.llmRepo,
                                            sizeMb = currentProfile.llmSize,
                                        ),
                                    )

                                    val totalBytes = downloads.sumOf { it.sizeMb.toLong() } * 1024L * 1024L
                                    var totalDownloaded = 0L

                                    for (dl in downloads) {
                                        downloadStatus = "${dl.name}..."
                                        val url = "https://huggingface.co/${dl.repo}/resolve/main/${dl.file}"

                                        val result = withContext(Dispatchers.IO) {
                                            modelManager.downloadModel(
                                                modelName = dl.file,
                                                remoteUrl = url,
                                                progress = { downloaded, total ->
                                                    val fileFraction = downloaded.toFloat() / total.toFloat()
                                                    downloadProgress = (totalDownloaded + fileFraction * totalBytes) / totalBytes
                                                    downloadStatus = "${dl.name} : ${downloaded / (1024 * 1024)}/${total / (1024 * 1024)} Mo"
                                                }
                                            )
                                        }

                                        if (result == null) {
                                            downloadError = "Échec du téléchargement de ${dl.name}"
                                            isDownloading = false
                                            return@launch
                                        }
                                        totalDownloaded += dl.sizeMb.toLong() * 1024L * 1024L
                                    }

                                    downloadProgress = 1f
                                    downloadStatus = "Mod\u00e8les pr\u00eats !"
                                    isDownloading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("T\u00e9l\u00e9charger les mod\u00e8les")
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
                        text = "Mod\u00e8les t\u00e9l\u00e9charg\u00e9s, enregistrements et transcriptions.",
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
