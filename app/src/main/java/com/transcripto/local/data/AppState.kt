package com.transcripto.local.data

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State partagé entre tous les écrans.
 */
class AppState {
    var recordings = mutableStateListOf<Recording>()
        private set

    var transcribingId by mutableStateOf<Long?>(null)
    var transcribeProgress by mutableStateOf(0f)

    var selectedRecordingId by mutableStateOf<Long?>(null)

    /** Navigation : permet à RecordScreen de basculer vers Transcriptions. */
    var selectedScreen by mutableStateOf(0)
    var onNavigateToScreen: (Int) -> Unit = {}

    private var nextId = 1L

    fun addRecording(date: String, time: String, duration: String, audioPath: String = "") {
        recordings.add(
            0,
            Recording(
                id = nextId++,
                date = date,
                time = time,
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

val LocalAppState = compositionLocalOf { AppState() }
