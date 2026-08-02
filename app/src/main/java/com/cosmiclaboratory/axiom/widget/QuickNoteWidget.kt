package com.cosmiclaboratory.axiom.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cosmiclaboratory.axiom.ui.navigation.AxiomDeepLinks

/**
 * Quick capture from the home screen.
 *
 * Two sizes rather than one fixed layout: small is a single "Write" target,
 * wide splits into Write / Voice / Prompt so the common actions are one tap
 * each instead of one tap plus in-app navigation.
 *
 * Navigation goes through axiom:// deep links, not bare `actionStartActivity`.
 * The previous version launched MainActivity with no extras at all, so the
 * "Quick Note" widget just opened the app — it never actually started a note.
 */
class QuickNoteWidget : GlanceAppWidget() {

    // Declared buckets rather than SizeMode.Single, so a resized widget gets a
    // layout designed for its shape instead of a stretched one.
    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL, WIDE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                if (size.width >= WIDE.width) WideContent() else SmallContent()
            }
        }
    }

    @Composable
    private fun SmallContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.accent)
                .cornerRadius(20.dp)
                .clickable(deepLink(AxiomDeepLinks.COMPOSER))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = WidgetColors.onAccent
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Write",
                    style = TextStyle(fontSize = 13.sp, color = WidgetColors.onAccent)
                )
            }
        }
    }

    @Composable
    private fun WideContent() {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.surface)
                .cornerRadius(20.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Action("Write", AxiomDeepLinks.COMPOSER, primary = true, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            Action("Voice", AxiomDeepLinks.COMPOSER_VOICE, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            Action("Prompt", AxiomDeepLinks.TODAY, modifier = GlanceModifier.defaultWeight())
        }
    }

    @Composable
    private fun Action(
        label: String,
        link: String,
        modifier: GlanceModifier = GlanceModifier,
        primary: Boolean = false
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(if (primary) WidgetColors.accent else WidgetColors.background)
                .cornerRadius(14.dp)
                .clickable(deepLink(link)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
                    color = if (primary) WidgetColors.onAccent else WidgetColors.ink
                )
            )
        }
    }

    private companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(180.dp, 110.dp)
    }
}

/** Launches the app at a specific destination rather than merely opening it. */
internal fun deepLink(uri: String) = actionStartActivity(
    Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        setPackage("com.cosmiclaboratory.axiom")
    }
)
