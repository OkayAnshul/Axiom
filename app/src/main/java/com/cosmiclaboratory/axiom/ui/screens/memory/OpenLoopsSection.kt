package com.cosmiclaboratory.axiom.ui.screens.memory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.humanizedMemory
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.design.rememberLocalized
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Things I'll ask you about.
 *
 * Axiom has always noticed commitments — "my scan is on Tuesday" becomes an
 * EVENT memory with a follow-up date, from a keyless on-device detector or from
 * the extraction model. Until now none of that was visible: the app silently
 * promised to ask, then asked once, days later, and closed the loop. The first
 * a user knew of it was a notification.
 *
 * A commitment made on someone's behalf should be visible before it comes due,
 * and refusable. That is the whole of this section.
 */
@Composable
fun OpenLoopsSection(
    loops: List<MemoryItem>,
    onDismissLoop: (MemoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (loops.isEmpty()) return
    val c = AxiomTheme.colors

    Column(modifier.fillMaxWidth().testTag("section:openLoops")) {
        Text(
            "Things I'll ask you about",
            style = AxiomTheme.type.uiTitle,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(
            "You mentioned these in passing. I'll bring them up once, then let them go.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.md))

        loops.forEach { loop ->
            AxiomCard(tone = CardTone.Ai, modifier = Modifier.padding(bottom = AxiomTheme.space.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            loop.text.humanizedMemory(),
                            style = AxiomTheme.type.readingBody,
                            color = c.ink
                        )
                        loop.dueAt?.let { due ->
                            Spacer(Modifier.height(AxiomTheme.space.xxs))
                            Text(
                                whenPhrase(due),
                                style = AxiomTheme.type.uiMeta,
                                color = c.inkFaint
                            )
                        }
                    }
                    AxiomIconButton(
                        icon = Icons.Outlined.Close,
                        label = "Don't ask about this",
                        onClick = { onDismissLoop(loop) },
                        tint = c.inkFaint
                    )
                }
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.lg))
    }
}

/**
 * A due date said the way a person would say it. "Thursday" and "tomorrow" are
 * how you'd mention a follow-up out loud; "2026-08-06T09:00" is a timestamp.
 */
@Composable
private fun whenPhrase(due: LocalDateTime): String {
    val weekday = rememberLocalized { locale -> DateTimeFormatter.ofPattern("EEEE", locale) }
    val dayMonth = rememberLocalized { locale -> DateTimeFormatter.ofPattern("d MMMM", locale) }
    val today = LocalDate.now()
    val dueDate = due.toLocalDate()
    val days = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        days < 0L -> "I've been meaning to ask"
        days == 0L -> "I'll ask today"
        days == 1L -> "I'll ask tomorrow"
        days < 7L -> "I'll ask on ${dueDate.format(weekday)}"
        else -> "I'll ask on ${dueDate.format(dayMonth)}"
    }
}
