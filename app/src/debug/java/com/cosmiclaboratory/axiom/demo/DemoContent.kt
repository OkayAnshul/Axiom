package com.cosmiclaboratory.axiom.demo

import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal data class DemoEntry(
    val weeksAgo: Int,
    val day: DayOfWeek,
    val hour: Int,
    val minute: Int = 0,
    val mood: Int,
    val emotion: Emotion,
    val kind: EntryKind = EntryKind.FREE_FORM,
    val title: String = "",
    val prompt: String? = null,
    val tags: List<String> = emptyList(),
    val favourite: Boolean = false,
    val body: String
) {
    /**
     * The most recent [day] on or before today, stepped back [weeksAgo] weeks.
     *
     * Anchoring on the weekday rather than on a day count is what makes the
     * "Mondays are harder" finding reproducible: a fixed `minusDays(9)` lands on
     * a different weekday depending on when the seeder runs, and the finding
     * would appear and disappear between screenshot sessions.
     */
    fun dateFor(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(day)).minusWeeks(weeksAgo.toLong())
}

internal enum class Speaker { USER, ASSISTANT }

internal data class Turn(
    val speaker: Speaker,
    val text: String,
    val cites: Boolean = false,
    val source: CompanionMessageEntity.Source = CompanionMessageEntity.Source.MODEL
) {
    val role: CompanionMessageEntity.Role
        get() = when (speaker) {
            Speaker.USER -> CompanionMessageEntity.Role.USER
            Speaker.ASSISTANT -> CompanionMessageEntity.Role.ASSISTANT
        }
}

internal data class DemoMemory(
    val kind: MemoryKind,
    val text: String,
    val weight: Float,
    val timesSeen: Int = 1,
    val firstSeenDaysAgo: Int,
    val lastSeenDaysAgo: Int,
    val source: MemorySource = MemorySource.CONVERSATION,
    val userEdited: Boolean = false,
    val dueInDays: Int? = null
)

internal data class DemoInsight(
    val entryIndex: Int,
    val summary: String,
    val followUp: String,
    val themes: String,
    val mood: String,
    val tokens: Int
)

/**
 * The invented life the screenshots show: Aarav, final-year CS, in placement
 * season, running to stay sane, missing a friend who moved away.
 *
 * Written rather than generated, because this text is the most-read thing in the
 * store listing. It is also the reason the persona is fictional — a portfolio
 * screenshot is a public document.
 */
internal object DemoContent {

    const val USER_NAME = "Aarav"

    val TAGS: List<Pair<String, Long>> = listOf(
        "placements" to 0xFFC2703D,
        "running" to 0xFF4F7A5B,
        "family" to 0xFF8A5A83,
        "friends" to 0xFF3F6F8F,
        "sleep" to 0xFF6B5E8C
    )

    /** Which entries the companion's cited reply points at (indices into [ENTRIES]). */
    val CITED_ENTRY_INDICES = listOf(0, 4)

