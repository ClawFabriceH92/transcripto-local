package com.transcripto.local.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State partagé entre tous les écrans.
 * Utilisé via [LocalAppState] pour que RecordScreen / TranscribeScreen /
 * AnalyzeScreen partagent les mêmes enregistrements.
 */
class AppState {
    var recordings = mutableStateListOf<Recording>()
        private set

    var transcribingId by mutableStateOf<Long?>(null)
    var transcribeProgress by mutableStateOf(0f)

    var selectedRecordingId by mutableStateOf<Long?>(null)

    /** Incrémenté à chaque nouvel enregistrement. */
    private var nextId = 1L

    fun addRecording(date: String, duration: String, audioPath: String = "") {
        recordings.add(
            0, // en premier
            Recording(
                id = nextId++,
                date = date,
                duration = duration,
                audioPath = audioPath,
            )
        )
    }

    fun getSelectedRecording(): Recording? {
        return recordings.find { it.id == selectedRecordingId }
    }

    fun setTranscription(id: Long, text: String) {
        val idx = recordings.indexOfFirst { it.id == id }
        if (idx >= 0) {
            recordings[idx] = recordings[idx].copy(
                fullText = text,
                isTranscribed = true,
            )
        }
    }

    fun setAnalysis(id: Long, summary: String, keyPoints: String, actions: String) {
        val idx = recordings.indexOfFirst { it.id == id }
        if (idx >= 0) {
            recordings[idx] = recordings[idx].copy(
                summary = summary,
                keyPoints = keyPoints,
                actions = actions,
            )
        }
    }
}

/** CompositionLocal pour partager l'AppState dans tout l'arbre Compose. */
val LocalAppState = compositionLocalOf { AppState() }
