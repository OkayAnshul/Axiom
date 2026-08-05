package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.ui.design.components.AxiomBottomSheet
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * One door to everywhere that is not the conversation.
 *
 * This replaces a three-tab bottom bar. A permanent nav bar is a claim that
 * Journal and Patterns are peers of the conversation, competing for attention at
 * all times; they are not — they are things the two of you have made, which you
 * visit occasionally. Removing the bar gives the conversation the whole screen,
 * which is the point of the app.
 *
 * Discoverability is preserved the way the old nav bar preserved it: every row
 * is labelled, and each carries a sentence saying what is behind it. That is
 * strictly more legible than three icons with one word each — the failure mode
 * of hidden navigation is unlabelled navigation, not absent chrome.
 */
@Composable
fun ShelfSheet(
    onDismiss: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenTalks: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
    onDisclosure: () -> Unit,
    /** Null when there is no conversation to clear. */
    onClearConversation: (() -> Unit)? = null
) {
    AxiomBottomSheet(title = "Where would you like to go?", onDismiss = onDismiss) {
        // Dismiss first so the sheet is already leaving as the destination
        // arrives; navigating out from under a live sheet leaves it stranded.
        fun go(action: () -> Unit): () -> Unit = {
            onDismiss()
            action()
        }

        ShelfRow(
            title = "Your story",
            body = "Everything we've written",
            onClick = go(onOpenJournal)
        )
        ShelfRow(
            title = "Our talks",
            body = "Every conversation, by the day it happened",
            onClick = go(onOpenTalks)
        )
        ShelfRow(
            title = "What I've noticed",
            body = "Gently, and never certain",
            onClick = go(onOpenPatterns)
        )
        ShelfRow(
            title = "What I remember",
            body = "Yours to edit or forget",
            onClick = go(onOpenMemories)
        )
        ShelfRow(
            title = "Your space",
            body = "Voice, appearance, and what leaves this phone",
            onClick = go(onOpenSettings)
        )

        if (onClearConversation != null) {
            Spacer(Modifier.height(AxiomTheme.space.sm))
            Text(
                text = "Start the conversation over",
                style = AxiomTheme.type.uiBodySmall,
                color = AxiomTheme.colors.critical,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AxiomTheme.shapes.md)
                    .clickable(onClick = go(onClearConversation))
                    .padding(
                        horizontal = AxiomTheme.space.sm,
                        vertical = AxiomTheme.space.md
                    )
            )
        }

        Spacer(Modifier.height(AxiomTheme.space.base))
        // Against three cloud-subscription competitors this is the difference,
        // so it stays one tap from home rather than buried in settings.
        Text(
            text = "What gets sent?",
            style = AxiomTheme.type.uiLabelSmall,
            color = AxiomTheme.colors.inkFaint,
            modifier = Modifier
                .clip(AxiomTheme.shapes.sm)
                .clickable(onClick = go(onDisclosure))
                .padding(
                    horizontal = AxiomTheme.space.xs,
                    vertical = AxiomTheme.space.sm
                )
        )
    }
}

@Composable
private fun ShelfRow(
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AxiomTheme.shapes.md)
            .clickable(onClick = onClick)
            .padding(
                horizontal = AxiomTheme.space.sm,
                vertical = AxiomTheme.space.md
            )
            .testTag("shelf:$title")
    ) {
        Text(title, style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink)
        Spacer(Modifier.height(AxiomTheme.space.xxs))
        Text(body, style = AxiomTheme.type.uiBodySmall, color = AxiomTheme.colors.inkMuted)
    }
}
