package com.transcripto.local.data

/**
 * Représente un enregistrement avec sa transcription.
 */
data class Recording(
    val id: Long,
    val date: String,
    val time: String,
    val duration: String,
    val audioPath: String = "",
    var fullText: String = "",
    var summary: String = "",
    var keyPoints: String = "",
    var actions: String = "",
    var isTranscribed: Boolean = false,
)
