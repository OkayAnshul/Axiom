package com.cosmiclaboratory.axiom.domain.model

data class TaskItem(
    val text: String,
    val isCompleted: Boolean,
    val lineIndex: Int,
    val startIndex: Int,
    val endIndex: Int
) {
    companion object {
        private val taskPattern = Regex("""^\s*-\s*\[([x\s])\]\s*(.*)$""", RegexOption.IGNORE_CASE)
        
        fun fromMarkdownLine(line: String, lineIndex: Int, startIndex: Int): TaskItem? {
            val match = taskPattern.find(line) ?: return null
            val isCompleted = match.groupValues[1].trim().lowercase() == "x"
            val text = match.groupValues[2]
            val endIndex = startIndex + line.length
            
            return TaskItem(
                text = text,
                isCompleted = isCompleted,
                lineIndex = lineIndex,
                startIndex = startIndex,
                endIndex = endIndex
            )
        }
        
        fun toMarkdownString(text: String, isCompleted: Boolean): String {
            val checkbox = if (isCompleted) "[x]" else "[ ]"
            return "- $checkbox $text"
        }
    }
}

object TaskParser {
    fun extractTasks(content: String): List<TaskItem> {
        val tasks = mutableListOf<TaskItem>()
        val lines = content.lines()
        var currentIndex = 0
        
        lines.forEachIndexed { lineIndex, line ->
            val task = TaskItem.fromMarkdownLine(line, lineIndex, currentIndex)
            if (task != null) {
                tasks.add(task)
            }
            currentIndex += line.length + 1 // +1 for newline character
        }
        
        return tasks
    }
    
    fun toggleTaskCompletion(content: String, taskItem: TaskItem): String {
        val lines = content.lines().toMutableList()
        if (taskItem.lineIndex < lines.size) {
            val currentLine = lines[taskItem.lineIndex]
            val newLine = if (taskItem.isCompleted) {
                // Mark as incomplete
                currentLine.replace(Regex("""\[x\]""", RegexOption.IGNORE_CASE), "[ ]")
            } else {
                // Mark as complete
                currentLine.replace(Regex("""\[\s\]"""), "[x]")
            }
            lines[taskItem.lineIndex] = newLine
        }
        return lines.joinToString("\n")
    }
    
    fun addNewTask(content: String, taskText: String, insertAtEnd: Boolean = true): String {
        val newTaskLine = TaskItem.toMarkdownString(taskText, false)
        
        return if (insertAtEnd) {
            if (content.isBlank()) {
                newTaskLine
            } else {
                "$content\n$newTaskLine"
            }
        } else {
            if (content.isBlank()) {
                newTaskLine
            } else {
                "$newTaskLine\n$content"
            }
        }
    }
}