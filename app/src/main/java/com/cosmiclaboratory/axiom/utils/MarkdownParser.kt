package com.cosmiclaboratory.axiom.utils

import com.cosmiclaboratory.axiom.domain.model.TaskItem

object MarkdownParser {
    
    sealed class Element {
        data class Header(val level: Int, val text: String) : Element()
        data class Text(val text: String) : Element()
        data class Bold(val text: String) : Element()
        data class Italic(val text: String) : Element()
        data class Strikethrough(val text: String) : Element()
        data class Highlight(val text: String) : Element()
        data class Underline(val text: String) : Element()
        data class Code(val text: String) : Element()
        data class CodeBlock(val language: String, val code: String) : Element()
        data class Link(val text: String, val url: String) : Element()
        data class Image(val alt: String, val url: String) : Element()
        data class Task(val text: String, val completed: Boolean) : Element()
        data class ListItem(val text: String, val level: Int = 0) : Element()
        data class NumberedListItem(val text: String, val number: Int, val level: Int = 0) : Element()
        data class Quote(val text: String) : Element()
        data class Table(val headers: List<String>, val rows: List<List<String>>, val alignments: List<TableAlignment>) : Element()
        object HorizontalRule : Element()
        data class Paragraph(val elements: List<Element>) : Element()
        object EmptyLine : Element()
    }
    
    enum class TableAlignment {
        LEFT, CENTER, RIGHT, NONE
    }
    
