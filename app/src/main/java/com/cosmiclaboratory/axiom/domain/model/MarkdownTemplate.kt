package com.cosmiclaboratory.axiom.domain.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Title

data class MarkdownTemplate(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val template: String,
    val category: TemplateCategory,
    val description: String = "",
    val cursorPosition: Int = -1, // Position to place cursor after insertion, -1 means end
    val isMarkdownOnly: Boolean = true, // true for pure markdown, false for HTML/extended syntax
    val placeholderText: String? = null, // Text that can be smart-deleted with backspace
    val supportsSmartDeletion: Boolean = false // Whether this template supports smart deletion
) {
    // Helper property to check if template contains placeholder
    val hasPlaceholder: Boolean get() = !placeholderText.isNullOrEmpty()
    
    // Get the placeholder text or extract from template
    fun getPlaceholder(): String? {
        return placeholderText ?: extractPlaceholderFromTemplate()
    }
    
    // Extract placeholder text from template string
    private fun extractPlaceholderFromTemplate(): String? {
        return when {
            template.contains("text") -> "text"
            template.contains("bold text") -> "bold text"
            template.contains("italic text") -> "italic text"
            template.contains("code") -> "code"
            template.contains("strikethrough text") -> "strikethrough text"
            template.contains("highlighted text") -> "highlighted text"
            template.contains("underlined text") -> "underlined text"
            template.contains("link text") -> "link text"
            template.contains("alt text") -> "alt text"
            else -> null
        }
    }
}

enum class TemplateCategory(val displayName: String) {
    HEADERS("Headers"),
    FORMATTING("Formatting"),
    LISTS("Lists"),
    TABLES("Tables"),
    LINKS("Links & Media"),
    ADVANCED("Advanced"),
    TEMPLATES("Templates")
}

object MarkdownTemplates {
    
    val headers = listOf(
        MarkdownTemplate(
            id = "h1",
            name = "Title (Largest)",
            icon = Icons.Filled.Title,
            template = "# Title Text",
            category = TemplateCategory.HEADERS,
            description = "Largest heading - 32sp",
            cursorPosition = 2,
            placeholderText = "Title Text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "h2",
            name = "Subtitle (Large)",
            icon = Icons.Filled.Subtitles,
            template = "## Subtitle Text",
            category = TemplateCategory.HEADERS,
            description = "Large heading - 28sp",
            cursorPosition = 3,
            placeholderText = "Subtitle Text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "h3",
            name = "Heading (Medium)",
            icon = Icons.Filled.FormatSize,
            template = "### Heading Text",
            category = TemplateCategory.HEADERS,
            description = "Medium heading - 24sp",
            cursorPosition = 4,
            placeholderText = "Heading Text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "h4",
            name = "Subheading (Small)",
            icon = Icons.Filled.Notes,
            template = "#### Subheading Text",
            category = TemplateCategory.HEADERS,
            description = "Small heading - 22sp",
            cursorPosition = 5,
            placeholderText = "Subheading Text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "h5",
            name = "Section (Smaller)",
            icon = Icons.Filled.ShortText,
            template = "##### Section Text",
            category = TemplateCategory.HEADERS,
            description = "Smaller heading - 18sp",
            cursorPosition = 6,
            placeholderText = "Section Text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "h6",
            name = "Note (Smallest)",
            icon = Icons.Filled.TextFields,
            template = "###### Note Text",
            category = TemplateCategory.HEADERS,
            description = "Smallest heading - 16sp",
            cursorPosition = 7,
            placeholderText = "Note Text",
            supportsSmartDeletion = true
        )
    )
    
