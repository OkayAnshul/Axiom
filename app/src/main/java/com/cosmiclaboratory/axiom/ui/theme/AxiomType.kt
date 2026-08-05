package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.cosmiclaboratory.axiom.R

/*
 * Three families, split by job:
 *
 *   Fraunces — the greeting, and nothing else. A serif whose SOFT and WONK axes
 *              exist specifically to make letterforms warmer and less mechanical;
 *              it is what makes "Good evening" read as a person speaking rather
 *              than a heading.
 *   Literata — anything the user reads or writes. A serif designed for long-form
 *              screen reading; it gives entries the weight of a page rather than
 *              of a settings screen.
 *   Figtree  — chrome. Labels, buttons, metadata. Geometric with soft terminals,
 *              which keeps controls quiet next to two serifs.
 *
 * All three are bundled variable fonts (OFL). Variable axes need API 26, which is
 * exactly our minSdk — so this must be smoke-tested on an API 26 emulator. If it
 * misbehaves there, the fallback is static instances per family.
 */

@OptIn(ExperimentalTextApi::class)
private fun literata(weight: Int, italic: Boolean = false) = Font(
    resId = if (italic) R.font.literata_variable_italic else R.font.literata_variable,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: Int) = Font(
    resId = R.font.figtree_variable,
    weight = FontWeight(weight),
    // Figtree's wght axis starts at 300; asking for less silently clamps.
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.coerceAtLeast(300)))
)

/**
 * Fraunces ships with hostile defaults — `wght` 900, `opsz` 9, `WONK` 1 — so
 * every axis is set explicitly here. Omitting any one of them renders ultra-bold
 * text cut for six-point captions, which is exactly the failure that looks like
 * "the font didn't load".
 *
 * `opsz` should track the size the style is actually used at: high for display,
 * low for anything approaching text.
 */
@OptIn(ExperimentalTextApi::class)
private fun frauncesFamily(
    weight: Int,
    opticalSize: Float,
    soft: Float,
    wonk: Float
) = FontFamily(
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("opsz", opticalSize),
            FontVariation.weight(weight),
            FontVariation.Setting("SOFT", soft),
            FontVariation.Setting("WONK", wonk)
        )
    )
)

val LiterataFamily = FontFamily(
    literata(400), literata(500), literata(600), literata(700),
    literata(400, italic = true), literata(500, italic = true)
)

val FigtreeFamily = FontFamily(
    figtree(400), figtree(500), figtree(600), figtree(700)
)

/**
 * The greeting. Softened, and wonky enough to feel handwritten at 28sp.
 *
 * `opsz` tracks that 28sp rather than sitting at display height. Cut for 48pt,
 * the letterforms carry display contrast — hairline thins against thick stems —
 * which at greeting size reads as spindly rather than warm.
 */
val FrauncesGreetingFamily = frauncesFamily(weight = 500, opticalSize = 32f, soft = 60f, wonk = 1f)

/** The line beneath it. Same warmth, no wonk — it is read, not glanced at. */
val FrauncesLeadFamily = frauncesFamily(weight = 400, opticalSize = 24f, soft = 50f, wonk = 0f)

