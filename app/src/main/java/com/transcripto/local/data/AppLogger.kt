package com.transcripto.local.data

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logger applicatif. Tous les logs sont stockés en mémoire
 * et accessibles depuis l'écran de diagnostic dans Paramètres.
 */
object AppLogger {
    private val tag = "TranscriptoLocal"
    private val entries = mutableListOf<LogEntry>()
    private val maxEntries = 200

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String,
    )

    fun i(msg: String) = add("INFO", msg)
    fun w(msg: String) = add("WARN", msg)
    fun e(msg: String) = add("ERROR", msg)

    private fun add(level: String, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.FRENCH).format(Date())
        val entry = LogEntry(time, level, msg)
        entries.add(entry)
        Log.d(tag, "[$level] $msg")
        if (entries.size > maxEntries) entries.removeAt(0)
    }

    fun getAll(): List<LogEntry> = entries.toList()

    fun getText(): String = entries.joinToString("\n") { "${it.timestamp} [${it.level}] ${it.message}" }
}
