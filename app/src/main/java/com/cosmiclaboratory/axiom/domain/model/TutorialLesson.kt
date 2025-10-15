package com.cosmiclaboratory.axiom.domain.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

data class TutorialLesson(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val difficulty: LessonDifficulty,
    val estimatedDuration: Int, // in minutes
    val steps: List<TutorialStep>,
    val isCompleted: Boolean = false
)

data class TutorialStep(
    val id: String,
    val title: String,
    val instruction: String,
    val example: String,
    val expectedOutput: String,
    val hints: List<String> = emptyList(),
    val isCompleted: Boolean = false
)

enum class LessonDifficulty(val displayName: String, val color: androidx.compose.ui.graphics.Color) {
    BEGINNER("Beginner", androidx.compose.ui.graphics.Color(0xFF4CAF50)),
    INTERMEDIATE("Intermediate", androidx.compose.ui.graphics.Color(0xFFFF9800)),
    ADVANCED("Advanced", androidx.compose.ui.graphics.Color(0xFFF44336))
}

object TutorialLessons {
    
    val beginnerLessons = listOf(
        TutorialLesson(
            id = "basic_formatting",
            title = "Basic Text Formatting",
            description = "Learn how to make text bold, italic, and styled",
            icon = Icons.Default.FormatBold,
            difficulty = LessonDifficulty.BEGINNER,
            estimatedDuration = 5,
            steps = listOf(
                TutorialStep(
                    id = "bold_text",
                    title = "Making Text Bold",
                    instruction = "Wrap text with **double asterisks** to make it bold",
                    example = "**This text is bold**",
                    expectedOutput = "This text is bold",
                    hints = listOf(
                        "Use ** on both sides of the text",
                        "Don't put spaces between ** and your text"
                    )
                ),
                TutorialStep(
                    id = "italic_text",
                    title = "Making Text Italic",
                    instruction = "Wrap text with *single asterisks* to make it italic",
                    example = "*This text is italic*",
                    expectedOutput = "This text is italic",
                    hints = listOf(
                        "Use * on both sides of the text",
                        "You can also use _underscores_ for italic"
                    )
                ),
                TutorialStep(
                    id = "strikethrough",
                    title = "Strikethrough Text",
                    instruction = "Use ~~double tildes~~ to cross out text",
                    example = "~~This text is crossed out~~",
                    expectedOutput = "This text is crossed out",
                    hints = listOf(
                        "Use ~~ on both sides",
                        "Great for showing edits or corrections"
                    )
                )
            )
        ),
        
        TutorialLesson(
            id = "headers",
            title = "Headers and Structure",
            description = "Organize your content with headers",
            icon = Icons.Default.Title,
            difficulty = LessonDifficulty.BEGINNER,
            estimatedDuration = 3,
            steps = listOf(
                TutorialStep(
                    id = "h1",
                    title = "Main Headers (H1)",
                    instruction = "Use # for the largest header",
                    example = "# This is a Main Header",
                    expectedOutput = "This is a Main Header",
                    hints = listOf(
                        "Only one # for the biggest header",
                        "Leave a space after the #"
                    )
                ),
                TutorialStep(
                    id = "h2_h3",
                    title = "Subheaders (H2, H3)",
                    instruction = "Use ## for H2 and ### for H3",
                    example = "## This is H2\n### This is H3",
                    expectedOutput = "This is H2\nThis is H3",
                    hints = listOf(
                        "More # symbols = smaller header",
                        "You can use up to 6 # symbols"
                    )
                )
            )
        ),
        
        TutorialLesson(
            id = "lists",
            title = "Creating Lists",
            description = "Make organized bullet points and numbered lists",
            icon = Icons.AutoMirrored.Filled.List,
            difficulty = LessonDifficulty.BEGINNER,
            estimatedDuration = 4,
            steps = listOf(
                TutorialStep(
                    id = "bullet_list",
                    title = "Bullet Lists",
                    instruction = "Use - or * to create bullet points",
                    example = "- First item\n- Second item\n- Third item",
                    expectedOutput = "• First item\n• Second item\n• Third item",
                    hints = listOf(
                        "Start each line with - or *",
                        "Leave a space after the symbol"
                    )
                ),
                TutorialStep(
                    id = "numbered_list",
                    title = "Numbered Lists",
                    instruction = "Use numbers followed by a period",
                    example = "1. First step\n2. Second step\n3. Third step",
                    expectedOutput = "1. First step\n2. Second step\n3. Third step",
                    hints = listOf(
                        "Numbers will auto-increment",
                        "You can use any number, markdown will fix it"
                    )
                )
            )
        )
    )
    
