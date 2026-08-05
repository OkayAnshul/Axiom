package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min

/**
 * Debug-only inventory of every design token.
 *
 * Debug source set on purpose: it must never ship, and keeping it out of `main`
 * means R8 has nothing to strip.
 *
 * The contrast column is the point. Ratios are computed, not eyeballed, so a
 * palette tweak that quietly drops body text under 4.5:1 shows up as a red FAIL
 * instead of surviving to production.
 */

/** WCAG 2.1 relative-luminance contrast ratio. */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

@Composable
private fun ContrastRow(label: String, fg: Color, bg: Color, large: Boolean = false) {
    val ratio = contrastRatio(fg, bg)
    val required = if (large) 3.0f else 4.5f
    val pass = ratio >= required
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = fg, style = AxiomTheme.type.uiBodySmall, modifier = Modifier.weight(1f))
        Text(
            text = "${"%.2f".format(ratio)}:1 ${if (pass) "PASS" else "FAIL"}",
            color = if (pass) AxiomTheme.colors.positive else AxiomTheme.colors.critical,
            style = AxiomTheme.type.uiNumeric
        )
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(modifier = Modifier.width(96.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(color, RoundedCornerShape(8.dp))
                .border(1.dp, AxiomTheme.colors.hairline, RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(name, style = AxiomTheme.type.uiLabelSmall, color = AxiomTheme.colors.inkMuted)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = AxiomTheme.type.uiOverline,
        color = AxiomTheme.colors.inkFaint,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun TokenGallery(modifier: Modifier = Modifier) {
    val c = AxiomTheme.colors
    val t = AxiomTheme.type

    val typeSamples = listOf<Pair<String, TextStyle>>(
        // Fraunces first: its axes are set explicitly, and a regression there
        // shows up as ultra-bold text rather than as a missing font.
        "greeting (Fraunces)" to t.greeting,
        "greetingLead (Fraunces)" to t.greetingLead,
        "promptDisplay" to t.promptDisplay,
        "readingTitle" to t.readingTitle,
        "readingLead" to t.readingLead,
        "readingBody" to t.readingBody,
        "readingQuote" to t.readingQuote,
        "mdH1" to t.mdH1,
        "mdH3" to t.mdH3,
        "uiDisplay" to t.uiDisplay,
        "uiTitle" to t.uiTitle,
        "uiBody" to t.uiBody,
        "uiLabel" to t.uiLabel,
        "uiMeta" to t.uiMeta,
        "uiNumeric 0123456789" to t.uiNumeric,
        "codeInline" to t.codeInline
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item { Text("Axiom tokens", style = t.uiDisplay, color = c.ink) }

        item { SectionHeader("Surfaces") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("canvas", c.canvas); Swatch("surface", c.surface)
                Swatch("raised", c.surfaceRaised); Swatch("sunken", c.surfaceSunken)
            }
        }

        item { SectionHeader("Accent / AI") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("accent", c.accent); Swatch("accentSoft", c.accentSoft)
                Swatch("aiTint", c.aiTint); Swatch("aiTintSoft", c.aiTintSoft)
            }
        }

        item { SectionHeader("Status") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("positive", c.positive); Swatch("caution", c.caution)
                Swatch("critical", c.critical); Swatch("highlight", c.highlight)
            }
        }

        item { SectionHeader("Mood ramp") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { Swatch("mood$it", c.mood(it)) }
            }
        }

        item { SectionHeader("Contrast (WCAG AA)") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ContrastRow("ink on canvas", c.ink, c.canvas)
                ContrastRow("ink on surface", c.ink, c.surface)
                ContrastRow("inkMuted on canvas", c.inkMuted, c.canvas)
                ContrastRow("inkMuted on surface", c.inkMuted, c.surface)
                ContrastRow("inkFaint on canvas", c.inkFaint, c.canvas)
                ContrastRow("inkFaint on surface", c.inkFaint, c.surface)
                // sunken is the tightest background for faint text and was where
                // the first pass of this palette failed; keep it checked.
                ContrastRow("inkFaint on sunken", c.inkFaint, c.surfaceSunken)
                ContrastRow("onAccent on accent", c.onAccent, c.accent)
                ContrastRow("onAccentSoft on accentSoft", c.onAccentSoft, c.accentSoft)
                ContrastRow("onAiTintSoft on aiTintSoft", c.onAiTintSoft, c.aiTintSoft)
                ContrastRow("aiTint on canvas", c.aiTint, c.canvas)
                ContrastRow("critical on canvas", c.critical, c.canvas)
                ContrastRow("positive on canvas", c.positive, c.canvas)
                ContrastRow("caution on canvas", c.caution, c.canvas)
                ContrastRow("outline on canvas (UI)", c.outline, c.canvas, large = true)
                ContrastRow("outline on surface (UI)", c.outline, c.surface, large = true)
            }
        }

        item { SectionHeader("Type scale") }
        items(typeSamples) { (name, style) ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(name, style = t.uiLabelSmall, color = c.inkFaint)
                Text("The quick brown fox jumps", style = style, color = c.ink)
            }
        }

        item { SectionHeader("Spacing") }
        item {
            val s = AxiomTheme.space
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "xs" to s.xs, "sm" to s.sm, "md" to s.md, "base" to s.base,
                    "lg" to s.lg, "xl" to s.xl, "xxl" to s.xxl
                ).forEach { (name, dp) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = t.uiLabelSmall, color = c.inkMuted, modifier = Modifier.width(40.dp))
                        Box(Modifier.height(12.dp).width(dp).background(c.accent, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text("$dp", style = t.uiNumeric, color = c.inkFaint)
                    }
                }
            }
        }

        item { SectionHeader("Shapes") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sh = AxiomTheme.shapes
                listOf("xs" to sh.xs, "sm" to sh.sm, "md" to sh.md, "lg" to sh.lg, "xl" to sh.xl)
                    .forEach { (name, shape) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).background(c.accentSoft, shape))
                            Text(name, style = t.uiLabelSmall, color = c.inkMuted)
                        }
                    }
            }
        }
    }
}

