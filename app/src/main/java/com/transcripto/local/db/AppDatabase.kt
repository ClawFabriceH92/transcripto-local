package com.transcripto.local.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ---- DAOs ----

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun count(): Int
}

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE recordingId = :recordingId")
    suspend fun getByRecordingId(recordingId: Long): TranscriptionEntity?

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getById(id: Long): TranscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Update
    suspend fun update(transcription: TranscriptionEntity)

    @Delete
    suspend fun delete(transcription: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun count(): Int
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE transcriptionId = :transcriptionId")
    suspend fun getByTranscriptionId(transcriptionId: Long): List<AnalysisEntity>

    @Query("SELECT * FROM analyses WHERE transcriptionId = :transcriptionId AND analysisType = :type LIMIT 1")
    suspend fun getByType(transcriptionId: Long, type: String): AnalysisEntity?

    @Query("SELECT * FROM analyses WHERE id = :id")
    suspend fun getById(id: Long): AnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AnalysisEntity): Long

    @Update
    suspend fun update(analysis: AnalysisEntity)

    @Delete
    suspend fun delete(analysis: AnalysisEntity)

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM analyses")
    suspend fun count(): Int
}

// ---- Database ----

@Database(
    entities = [
        RecordingEntity::class,
        TranscriptionEntity::class,
        AnalysisEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun analysisDao(): AnalysisDao

    companion object {
        private const val DB_NAME = "transcripto_local.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