    /**
     * Thirty-five entries across nine weeks, on a Monday / Wednesday / Friday
     * spine with weekend extras.
     *
     * The moods are chosen so every finding in `PatternFinder` fires:
     *
     * - **hardestDayOfWeek** needs two weekdays with three-plus samples and a
     *   gap of at least 0.6. Mondays average ≈2.1 against ≈3.8 for Wednesdays
     *   and Fridays — a gap of ≈1.7.
     * - **moodTrend** needs three mood-bearing days in each of the last two
     *   weeks. `weeksAgo = 0` averages 4.0 against 2.7 the week before, so the
     *   trend reads as lifting.
     * - **personCorrelations** needs three-plus entries naming a person who also
     *   has a PERSON memory. Ishita and Rohan each appear in four.
     * - **recurringThemes** needs a memory with `timesSeen >= 3`; see [MEMORIES].
     *
     * Weekend entries are kept to two per weekday so they stay below
     * `MIN_DAY_SAMPLES` and never compete to be the "hardest day".
     */
    val ENTRIES: List<DemoEntry> = listOf(
        // ---- this week: things are lifting ---------------------------------
        DemoEntry(
            weeksAgo = 0, day = FRIDAY, hour = 21, minute = 40,
            mood = 5, emotion = Emotion.RELIEF,
            title = "Offer",
            tags = listOf("placements"), favourite = true,
            body = "The Bangalore team called at 4. I had to ask them to repeat " +
                "the sentence because I genuinely did not process it the first time.\n\n" +
                "Nine weeks of this. Six rounds. The two I was sure I had failed " +
                "turned out to be the ones they liked.\n\n" +
                "Called home before I called anyone else. Ma cried, then " +
                "immediately asked whether the city is safe and whether there is " +
                "a good doctor nearby. Papa said \"achha\" four times and then " +
                "asked about the salary in a very casual voice."
        ),
        DemoEntry(
            weeksAgo = 0, day = WEDNESDAY, hour = 22, minute = 10,
            mood = 4, emotion = Emotion.CALM,
            tags = listOf("running", "sleep"),
            body = "Ran 6k without stopping, which two months ago would have been " +
                "unthinkable. Same loop past the lake, same dogs, same uncle who " +
                "does not run so much as walk aggressively.\n\n" +
                "Slept seven hours for the third night running. I keep noticing " +
                "that the days I run are the days I do not lie awake rehearsing " +
                "conversations that have not happened.\n\n" +
                "Ishita sent a photo of her desk at 11pm. Neither of us is good " +
                "at this."
        ),
        DemoEntry(
            weeksAgo = 0, day = MONDAY, hour = 23, minute = 15,
            mood = 3, emotion = Emotion.ANXIETY,
            kind = EntryKind.PROMPTED,
            prompt = "What are you carrying into this week?",
            tags = listOf("placements"),
            body = "One result outstanding. That is genuinely all it is, and I have " +
                "still managed to build a whole week of dread out of it.\n\n" +
                "Rohan pointed out that I have said \"I'll relax once this is over\" " +
                "about four separate things this year. He is annoying mostly " +
                "because he is right."
        ),

        // ---- last week: the low stretch -------------------------------------
        DemoEntry(
            weeksAgo = 1, day = FRIDAY, hour = 20, minute = 30,
            mood = 3, emotion = Emotion.STRESS,
            tags = listOf("placements"),
            body = "Final round. Two system design questions and one where they " +
                "asked me to defend a decision I had made twenty minutes earlier " +
                "and had already half forgotten.\n\n" +
                "Walked out unable to tell whether it went well. That is the part " +
                "nobody warns you about — not failing, just not knowing."
        ),
        DemoEntry(
            weeksAgo = 1, day = WEDNESDAY, hour = 21, minute = 0,
            mood = 3, emotion = Emotion.LONELINESS,
            tags = listOf("friends"), favourite = true,
            body = "Ishita called for the first time in three weeks. Forty minutes, " +
                "most of it nonsense.\n\n" +
                "It is strange how much lighter I felt afterwards and how little " +
                "we actually said. She is settling in there. I am glad and I am " +
                "also not, and I have decided both can be true without me having " +
                "to pick one."
        ),
        DemoEntry(
            weeksAgo = 1, day = MONDAY, hour = 23, minute = 50,
            mood = 2, emotion = Emotion.BURNOUT,
            tags = listOf("placements", "sleep"),
            body = "Third Monday in a row I have written something at midnight " +
                "instead of sleeping.\n\n" +
                "Two rejections in the same inbox refresh. Both templated, both " +
                "polite. One of them spelled my name wrong, which somehow stung " +
                "more than the rejection did."
        ),

        // ---- weeks 2-8: the long middle -------------------------------------
        DemoEntry(
            weeksAgo = 2, day = FRIDAY, hour = 19, minute = 45,
            mood = 4, emotion = Emotion.MOTIVATION,
            tags = listOf("running"),
            body = "5k, and the last kilometre did not feel like a negotiation. " +
                "Rohan came along and complained the entire way, which is his " +
                "version of encouragement."
        ),
        DemoEntry(
            weeksAgo = 2, day = WEDNESDAY, hour = 22, minute = 20,
            mood = 4, emotion = Emotion.CONFIDENCE,
            tags = listOf("placements"),
            body = "Mock interview with Meera ma'am. She stopped me mid-answer and " +
                "said I explain the thing I built instead of the problem it solved.\n\n" +
                "Twenty years of teaching and she diagnosed in four minutes what I " +
                "had been doing wrong in every round so far."
        ),
        DemoEntry(
            weeksAgo = 2, day = MONDAY, hour = 23, minute = 30,
            mood = 2, emotion = Emotion.ANXIETY,
            tags = listOf("placements"),
            body = "Shortlist out. My name is on it. I refreshed the page four " +
                "times to check it was still there.\n\n" +
                "Told Ishita before I told anyone here. Cannot work out why good " +
                "news arrives feeling like a deadline."
        ),
        DemoEntry(
            weeksAgo = 3, day = FRIDAY, hour = 20, minute = 10,
            mood = 4, emotion = Emotion.GRATITUDE,
            tags = listOf("family"),
            body = "Ma sent photographs of the terrace garden. The tomatoes have " +
                "genuinely worked this year.\n\n" +
                "She has started sending these instead of asking how the " +
                "placements are going. I do not think that is an accident."
        ),
        DemoEntry(
            weeksAgo = 3, day = WEDNESDAY, hour = 21, minute = 30,
            mood = 3, emotion = Emotion.CONFUSION,
            kind = EntryKind.PROMPTED,
            prompt = "What is something you changed your mind about recently?",
            body = "That I want the highest-paying offer. I think I want the one " +
                "where I would not dread Monday, and I have no idea how you are " +
                "supposed to assess that from a forty-minute call.\n\n" +
                "Ishita said it took her four months to work out she had picked " +
                "wrong. Rohan thinks this is overthinking. They are both a bit right."
        ),
        DemoEntry(
            weeksAgo = 3, day = MONDAY, hour = 22, minute = 55,
            mood = 1, emotion = Emotion.DESPAIR,
            tags = listOf("placements"),
            body = "Cleared three rounds and got dropped at the last one. No " +
                "reason given. The recruiter was kind about it, which made it worse.\n\n" +
                "Sat on the stairs outside the department for a while. Did not " +
                "cry, did not do anything, just sat there until it got dark."
        ),
        DemoEntry(
            weeksAgo = 4, day = FRIDAY, hour = 21, minute = 15,
            mood = 4, emotion = Emotion.JOY,
            tags = listOf("friends"),
            body = "Ishita is back for four days. We went to the same terrible " +
                "chai place and it was exactly as terrible as it has always been.\n\n" +
                "She said I sound tired. Not in a concerned way, just as an " +
                "observation, which is somehow harder to argue with."
        ),
        DemoEntry(
            weeksAgo = 4, day = WEDNESDAY, hour = 22, minute = 0,
            mood = 3, emotion = Emotion.STEADY,
            tags = listOf("running"),
            body = "Fourth run this week. Slow, but I went.\n\n" +
                "The trick seems to be leaving before I have finished deciding " +
                "whether to leave."
        ),
        DemoEntry(
            weeksAgo = 4, day = MONDAY, hour = 23, minute = 40,
            mood = 2, emotion = Emotion.STRESS,
            tags = listOf("placements", "sleep"),
            body = "Aptitude test at 9am, and I was awake at 3 thinking about an " +
                "aptitude test. There is a joke in there that I am too tired to " +
                "find funny."
        ),
        DemoEntry(
            weeksAgo = 5, day = FRIDAY, hour = 20, minute = 0,
            mood = 4, emotion = Emotion.EXCITEMENT,
            tags = listOf("placements"),
            body = "First real interview of the season and I actually enjoyed it. " +
                "The interviewer got interested in the caching bit and we went ten " +
                "minutes over.\n\n" +
                "Whatever the result, that was the first time this month I felt " +
                "like a person who knows things."
        ),
        DemoEntry(
            weeksAgo = 5, day = WEDNESDAY, hour = 21, minute = 45,
            mood = 3, emotion = Emotion.ANXIETY,
            tags = listOf("placements"),
            body = "Rohan has two offers already. I am happy for him in the real " +
                "way and also there is a smaller, worse feeling underneath it that " +
                "I would rather not write down but probably should."
        ),
        DemoEntry(
            weeksAgo = 5, day = MONDAY, hour = 23, minute = 20,
            mood = 2, emotion = Emotion.BURNOUT,
            tags = listOf("sleep"),
            body = "Six hours of revision and I could not tell you one thing I " +
                "revised. Monday again."
        ),
        DemoEntry(
            weeksAgo = 6, day = FRIDAY, hour = 19, minute = 30,
            mood = 4, emotion = Emotion.CALM,
            tags = listOf("running"),
            body = "Ran at dawn for once instead of at night. Everything was " +
                "closed and the road was empty and it was the quietest hour I " +
                "have had in weeks."
        ),
        DemoEntry(
            weeksAgo = 6, day = WEDNESDAY, hour = 22, minute = 30,
            mood = 3, emotion = Emotion.LONELINESS,
            tags = listOf("friends"),
            body = "Ishita's messages have got shorter. Not colder — shorter. She " +
                "is busy and I am busy and neither of us has said anything about it.\n\n" +
                "I keep drafting a longer message and then sending \"haha same\"."
        ),
        DemoEntry(
            weeksAgo = 6, day = MONDAY, hour = 23, minute = 10,
            mood = 2, emotion = Emotion.ANXIETY,
            tags = listOf("placements"),
            body = "Resume rewritten for the fifth time. At some point this stopped " +
                "being preparation and started being avoidance."
        ),
        DemoEntry(
            weeksAgo = 7, day = FRIDAY, hour = 20, minute = 45,
            mood = 4, emotion = Emotion.MOTIVATION,
            tags = listOf("running"),
            body = "Signed up for the 10k in December. Paid the fee specifically " +
                "so that backing out costs me something."
        ),
        DemoEntry(
            weeksAgo = 7, day = WEDNESDAY, hour = 21, minute = 20,
            mood = 3, emotion = Emotion.STEADY,
            kind = EntryKind.PROMPTED,
            prompt = "What does a good day look like for you right now?",
            body = "Up by seven. One real problem solved before noon. A run. One " +
                "conversation that is not about placements.\n\n" +
                "Written down it looks very achievable, which is its own kind of " +
                "annoying."
        ),
        DemoEntry(
            weeksAgo = 7, day = MONDAY, hour = 22, minute = 40,
            mood = 2, emotion = Emotion.STRESS,
            tags = listOf("placements"),
            body = "Placement season officially opens next week and the group chat " +
                "has become unbearable. Muted it. Immediately checked it."
        ),
        DemoEntry(
            weeksAgo = 8, day = FRIDAY, hour = 21, minute = 0,
            mood = 4, emotion = Emotion.GRATITUDE,
            tags = listOf("family"),
            body = "Home for the weekend. Papa has learnt to use the video call " +
                "button by himself and is extremely pleased about it."
        ),
        DemoEntry(
            weeksAgo = 8, day = WEDNESDAY, hour = 22, minute = 15,
            mood = 3, emotion = Emotion.CONFUSION,
            body = "Started the project I have been circling for a month. Deleted " +
                "it. Started again with a smaller idea that I can actually finish."
        ),
        DemoEntry(
            weeksAgo = 8, day = MONDAY, hour = 23, minute = 0,
            mood = 3, emotion = Emotion.ANXIETY,
            tags = listOf("placements"),
            body = "First Monday of the last semester. It has not started and I am " +
                "already behind, which cannot possibly be correct and is exactly " +
                "how it feels."
        ),

        // ---- filling out this week ------------------------------------------
        // Tuesday and Thursday appear exactly once each, which keeps them under
        // MIN_DAY_SAMPLES so they never compete with Monday to be the hardest
        // day. Together with the Mon/Wed/Fri spine and the weekend entries below
        // they complete the current week, which is what puts a real number on
        // the streak card instead of "1 day".
        DemoEntry(
            weeksAgo = 0, day = DayOfWeek.TUESDAY, hour = 22, minute = 5,
            mood = 4, emotion = Emotion.STEADY,
            tags = listOf("placements"),
            body = "Rewrote the two answers Meera pulled apart. They are shorter " +
                "now and they say more, which is irritating in a useful way."
        ),
        DemoEntry(
            weeksAgo = 0, day = DayOfWeek.THURSDAY, hour = 21, minute = 25,
            mood = 3, emotion = Emotion.ANXIETY,
            tags = listOf("placements", "sleep"),
            body = "Waiting. Nothing to report, which is itself the report.\n\n" +
                "Wrote this mostly so the day had something in it."
        ),
        DemoEntry(
            weeksAgo = 0, day = SATURDAY, hour = 19, minute = 10,
            mood = 4, emotion = Emotion.GRATITUDE,
            tags = listOf("friends"),
            body = "Rohan cooked, badly, and would not accept help. Ishita joined " +
                "on video for the last twenty minutes and the three of us argued " +
                "about nothing for an hour.\n\n" +
                "First evening in weeks I did not check my email."
        ),

        // ---- weekend colour (kept below MIN_DAY_SAMPLES) --------------------
        DemoEntry(
            weeksAgo = 0, day = SUNDAY, hour = 10, minute = 30,
            mood = 4, emotion = Emotion.CALM,
            tags = listOf("running"),
            body = "Long slow run and then nothing at all for the rest of the day. " +
                "Deliberately nothing."
        ),
        DemoEntry(
            weeksAgo = 2, day = SUNDAY, hour = 11, minute = 0,
            mood = 4, emotion = Emotion.GRATITUDE,
            tags = listOf("family"),
            body = "Sunday call ran to an hour. Nobody asked about placements once, " +
                "and I know that was on purpose."
        ),
        DemoEntry(
            weeksAgo = 1, day = SATURDAY, hour = 18, minute = 20,
            mood = 3, emotion = Emotion.STEADY,
            tags = listOf("friends"),
            body = "Rohan dragged me out for exactly two hours. I resisted the " +
                "entire way there and then did not want to leave."
        ),
        DemoEntry(
            weeksAgo = 4, day = SATURDAY, hour = 17, minute = 45,
            mood = 5, emotion = Emotion.JOY,
            tags = listOf("friends"), favourite = true,
            body = "Ishita's last evening before she flew back. We walked most of " +
                "it. She said the thing about how we will be fine and I believed " +
                "her for the whole walk home."
        ),
        DemoEntry(
            weeksAgo = 6, day = SATURDAY, hour = 16, minute = 0,
            mood = 3, emotion = Emotion.CONFUSION,
            body = "Spent the afternoon reading about what I actually want to build " +
                "and came away with more tabs and no more clarity."
        ),
        DemoEntry(
            weeksAgo = 3, day = SUNDAY, hour = 9, minute = 40,
            mood = 2, emotion = Emotion.SADNESS,
            tags = listOf("sleep"),
            body = "Woke up heavy for no reason I can point at. Some days there is " +
                "no reason and I am slowly learning to let that be the whole entry."
        ),
        DemoEntry(
            weeksAgo = 5, day = SATURDAY, hour = 20, minute = 0,
            mood = 4, emotion = Emotion.RELIEF,
            tags = listOf("running"),
            body = "First week I have run three times. Small thing. Counting it anyway."
        ),
        DemoEntry(
            weeksAgo = 7, day = SUNDAY, hour = 12, minute = 15,
            mood = 3, emotion = Emotion.STEADY,
            tags = listOf("family"),
            body = "Helped Ma sort out her phone storage, which took two hours and " +
                "involved deciding which of four hundred photographs of the same " +
                "flower to keep."
        )
    )

