package com.transcripto.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single audio recording session.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Absolute path to the audio file on disk. */
    val filePath: String,

    /** Duration of the recording in milliseconds. */
    val durationMs: Long = 0L,

    /** Timestamp (epoch millis) when the recording was created. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Whether the audio file is encrypted on disk. */
    val isEncrypted: Boolean = false,
)

/**
 * Represents the transcription result of a recording.
 */
@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
    indices = [androidx.room.Index(value = ["recordingId"])],
)
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK to the parent recording. */
    val recordingId: Long,

    /** Full transcribed text. */
    val fullText: String,

    /** Detected language code (e.g. "fr", "en"). */
    val language: String = "fr",

    /** Number of transcribed segments. */
    val segmentCount: Int = 0,

    /** Timestamp when transcription was completed. */
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Represents an LLM analysis result derived from a transcription.
 */
@Entity(
    tableName = "analyses",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = TranscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptionId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
    indices = [androidx.room.Index(value = ["transcriptionId"])],
)
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK to the parent transcription. */
    val transcriptionId: Long,

    /** Type of analysis (e.g. "SUMMARY", "KEY_POINTS", "ACTIONS"). */
    val analysisType: String,

    /** The generated analysis text. */
    val content: String,

    /** Timestamp when the analysis was generated. */
    val createdAt: Long = System.currentTimeMillis(),
)