    val formatting = listOf(
        MarkdownTemplate(
            id = "bold",
            name = "Bold",
            icon = Icons.Filled.FormatBold,
            template = "**bold text**",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2,
            placeholderText = "bold text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "italic",
            name = "Italic",
            icon = Icons.Filled.FormatItalic,
            template = "*italic text*",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 1,
            placeholderText = "italic text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "strikethrough",
            name = "Strikethrough",
            icon = Icons.Filled.FormatStrikethrough,
            template = "~~strikethrough text~~",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2,
            placeholderText = "strikethrough text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "highlight",
            name = "Highlight",
            icon = Icons.Filled.Highlight,
            template = "==highlighted text==",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2,
            placeholderText = "highlighted text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "underline",
            name = "Underline",
            icon = Icons.Filled.FormatUnderlined,
            template = "__underlined text__",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2,
            placeholderText = "underlined text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "code_inline",
            name = "Inline Code",
            icon = Icons.Filled.Code,
            template = "`code`",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 1,
            placeholderText = "code",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "code_block",
            name = "Code Block",
            icon = Icons.Filled.Code,
            template = "```language\ncode here\n```",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 3,
            placeholderText = "code here",
            supportsSmartDeletion = false // Complex multiline template
        )
    )
    
    val lists = listOf(
        MarkdownTemplate(
            id = "bullet_list",
            name = "Bullet List",
            icon = Icons.AutoMirrored.Filled.List,
            template = "- Item 1\n- Item 2\n- Item 3",
            category = TemplateCategory.LISTS
        ),
        MarkdownTemplate(
            id = "numbered_list",
            name = "Numbered List",
            icon = Icons.AutoMirrored.Filled.List,
            template = "1. First item\n2. Second item\n3. Third item",
            category = TemplateCategory.LISTS
        ),
        MarkdownTemplate(
            id = "task_list",
            name = "Task List",
            icon = Icons.Default.CheckBox,
            template = "- [ ] Task 1\n- [ ] Task 2\n- [x] Completed task",
            category = TemplateCategory.LISTS
        ),
        MarkdownTemplate(
            id = "nested_list",
            name = "Nested List",
            icon = Icons.AutoMirrored.Filled.List,
            template = "- Main item 1\n  - Sub item 1\n  - Sub item 2\n- Main item 2",
            category = TemplateCategory.LISTS
        )
    )
    
    val tables = listOf(
        MarkdownTemplate(
            id = "table_2x2",
            name = "2×2 Table",
            icon = Icons.Filled.TableChart,
            template = "| Column 1 | Column 2 |\n|----------|----------|\n| Cell 1   | Cell 2   |\n| Cell 3   | Cell 4   |",
            category = TemplateCategory.TABLES,
            description = "Simple 2x2 table",
            cursorPosition = 2 // Positions cursor at "Column 1" text for easy editing
        ),
        MarkdownTemplate(
            id = "table_3x3",
            name = "3×3 Table",
            icon = Icons.Filled.TableChart,
            template = "| Column 1 | Column 2 | Column 3 |\n|----------|----------|----------|\n| Cell 1   | Cell 2   | Cell 3   |\n| Cell 4   | Cell 5   | Cell 6   |\n| Cell 7   | Cell 8   | Cell 9   |",
            category = TemplateCategory.TABLES,
            description = "3x3 table with headers",
            cursorPosition = 2 // Positions cursor at "Column 1" text for easy editing
        ),
        MarkdownTemplate(
            id = "table_aligned",
            name = "Aligned Table",
            icon = Icons.Filled.TableChart,
            template = "| Left | Center | Right |\n|:-----|:------:|------:|\n| Text | Text   | Text  |",
            category = TemplateCategory.TABLES,
            description = "Table with column alignment",
            cursorPosition = 2 // Positions cursor at "Left" text for easy editing
        )
    )
    
    val linksAndMedia = listOf(
        MarkdownTemplate(
            id = "link",
            name = "Link",
            icon = Icons.Filled.Link,
            template = "[link text](https://example.com)",
            category = TemplateCategory.LINKS,
            cursorPosition = 1,
            placeholderText = "link text",
            supportsSmartDeletion = true
        ),
        MarkdownTemplate(
            id = "image",
            name = "Image",
            icon = Icons.Filled.Image,
            template = "![alt text](image-url)",
            category = TemplateCategory.LINKS,
            cursorPosition = 2,
            placeholderText = "alt text",
            supportsSmartDeletion = true
        )
    )
    
