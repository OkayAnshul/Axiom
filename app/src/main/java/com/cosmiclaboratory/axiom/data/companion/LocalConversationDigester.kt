package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.ml.MoodClassifier
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import com.cosmiclaboratory.axiom.domain.nlp.CommitmentDetector
import com.cosmiclaboratory.axiom.domain.text.TextTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.cosmiclaboratory.axiom.domain.memory.MemoryHygiene
import com.cosmiclaboratory.axiom.domain.memory.MemorySimilarity

/**
 * Turns a finished conversation into journal and memory **without an API key**.
 *
 * Before this, a keyless user's conversations were saved and then inert: the
 * digest worker called the model, got `NoKey`, and returned. Talking taught the
 * companion nothing, while writing an entry did — an asymmetry nobody chose.
 *
 * The line this holds: **it never writes prose on the user's behalf.** The
 * entry is the user's own turns, verbatim, stitched into paragraphs. Producing
 * a first-person "I felt…" narrative genuinely needs a model, and faking one
 * with templates would put words in someone's mouth inside their own journal.
 * Everything else — who they mentioned, what keeps coming up, what they said
 * they'd do, how it read emotionally — is honest mechanical extraction.
 *
 * When a key is connected later the AI digest still runs over anything it has
 * not summarised, so these entries are a floor, not a ceiling.
 */