    /**
     * A couple of entries carry a cloud insight, so the Reader shows the AI
     * summary card. Most do not — a missing row means "no AI summary", never
     * "no insight", so leaving most entries bare is the honest default.
     */
    val INSIGHTS: List<DemoInsight> = listOf(
        DemoInsight(
            entryIndex = 0,
            summary = "Relief arriving faster than belief. The instinct to call " +
                "home first says something about who this was really for.",
            followUp = "What did you notice in yourself between the call and telling anyone?",
            themes = "placements,family,relief",
            mood = "relief",
            tokens = 412
        ),
        DemoInsight(
            entryIndex = 11,
            summary = "A hard night written plainly, without reaching for a lesson. " +
                "Sitting on the stairs until dark is the whole entry, and it does " +
                "not need to be more than that.",
            followUp = "Was there anyone you nearly called?",
            themes = "placements,rejection",
            mood = "despair",
            tokens = 388
        )
    )

    /**
     * Eighteen memories. Note the `timesSeen = 4` entries: `PatternFinder`
     * requires `MIN_RECURRENCE = 3` before a theme is worth saying aloud, so
     * these are what make the recurring-theme findings appear.
     *
     * The three with `dueInDays` set in the past are overdue open loops, which
     * is what `OpenLoopsSection` renders.
     */
    val MEMORIES: List<DemoMemory> = listOf(
        // PERSON memories lead with the name on purpose: PatternFinder.nameFrom
        // takes the first capitalised token that isn't a stop word, and that name
        // is what person correlations are keyed on.
        DemoMemory(
            MemoryKind.PERSON,
            "Ishita is your closest friend. She moved to Bangalore in August.",
            weight = 0.92f, timesSeen = 6, firstSeenDaysAgo = 58, lastSeenDaysAgo = 9
        ),
        DemoMemory(
            MemoryKind.PERSON,
            "Rohan is your flatmate and classmate, and is already placed.",
            weight = 0.84f, timesSeen = 5, firstSeenDaysAgo = 55, lastSeenDaysAgo = 2
        ),
        DemoMemory(
            MemoryKind.PERSON,
            "Meera supervises your final-year project and runs your mock interviews.",
            weight = 0.71f, timesSeen = 3, firstSeenDaysAgo = 40, lastSeenDaysAgo = 14
        ),
        DemoMemory(
            MemoryKind.THEME,
            "Mondays are consistently the heaviest day of your week.",
            weight = 0.88f, timesSeen = 4, firstSeenDaysAgo = 52, lastSeenDaysAgo = 1,
            source = MemorySource.ENTRY
        ),
        DemoMemory(
            MemoryKind.THEME,
            "Your sleep goes first, before anything else does.",
            weight = 0.79f, timesSeen = 4, firstSeenDaysAgo = 47, lastSeenDaysAgo = 6,
            source = MemorySource.ENTRY
        ),
        DemoMemory(
            MemoryKind.THEME,
            "Running steadies you, and you under-rate it every single time.",
            weight = 0.83f, timesSeen = 5, firstSeenDaysAgo = 44, lastSeenDaysAgo = 3,
            source = MemorySource.ENTRY
        ),
        DemoMemory(
            MemoryKind.GOAL,
            "You want the offer where Monday isn't dread, not the biggest number.",
            weight = 0.90f, timesSeen = 3, firstSeenDaysAgo = 22, lastSeenDaysAgo = 4,
            userEdited = true
        ),
        DemoMemory(
            MemoryKind.GOAL,
            "You're running the 10k in December — the entry is already paid for.",
            weight = 0.74f, timesSeen = 2, firstSeenDaysAgo = 50, lastSeenDaysAgo = 11
        ),
        DemoMemory(
            MemoryKind.GOAL,
            "You want to finish the small project rather than restart the big one.",
            weight = 0.62f, timesSeen = 2, firstSeenDaysAgo = 57, lastSeenDaysAgo = 30
        ),
        DemoMemory(
            MemoryKind.FACT,
            "You're a final-year computer science student, graduating in May.",
            weight = 0.95f, timesSeen = 7, firstSeenDaysAgo = 60, lastSeenDaysAgo = 1
        ),
        DemoMemory(
            MemoryKind.FACT,
            "Your family is in Jaipur, and you call home most Sundays.",
            weight = 0.81f, timesSeen = 4, firstSeenDaysAgo = 59, lastSeenDaysAgo = 7,
            source = MemorySource.ENTRY
        ),
        DemoMemory(
            MemoryKind.FACT,
            "You run a loop past the lake, usually at night.",
            weight = 0.58f, timesSeen = 3, firstSeenDaysAgo = 43, lastSeenDaysAgo = 2,
            source = MemorySource.ENTRY
        ),
        DemoMemory(
            MemoryKind.PREFERENCE,
            "You'd rather be asked one real question than offered advice.",
            weight = 0.86f, timesSeen = 3, firstSeenDaysAgo = 35, lastSeenDaysAgo = 5,
            userEdited = true
        ),
        DemoMemory(
            MemoryKind.PREFERENCE,
            "You don't want congratulating before you've decided how you feel.",
            weight = 0.69f, timesSeen = 2, firstSeenDaysAgo = 28, lastSeenDaysAgo = 8
        ),
        DemoMemory(
            MemoryKind.EVENT,
            "Your final interview with the Bangalore team was last Friday.",
            weight = 0.77f, timesSeen = 2, firstSeenDaysAgo = 12, lastSeenDaysAgo = 9,
            dueInDays = -2
        ),
        DemoMemory(
            MemoryKind.EVENT,
            "One placement result is still outstanding this week.",
            weight = 0.80f, timesSeen = 2, firstSeenDaysAgo = 6, lastSeenDaysAgo = 1,
            dueInDays = -1
        ),
        DemoMemory(
            MemoryKind.EVENT,
            "You said you'd send Ishita a proper message instead of \"haha same\".",
            weight = 0.64f, timesSeen = 1, firstSeenDaysAgo = 41, lastSeenDaysAgo = 41,
            dueInDays = -4
        ),
        DemoMemory(
            MemoryKind.EVENT,
            "Your project review with Meera ma'am is at the end of the month.",
            weight = 0.55f, timesSeen = 1, firstSeenDaysAgo = 15, lastSeenDaysAgo = 15,
            dueInDays = 9
        )
    )

