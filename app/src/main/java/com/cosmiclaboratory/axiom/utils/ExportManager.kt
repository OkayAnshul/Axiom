package com.cosmiclaboratory.axiom.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cosmiclaboratory.axiom.domain.model.Note
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class ExportManager(private val context: Context) {
    
    enum class ExportFormat(val extension: String, val mimeType: String) {
        MARKDOWN(".md", "text/markdown"),
        TEXT(".txt", "text/plain")
    }
    
    fun exportNote(note: Note, format: ExportFormat): Intent? {
        return try {
            val fileName = sanitizeFileName("${note.title}_${getCurrentTimestamp()}")
            val file = createExportFile(fileName, format.extension)
            
            when (format) {
                ExportFormat.MARKDOWN -> writeMarkdownFile(file, note)
                ExportFormat.TEXT -> writeTextFile(file, note)
            }
            
            createShareIntent(file, format.mimeType)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createExportFile(fileName: String, extension: String): File {
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        return File(exportsDir, "$fileName$extension")
    }
    
    private fun writeMarkdownFile(file: File, note: Note) {
        val markdown = note.markdown.ifBlank {
            PlainTextToMarkdownConverter.convert(note.content, note.title)
        }
        FileWriter(file).use { writer ->
            writer.write("# ${note.title}\n\n")
            writer.write("*Created: ${formatDate(note.createdAt)}*\n")
            writer.write("*Modified: ${formatDate(note.updatedAt)}*\n\n")
            writer.write("---\n\n")
            writer.write(markdown)
        }
    }
    
    private fun writeTextFile(file: File, note: Note) {
        FileWriter(file).use { writer ->
            writer.write("${note.title}\n")
            writer.write("${"=".repeat(note.title.length)}\n\n")
            writer.write("Created: ${formatDate(note.createdAt)}\n")
            writer.write("Modified: ${formatDate(note.updatedAt)}\n\n")
            writer.write(stripMarkdown(note.content))
        }
    }
    
    private fun createShareIntent(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(50)
    }
    
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return formatter.format(Date())
    }
    
    private fun formatDate(dateTime: LocalDateTime): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return formatter.format(Date(timestamp))
    }
    
    private fun stripMarkdown(content: String): String {
        return content
            .replace(Regex("#{1,6}\\s+"), "") // Remove headers
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold
            .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic
            .replace(Regex("`(.*?)`"), "$1") // Remove inline code
            .replace(Regex("```[\\s\\S]*?```"), "[Code Block]") // Replace code blocks
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // Remove links, keep text
            .replace(Regex("^\\s*-\\s*\\[[ x]\\]\\s*", RegexOption.MULTILINE), "• ") // Convert tasks to bullets
            .replace(Regex("^\\s*[-*+]\\s+", RegexOption.MULTILINE), "• ") // Convert lists to bullets
    }
}
