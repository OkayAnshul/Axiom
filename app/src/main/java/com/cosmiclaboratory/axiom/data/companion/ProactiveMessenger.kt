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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.cosmiclaboratory.axiom.MainActivity
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.AiTasks
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.notification.AxiomNotifications
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.notification.CompanionReplyReceiver
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity
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
    private val questions: QuestionRepository,
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
        // And if they recently told us they were in a bad way, an unprompted
        // "how was your day?" is the worst thing this app could say. Let them
        // come back on their own terms.
        if (recentlyDistressed(now)) return false

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

        // A check-in that can only be answered by typing is a check-in most
        // people leave unanswered. Offering the question to *write about*
        // converts the moment they are already paying attention into the thing
        // the app is actually for.
        //
        // An open loop makes a better prompt than a generated question, because
        // it is a thing they told us themselves and asked to be reminded of.
        val writingPrompt = loops.firstOrNull()?.text?.let { "You mentioned: $it" }
            ?: runCatching {
                questions.nextQuestion(prefs.activePersonaKey.first(), lastTheme = null)?.text
            }.getOrNull()

        deliver(message.text, message.fromModel, now, writingPrompt)
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
        val message = compose(prompt, fallback, quality = true)

        deliver(message.text, message.fromModel, now)
        return true
    }

    // ---- composition --------------------------------------------------------

    private data class Composed(val text: String, val fromModel: Boolean)

    /**
     * [quality] routes the weekly recap to the strong model. A daily check-in is
     * one throwaway line and the cheap model writes it fine; the Sunday recap is
     * the app looking back over someone's week, and that is worth the better
     * model and room to say something.
     */
    private suspend fun compose(
        prompt: String,
        fallback: String,
        quality: Boolean = false
    ): Composed {
        val result = runCatching {
            ai.chat(
                systemPrompt = PromptTemplates.PROACTIVE_SYSTEM,
                messages = listOf("user" to prompt),
                maxTokens = if (quality) RECAP_MAX_TOKENS else CHECK_IN_MAX_TOKENS,
                model = if (quality) AiTasks.QUALITY else AiTasks.CHEAP
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

    private suspend fun deliver(
        text: String,
        fromModel: Boolean,
        now: LocalDateTime,
        writingPrompt: String? = null
    ) {
        if (fromModel) {
            companionRepo.appendAssistant(THREAD_ID, text, emptyList())
        } else {
            companionRepo.appendLocal(THREAD_ID, text)
        }
        prefs.setLastProactiveDate(now.toLocalDate().toString())
        notify(text, writingPrompt)
    }

    /**
     * Posts the companion's answer to a reply sent from the shade. Public
     * because [com.cosmiclaboratory.axiom.data.work.CompanionReplyWorker] owns
     * that round trip — a broadcast receiver cannot wait for a model.
     */
    suspend fun notifyReply(text: String) = notify(text, writingPrompt = null)

    private suspend fun notify(text: String, writingPrompt: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        AxiomNotifications.ensureChannels(context)

        // A message from someone, so it is titled with who is speaking — and
        // "who" is the name the user gave the companion, which is what the
        // place bar and the greeting already call it. It used to be the persona's
        // display name, so a notification arrived from "Calm Companion" while
        // every surface inside the app said something else.
        val title = runCatching { CompanionIdentity.resolve(prefs.companionName.first()) }
            .getOrNull() ?: CompanionIdentity.DEFAULT_NAME

        val builder = NotificationCompat.Builder(context, AxiomNotifications.CHANNEL_COMPANION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openConversationIntent())
            .setAutoCancel(true)
            .addAction(replyAction(title))

        // Second action, and second only: replying is the lower-effort answer
        // and belongs first. Writing is the one that produces something.
        writingPrompt?.let { builder.addAction(writeAboutAction(it)) }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun openConversationIntent(): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AxiomDeepLinks.COMPANION),
            context,
            MainActivity::class.java
        ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        return PendingIntent.getActivity(
            context, REQUEST_OPEN, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Inline reply. MUTABLE rather than IMMUTABLE, and that is not an oversight:
     * a RemoteInput action exists precisely so the system can write the typed
     * text into the intent before sending it, which an immutable PendingIntent
     * forbids. The receiver is unexported, so nothing outside the app can reach
     * it.
     */
    private fun replyAction(title: String): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(CompanionReplyReceiver.KEY_REPLY_TEXT)
            .setLabel("Reply to $title")
            .build()
        val intent = Intent(context, CompanionReplyReceiver::class.java).apply {
            action = CompanionReplyReceiver.ACTION_REPLY
            putExtra(CompanionReplyReceiver.EXTRA_THREAD_ID, THREAD_ID)
            putExtra(CompanionReplyReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_REPLY, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_notification, "Reply", pendingIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()
    }

    /** One tap from the shade into the composer, question already at the top. */
    private fun writeAboutAction(prompt: String): NotificationCompat.Action {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AxiomDeepLinks.composerWithPrompt(prompt)),
            context,
            MainActivity::class.java
        ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_WRITE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification, "Write about this", pendingIntent
        ).build()
    }

    // ---- context ------------------------------------------------------------

    /** One unprompted message a day, whatever kind it is. */
    private suspend fun sentProactiveToday(now: LocalDateTime): Boolean =
        prefs.lastProactiveDate.first() == now.toLocalDate().toString()

    /**
     * True for [DISTRESS_QUIET_DAYS] after someone plainly said they were
     * struggling. Deliberately quiet rather than deliberately attentive: an
     * automated cheerful nudge in that window reads as not having listened.
     */
    private suspend fun recentlyDistressed(now: LocalDateTime): Boolean {
        val raw = runCatching { prefs.lastDistressDate.first() }.getOrDefault("")
        if (raw.isBlank()) return false
        val last = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return false
        return ChronoUnit.DAYS.between(last, now.toLocalDate()) < DISTRESS_QUIET_DAYS
    }

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

        // Distinct request codes: PendingIntents with the same code and matching
        // extras are the same object, so sharing one would have the reply action
        // quietly overwrite the tap target.
        const val REQUEST_OPEN = 0
        const val REQUEST_REPLY = 1
        const val REQUEST_WRITE = 2

        /** How long to stay quiet after someone said they were struggling. */
        const val DISTRESS_QUIET_DAYS = 2L

        const val CHECK_IN_MAX_TOKENS = 120

        /** The Sunday look-back deserves more than a notification blurb. */
        const val RECAP_MAX_TOKENS = 400
    }
}
