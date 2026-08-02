package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.LocalDate

/**
 * Streak summary: current, longest, and the last seven days.
 *
 * The dot row is decorative — the stateDescription on the container carries the
 * meaning, so TalkBack says "4 day streak, 5 of the last 7 days" instead of
 * reading seven anonymous shapes.
 */
@Composable
fun StreakStrip(
    result: StreakCalculator.Result,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val c = AxiomTheme.colors
    val activeDays = result.lastSevenDays.count { it }

    Row(
        modifier = modifier.semantics {
            stateDescription =
                "${result.current} day streak. $activeDays of the last 7 days written. " +
                    "Longest ${result.longest} days."
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.md)
    ) {
        Column {
            Text("${result.current}", style = AxiomTheme.type.uiNumeric, color = c.ink)
            Text("day streak", style = AxiomTheme.type.uiLabelSmall, color = c.inkFaint)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.clearAndSetSemantics { }
        ) {
            result.lastSevenDays.forEach { written ->
                Box(
                    Modifier
                        .size(if (compact) 8.dp else 10.dp)
                        .clip(AxiomTheme.shapes.full)
                        .background(if (written) c.accent else c.surfaceSunken)
                )
            }
        }
        if (!compact && result.longest > 0) {
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("${result.longest}", style = AxiomTheme.type.uiNumeric, color = c.inkMuted)
                Text("longest", style = AxiomTheme.type.uiLabelSmall, color = c.inkFaint)
            }
        }
    }
}

/** GitHub-style density grid over the last [weeks] weeks. */
@Composable
fun ContributionGrid(
    dates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    today: LocalDate = LocalDate.now(),
    onDayClick: ((LocalDate) -> Unit)? = null
) {
    val c = AxiomTheme.colors
    val start = today.minusWeeks(weeks.toLong() - 1).with(java.time.DayOfWeek.MONDAY)

    Column(
        modifier = modifier.semantics {
            contentDescription = "${dates.size} days written in the last $weeks weeks"
        },
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(7) { dow ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(weeks) { week ->
                    val date = start.plusWeeks(week.toLong()).plusDays(dow.toLong())
                    val future = date.isAfter(today)
                    val written = date in dates
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(AxiomTheme.shapes.xs)
                            .background(
                                when {
                                    future -> c.surfaceSunken.copy(alpha = 0.4f)
                                    written -> c.accent
                                    else -> c.surfaceSunken
                                }
                            )
                            .then(
                                if (onDayClick != null && !future) {
                                    Modifier.clickable { onDayClick(date) }
                                } else Modifier
                            )
                    )
                }
            }
        }
    }
}

/**
 * Line chart over an optionally-sparse series.
 *
 * [points] accepts nulls and renders them as GAPS, not zeros. That distinction
 * is the whole point for mood: a day with no entry means "unknown", and drawing
 * it as 0 would invent a crash the user never had.
 */
@Composable
fun Sparkline(
    points: List<Float?>,
    modifier: Modifier = Modifier,
    minValue: Float = 1f,
    maxValue: Float = 5f
) {
    val c = AxiomTheme.colors
    val known = points.count { it != null }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                contentDescription = "Trend over ${points.size} days, $known recorded"
            }
    ) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val range = (maxValue - minValue).coerceAtLeast(0.001f)

        fun yFor(v: Float) = size.height - ((v - minValue) / range) * size.height

        // Baseline so a sparse series still reads as a chart.
        drawLine(
            color = c.hairline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f
        )

        // Draw each contiguous run of known values as its own path; gaps stay gaps.
        var path: Path? = null
        points.forEachIndexed { i, value ->
            if (value == null) {
                path?.let { drawPath(it, c.accent, style = Stroke(width = 2.5f, cap = StrokeCap.Round)) }
                path = null
            } else {
                val x = i * stepX
                val y = yFor(value.coerceIn(minValue, maxValue))
                if (path == null) {
                    path = Path().apply { moveTo(x, y) }
                } else {
                    path!!.lineTo(x, y)
                }
                drawCircle(c.accent, radius = 2.5f, center = Offset(x, y))
            }
        }
        path?.let { drawPath(it, c.accent, style = Stroke(width = 2.5f, cap = StrokeCap.Round)) }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: Float? = null
) {
    val c = AxiomTheme.colors
    Column(
        modifier = modifier
            .clip(AxiomTheme.shapes.md)
            .background(c.surfaceSunken)
            .padding(AxiomTheme.space.md)
            .semantics {
                stateDescription = "$label: $value" +
                    (delta?.let { ", ${if (it >= 0) "up" else "down"} ${"%.0f".format(kotlin.math.abs(it))} percent" } ?: "")
            }
    ) {
        Text(label, style = AxiomTheme.type.uiLabelSmall, color = c.inkFaint)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AxiomTheme.type.uiNumeric.copy(fontSize = AxiomTheme.type.uiTitleLarge.fontSize), color = c.ink)
            if (delta != null) {
                Spacer(Modifier.width(AxiomTheme.space.xs))
                Text(
                    text = (if (delta >= 0) "+" else "") + "%.0f%%".format(delta),
                    style = AxiomTheme.type.uiMeta,
                    color = if (delta >= 0) c.positive else c.critical
                )
            }
        }
    }
}

/**
 * Container for one Patterns module.
 *
 * [onInfo] exists so every statistical claim can explain itself in plain
 * language. A pattern the user cannot interrogate is just a horoscope.
 */
@Composable
fun PatternCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onInfo: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AxiomCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink)
                if (subtitle != null) {
                    Text(subtitle, style = AxiomTheme.type.uiBodySmall, color = AxiomTheme.colors.inkFaint)
                }
            }
            if (onInfo != null) {
                AxiomIconButton(
                    icon = Icons.Outlined.Info,
                    label = "How this is calculated",
                    onClick = onInfo,
                    tint = AxiomTheme.colors.inkFaint
                )
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.md))
        content()
    }
}
