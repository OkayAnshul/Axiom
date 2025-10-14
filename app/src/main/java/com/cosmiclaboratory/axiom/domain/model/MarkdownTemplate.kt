package com.cosmiclaboratory.axiom.domain.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

data class MarkdownTemplate(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val template: String,
    val category: TemplateCategory,
    val description: String = "",
    val cursorPosition: Int = -1 // Position to place cursor after insertion, -1 means end
)

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
            name = "Heading 1",
            icon = Icons.Filled.Title,
            template = "# Heading 1",
            category = TemplateCategory.HEADERS,
            cursorPosition = 11
        ),
        MarkdownTemplate(
            id = "h2",
            name = "Heading 2",
            icon = Icons.Filled.Title,
            template = "## Heading 2",
            category = TemplateCategory.HEADERS,
            cursorPosition = 12
        ),
        MarkdownTemplate(
            id = "h3",
            name = "Heading 3",
            icon = Icons.Filled.Title,
            template = "### Heading 3",
            category = TemplateCategory.HEADERS,
            cursorPosition = 13
        )
    )
    
    val formatting = listOf(
        MarkdownTemplate(
            id = "bold",
            name = "Bold",
            icon = Icons.Filled.FormatBold,
            template = "**bold text**",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2
        ),
        MarkdownTemplate(
            id = "italic",
            name = "Italic",
            icon = Icons.Filled.FormatItalic,
            template = "*italic text*",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 1
        ),
        MarkdownTemplate(
            id = "strikethrough",
            name = "Strikethrough",
            icon = Icons.Filled.FormatStrikethrough,
            template = "~~strikethrough text~~",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2
        ),
        MarkdownTemplate(
            id = "highlight",
            name = "Highlight",
            icon = Icons.Filled.Highlight,
            template = "==highlighted text==",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2
        ),
        MarkdownTemplate(
            id = "underline",
            name = "Underline",
            icon = Icons.Filled.FormatUnderlined,
            template = "__underlined text__",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 2
        ),
        MarkdownTemplate(
            id = "code_inline",
            name = "Inline Code",
            icon = Icons.Filled.Code,
            template = "`code`",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 1
        ),
        MarkdownTemplate(
            id = "code_block",
            name = "Code Block",
            icon = Icons.Filled.DataObject,
            template = "```language\ncode here\n```",
            category = TemplateCategory.FORMATTING,
            cursorPosition = 3
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
            icon = Icons.Default.Check,
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
            category = TemplateCategory.TABLES
        ),
        MarkdownTemplate(
            id = "table_3x3",
            name = "3×3 Table",
            icon = Icons.Filled.TableChart,
            template = "| Column 1 | Column 2 | Column 3 |\n|----------|----------|----------|\n| Cell 1   | Cell 2   | Cell 3   |\n| Cell 4   | Cell 5   | Cell 6   |\n| Cell 7   | Cell 8   | Cell 9   |",
            category = TemplateCategory.TABLES
        ),
        MarkdownTemplate(
            id = "table_aligned",
            name = "Aligned Table",
            icon = Icons.Filled.TableChart,
            template = "| Left | Center | Right |\n|:-----|:------:|------:|\n| Text | Text   | Text  |",
            category = TemplateCategory.TABLES
        )
    )
    
    val linksAndMedia = listOf(
        MarkdownTemplate(
            id = "link",
            name = "Link",
            icon = Icons.Filled.Link,
            template = "[link text](https://example.com)",
            category = TemplateCategory.LINKS,
            cursorPosition = 1
        ),
        MarkdownTemplate(
            id = "image",
            name = "Image",
            icon = Icons.Filled.Image,
            template = "![alt text](image-url)",
            category = TemplateCategory.LINKS,
            cursorPosition = 2
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
    
    val documentTemplates = listOf(
        MarkdownTemplate(
            id = "meeting_notes",
            name = "Meeting Notes",
            icon = Icons.Default.DateRange,
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
            icon = Icons.Default.Check,
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
            icon = Icons.Default.DateRange,
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
    
    fun getTemplateById(id: String): MarkdownTemplate? {
        return getAllTemplates().values.flatten().find { it.id == id }
    }
}