    val advanced = listOf(
        MarkdownTemplate(
            id = "quote",
            name = "Quote",
            icon = Icons.Filled.FormatQuote,
            template = "> This is a blockquote\n> \n> With multiple lines",
            category = TemplateCategory.ADVANCED
        ),
        MarkdownTemplate(
            id = "horizontal_rule",
            name = "Horizontal Rule",
            icon = Icons.Filled.HorizontalRule,
            template = "---",
            category = TemplateCategory.ADVANCED
        )
    )
    
    val colors = listOf(
        MarkdownTemplate(
            id = "color_red",
            name = "Red Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #FF0000;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Red colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "color_blue",
            name = "Blue Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #0000FF;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Blue colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "color_green",
            name = "Green Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #00FF00;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Green colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "color_yellow",
            name = "Yellow Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #FFFF00;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Yellow colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "color_purple",
            name = "Purple Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #800080;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Purple colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "color_orange",
            name = "Orange Text",
            icon = Icons.Filled.ColorLens,
            template = "<span style=\"color: #FFA500;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Orange colored text",
            cursorPosition = 29,
            isMarkdownOnly = false // HTML extension
        )
    )
    
    val fontSizes = listOf(
        MarkdownTemplate(
            id = "font_size_small",
            name = "Small Text",
            icon = Icons.Filled.TextDecrease,
            template = "<small>small text</small>",
            category = TemplateCategory.FORMATTING,
            description = "Smaller font size",
            cursorPosition = 7,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_large",
            name = "Large Text",
            icon = Icons.Filled.TextIncrease,
            template = "<big>large text</big>",
            category = TemplateCategory.FORMATTING,
            description = "Larger font size",
            cursorPosition = 5,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_14",
            name = "14px Text",
            icon = Icons.Filled.FormatSize,
            template = "<span style=\"font-size: 14px;\">text</span>",
            category = TemplateCategory.FORMATTING,
            description = "14 pixel font",
            cursorPosition = 31,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_16",
            name = "16px Text",
            icon = Icons.Filled.FormatSize,
            template = "<span style=\"font-size: 16px;\">text</span>",
            category = TemplateCategory.FORMATTING,
            description = "16 pixel font",
            cursorPosition = 31,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_18",
            name = "18px Text",
            icon = Icons.Filled.FormatSize,
            template = "<span style=\"font-size: 18px;\">text</span>",
            category = TemplateCategory.FORMATTING,
            description = "18 pixel font",
            cursorPosition = 31,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_20",
            name = "20px Text",
            icon = Icons.Filled.FormatSize,
            template = "<span style=\"font-size: 20px;\">text</span>",
            category = TemplateCategory.FORMATTING,
            description = "20 pixel font",
            cursorPosition = 31,
            isMarkdownOnly = false // HTML extension
        ),
        MarkdownTemplate(
            id = "font_size_24",
            name = "24px Text",
            icon = Icons.Filled.FormatSize,
            template = "<span style=\"font-size: 24px;\">text</span>",
            category = TemplateCategory.FORMATTING,
            description = "24 pixel font",
            cursorPosition = 31,
            isMarkdownOnly = false // HTML extension
        )
    )
    
