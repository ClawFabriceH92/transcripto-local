package com.transcripto.local.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ModelProfileInfo(
    val name: String,
    val sttModel: String,
    val llmModel: String,
    val isActive: Boolean,
)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val profiles = remember {
        listOf(
            ModelProfileInfo("< 4 Go", "Whisper Tiny", "Qwen2.5 1.5B", false),
            ModelProfileInfo("4-6 Go", "Whisper Base", "Llama 3.2 3B", false),
            ModelProfileInfo("6-8 Go", "Whisper Small", "Phi-4 Mini 3.8B", false),
            ModelProfileInfo("8-12 Go", "Whisper Small", "Mistral 7B", true),
            ModelProfileInfo(">12 Go", "Whisper Medium", "Qwen2.5 7B", false),
        )
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
                text = "Paramètres",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profil matériel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    profiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile.isActive,
                                onClick = { /* changer de profil */ },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "RAM ${profile.name}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${profile.sttModel} + ${profile.llmModel}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sécurité",
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
                            Text("Utiliser la biométrie", modifier = Modifier.weight(1f))
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { biometricEnabled = it }
                            )
                        }
                    }
                }
            }
        }

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
                        text = "Modèles téléchargés, enregistrements et transcriptions.",
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
                text = "100% local — aucune donnée ne quitte l'appareil",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
