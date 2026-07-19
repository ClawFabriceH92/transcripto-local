package com.transcripto.local.db

import androidx.room.*

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAll(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)
}

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions WHERE recordingId = :recordingId")
    suspend fun getByRecordingId(recordingId: Long): TranscriptionEntity?

    @Insert
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Update
    suspend fun update(transcription: TranscriptionEntity)

    @Delete
    suspend fun delete(transcription: TranscriptionEntity)
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses WHERE transcriptionId = :transcriptionId")
    suspend fun getByTranscriptionId(transcriptionId: Long): List<AnalysisEntity>

    @Insert
    suspend fun insert(analysis: AnalysisEntity): Long

    @Delete
    suspend fun delete(analysis: AnalysisEntity)
}
