package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors - Deep blue inspired by knowledge and focus
val AxiomBlue80 = Color(0xFF4A90E2)
val AxiomBlue60 = Color(0xFF2E5BDA)
val AxiomBlue40 = Color(0xFF1A365D)
val AxiomBlue20 = Color(0xFF000000)  // Pure black for all-black theme

// Secondary colors - Warm accent for highlights and interactions
val AxiomAmber80 = Color(0xFFFFB74D)
val AxiomAmber60 = Color(0xFFFF9800)
val AxiomAmber40 = Color(0xFFE65100)

// Neutral colors for text and backgrounds
val AxiomGray90 = Color(0xFFF5F5F5)
val AxiomGray80 = Color(0xFFE0E0E0)
val AxiomGray60 = Color(0xFF9E9E9E)
val AxiomGray40 = Color(0xFF616161)
val AxiomGray20 = Color(0xFF000000)  // Pure black for all-black theme

// Status colors
val AxiomGreen = Color(0xFF4CAF50)
val AxiomRed = Color(0xFFF44336)
val AxiomOrange = Color(0xFFFF9800)

// Tag colors for note categorization
val TagBlue = Color(0xFF2196F3)
val TagGreen = Color(0xFF4CAF50)
val TagOrange = Color(0xFFFF9800)
val TagPurple = Color(0xFF9C27B0)
val TagRed = Color(0xFFF44336)
val TagTeal = Color(0xFF009688)

// Modern Editor Color Palette - All-black theme with precise borders
val EditorBackground = Color(0xFF000000)        // Pure black for OLED-friendly display
val EditorSurface = Color(0xFF0A0A0A)           // Very subtle elevation for editor panes
val EditorSurfaceVariant = Color(0xFF121212)    // Input field backgrounds with minimal lift
val EditorBorder = Color(0xFF404040)            // Enhanced borders for precise component separation
val EditorFocus = Color(0xFF4A90E2)             // Focus indicators and active states
val EditorText = Color(0xFFE8E8E8)              // Primary text color (high contrast on black)
val EditorTextSecondary = Color(0xFFB0B0B0)     // Secondary text and metadata
val EditorTextTertiary = Color(0xFF808080)      // Placeholder text and hints
val EditorAccent = Color(0xFF64B5F6)            // Highlights and selection
val EditorSuccess = Color(0xFF66BB6A)           // Success states and positive indicators
val EditorWarning = Color(0xFFFFB74D)           // Warning states
val EditorError = Color(0xFFEF5350)             // Error states

// Light theme editor colors
val EditorBackgroundLight = Color(0xFFFAFAFA)   // Clean white-gray background
val EditorSurfaceLight = Color(0xFFFFFFFF)      // Pure white surfaces
val EditorSurfaceVariantLight = Color(0xFFF5F5F5) // Light gray input backgrounds
val EditorBorderLight = Color(0xFFE0E0E0)       // Light borders
val EditorFocusLight = Color(0xFF2E5BDA)        // Blue focus indicators
val EditorTextLight = Color(0xFF1A1A1A)         // Dark text on light background
val EditorTextSecondaryLight = Color(0xFF666666) // Gray secondary text
val EditorTextTertiaryLight = Color(0xFF999999) // Light gray placeholders

// Modern Formatting Toolbar Colors - Premium golden selection states
val FormatGoldenPrimary = Color(0xFFD4AF37)     // Rich gold for active states
val FormatGoldenSecondary = Color(0xFFFFB74D)   // Lighter amber for hover/focus
val FormatGoldenSurface = Color(0x1AD4AF37)     // Subtle golden background (10% opacity)
val FormatGoldenSurfaceVariant = Color(0x33D4AF37) // Medium golden background (20% opacity)

// Quick Tile Colors - All-black theme with precise borders
val QuickTileBackground = Color(0xFF0A0A0A)     // Near-black surface for quick tiles
val QuickTileBackgroundLight = Color(0xFFF8F8F8) // Light surface for quick tiles
val QuickTileActive = FormatGoldenPrimary       // Golden active state
val QuickTileActiveText = Color(0xFF000000)     // Pure black text on golden background
val QuickTileInactive = Color(0xFF1A1A1A)      // Black inactive background with subtle lift
val QuickTileInactiveLight = Color(0xFFEEEEEE)  // Light inactive background
val QuickTileText = Color(0xFFE8E8E8)          // Standard text on dark tiles
val QuickTileTextLight = Color(0xFF1A1A1A)     // Standard text on light tiles

// Bottom Sheet Format Panel Colors - All-black theme with precise dividers
val FormatPanelBackground = Color(0xFF000000)   // Pure black background for format panel
val FormatPanelBackgroundLight = Color(0xFFFFFFFF) // Light background for format panel
val FormatPanelSurface = Color(0xFF0A0A0A)      // Near-black surface for format options
val FormatPanelSurfaceLight = Color(0xFFF5F5F5) // Light surface for format options
val FormatPanelSelectedText = Color(0xFF000000) // Pure black text on golden selection
val FormatPanelText = Color(0xFFE8E8E8)         // Regular text in format panel
val FormatPanelTextLight = Color(0xFF1A1A1A)    // Regular text in light format panel
val FormatPanelDivider = Color(0xFF404040)      // Enhanced divider lines for precise separation
val FormatPanelDividerLight = Color(0xFFE0E0E0) // Light divider lines

// Legacy colors (keeping for compatibility)
val Purple80 = AxiomBlue80
val PurpleGrey80 = AxiomGray80
val Pink80 = AxiomAmber80

val Purple40 = AxiomBlue40
val PurpleGrey40 = AxiomGray40
val Pink40 = AxiomAmber40