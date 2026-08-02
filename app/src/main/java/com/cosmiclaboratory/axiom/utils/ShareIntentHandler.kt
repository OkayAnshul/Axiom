package com.cosmiclaboratory.axiom.utils

import android.content.Intent
import android.net.Uri
import java.io.InputStream

object ShareIntentHandler {
    
    data class SharedContent(
        val text: String,
        val title: String? = null,
        val sourceApp: String? = null
    )
    
    fun extractSharedContent(intent: Intent): SharedContent? {
        return when (intent.action) {
            Intent.ACTION_SEND -> handleSingleShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleShare(intent)
            else -> null
        }
    }
    
    private fun handleSingleShare(intent: Intent): SharedContent? {
        return when {
            intent.type?.startsWith("text/") == true -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                val sourceApp = intent.getStringExtra("android.intent.extra.ORIGINATING_PACKAGE")
                
                if (!sharedText.isNullOrBlank()) {
                    SharedContent(
                        text = formatSharedText(sharedText, sharedSubject, sourceApp),
                        title = generateTitleFromContent(sharedText, sharedSubject),
                        sourceApp = sourceApp
                    )
                } else null
            }
            else -> null
        }
    }
    
    private fun handleMultipleShare(intent: Intent): SharedContent? {
        val sharedTexts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val sourceApp = intent.getStringExtra("android.intent.extra.ORIGINATING_PACKAGE")
        
        return if (!sharedTexts.isNullOrEmpty()) {
            val combinedText = sharedTexts.joinToString("\n\n")
            SharedContent(
                text = formatSharedText(combinedText, sharedSubject, sourceApp),
                title = generateTitleFromContent(combinedText, sharedSubject),
                sourceApp = sourceApp
            )
        } else null
    }
    
    private fun formatSharedText(text: String, subject: String?, sourceApp: String?): String {
        val formattedText = StringBuilder()
        
        // Add subject as title if available
        if (!subject.isNullOrBlank() && subject != text) {
            formattedText.append("# $subject\n\n")
        }
        
        // Add source app info if available
        if (!sourceApp.isNullOrBlank()) {
            formattedText.append("*Shared from: $sourceApp*\n")
            formattedText.append("*Shared at: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))}*\n\n")
            formattedText.append("---\n\n")
        }
        
        // Add the main content
        formattedText.append(text)
        
        return formattedText.toString()
    }
    
    private fun generateTitleFromContent(text: String, subject: String?): String {
        return when {
            !subject.isNullOrBlank() -> subject
            text.isNotBlank() -> {
                // Extract first line or first 50 characters as title
                val firstLine = text.lines().firstOrNull { it.isNotBlank() }
                when {
                    firstLine != null && firstLine.length <= 50 -> firstLine
                    firstLine != null -> "${firstLine.take(47)}..."
                    else -> "Shared Note"
                }
            }
            else -> "Shared Note"
        }
    }
}