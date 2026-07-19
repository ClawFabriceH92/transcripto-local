package com.transcripto.local.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.IOException

/**
 * Interface for audio recording operations.
 */
interface AudioRecorder {
    /**
     * Current recording state.
     */
    val state: RecordingState

    /**
     * Current recording duration in milliseconds.
     */
    val currentDurationMs: Long

    /**
     * Normalized sound amplitude (0.0 – 1.0) for UI visualisation.
     */
    val soundLevel: Float

    /**
     * Start recording to the given output file.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(outputFile: File)

    /**
     * Pause the ongoing recording.
     */
    fun pause()

    /**
     * Resume a paused recording.
     */
    fun resume()

    /**
     * Stop recording and finalise the output file.
     * @return the recorded audio file, or null if recording was empty.
     */
    fun stop(): File?

    /**
     * Release all resources held by this recorder.
     */
    fun release()
}

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR,
}

/**
 * Android [MediaRecorder]-based implementation.
 *
 * Produces AAC/ADTS files and exposes a [soundLevel] callback via an
 * optional amplitude-polling loop.
 */
class MediaRecorderAudioRecorder(
    private val context: Context,
    private val onSoundLevel: ((Float) -> Unit)? = null,
) : AudioRecorder {

    override var state: RecordingState = RecordingState.IDLE
        private set

    override var currentDurationMs: Long = 0L
        private set

    override var soundLevel: Float = 0f
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var activeFile: File? = null
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private var isPausedBySystem = false

    // Polling for amplitude (optional, only when a listener is attached)
    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            val recorder = mediaRecorder
            if (recorder != null && state == RecordingState.RECORDING) {
                try {
                    val amplitude = recorder.maxAmplitude.toFloat()
                    // Normalise to 0..1 (MediaRecorder max ~ 32767)
                    soundLevel = (amplitude / 32767f).coerceIn(0f, 1f)
                    onSoundLevel?.invoke(soundLevel)
                } catch (_: IllegalStateException) {
                    // Recorder no longer valid
                }
                amplitudeHandler.postDelayed(this, 100L)
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(outputFile: File) {
        if (state == RecordingState.RECORDING) return

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outputFile.absolutePath)

            // Prepare synchronously (or use setOnInfoListener for async)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            activeFile = outputFile
            state = RecordingState.RECORDING
            currentDurationMs = 0L

            amplitudeHandler.post(amplitudeRunnable)
        } catch (e: IOException) {
            state = RecordingState.ERROR
        } catch (e: IllegalStateException) {
            state = RecordingState.ERROR
        }
    }

    override fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && state == RecordingState.RECORDING) {
            mediaRecorder?.let {
                try {
                    it.pause()
                    state = RecordingState.PAUSED
                    amplitudeHandler.removeCallbacks(amplitudeRunnable)
                } catch (_: IllegalStateException) {
                    state = RecordingState.ERROR
                }
            }
        }
        // On older APIs pause is unsupported — keep recording silently
    }

    override fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && state == RecordingState.PAUSED) {
            mediaRecorder?.let {
                try {
                    it.resume()
                    state = RecordingState.RECORDING
                    amplitudeHandler.post(amplitudeRunnable)
                } catch (_: IllegalStateException) {
                    state = RecordingState.ERROR
                }
            }
        }
    }

    override fun stop(): File? {
        amplitudeHandler.removeCallbacks(amplitudeRunnable)

        mediaRecorder?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    it.stop()
                } else {
                    @Suppress("DEPRECATION")
                    it.stop()
                }
            } catch (e: RuntimeException) {
                // Stop raised when recorder was already released or too short
            }
            it.release()
        }
        mediaRecorder = null

        val file = activeFile
        state = if (file != null && file.exists() && file.length() > 0L) {
            RecordingState.STOPPED
        } else {
            RecordingState.ERROR
        }
        activeFile = null
        return file?.takeIf { it.exists() && it.length() > 0L }
    }

    override fun release() {
        amplitudeHandler.removeCallbacks(amplitudeRunnable)
        mediaRecorder?.release()
        mediaRecorder = null
        activeFile = null
        state = RecordingState.IDLE
    }

    // ---- system-interruption handling (to be wired into Activity lifecycle) ----

    /** Call from Activity.onPause() / onStop(). */
    fun handleSystemInterruptionStart() {
        if (state == RecordingState.RECORDING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pause()
            } else {
                // On older builds, MediaRecorder cannot survive configuration
                // changes; caller should stop() and save the partial file.
            }
            isPausedBySystem = true
        }
    }

    /** Call from Activity.onResume() when returning from an interruption. */
    fun handleSystemInterruptionEnd() {
        if (isPausedBySystem && state == RecordingState.PAUSED) {
            resume()
            isPausedBySystem = false
        }
    }
}
