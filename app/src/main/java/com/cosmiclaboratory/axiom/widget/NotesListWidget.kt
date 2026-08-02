package com.cosmiclaboratory.axiom.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.navigation.AxiomDeepLinks
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Recent entries on the home screen.
 *
 * Three changes from the previous version, all correctness rather than taste:
 *  - Colours come from the shared palette, so it is readable on a dark home
 *    screen. It previously hardcoded a blue and a near-white belonging to no
 *    palette in the app.
 *  - A Glance LazyColumn replaces a fixed 3-item Column, so a taller widget
 *    shows more rather than padding empty space.
 *  - Rows deep-link to the specific entry. They previously all opened
 *    MainActivity with no arguments, so tapping any row did the same thing.
 */
class NotesListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recent = runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                .journalRepository()
                .observeCompleted()
                .first()
                .take(MAX_ROWS)
        }.getOrDefault(emptyList())

        provideContent {
            GlanceTheme {
                Content(recent)
            }
        }
    }

    @Composable
    private fun Content(entries: List<Entry>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.background)
                .cornerRadius(20.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Journal",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = WidgetColors.ink
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "New",
                    style = TextStyle(fontSize = 13.sp, color = WidgetColors.accent),
                    modifier = GlanceModifier.clickable(deepLink(AxiomDeepLinks.COMPOSER))
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(deepLink(AxiomDeepLinks.COMPOSER)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing written yet.\nTap to start.",
                        style = TextStyle(fontSize = 13.sp, color = WidgetColors.inkFaint)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(entries, itemId = { it.id }) { entry -> EntryRow(entry) }
                }
            }
        }
    }

    @Composable
    private fun EntryRow(entry: Entry) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clickable(deepLink(AxiomDeepLinks.reader(entry.id)))
        ) {
            Text(
                text = entry.displayTitle.ifBlank { "Untitled" },
                maxLines = 1,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.ink
                )
            )
            Text(
                text = relativeDate(entry.createdAt.toLocalDate()),
                style = TextStyle(fontSize = 11.sp, color = WidgetColors.inkFaint)
            )
        }
    }

    private companion object {
        const val MAX_ROWS = 20
        val SMALL = DpSize(180.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 180.dp)
        val LARGE = DpSize(320.dp, 260.dp)
    }
}

/**
 * Localised relative date. The previous version hardcoded "MMM dd", which reads
 * wrong in most locales.
 */
private fun relativeDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(
            DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )
    }
}
