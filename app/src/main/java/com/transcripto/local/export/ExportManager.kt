package com.transcripto.local.export

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export des analyses et transcriptions vers des fichiers locaux.
 */
class ExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    data class ExportResult(
        val file: File,
        val mimeType: String = "text/plain",
    )

    fun exportText(
        title: String,
        content: String,
        subdirectory: String = "exports",
    ): ExportResult {
        val exportDir = File(context.filesDir, subdirectory)
        exportDir.mkdirs()

        val timestamp = dateFormat.format(Date())
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(50)
        val file = File(exportDir, "${safeTitle}_${timestamp}.txt")

        file.writeText(
            """
            |=== $title ===
            |Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}
            |
            |$content
            |
            |=== Fin ===
            |--- Généré par Transcripto Local ---
            |""".trimMargin()
        )

        return ExportResult(file)
    }

    fun exportAnalysisReport(
        transcriptionTitle: String,
        transcription: String,
        summary: String,
        keyPoints: List<String>,
        actionItems: List<String>,
    ): ExportResult {
        val exportDir = File(context.filesDir, "exports")
        exportDir.mkdirs()

        val timestamp = dateFormat.format(Date())
        val safeTitle = transcriptionTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50)
        val file = File(exportDir, "analyse_${safeTitle}_${timestamp}.txt")

        val report = buildString {
            appendLine("========================================")
            appendLine("RAPPORT D'ANALYSE — $transcriptionTitle")
            appendLine("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine("========================================")
            appendLine()

            appendLine("RÉSUMÉ")
            appendLine("-------")
            appendLine(summary)
            appendLine()

            appendLine("POINTS CLÉS")
            appendLine("-----------")
            keyPoints.forEachIndexed { i, point ->
                appendLine("${i + 1}. $point")
            }
            appendLine()

            appendLine("ACTIONS À SUIVRE")
            appendLine("----------------")
            if (actionItems.isEmpty()) {
                appendLine("Aucune action identifiée.")
            } else {
                actionItems.forEachIndexed { i, action ->
                    appendLine("☐ $action")
                }
            }
            appendLine()

            appendLine("--- Généré par Transcripto Local ---")
            appendLine("Application 100% locale — confidentiel")
        }

        file.writeText(report)
        return ExportResult(file)
    }

    fun listExports(): List<File> {
        val exportDir = File(context.filesDir, "exports")
        return exportDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList()
            ?: emptyList()
    }
}
