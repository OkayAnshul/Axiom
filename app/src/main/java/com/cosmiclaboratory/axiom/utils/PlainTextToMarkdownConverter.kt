package com.cosmiclaboratory.axiom.utils

/**
 * Converts normal, user-written text into the markdown representation Axiom uses
 * for preview and export. The rules are deterministic and offline-safe so writing
 * never depends on an AI call.
 */
object PlainTextToMarkdownConverter {
    private val urlRegex = Regex("""(?<!\()https?://[^\s<>()]+""")
    private val numberedRegex = Regex("""^\s*(\d+)[.)]\s+(.+)$""")
    private val bulletRegex = Regex("""^\s*[-*+•]\s+(.+)$""")
    private val existingMarkdownRegex = Regex(
        """^\s*(#{1,6}\s+|>\s+|[-*+]\s+\[[ xX]\]\s+|[-*+]\s+|\d+\.\s+|```|\|.+\|)"""
    )

    fun convert(
        plainText: String,
        title: String? = null,
        includeTitle: Boolean = false
    ): String {
        val normalized = plainText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()

        if (normalized.isBlank()) {
            return title
                ?.takeIf { includeTitle && it.isNotBlank() }
                ?.let { "# ${escapeHeading(it.trim())}" }
                .orEmpty()
        }

        val markdownLines = mutableListOf<String>()
        if (includeTitle && !title.isNullOrBlank()) {
            markdownLines += "# ${escapeHeading(title.trim())}"
            markdownLines += ""
        }

        val lines = normalized.lines()
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            val previousBlank = index == 0 || lines.getOrNull(index - 1).isNullOrBlank()
            val nextLooksBody = lines.getOrNull(index + 1)?.trim()?.let {
                it.length > 24 && !looksLikeList(it)
            } == true

            val converted = when {
                trimmed.isBlank() -> ""
                looksLikeExistingMarkdown(trimmed) -> line
                looksLikeSectionHeading(trimmed, previousBlank, nextLooksBody) -> "## ${escapeHeading(trimmed.removeSuffix(":"))}"
                looksLikeQuote(trimmed) -> "> ${trimmed.substringAfter(":").ifBlank { trimmed.removePrefix("quote").trim() }}"
                looksLikePlainList(trimmed) -> "- ${sentenceCase(cleanListPrefix(trimmed))}"
                looksLikeDone(trimmed) -> "- [x] ${sentenceCase(cleanTaskPrefix(trimmed))}"
                looksLikeTodo(trimmed) -> "- [ ] ${sentenceCase(cleanTaskPrefix(trimmed))}"
                bulletRegex.matches(line) -> "- ${bulletRegex.find(line)?.groupValues?.get(1).orEmpty().trim()}"
                numberedRegex.matches(line) -> {
                    val match = numberedRegex.find(line)
                    "${match?.groupValues?.get(1)}. ${match?.groupValues?.get(2).orEmpty().trim()}"
                }
                else -> autolink(trimmed)
            }

            markdownLines += converted
        }

        return markdownLines
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun looksLikeExistingMarkdown(line: String): Boolean {
        return existingMarkdownRegex.containsMatchIn(line)
    }

    private fun looksLikeSectionHeading(
        line: String,
        previousBlank: Boolean,
        nextLooksBody: Boolean
    ): Boolean {
        if (!previousBlank || line.length !in 3..64) return false
        if (line.endsWith(":")) return true
        if (line.any { it in ".!?" }) return false
        val words = line.split(Regex("\\s+"))
        if (words.size > 7) return false
        return nextLooksBody && words.any { it.firstOrNull()?.isUpperCase() == true }
    }

    private fun looksLikeList(line: String): Boolean {
        return bulletRegex.matches(line) || numberedRegex.matches(line) || looksLikeTodo(line) || looksLikeDone(line)
    }

    private fun looksLikePlainList(line: String): Boolean {
        val value = line.lowercase()
        return value.startsWith("list:") || value.startsWith("list ")
    }

    private fun looksLikeQuote(line: String): Boolean {
        return line.startsWith("quote:", ignoreCase = true) || line.startsWith("quote ", ignoreCase = true)
    }

    private fun looksLikeTodo(line: String): Boolean {
        val value = line.lowercase()
        return value.startsWith("todo ") ||
            value.startsWith("todo:") ||
            value.startsWith("task ") ||
            value.startsWith("task:") ||
            value.startsWith("remember to ") ||
            (value.startsWith("i need to ") && line.length <= 90 && !line.endsWith("."))
    }

    private fun looksLikeDone(line: String): Boolean {
        val value = line.lowercase()
        return value.startsWith("done ") ||
            value.startsWith("done:") ||
            value.startsWith("completed ") ||
            value.startsWith("completed:")
    }

    private fun cleanTaskPrefix(line: String): String {
        return line
            .replace(Regex("""^(todo|task|done|completed)\s*:?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^remember to\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^i need to\s+""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanListPrefix(line: String): String {
        return line
            .replace(Regex("""^list\s*:?\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun autolink(line: String): String {
        return urlRegex.replace(line) { matchResult ->
            val value = matchResult.value.trimEnd('.', ',', ';', ':')
            val suffix = matchResult.value.removePrefix(value)
            "<$value>$suffix"
        }
    }

    private fun escapeHeading(value: String): String {
        return value.replace(Regex("""^#+\s*"""), "").trim()
    }

    private fun sentenceCase(value: String): String {
        if (value.isBlank()) return value
        val first = value.first()
        return if (first.isLowerCase()) first.uppercaseChar() + value.drop(1) else value
    }
}
