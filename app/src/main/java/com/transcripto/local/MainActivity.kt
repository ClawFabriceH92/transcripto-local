package com.transcripto.local

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Transcribe
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.transcripto.local.data.AppState
import com.transcripto.local.data.LocalAppState
import com.transcripto.local.ui.AnalyzeScreen
import com.transcripto.local.ui.RecordScreen
import com.transcripto.local.ui.SettingsScreen
import com.transcripto.local.ui.TranscribeScreen

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermission()
        setContent {
            val appState = remember { AppState() }
            CompositionLocalProvider(LocalAppState provides appState) {
                TranscriptoLocalApp()
            }
        }
    }

    private fun requestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

sealed class Screen(
    val title: String,
    val icon: ImageVector,
) {
    data object Record : Screen("Enregistrer", Icons.Default.Mic)
    data object Transcribe : Screen("Transcrire", Icons.Default.Transcribe)
    data object Analyze : Screen("Analyser", Icons.Default.Analytics)
    data object Settings : Screen("Paramètres", Icons.Default.Settings)
}

private val screens = listOf(
    Screen.Record,
    Screen.Transcribe,
    Screen.Analyze,
    Screen.Settings,
)

@Composable
fun TranscriptoLocalApp() {
    var selectedScreen by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedScreen == index,
                        onClick = { selectedScreen = index },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedScreen) {
            0 -> RecordScreen(modifier = Modifier.padding(innerPadding))
            1 -> TranscribeScreen(modifier = Modifier.padding(innerPadding))
            2 -> AnalyzeScreen(modifier = Modifier.padding(innerPadding))
            3 -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