    fun parse(content: String): List<Element> {
        if (content.isBlank()) return emptyList()
        
        val elements = mutableListOf<Element>()
        val lines = content.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()
            
            when {
                // Empty line
                trimmedLine.isEmpty() -> {
                    elements.add(Element.EmptyLine)
                    i++
                }
                
                // Code block
                trimmedLine.startsWith("```") -> {
                    val language = trimmedLine.removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    
                    elements.add(Element.CodeBlock(language, codeLines.joinToString("\n")))
                    if (i < lines.size) i++ // Skip closing ```
                }
                
                // Header (supports # to ######)
                trimmedLine.startsWith("#") -> {
                    val headerRegex = Regex("^(#{1,6})\\s*(.*)")
                    val match = headerRegex.find(trimmedLine)
                    if (match != null) {
                        val level = match.groupValues[1].length
                        val text = match.groupValues[2].ifEmpty { "Header" }
                        elements.add(Element.Header(level, text))
                    } else {
                        elements.add(Element.Text(line))
                    }
                    i++
                }
                
                // Task (checkbox)
                trimmedLine.matches(Regex("^-\\s*\\[[xX\\s]\\]\\s*.*")) -> {
                    val taskRegex = Regex("^-\\s*\\[([xX\\s])\\]\\s*(.*)")
                    val match = taskRegex.find(trimmedLine)
                    if (match != null) {
                        val isCompleted = match.groupValues[1].trim().lowercase() == "x"
                        val taskText = match.groupValues[2].ifEmpty { "Task" }
                        elements.add(Element.Task(taskText, isCompleted))
                    } else {
                        elements.add(Element.Text(line))
                    }
                    i++
                }
                
                // Horizontal rule
                trimmedLine.matches(Regex("^(---+|\\*\\*\\*+|___+)$")) -> {
                    elements.add(Element.HorizontalRule)
                    i++
                }
                
                // Table (detect by pipe characters)
                trimmedLine.contains("|") && trimmedLine.count { it == '|' } >= 2 -> {
                    val tableResult = parseTable(lines, i)
                    if (tableResult != null) {
                        elements.add(tableResult.first)
                        i = tableResult.second
                    } else {
                        elements.add(Element.Text(line))
                        i++
                    }
                }
                
                // Numbered list item
                trimmedLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val numberedListRegex = Regex("^(\\d+)\\.\\s+(.*)")
                    val match = numberedListRegex.find(trimmedLine)
                    if (match != null) {
                        val number = match.groupValues[1].toIntOrNull() ?: 1
                        val text = match.groupValues[2]
                        val level = (line.length - line.trimStart().length) / 2
                        elements.add(Element.NumberedListItem(text, number, level))
                    } else {
                        elements.add(Element.Text(line))
                    }
                    i++
                }
                
                // List item (with level support)
                trimmedLine.matches(Regex("^[-*+]\\s+.*")) -> {
                    val listRegex = Regex("^[-*+]\\s+(.*)")
                    val match = listRegex.find(trimmedLine)
                    if (match != null) {
                        val level = (line.length - line.trimStart().length) / 2
                        elements.add(Element.ListItem(match.groupValues[1], level))
                    } else {
                        elements.add(Element.Text(line))
                    }
                    i++
                }
                
                // Quote
                trimmedLine.startsWith(">") -> {
                    val quoteText = trimmedLine.removePrefix(">").trimStart()
                    elements.add(Element.Quote(quoteText))
                    i++
                }
                
                // Regular text with inline formatting
                else -> {
                    val inlineElements = parseInlineFormatting(line)
                    if (inlineElements.size == 1 && inlineElements[0] is Element.Text) {
                        elements.add(inlineElements[0])
                    } else {
                        elements.add(Element.Paragraph(inlineElements))
                    }
                    i++
                }
            }
        }
        
        return elements
    }
    
    private fun parseInlineFormatting(text: String): List<Element> {
        if (text.isBlank()) return listOf(Element.Text(""))
        
        val elements = mutableListOf<Element>()
        var currentText = text
        var currentIndex = 0
        
        // Simple approach: find the first formatting element and process it
        while (currentIndex < currentText.length) {
            // Look for various formatting patterns (ordered by priority)
            val codeMatch = Regex("`([^`]+)`").find(currentText, currentIndex)
            val linkMatch = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)").find(currentText, currentIndex)
            val imageMatch = Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)").find(currentText, currentIndex)
            val strikethroughMatch = Regex("~~([^~]+)~~").find(currentText, currentIndex)
            val highlightMatch = Regex("==([^=]+)==").find(currentText, currentIndex)
            val underlineMatch = Regex("__([^_]+)__").find(currentText, currentIndex)
            val boldMatch = Regex("\\*\\*([^*]+)\\*\\*").find(currentText, currentIndex)
            val italicMatch = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").find(currentText, currentIndex)
            
            // Find the earliest match
            val matches = listOfNotNull(
                codeMatch?.let { "code" to it },
                linkMatch?.let { "link" to it },
                imageMatch?.let { "image" to it },
                strikethroughMatch?.let { "strikethrough" to it },
                highlightMatch?.let { "highlight" to it },
                underlineMatch?.let { "underline" to it },
                boldMatch?.let { "bold" to it },
                italicMatch?.let { "italic" to it }
            ).sortedBy { it.second.range.first }
            
            if (matches.isEmpty()) {
                // No more formatting, add remaining text
                val remainingText = currentText.substring(currentIndex)
                if (remainingText.isNotEmpty()) {
                    elements.add(Element.Text(remainingText))
                }
                break
            }
            
            val (type, match) = matches.first()
            
            // Add text before the match
            if (match.range.first > currentIndex) {
                val beforeText = currentText.substring(currentIndex, match.range.first)
                if (beforeText.isNotEmpty()) {
                    elements.add(Element.Text(beforeText))
                }
            }
            
            // Add the formatted element
            when (type) {
                "code" -> elements.add(Element.Code(match.groupValues[1]))
                "link" -> elements.add(Element.Link(match.groupValues[1], match.groupValues[2]))
                "image" -> elements.add(Element.Image(match.groupValues[1], match.groupValues[2]))
                "strikethrough" -> elements.add(Element.Strikethrough(match.groupValues[1]))
                "highlight" -> elements.add(Element.Highlight(match.groupValues[1]))
                "underline" -> elements.add(Element.Underline(match.groupValues[1]))
                "bold" -> elements.add(Element.Bold(match.groupValues[1]))
                "italic" -> elements.add(Element.Italic(match.groupValues[1]))
            }
            
            currentIndex = match.range.last + 1
        }
        
        return elements.ifEmpty { listOf(Element.Text(text)) }
    }
    
    private fun parseTable(lines: List<String>, startIndex: Int): Pair<Element.Table, Int>? {
        if (startIndex >= lines.size) return null
        
        val headerLine = lines[startIndex].trim()
        if (!headerLine.contains("|")) return null
        
        // Parse header
        val headers = headerLine.split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (headers.isEmpty()) return null
        
        // Check for separator line
        var currentIndex = startIndex + 1
        if (currentIndex >= lines.size) {
            // Single row table (header only)
            return Pair(
                Element.Table(headers, emptyList(), List(headers.size) { TableAlignment.NONE }),
                currentIndex
            )
        }
        
        val separatorLine = lines[currentIndex].trim()
        val alignments = if (separatorLine.matches(Regex("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$"))) {
            // Parse alignments from separator line
            separatorLine.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { cell ->
                    when {
                        cell.startsWith(":") && cell.endsWith(":") -> TableAlignment.CENTER
                        cell.endsWith(":") -> TableAlignment.RIGHT
                        cell.startsWith(":") -> TableAlignment.LEFT
                        else -> TableAlignment.NONE
                    }
                }
                .let { alignList ->
                    // Pad or trim to match header count
                    if (alignList.size < headers.size) {
                        alignList + List(headers.size - alignList.size) { TableAlignment.NONE }
                    } else {
                        alignList.take(headers.size)
                    }
                }
        } else {
            // No separator line, treat as header-only table
            return Pair(
                Element.Table(headers, emptyList(), List(headers.size) { TableAlignment.NONE }),
                currentIndex
            )
        }
        
        currentIndex++ // Skip separator line
        
        // Parse data rows
        val rows = mutableListOf<List<String>>()
        while (currentIndex < lines.size) {
            val line = lines[currentIndex].trim()
            if (!line.contains("|")) break
            
            val cells = line.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (cells.isEmpty()) break
            
            // Pad or trim cells to match header count
            val paddedCells = if (cells.size < headers.size) {
                cells + List(headers.size - cells.size) { "" }
            } else {
                cells.take(headers.size)
            }
            
            rows.add(paddedCells)
            currentIndex++
        }
        
        return Pair(Element.Table(headers, rows, alignments), currentIndex)
    }
}