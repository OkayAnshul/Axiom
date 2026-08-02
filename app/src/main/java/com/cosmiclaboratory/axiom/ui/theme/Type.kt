package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AxiomDisplayFont = FontFamily.Serif
private val AxiomBodyFont = FontFamily.Serif
private val AxiomUiFont = FontFamily.SansSerif

// Typography optimized for note-taking and reading
val Typography = Typography(
    // Display styles for headers and titles
    displayLarge = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles for section headers
    headlineLarge = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles for note titles and UI elements
    titleLarge = TextStyle(
        fontFamily = AxiomDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    
    // Body styles for note content - optimized for reading
    bodyLarge = TextStyle(
        fontFamily = AxiomBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp, // Increased line height for better readability
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AxiomBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    
    // Label styles for UI components
    labelLarge = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AxiomUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

// Modern Editor Typography Styles - Premium note-taking experience
val ModernNoteTitleStyle = TextStyle(
    fontFamily = AxiomDisplayFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,          // Larger, more prominent title
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

val EditorContentStyle = TextStyle(
    fontFamily = AxiomBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,          // Optimized for extended reading
    lineHeight = 30.sp,        // Generous line spacing for comfort
    letterSpacing = 0.sp
)

// Markdown Header Styles for Font Size Control (H1-H6)
val MarkdownH1Style = TextStyle(
    fontFamily = AxiomDisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,          // Largest - Title level
    lineHeight = 40.sp,
    letterSpacing = 0.sp
)

val MarkdownH2Style = TextStyle(
    fontFamily = AxiomDisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,          // Large - Subtitle level
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)

val MarkdownH3Style = TextStyle(
    fontFamily = AxiomDisplayFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,          // Medium - Heading level
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

val MarkdownH4Style = TextStyle(
    fontFamily = AxiomDisplayFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,          // Small - Subheading level
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

val MarkdownH5Style = TextStyle(
    fontFamily = AxiomUiFont,
    fontWeight = FontWeight.Medium,
    fontSize = 18.sp,          // Smaller - Section level
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)

val MarkdownH6Style = TextStyle(
    fontFamily = AxiomUiFont,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,          // Smallest - Entry level
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

val EditorMetadataStyle = TextStyle(
    fontFamily = AxiomUiFont,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

val EditorPlaceholderStyle = TextStyle(
    fontFamily = AxiomBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.sp
)

val EditorHeaderStyle = TextStyle(
    fontFamily = AxiomUiFont,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

val EditorSubtleHintStyle = TextStyle(
    fontFamily = AxiomUiFont,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

// Legacy styles (keeping for backward compatibility)
val NoteContentStyle = EditorContentStyle
val NoteTitleStyle = ModernNoteTitleStyle  
val NoteMetadataStyle = EditorMetadataStyle

// Helper function to get markdown header style by level
fun getMarkdownHeaderStyle(level: Int): TextStyle {
    return when (level) {
        1 -> MarkdownH1Style
        2 -> MarkdownH2Style
        3 -> MarkdownH3Style
        4 -> MarkdownH4Style
        5 -> MarkdownH5Style
        6 -> MarkdownH6Style
        else -> EditorContentStyle // Fallback to body text
    }
}
