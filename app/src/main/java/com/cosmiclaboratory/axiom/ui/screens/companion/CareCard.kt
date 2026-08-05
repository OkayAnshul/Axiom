package com.cosmiclaboratory.axiom.ui.screens.companion

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.domain.safety.CareLevel
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Shown when someone has plainly said they are struggling.
 *
 * Everything about this is deliberately quiet. It appears *below* the
 * conversation rather than over it, it never blocks sending, and it can be
 * dismissed with a word that isn't a refusal. Someone who has just said the
 * hardest thing they can say should not then have to fight a modal.
 *
 * It does not say "please seek help" or "you matter" — the first is a brush-off
 * and the second is a stranger's platitude. It says the numbers, once, and gets
 * out of the way. The companion itself does the talking.
 */
@Composable
fun CareCard(
    level: CareLevel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    val context = LocalContext.current

    AxiomCard(tone = CardTone.Ai, modifier = modifier.testTag("card:care")) {
        Text(
            text = when (level) {
                CareLevel.Acute -> "I'm glad you told me."
                CareLevel.Struggling -> "That sounds genuinely hard."
            },
            style = AxiomTheme.type.readingBody,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(
            text = when (level) {
                CareLevel.Acute ->
                    "I'm here and I'm not going anywhere. If you'd rather talk to a person " +
                        "tonight, these are free, confidential, and open right now."
                CareLevel.Struggling ->
                    "We can keep talking as long as you want. If it would help to say it to " +
                        "someone else too, these are free and confidential."
            },
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )

        Spacer(Modifier.height(AxiomTheme.space.md))
        HELPLINES.forEach { line ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(AxiomTheme.shapes.sm)
                    .clickable {
                        // ACTION_DIAL, never ACTION_CALL: it opens the dialer
                        // with the number filled in and lets them decide. An app
                        // that places the call itself has taken the choice away.
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${line.number}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    .padding(vertical = AxiomTheme.space.sm)
            ) {
                Column {
                    Text(line.name, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                    Text(
                        "${line.number} · ${line.note}",
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(AxiomTheme.space.xs))
        TextButton(onClick = onDismiss) {
            Text("Not now", style = AxiomTheme.type.uiLabel, color = c.inkFaint)
        }
    }
}

private data class Helpline(val name: String, val number: String, val note: String)

/**
 * India-first, matching where this app is being built and used. All three are
 * free and confidential; Tele-MANAS is the government's 24/7 national line.
 */
private val HELPLINES = listOf(
    Helpline("Tele-MANAS", "14416", "24/7, many languages"),
    Helpline("AASRA", "9820466726", "24/7"),
    Helpline("iCall", "9152987821", "Mon–Sat, 10am–8pm")
)
