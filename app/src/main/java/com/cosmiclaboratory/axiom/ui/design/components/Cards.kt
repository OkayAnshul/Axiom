package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.design.currentLocale
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CardTone { Neutral, Accent, Ai, Caution }

/**
 * The base surface.
 *
 * Elevation policy lives here rather than at call sites, because it differs by
 * theme: on dark, a shadow against near-black is invisible, so cards separate
 * with a hairline and a surface step; on light, a small shadow plus a hairline
 * reads correctly. Encoding it once means no screen can get it wrong.
 */
@Composable
fun AxiomCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Neutral,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AxiomTheme.colors
    val container = when (tone) {
        CardTone.Neutral -> c.surface
        CardTone.Accent -> c.accentSoft
        CardTone.Ai -> c.aiTintSoft
        CardTone.Caution -> c.cautionSoft
    }
    val border = when (tone) {
        CardTone.Neutral -> c.hairline
        CardTone.Accent -> c.accent.copy(alpha = 0.25f)
        CardTone.Ai -> c.aiTint.copy(alpha = 0.25f)
        CardTone.Caution -> c.caution.copy(alpha = 0.30f)
    }

    val shaped = modifier
        .then(if (c.isDark) Modifier else Modifier.shadow(2.dp, AxiomTheme.shapes.md, clip = false))
        .clip(AxiomTheme.shapes.md)
        .background(container)
        .border(1.dp, border, AxiomTheme.shapes.md)
        .then(
            if (onClick != null) {
                Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            } else Modifier
        )

    Column(modifier = shaped.padding(AxiomTheme.space.cardPadding), content = content)
}

/**
 * One entry in a list.
 *
 * Uses `mergeDescendants` so a screen reader announces the whole card as a
 * single node — "March 12, Slept badly, 240 words, mood Good" — instead of
 * walking five separate unlabelled fragments.
 */
@Composable
fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showKind: Boolean = true
) {
    val c = AxiomTheme.colors
    val t = AxiomTheme.type
    val locale = currentLocale()
    val time = remember(entry.createdAt, locale) {
        entry.createdAt.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    }
    val snippet = remember(entry.content) {
        entry.content.lineSequence().filter { it.isNotBlank() }.drop(if (entry.title.isBlank()) 1 else 0)
            .joinToString(" ").take(160)
    }

    AxiomCard(
        modifier = modifier.semantics(mergeDescendants = true) { },
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.displayTitle.ifBlank { "Untitled" },
                style = t.uiTitle,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (entry.isFavorite) {
                Spacer(Modifier.width(AxiomTheme.space.sm))
                MoodDot(mood = null, size = 8.dp, color = c.caution)
            }
        }

        if (snippet.isNotBlank()) {
            Spacer(Modifier.height(AxiomTheme.space.xs))
            Text(
                text = snippet,
                style = t.readingBody.copy(fontSize = t.uiBody.fontSize),
                color = c.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(AxiomTheme.space.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)
        ) {
            Text(time, style = t.uiMeta, color = c.inkFaint)
            if (entry.wordCount > 0) {
                Text("·", style = t.uiMeta, color = c.inkFaint)
                Text("${entry.wordCount} words", style = t.uiMeta, color = c.inkFaint)
            }
            if (entry.mood != null) {
                Text("·", style = t.uiMeta, color = c.inkFaint)
                MoodDot(mood = entry.mood, size = 10.dp)
            }
            if (showKind && entry.kind != EntryKind.FREE_FORM) {
                Spacer(Modifier.weight(1f))
                KindBadge(entry.kind)
            }
        }
    }
}

@Composable
private fun KindBadge(kind: EntryKind) {
    val c = AxiomTheme.colors
    val text = when (kind) {
        EntryKind.PROMPTED -> "Prompted"
        EntryKind.VOICE -> "Voice"
        EntryKind.FREE_FORM -> return
    }
    Text(
        text = text,
        style = AxiomTheme.type.uiLabelSmall,
        color = c.inkMuted,
        modifier = Modifier
            .clip(AxiomTheme.shapes.xs)
            .background(c.surfaceSunken)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Placeholder while entries load. Static — a calm app should not shimmer. */
@Composable
fun EntryCardSkeleton(modifier: Modifier = Modifier) {
    val c = AxiomTheme.colors
    AxiomCard(modifier = modifier) {
        SkeletonBar(widthFraction = 0.6f, height = 18.dp)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        SkeletonBar(widthFraction = 0.95f, height = 13.dp)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        SkeletonBar(widthFraction = 0.8f, height = 13.dp)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        SkeletonBar(widthFraction = 0.3f, height = 11.dp)
    }
}

@Composable
fun SkeletonBar(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(AxiomTheme.colors.surfaceSunken)
    )
}

/** Compact reference to an entry — AI citations, "entries like this". */
@Composable
fun EntryChip(entry: Entry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AxiomTheme.colors
    val locale = currentLocale()
    val date = remember(entry.createdAt, locale) {
        entry.createdAt.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }
    Row(
        modifier = modifier
            .clip(AxiomTheme.shapes.xs)
            .background(c.surfaceSunken)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = AxiomTheme.space.sm, vertical = 6.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(date, style = AxiomTheme.type.uiMeta, color = c.inkFaint)
        Spacer(Modifier.width(6.dp))
        Text(
            text = entry.displayTitle.ifBlank { "Untitled" },
            style = AxiomTheme.type.uiLabelSmall,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

/**
 * A settings row that always shows its CURRENT VALUE, so nothing is hidden
 * behind a bare chevron.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = AxiomTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = AxiomTheme.space.screenH, vertical = AxiomTheme.space.md)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = c.inkMuted)
        Spacer(Modifier.width(AxiomTheme.space.base))
        Column(Modifier.weight(1f)) {
            Text(title, style = AxiomTheme.type.uiBody, color = c.ink)
            if (!summary.isNullOrBlank()) {
                Text(summary, style = AxiomTheme.type.uiBodySmall, color = c.inkFaint)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(currentLocale()),
            style = AxiomTheme.type.uiOverline,
            color = AxiomTheme.colors.inkFaint,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}