/**
 * Line height should shrink the gap above the first line and below the last,
 * otherwise generous reading leading leaves cards looking top-heavy.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun reading(
    size: Int,
    lineHeight: Int,
    weight: Int = 400,
    tracking: Float = 0f,
    italic: Boolean = false
) = TextStyle(
    fontFamily = LiterataFamily,
    fontWeight = FontWeight(weight),
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight
)

private fun ui(
    size: Int,
    lineHeight: Int,
    weight: Int = 400,
    tracking: Float = 0f
) = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight
)

private fun greetingStyle(
    family: FontFamily,
    size: Int,
    lineHeight: Int,
    weight: Int,
    tracking: Float
) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight
)

@Immutable
data class AxiomTypography(
    // ---- the greeting (Fraunces) ----
    /**
     * "Good evening, Anshul". The one place the app raises its voice — but it
     * raises it, it does not shout. At 34sp this was the largest thing on screen
     * by a wide margin and took the spotlight from the conversation, which is
     * what the app is actually for.
     */
    val greeting: TextStyle = greetingStyle(FrauncesGreetingFamily, 28, 36, 500, -0.4f),
    /** "I've been wondering how today treated you." */
    val greetingLead: TextStyle = greetingStyle(FrauncesLeadFamily, 20, 30, 400, -0.1f),

    // ---- reading (Literata) ----
    val promptDisplay: TextStyle = reading(24, 32, 500, -0.1f),
    val readingTitle: TextStyle = reading(28, 34, 600, -0.2f),
    val readingSubtitle: TextStyle = reading(20, 28, 500),
    val readingLead: TextStyle = reading(19, 30, 400),
    /** The core body style. Everything else exists to frame this. */
    val readingBody: TextStyle = reading(17, 30, 400, 0.1f),
    val readingBodyLoose: TextStyle = reading(17, 33, 400, 0.1f),
    val readingQuote: TextStyle = reading(17, 28, 400, italic = true),

    // ---- markdown headers inside reading content ----
    val mdH1: TextStyle = reading(26, 32, 700, -0.2f),
    val mdH2: TextStyle = reading(22, 28, 700, -0.1f),
    val mdH3: TextStyle = reading(19, 26, 600),
    val mdH4: TextStyle = reading(17, 24, 600),
    val mdH5: TextStyle = ui(15, 22, 600),
    val mdH6: TextStyle = ui(14, 20, 600, 0.5f),

    // ---- chrome (Inter) ----
    val uiDisplay: TextStyle = ui(32, 38, 600, -0.4f),
    val uiTitleLarge: TextStyle = ui(22, 28, 600, -0.2f),
    val uiTitle: TextStyle = ui(17, 24, 600, -0.1f),
    val uiTitleSmall: TextStyle = ui(15, 20, 600),
    val uiBody: TextStyle = ui(15, 24, 400),
    val uiBodySmall: TextStyle = ui(13, 18, 400),
    val uiLabel: TextStyle = ui(14, 18, 500, 0.1f),
    val uiLabelSmall: TextStyle = ui(12, 16, 500, 0.3f),
    val uiMeta: TextStyle = ui(12, 16, 500, 0.2f),
    val uiOverline: TextStyle = ui(11, 14, 600, 0.8f),

    /**
     * Tabular figures. Without this, a streak counter ticking 9 -> 10 visibly
     * jitters because proportional digits have different advances.
     */
    val uiNumeric: TextStyle = ui(15, 20, 600).copy(
        fontFeatureSettings = "tnum"
    ),

    val codeInline: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    val codeBlock: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
) {
    fun markdownHeader(level: Int): TextStyle = when (level) {
        1 -> mdH1
        2 -> mdH2
        3 -> mdH3
        4 -> mdH4
        5 -> mdH5
        else -> mdH6
    }

    /**
     * Applies the in-app reading-size preference. Deliberately scales ONLY the
     * reading styles — chrome stays put so controls don't reflow when someone
     * just wants larger prose. Multiplies with the OS font scale rather than
     * replacing it.
     */
    fun scaledForReading(factor: Float): AxiomTypography {
        if (factor == 1f) return this
        fun TextStyle.s() = copy(
            fontSize = fontSize * factor,
            lineHeight = lineHeight * factor
        )
        return copy(
            greeting = greeting.s(),
            greetingLead = greetingLead.s(),
            promptDisplay = promptDisplay.s(),
            readingTitle = readingTitle.s(),
            readingSubtitle = readingSubtitle.s(),
            readingLead = readingLead.s(),
            readingBody = readingBody.s(),
            readingBodyLoose = readingBodyLoose.s(),
            readingQuote = readingQuote.s(),
            mdH1 = mdH1.s(), mdH2 = mdH2.s(), mdH3 = mdH3.s(),
            mdH4 = mdH4.s(), mdH5 = mdH5.s(), mdH6 = mdH6.s(),
            codeInline = codeInline.s(), codeBlock = codeBlock.s()
        )
    }
}

/**
 * Material's slots, filled from ours, so stock Button / AlertDialog / ListItem
 * pick up the right family without every call site restyling them.
 */
fun AxiomTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = uiDisplay,
    displayMedium = uiDisplay,
    displaySmall = uiTitleLarge,
    headlineLarge = readingTitle,
    headlineMedium = readingSubtitle,
    headlineSmall = uiTitleLarge,
    titleLarge = uiTitleLarge,
    titleMedium = uiTitle,
    titleSmall = uiTitleSmall,
    bodyLarge = readingBody,
    bodyMedium = uiBody,
    bodySmall = uiBodySmall,
    labelLarge = uiLabel,
    labelMedium = uiLabel,
    labelSmall = uiLabelSmall
)