@Singleton
class LocalConversationDigester @Inject constructor(
    private val journalRepo: JournalRepository,
    private val memoryRepo: MemoryRepository
) {

    data class Result(
        val entryId: Long?,
        val memoriesAdded: Int,
        val loopsAdded: Int
    )

    suspend fun digest(
        session: List<CompanionMessageEntity>,
        today: LocalDate = LocalDate.now()
    ): Result {
        val userMessages = session
            .filter { it.role == CompanionMessageEntity.Role.USER.name }
            .filter { it.content.isNotBlank() }
        val userTurns = userMessages.map { it.content.trim() }
        if (userTurns.isEmpty()) return Result(null, 0, 0)

        val sessionEndedAt = session.lastOrNull()?.createdAt ?: today.atStartOfDay()
        val body = userTurns.joinToString("\n\n")

        val entryId = saveEntry(body, sessionEndedAt)
        val loops = recordCommitments(userMessages, today)
        val memories = recordPeopleAndThemes(userMessages)
        return Result(entryId, memories, loops)
    }

    /**
     * Appends to the day's conversation entry if one exists, mirroring the AI
     * digest so the two paths can never produce two entries for one day.
     */
    private suspend fun saveEntry(body: String, sessionEndedAt: LocalDateTime): Long? {
        val sessionDate = sessionEndedAt.toLocalDate()
        val mood = inferMood(body)
        val existing = runCatching { journalRepo.forDay(sessionDate) }
            .getOrDefault(emptyList())
            .firstOrNull { it.kind == EntryKind.CONVERSATION }

        return runCatching {
            if (existing != null) {
                val content = existing.content + "\n\n" + body
                journalRepo.upsert(
                    existing.copy(
                        content = content,
                        markdown = content,
                        wordCount = content.split(WHITESPACE).count { it.isNotBlank() },
                        charCount = content.length,
                        mood = existing.mood ?: mood
                    )
                )
            } else {
                journalRepo.upsert(
                    Entry(
                        title = "From a conversation — " + sessionDate.format(TITLE_DATE),
                        content = body,
                        markdown = body,
                        kind = EntryKind.CONVERSATION,
                        isComplete = true,
                        // Dated to the conversation, not to whenever the worker
                        // got round to it. See JournalRepository.upsert.
                        createdAt = sessionEndedAt,
                        mood = mood,
                        // null moodCapturedAt marks the mood as inferred.
                        moodCapturedAt = null,
                        wordCount = body.split(WHITESPACE).count { it.isNotBlank() },
                        charCount = body.length
                    )
                )
            }
        }.getOrNull()
    }

    /**
     * Mood from the classifier trained on the user's own taps. Returns null
     * below its confidence gate — an unrecorded mood is honest, a guessed one
     * poisons the pattern engine.
     */
    private suspend fun inferMood(text: String): Int? {
        val samples = runCatching { journalRepo.moodTrainingSamples() }.getOrDefault(emptyList())
        val model = MoodClassifier.train(samples) ?: return null
        return MoodClassifier.predict(model, text)?.mood
    }

    /** "My scan is on Tuesday" → an EVENT the companion will circle back to. */
    private suspend fun recordCommitments(
        userMessages: List<CompanionMessageEntity>,
        today: LocalDate
    ): Int {
        // Per turn, NOT on the turns joined together. The detector splits on
        // sentence punctuation, and chat messages routinely have none — joining
        // them produced a single "sentence" the length of the whole
        // conversation, which was then stored verbatim as one memory.
        //
        // Detecting per turn also means each commitment knows the message it
        // came from, so its sourceId is the exact turn rather than a stand-in.
        val commitments = userMessages
            .flatMap { message ->
                runCatching { CommitmentDetector.detect(message.content.trim(), today) }
                    .getOrDefault(emptyList())
                    .map { message.id to it }
            }
            .distinctBy { (_, commitment) -> commitment.sentence.lowercase() }
            .take(MAX_LOOPS)
        if (commitments.isEmpty()) return 0

        val existing = existingTexts()
        var added = 0
        commitments.forEach { (messageId, commitment) ->
            if (commitment.sentence.lowercase() in existing) return@forEach
            runCatching {
                memoryRepo.insert(
                    kind = MemoryKind.EVENT,
                    // Belt and braces: a memory is a sentence, never a wall.
                    text = commitment.sentence.take(MAX_MEMORY_CHARS),
                    weight = COMMITMENT_WEIGHT,
                    source = MemorySource.CONVERSATION,
                    sourceId = messageId,
                    dueAt = commitment.followUpOn.atTime(MORNING_HOUR, 0)
                )
            }.onSuccess { added++ }
        }
        return added
    }

    /**
     * People and recurring topics, at deliberately low weight.
     *
     * These are guesses from repetition, not extractions from understanding, so
     * they start weak and either get reinforced by being mentioned again or
     * fade out on their own. The weekly consolidator prunes the ones that were
     * seen once and never edited, which is exactly the failure mode here.
     */
    private suspend fun recordPeopleAndThemes(userMessages: List<CompanionMessageEntity>): Int {
        val userTurns = userMessages.map { it.content.trim() }
        val existing = existingTexts()
        // A CONVERSATION memory's sourceId must address the messages table — it
        // used to hold the journal entry id, which resolved against the wrong
        // table entirely and silently pointed at whatever message shared that
        // number. Themes come from repetition across the whole session, so they
        // get the session's last turn; a name gets the turn it first appeared in,
        // which is exactly derivable.
        val sessionId = userMessages.lastOrNull()?.id
        var added = 0

        candidateNames(userTurns).take(MAX_PEOPLE).forEach { name ->
            val text = "$name came up in conversation"
            if (isNovel(text, existing)) {
                runCatching {
                    memoryRepo.insert(
                        kind = MemoryKind.PERSON,
                        text = text,
                        weight = SOFT_WEIGHT,
                        source = MemorySource.CONVERSATION,
                        sourceId = firstMentionOf(name, userMessages) ?: sessionId
                    )
                }.onSuccess { added++ }
            }
        }

        candidateThemes(userTurns).take(MAX_THEMES).forEach { theme ->
            val text = "Keeps coming back to $theme"
            if (isNovel(text, existing)) {
                runCatching {
                    memoryRepo.insert(
                        kind = MemoryKind.THEME,
                        text = text,
                        weight = SOFT_WEIGHT - 0.05f,
                        source = MemorySource.CONVERSATION,
                        sourceId = sessionId
                    )
                }.onSuccess { added++ }
            }
        }
        return added
    }

    private fun firstMentionOf(name: String, messages: List<CompanionMessageEntity>): Long? =
        messages.firstOrNull { it.content.contains(name, ignoreCase = true) }?.id

    private suspend fun existingTexts(): Set<String> =
        runCatching { memoryRepo.snapshotForExtraction() }
            .getOrDefault(emptyList())
            .map { it.text.lowercase() }
            .toSet()

    internal companion object {
    /**
     * Capitalised words that recur and look like names rather than grammar.
     *
     * Two passes, and the split matters. Sentence-initial capitals prove
     * nothing — "Work was hard." capitalises Work by position. But requiring
     * *every* mention to be mid-sentence loses the common case where someone
     * writes "Riya seemed happier", so:
     *
     *  1. a word must appear capitalised mid-sentence **at least once**, which
     *     is what distinguishes a name from a sentence opener;
     *  2. once it has, **all** its mentions count toward the threshold.
     *
     * Crude on purpose. The alternative is a named-entity model — a 30 MB
     * dependency to occasionally notice a name the keyed path already gets
     * right — and this only has to be good enough to be worth reinforcing.
     */
    fun candidateNames(userTurns: List<String>): List<String> {
        val midSentence = mutableSetOf<String>()
        val counts = mutableMapOf<String, Int>()

        userTurns.forEach { turn ->
            turn.split(SENTENCE_BREAK).forEach { sentence ->
                val words = sentence.trim().split(WHITESPACE)
                    .map { it.trim(*TRIM_CHARS) }
                    .filter { it.isNotEmpty() }
                words.forEachIndexed { index, word ->
                    if (!looksLikeName(word)) return@forEachIndexed
                    counts[word] = (counts[word] ?: 0) + 1
                    if (index > 0) midSentence.add(word)
                }
            }
        }

        return counts
            .filterKeys { it in midSentence }
            .filterValues { it >= MIN_NAME_MENTIONS }
            .entries.sortedByDescending { it.value }
            .map { it.key }
    }

    /** Capitalised, not an acronym, long enough, and not a calendar word. */
    private fun looksLikeName(word: String): Boolean =
        word.length >= MIN_NAME_LENGTH &&
            word.first().isUpperCase() &&
            word.drop(1).none { it.isUpperCase() } &&
            word.lowercase() !in NON_NAMES

    /** Content words appearing across several turns — what the session was about. */
    fun candidateThemes(userTurns: List<String>): List<String> {
        if (userTurns.size < MIN_TURNS_FOR_THEME) return emptyList()
        val turnsContaining = mutableMapOf<String, Int>()
        userTurns.forEach { turn ->
            FtsQuerySanitizer.retrievalTerms(turn, maxTerms = 40).toSet().forEach { term ->
                turnsContaining[term] = (turnsContaining[term] ?: 0) + 1
            }
        }
        // A frequency count alone will happily decide someone's recurring theme
        // is "long" or "thing". Frequency says a word was repeated; it does not
        // say the word carries meaning, so a theme has to clear a floor:
        // substantial enough to be a topic, and not a conversational filler.
        val threshold = maxOf(MIN_TURNS_FOR_THEME, (userTurns.size + 1) / 2)
        return turnsContaining
            .filterKeys { it.length >= MemoryHygiene.MIN_THEME_LENGTH && it !in MemoryHygiene.NON_THEMES }
            .filterValues { it >= threshold }
            .entries.sortedByDescending { it.value }
            .map { it.key }
    }

    /**
     * A memory that mostly repeats one already held is not new.
     *
     * Containment rather than Jaccard, and deliberately so: these candidates are
     * short generated sentences ("Riya came up in conversation"), and asking
     * whether one is wholly inside an existing memory is the right question,
     * where asking whether they are the same length is not.
     */
    fun isNovel(candidate: String, existing: Set<String>): Boolean {
        val terms = MemorySimilarity.tokens(candidate)
        if (terms.isEmpty()) return false
        return existing.none { held ->
            MemorySimilarity.containment(terms, MemorySimilarity.tokens(held)) >= DUPLICATE_OVERLAP
        }
    }

        val WHITESPACE = Regex("\\s+")
        val SENTENCE_BREAK = Regex("[.!?\\n]+")
        val TITLE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
        val TRIM_CHARS = charArrayOf(',', '.', '!', '?', ';', ':', '"', '\'', ')', '(')

        const val MAX_LOOPS = 2
        const val MAX_PEOPLE = 2
        const val MAX_THEMES = 2
        const val MIN_NAME_LENGTH = 3
        const val MIN_NAME_MENTIONS = 2
        const val MIN_TURNS_FOR_THEME = 3
        const val COMMITMENT_WEIGHT = 0.5f

        /** A memory is a sentence. This is the hard ceiling on one. */
        const val MAX_MEMORY_CHARS = 200

        /** Low: these are guesses from repetition, and should fade unless repeated. */
        const val SOFT_WEIGHT = 0.35f
        const val DUPLICATE_OVERLAP = 0.6
        const val MORNING_HOUR = 9

        val NON_NAMES = setOf(
            "i", "the", "and", "but", "so", "it", "im", "ive", "id", "ill",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "january", "february", "march", "april", "may", "june", "july",
            "august", "september", "october", "november", "december",
            "today", "tomorrow", "yesterday", "okay", "yeah", "thanks", "axiom"
        )
    }
}