    val documentTemplates = listOf(
        MarkdownTemplate(
            id = "meeting_notes",
            name = "Meeting Notes",
            icon = Icons.Default.EventNote,
            template = """# Meeting Notes - [Date]

## Attendees
- Name 1
- Name 2

## Agenda
1. Topic 1
2. Topic 2

## Discussion
- [ ] Action item 1
- [ ] Action item 2

## Next Steps
- [ ] Follow up task
- [ ] Schedule next meeting""",
            category = TemplateCategory.TEMPLATES
        ),
        MarkdownTemplate(
            id = "project_plan",
            name = "Project Plan",
            icon = Icons.Default.Assignment,
            template = """# Project Plan - [Project Name]

## Overview
Brief description of the project

## Goals
- [ ] Goal 1
- [ ] Goal 2
- [ ] Goal 3

## Timeline
| Phase | Description | Due Date |
|-------|-------------|----------|
| Phase 1 | Initial setup | [Date] |
| Phase 2 | Development | [Date] |
| Phase 3 | Testing | [Date] |

## Resources
- Resource 1
- Resource 2

## Notes
Additional notes here""",
            category = TemplateCategory.TEMPLATES
        ),
        MarkdownTemplate(
            id = "daily_journal",
            name = "Daily Journal",
            icon = Icons.Default.AutoStories,
            template = """# Daily Journal - [Date]

## Mood
😊 😐 😔 (circle one)

## Gratitude
- I'm grateful for...
- I appreciate...

## Today's Highlights
-
-
-

## Challenges
-

## Tomorrow's Goals
- [ ]
- [ ]
- [ ]

## Reflection
""",
            category = TemplateCategory.TEMPLATES
        )
    )
    
    fun getAllTemplates(): Map<TemplateCategory, List<MarkdownTemplate>> {
        return mapOf(
            TemplateCategory.HEADERS to headers,
            TemplateCategory.FORMATTING to formatting,
            TemplateCategory.LISTS to lists,
            TemplateCategory.TABLES to tables,
            TemplateCategory.LINKS to linksAndMedia,
            TemplateCategory.ADVANCED to advanced,
            TemplateCategory.TEMPLATES to documentTemplates
        )
    }
    
    fun getMarkdownOnlyTemplates(): Map<TemplateCategory, List<MarkdownTemplate>> {
        return getAllTemplates().mapValues { (_, templates) ->
            templates.filter { it.isMarkdownOnly }
        }.filterValues { it.isNotEmpty() }
    }
    
    fun getHeaderTemplatesForFontSizing(): List<MarkdownTemplate> {
        return headers // All 6 header levels for font size control
    }
    
    fun getTemplateById(id: String): MarkdownTemplate? {
        return getAllTemplates().values.flatten().find { it.id == id }
    }
    
    fun getFontSizeTemplate(size: Int): MarkdownTemplate? {
        return fontSizes.find { template ->
            when (size) {
                14 -> template.id == "font_size_14"
                16 -> template.id == "font_size_16"
                18 -> template.id == "font_size_18"
                20 -> template.id == "font_size_20"
                24 -> template.id == "font_size_24"
                else -> false
            }
        }
    }
    
    fun getColorTemplate(color: androidx.compose.ui.graphics.Color): MarkdownTemplate {
        // Safer color conversion using Android's toArgb() method
        val argb = try {
            android.graphics.Color.rgb(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        } catch (e: Exception) {
            android.graphics.Color.BLACK // Default to black if conversion fails
        }
        val hexColor = String.format("#%06X", argb and 0xFFFFFF)
        
        return MarkdownTemplate(
            id = "color_custom",
            name = "Custom Color",
            icon = Icons.Filled.Palette,
            template = "<span style=\"color: $hexColor;\">colored text</span>",
            category = TemplateCategory.FORMATTING,
            description = "Custom colored text",
            cursorPosition = (29 + hexColor.length),
            isMarkdownOnly = false // HTML extension
        )
    }
    
    fun getPredefinedColorTemplate(colorName: String): MarkdownTemplate? {
        return colors.find { template ->
            when (colorName.lowercase()) {
                "red" -> template.id == "color_red"
                "blue" -> template.id == "color_blue"
                "green" -> template.id == "color_green"
                "yellow" -> template.id == "color_yellow"
                "purple" -> template.id == "color_purple"
                "orange" -> template.id == "color_orange"
                else -> false
            }
        }
    }
}