    /**
     * One evening's conversation, written out rather than generated.
     *
     * Pre-authored so that every screenshot run is identical and no live model
     * can put an awkward sentence into a store listing. The last assistant turn
     * cites entries, which is what puts the tappable chips on screen.
     */
    val CONVERSATION: List<Turn> = listOf(
        Turn(
            Speaker.ASSISTANT,
            "Evening, Aarav. You said Friday was the one that mattered — has anything come back?",
            source = CompanionMessageEntity.Source.LOCAL
        ),
        Turn(Speaker.USER, "they called. i got it"),
        Turn(
            Speaker.ASSISTANT,
            "You got it.\n\nNine weeks, six rounds, and the two you were sure you'd " +
                "failed were the ones that landed. How are you actually doing right now — " +
                "not how you think you should be doing?"
        ),
        Turn(
            Speaker.USER,
            "honestly kind of numb? i thought id feel more. i called home before " +
                "i even told rohan"
        ),
        Turn(
            Speaker.ASSISTANT,
            "Numb sounds about right for something you've been braced against for " +
                "two months. The bracing doesn't switch off the moment the news " +
                "changes.\n\nAnd you called home first. That's not nothing — you've " +
                "been carrying this partly for them the whole way through."
        ),
        Turn(Speaker.USER, "ma cried. then asked if bangalore is safe"),
        Turn(
            Speaker.ASSISTANT,
            "That's the most Ma sentence imaginable.\n\nIshita's already there, " +
                "isn't she? You wrote a few weeks ago that her messages had got " +
                "shorter and neither of you had said anything about it. This changes " +
                "the shape of that quite a lot."
        ),
        Turn(
            Speaker.USER,
            "i hadnt even thought about that yet. i still owe her a proper message " +
                "instead of haha same"
        ),
        Turn(
            Speaker.ASSISTANT,
            "You've been meaning to send that for about six weeks now.\n\nThis " +
                "might be the easiest it will ever be to send — you finally have " +
                "something to tell her that isn't just \"still waiting\".",
            cites = true
        )
    )
}