/*
 * Every anchor AND the blends between them.
 *
 * The adaptive palette spends most of the day rendering a point *between* two
 * anchors, and interpolating between two AA-passing colours does not guarantee
 * an AA-passing midpoint — nothing in the maths says the ratio moves
 * monotonically. So the 11:00 and 20:30 previews below are not decoration: they
 * are the cases that actually failed on the first pass of this palette, and the
 * contrast section must read PASS in all of them.
 *
 * The hour is injected rather than mocked; see AxiomTheme's `clock` parameter.
 */
@Preview(name = "Morning 08:00", heightDp = 1700)
@Composable
private fun PreviewMorning() =
    AxiomTheme(themeMode = ThemeMode.LIGHT, clock = { LocalTime.of(8, 0) }) { TokenGallery() }

@Preview(name = "Morning~Day blend 11:00", heightDp = 1700)
@Composable
private fun PreviewMorningDayBlend() =
    AxiomTheme(themeMode = ThemeMode.LIGHT, clock = { LocalTime.of(11, 0) }) { TokenGallery() }

@Preview(name = "Day 14:00", heightDp = 1700)
@Composable
private fun PreviewDay() =
    AxiomTheme(themeMode = ThemeMode.LIGHT, clock = { LocalTime.of(14, 0) }) { TokenGallery() }

@Preview(name = "Dusk 19:00", heightDp = 1700)
@Composable
private fun PreviewDusk() =
    AxiomTheme(themeMode = ThemeMode.DARK, clock = { LocalTime.of(19, 0) }) { TokenGallery() }

@Preview(name = "Dusk~Night blend 20:30", heightDp = 1700)
@Composable
private fun PreviewDuskNightBlend() =
    AxiomTheme(themeMode = ThemeMode.DARK, clock = { LocalTime.of(20, 30) }) { TokenGallery() }

@Preview(name = "Night 00:00", heightDp = 1700)
@Composable
private fun PreviewNight() =
    AxiomTheme(themeMode = ThemeMode.DARK, clock = { LocalTime.of(0, 0) }) { TokenGallery() }

@Preview(name = "Midnight OLED", heightDp = 1700)
@Composable
private fun PreviewOled() = AxiomTheme(themeMode = ThemeMode.AMOLED) { TokenGallery() }

/** Adaptive off: the palette must hold still at whatever hour it is. */
@Preview(name = "Adaptive off, light", heightDp = 1700)
@Composable
private fun PreviewAdaptiveOff() =
    AxiomTheme(themeMode = ThemeMode.LIGHT, adaptiveLight = false) { TokenGallery() }

// 200% font scale — where clipped labels and broken layouts surface.
@Preview(name = "OLED @200% font", heightDp = 2400, fontScale = 2.0f)
@Composable
private fun PreviewLargeType() = AxiomTheme(themeMode = ThemeMode.AMOLED) { TokenGallery() }

// Reading-size preference at its maximum, on top of the OS scale.
@Preview(name = "Light, reading 1.4x", heightDp = 1800)
@Composable
private fun PreviewReadingScale() =
    AxiomTheme(themeMode = ThemeMode.LIGHT, readingScale = 1.4f) { TokenGallery() }