    val intermediateLessons = listOf(
        TutorialLesson(
            id = "links_images",
            title = "Links and Images",
            description = "Add clickable links and embed images",
            icon = Icons.Default.Link,
            difficulty = LessonDifficulty.INTERMEDIATE,
            estimatedDuration = 6,
            steps = listOf(
                TutorialStep(
                    id = "links",
                    title = "Creating Links",
                    instruction = "Use [text](url) format for links",
                    example = "[Visit Google](https://google.com)",
                    expectedOutput = "Visit Google",
                    hints = listOf(
                        "Text goes in square brackets",
                        "URL goes in parentheses right after"
                    )
                ),
                TutorialStep(
                    id = "images",
                    title = "Embedding Images",
                    instruction = "Use ![alt text](image-url) for images",
                    example = "![A beautiful sunset](https://example.com/sunset.jpg)",
                    expectedOutput = "A beautiful sunset",
                    hints = listOf(
                        "Start with an exclamation mark !",
                        "Alt text helps with accessibility"
                    )
                )
            )
        ),
        
        TutorialLesson(
            id = "code",
            title = "Code and Formatting",
            description = "Display code snippets and technical content",
            icon = Icons.Default.Code,
            difficulty = LessonDifficulty.INTERMEDIATE,
            estimatedDuration = 7,
            steps = listOf(
                TutorialStep(
                    id = "inline_code",
                    title = "Inline Code",
                    instruction = "Use `backticks` for inline code",
                    example = "Use the `print()` function in Python",
                    expectedOutput = "Use the print() function in Python",
                    hints = listOf(
                        "Backticks are found on the ~ key",
                        "Great for function names or short code"
                    )
                ),
                TutorialStep(
                    id = "code_blocks",
                    title = "Code Blocks",
                    instruction = "Use ``` for multi-line code blocks",
                    example = "```python\ndef hello():\n    print('Hello World')\n```",
                    expectedOutput = "def hello():\n    print('Hello World')",
                    hints = listOf(
                        "Use three backticks ```",
                        "Add language name for syntax highlighting"
                    )
                )
            )
        ),
        
        TutorialLesson(
            id = "tables",
            title = "Creating Tables",
            description = "Organize data in rows and columns",
            icon = Icons.Default.TableChart,
            difficulty = LessonDifficulty.INTERMEDIATE,
            estimatedDuration = 8,
            steps = listOf(
                TutorialStep(
                    id = "basic_table",
                    title = "Basic Table Structure",
                    instruction = "Use | to separate columns and --- for headers",
                    example = "| Name | Age |\n|------|-----|\n| John | 25 |\n| Jane | 30 |",
                    expectedOutput = "Name | Age\nJohn | 25\nJane | 30",
                    hints = listOf(
                        "First row is the header",
                        "Second row defines the table structure",
                        "Use | to separate columns"
                    )
                )
            )
        )
    )
    
    val advancedLessons = listOf(
        TutorialLesson(
            id = "advanced_formatting",
            title = "Advanced Formatting",
            description = "Master quotes, highlights, and complex layouts",
            icon = Icons.Default.FormatQuote,
            difficulty = LessonDifficulty.ADVANCED,
            estimatedDuration = 10,
            steps = listOf(
                TutorialStep(
                    id = "blockquotes",
                    title = "Block Quotes",
                    instruction = "Use > to create block quotes",
                    example = "> This is a quote\n> that spans multiple lines",
                    expectedOutput = "This is a quote\nthat spans multiple lines",
                    hints = listOf(
                        "Start each line with >",
                        "Great for citations or emphasis"
                    )
                ),
                TutorialStep(
                    id = "highlights",
                    title = "Highlighting Text",
                    instruction = "Use ==double equals== to highlight text",
                    example = "This is ==highlighted text== in a sentence",
                    expectedOutput = "This is highlighted text in a sentence",
                    hints = listOf(
                        "Not all markdown parsers support this",
                        "Axiom supports highlighting!"
                    )
                )
            )
        ),
        
        TutorialLesson(
            id = "task_lists",
            title = "Interactive Task Lists",
            description = "Create checkboxes and todo items",
            icon = Icons.Default.CheckBox,
            difficulty = LessonDifficulty.ADVANCED,
            estimatedDuration = 5,
            steps = listOf(
                TutorialStep(
                    id = "checkboxes",
                    title = "Creating Checkboxes",
                    instruction = "Use - [ ] for empty and - [x] for checked",
                    example = "- [ ] Todo item\n- [x] Completed item\n- [ ] Another todo",
                    expectedOutput = "☐ Todo item\n☑ Completed item\n☐ Another todo",
                    hints = listOf(
                        "Space between brackets for empty: [ ]",
                        "x or X in brackets for checked: [x]"
                    )
                )
            )
        )
    )
    
    fun getAllLessons(): Map<LessonDifficulty, List<TutorialLesson>> {
        return mapOf(
            LessonDifficulty.BEGINNER to beginnerLessons,
            LessonDifficulty.INTERMEDIATE to intermediateLessons,
            LessonDifficulty.ADVANCED to advancedLessons
        )
    }
    
    fun getLessonById(id: String): TutorialLesson? {
        return getAllLessons().values.flatten().find { it.id == id }
    }
}