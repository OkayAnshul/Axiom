package com.cosmiclaboratory.axiom.data.companion

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cosmiclaboratory.axiom.MainActivity
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.GroqModels
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.notification.AxiomNotifications
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.navigation.AxiomDeepLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The companion speaking first.
 *
 * Everything proactive goes through here so the restraint rules live in one
 * place: at most one unprompted message per day, never a nag, and never a
 * guilt trip about not writing. A message is composed by the cheap model when
 * a key exists and falls back to a warm local template when it does not — the
 * feature must work for keyless users, since proactivity is exactly what makes
 * the app feel alive.
 *
 * A proactive message is posted into the conversation *and* mirrored as the
 * notification text. Tapping the notification therefore lands the user in a
 * conversation that has already begun, rather than on an empty prompt.
 */
@Singleton
class ProactiveMessenger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ai: AiProvider,
    private val companionRepo: CompanionRepository,
    private val memories: MemoryRepository,
    private val entries: JournalRepository,
    private val personas: PersonaRepository,
    private val prefs: UserPreferences
) {

    /**
     * Composes and delivers the daily check-in. Returns false when nothing was
     * sent (already spoke today, or nothing worth saying).
     */
    suspend fun sendDailyCheckIn(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (sentProactiveToday(now)) return false
        // A check-in is for the days they don't come by. If they have already
        // written or talked today, silence is the friendlier choice.
        if (activeToday(now)) return false

        val loops = runCatching { memories.dueOpenLoops(now) }.getOrDefault(emptyList())
        val dates = runCatching { entries.entryDates() }.getOrDefault(emptyList())
        val daysSince = dates.maxOrNull()?.let { ChronoUnit.DAYS.between(it, now.toLocalDate()).toInt() }
        val themes = runCatching { entries.recentSummariesForContext(2) }.getOrDefault(emptyList())

        val prompt = PromptTemplates.dailyCheckIn(
            displayName = prefs.displayName.first(),
            timeOfDay = timeOfDay(now.hour),
            openLoops = loops.map { it.text },
            recentThemes = themes,
            daysSinceLastEntry = daysSince
        )
        val fallback = localCheckIn(prefs.displayName.first(), now.hour, loops.firstOrNull()?.text)
        val message = compose(prompt, fallback)

        deliver(message.text, message.fromModel, now)
        // Asked once, closed once — a friend does not ask twice.
        loops.forEach { runCatching { memories.closeLoop(it.id) } }
        return true
    }

    /**
     * The Sunday look back. Unlike the check-in this is a ritual rather than a
     * nudge, so it still arrives on a day the user has already used the app.
     */
    suspend fun sendWeeklyRecap(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (sentProactiveToday(now)) return false

        val weekEntries = weekEntries(now.toLocalDate())
        if (weekEntries.isEmpty()) return false

        val prompt = PromptTemplates.weeklyRecap(
            displayName = prefs.displayName.first(),
            entryCount = weekEntries.size,
            moodNote = moodNote(weekEntries),
            highlights = weekEntries.mapNotNull { entry ->
                runCatching { entries.getInsight(entry.id)?.summary }.getOrNull()
                    ?: entry.displayTitle.takeIf { it.isNotBlank() }
            }.take(5)
        )
        val fallback = "That's another week down. Anything from it you want to keep hold of?"
        val message = compose(prompt, fallback)

        deliver(message.text, message.fromModel, now)
        return true
    }

    // ---- composition --------------------------------------------------------

    private data class Composed(val text: String, val fromModel: Boolean)

    private suspend fun compose(prompt: String, fallback: String): Composed {
        val result = runCatching {
            ai.chat(
                systemPrompt = PromptTemplates.PROACTIVE_SYSTEM,
                messages = listOf("user" to prompt),
                maxTokens = 120,
                model = GroqModels.BACKGROUND
            )
        }.getOrElse { return Composed(fallback, fromModel = false) }

        val text = (result as? AiResult.Ok)?.value?.trim()?.trim('"')
        return if (text.isNullOrBlank()) Composed(fallback, false) else Composed(text, true)
    }

    /**
     * The keyless path. Deliberately not a fake conversation: it says the one
     * true thing it knows, rather than pretending to have composed something.
     */
    private fun localCheckIn(name: String, hour: Int, openLoop: String?): String {
        if (openLoop != null) return "Earlier you mentioned: $openLoop. How did that go?"
        val suffix = if (name.isBlank()) "" else ", $name"
        return when (hour) {
            in 5..11 -> "Morning$suffix. How are you starting today?"
            in 12..16 -> "How's the day treating you$suffix?"
            in 17..21 -> "How was today$suffix?"
            else -> "Still up$suffix? How are you doing?"
        }
    }

    // ---- delivery -----------------------------------------------------------

    private suspend fun deliver(text: String, fromModel: Boolean, now: LocalDateTime) {
        if (fromModel) {
            companionRepo.appendAssistant(THREAD_ID, text, emptyList())
        } else {
            companionRepo.appendLocal(THREAD_ID, text)
        }
        prefs.setLastProactiveDate(now.toLocalDate().toString())
        notify(text)
    }

    private suspend fun notify(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        AxiomNotifications.ensureChannels(context)

        // A message from someone, so it is titled with who is speaking.
        val title = runCatching { personas.getByKey(prefs.activePersonaKey.first())?.displayName }
            .getOrNull() ?: "Axiom"

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AxiomDeepLinks.COMPANION),
            context,
            MainActivity::class.java
        ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, AxiomNotifications.CHANNEL_COMPANION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    // ---- context ------------------------------------------------------------

    /** One unprompted message a day, whatever kind it is. */
    private suspend fun sentProactiveToday(now: LocalDateTime): Boolean =
        prefs.lastProactiveDate.first() == now.toLocalDate().toString()

    /** They have already been here today — through the conversation or the journal. */
    private suspend fun activeToday(now: LocalDateTime): Boolean {
        val today = now.toLocalDate()
        val latest = companionRepo.latestMessage(THREAD_ID)
        if (latest != null && latest.createdAt.toLocalDate() == today) return true
        return runCatching { entries.forDay(today).isNotEmpty() }.getOrDefault(false)
    }

    private suspend fun weekEntries(today: LocalDate): List<Entry> =
        (0L until 7L).flatMap { offset ->
            runCatching { entries.forDay(today.minusDays(offset)) }.getOrDefault(emptyList())
        }.filter { it.content.isNotBlank() || it.markdown.isNotBlank() }

    private fun moodNote(weekEntries: List<Entry>): String {
        val moods = weekEntries.mapNotNull { entry -> entry.mood?.let { entry.createdAt.dayOfWeek to it } }
        if (moods.isEmpty()) return "not recorded"
        return moods.joinToString(", ") { (day, mood) ->
            "${day.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} $mood/5"
        }
    }

    private fun timeOfDay(hour: Int): String = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "late at night"
    }

    private companion object {
        const val THREAD_ID = "companion"
        const val NOTIFICATION_ID = 4201
    }
}
