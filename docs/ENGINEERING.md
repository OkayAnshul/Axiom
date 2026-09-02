# Axiom — Engineering

A technical write-up of how Axiom is built and, more usefully, *why* it is built that way.
Roughly 32,000 lines of Kotlin across 227 files, single-module, Compose-only.

---

## Contents

1. [Constraints that shaped everything](#1-constraints-that-shaped-everything)
2. [Architecture](#2-architecture)
3. [Type-safe navigation](#3-type-safe-navigation)
4. [Persistence and schema evolution](#4-persistence-and-schema-evolution)
5. [The AI layer](#5-the-ai-layer)
6. [The companion pipeline](#6-the-companion-pipeline)
7. [The memory system](#7-the-memory-system)
8. [On-device intelligence](#8-on-device-intelligence)
9. [Safety](#9-safety)
10. [Security](#10-security)
11. [Release engineering](#11-release-engineering)
12. [Testing](#12-testing)
13. [Known limitations](#13-known-limitations)

---

## 1. Constraints that shaped everything

Three constraints drove most of the interesting decisions.

**No server.** There is no backend, no account system and no telemetry. Every feature has to work
from a single device's storage, which rules out the usual answer to "just compute it server-side".

**No API key, for most users.** AI is bring-your-own-key. A keyless user is not a degraded user —
they must still get mood tracking, patterns, search, digests and memory. Any feature that only
works with a key is a feature most people never see, so the interesting work is in the keyless
paths.

**The data is somebody's journal.** This makes destructive operations unusually expensive.
It is why migrations are hand-written from a fixed baseline, why memory pruning is deliberately
timid, and why the automatic-backup rules exclude the database rather than risk a partial restore.

---

## 2. Architecture

Single Gradle module, three source layers:

```
data/       Room, DataStore, Ktor, WorkManager, repositories, security
domain/     Pure Kotlin: models, ML, NLP, search, patterns, memory, safety
ui/         Compose screens, ViewModels, design system, navigation
```

`domain/` has no Android dependencies, which is what makes the interesting logic — the mood
classifier, the semantic index, the pattern finder, the distress detector — testable as plain
JVM unit tests with no Robolectric and no emulator. That is not an accident of layering; it is
the reason the test suite is fast enough to actually run.

**DI:** Hilt throughout, including `@HiltWorker` for WorkManager. `AxiomApplication` implements
`Configuration.Provider` to supply `HiltWorkerFactory`, and the manifest removes
`WorkManagerInitializer` from `androidx.startup` so that provider is actually consulted — a step
that is easy to miss and fails at runtime rather than at compile time.

**State:** unidirectional. ViewModels expose a single immutable `UiState` via `StateFlow`;
screens are functions of that state.

**Shared-element transitions** let a card you tapped travel to the screen it opens.
`SharedTransitionScope` and `AnimatedVisibilityScope` are passed through **composition locals**
rather than screen parameters, for two reasons. Threading both down every screen signature to
reach one card would put navigation machinery in the parameter list of components that are
otherwise about content — and both scopes are genuinely ambient: they describe *where the
composition is*, which is what a composition local is for.

It uses `sharedBounds`, not `sharedElement`, because the two ends are not the same composable — a
timeline card and a full entry in the reader have different content and different sizes, so the
container is what animates. Outside a `SharedTransitionLayout` (previews, tests, any screen
reached outside the NavHost) the locals are null and the modifier degrades to doing nothing
rather than crashing.

---

## 3. Type-safe navigation

Destinations are `@Serializable` Kotlin types, not strings:

```kotlin
@Serializable data object Companion
@Serializable data class Composer(
    val entryId: Long? = null,
    val questionId: Long? = null,
    val initialText: String? = null,
    val voice: Boolean = false,
    val promptText: String? = null
)
@Serializable data class Reader(val entryId: Long)
```

This replaced string routes plus `createRoute()` builders plus
`savedStateHandle.get<Long>("noteId")`. That triple had a specific failure mode: an argument-name
typo compiles cleanly and fails at runtime. It shipped a real bug — `MainActivity` popped to
route `"notes_list"`, a name that had not existed since the screen was renamed to `"library"`, so
the share deep-link silently stopped trimming its back stack. Nothing detected it because
nothing *could*; both halves were strings.

Deep links are declared once in `AxiomDeepLinks` so widgets, the Quick Settings tile and
notifications cannot drift apart from the graph.

---

## 4. Persistence and schema evolution

Room at **version 12** — 13 entities, 11 DAOs.

### Migrations

Version 7 is the baseline. Below it, versions belong to development builds that never shipped and
are still handled destructively. At 7 and above, every schema change ships with a hand-written
`Migration`:

| Migration | What changed |
|---|---|
| 7 → 8 | Dropped `attachments`, created in v1 and never once written to |
| 8 → 9 | **Data repair:** nulls a `memory_items.sourceId` that had been recording the wrong conversation turn |
| 9 → 10 | **Data repair:** deletes theme memories whose subject is a filler word, and rows long enough to be transcripts |
| 10 → 11 | Adds `companion_messages.questionId`, fixing curated prompts that recycled forever |
| 11 → 12 | Adds `parkedUpToMessageId`, and indices on `memory_items` (`lastSeenAt`, `kind`, `dueAt`) |

Three of these are more interesting than schema changes.

**8 → 9 chooses honesty over false precision.** The correct source id was never recorded and
cannot be recovered, so the repair is to admit that: a null reads as "we don't know which turn",
which is true, where the old value silently asserted a specific wrong one.

**9 → 10 contains the best single decision in the persistence layer.** The digester's frequency
counter had decided that someone "keeps coming back to *message*" and "keeps coming back to
*long*". A length floor and a stop list were added later — but only at *write* time, so the rows
already stored kept going out in every prompt, telling the model these were recurring themes in a
person's life alongside real facts about them. The migration deletes them. Crucially, the word
list inside it is a **deliberately frozen copy** of `MemoryHygiene.NON_THEMES` rather than a
reference to it: a migration must produce the same result on every device forever, and one that
read a live constant would quietly change what a 9→10 upgrade does the day someone edits that
list. The duplication is the point, not an oversight.

**10 → 11 fixes a bug caused by an otherwise elegant design.** Questions have never carried an
"answered" flag; `QuestionDao` derives it by joining `entries.questionId`, which is self-healing —
delete the entry and the question returns to rotation with no cleanup code. But the join only
works if the id reaches the entry, and it never did: the opener persisted the question's *text*
and threw the id away, so `entries.questionId` was always null and the fifty curated prompts
recycled indefinitely. The column is nullable and additive; existing openers cannot be credited
retroactively, because guessing which question they came from by string-matching would be a fine
way to mark the wrong one answered.

**11 → 12** is measure-before-optimise: `memory_items` had no indices at all, and every companion
turn reads the whole table — `lastSeenAt` is what decay orders by, `kind` is what the per-kind
prompt caps group by, `dueAt` is what the open-loop lookup filters on.

Earlier work collapsed `answer_entries` into `entries` (v4). Splitting guided-prompt answers from
free-form notes had cost a companion that could not see journal answers and a summarizer that
could not see notes; one table removes both by construction, and a `kind` discriminator plus
`promptSnapshot` carries what the separate table used to.

### The `exportSchema = false` problem

`exportSchema` is off, and the reason is worth stating precisely because it is the kind of thing
an interviewer will push on.

It is **not** a Room limitation. Room 2.8.4 exports a schema happily on a clean tree, then fails
the moment a previous schema exists and must be read back for comparison —
`AbstractMethodError` on `FieldBundle$$serializer`. Room's schema bundles are compiled against
kotlinx-serialization 1.8+, whose `GeneratedSerializer` no longer declares
`typeParametersSerializers()`, while the serialization compiler plugin bundled with Kotlin 2.0.21
still expects it. Bumping the serialization *runtime* does not help — the mismatch is in the
*plugin*. The fix is Kotlin 2.1+, which also moves the Compose compiler, so it is its own piece
of work. The `room.schemaLocation` argument is already declared so turning it on later is a
one-line change.

The same clash blocks `MigrationTestHelper`: `room-testing` drags `room-migration` onto the
androidTest compile classpath and androidTest stops compiling. So migrations are guarded by a
unit test asserting the chain is unbroken from the baseline to `LATEST_VERSION`, plus manual
verification on an emulator — 7→8 was checked by hand with twelve entries and three memories
surviving the upgrade. That is a weaker guarantee than a real migration test, and it is
documented as such in the source rather than papered over.

### Full-text search

`EntryFts` is an FTS4 table with `contentEntity = EntryEntity::class`, so Room generates the
triggers that keep the index in step — no manual sync. The tokenizer is `unicode61` rather than
the default `simple`, because `simple` treats every non-ASCII byte as a non-word character, which
made Devanagari and accented Latin silently unsearchable. For an app whose voice layer ships
Hindi and Hinglish, that was a real bug, not a nicety. `unicode61` also folds diacritics, so
"cafe" finds "café". `promptSnapshot` is indexed too, so a guided entry is findable by its
question and not only by the answer.

---

## 5. The AI layer

An `AiProvider` interface with two implementations (Groq, Gemini) and a `RoutingAiProvider` in
front of both.

Routing happens **per call**, not at injection time, for two reasons: the user can switch vendor
while the app is running, and two behaviours have to survive the switch.

- Transcription always goes to Groq. Gemini cannot do it here, so a user on Gemini who taps the
  mic still gets their Hinglish voice note transcribed if they happen to hold a Groq key too.
- If the chosen vendor has no key but the other does, use the other. The alternative is telling
  somebody holding a perfectly good key that AI features need a key.

### Model routing

Two models, assigned by *what the output is for* rather than by how visible it is:

```kotlin
object AiTasks {
    /** Journal entry in the user's voice, memory extraction, weekly recap. */
    const val QUALITY = GroqModels.CHAT        // openai/gpt-oss-120b
    /** Openers, proactive one-liners, query rewriting, connection tests. */
    const val CHEAP = GroqModels.BACKGROUND    // openai/gpt-oss-20b
}
```

**Pinning a model ID is a dependency with an expiry date.** These were
`llama-3.3-70b-versatile` and `llama-3.1-8b-instant`; Groq withdrew both from the free and
developer tiers on 16 August 2026, as Google had withdrawn the whole Gemini 2.0 Flash family on
1 June. The app kept accepting a valid API key and then failed every call behind it, because a
`404` for a decommissioned model was mapped to "network error" — so background work retried it
forever, and Settings rolled the user's key back and told them the connection was at fault.

Two changes came out of that, both structural rather than a version bump. `AiResult.Unsupported`
makes "this model is gone" its own case, so the exhaustive `when`s force every caller to decide
what a permanent failure means for it (workers fail instead of retrying; the digester falls back
to the on-device path). And `AiResult.indictsKey` is now the only thing that may roll a key back,
so **only a rejected key is ever treated as evidence against the key.**

Both replacements are reasoning models, which the previous two were not: their thinking is billed
against the same `max_tokens` the caller asked for. At the budgets here (120 for a check-in line,
900 for a session digest) reasoning alone can consume the cap and return a well-formed response
with empty content. `GroqAiProvider.budget()` adds the thinking allowance on top, so the numbers
at the call sites keep meaning what they say, and every request runs at `reasoning_effort: "low"`
— none of these tasks improve with deliberation.

Picking the replacements does not stop this happening again, so the Gemini side now degrades
instead of failing. `gemini-3.7-flash` is the current workhorse, but its free-tier availability
cannot be checked from a shipped build — that table is behind a login, and a free tier is the
premise of the whole feature. `withChatFallback` drops the quality tier to
`GeminiModels.CHAT_FALLBACK` when the preferred model will not serve a request, treating the two
recoverable failures differently on purpose:

- **404 is permanent** — the model is gone. A `@Volatile` flag remembers it, so the cost of
  discovering it is one call per process rather than one per request. Not persisted: a model
  coming back should cost a restart, not a reinstall.
- **429 is a busy minute.** Fall back for that one call, but never remember it — a sticky
  downgrade would demote someone to the weaker model for the rest of their session over one
  rate limit. If the fallback fails too, the original 429 is what surfaces.

Streaming falls back only when nothing has been emitted yet. A failure after the first delta means
someone is already reading, and starting a second generation underneath them is precisely the
hazard the per-request `retry { noRetry() }` on both `chatStream` calls exists to prevent — an
opt-out `AiModule` had documented for a long time without either provider actually doing it.

This is the inverse of the obvious allocation. The strong model originally had exactly one call
site — the live conversation — while the tasks whose quality is *durable* ran on the cheap one.
That is backwards: **a mediocre chat reply scrolls away; a mediocre memory persists for months.**

The cost was paid for by consolidation rather than by budget: merging the digest's three calls
into one and the entry's two into one means the same text is sent once instead of three times, so
the stronger model costs roughly what the old fan-out did.

**Both vendors have to honour that choice, and for a long time only one did.** Gemini's `jsonCall`
hardcoded the cheap tier and `completeJson` never passed its `model` through, so the digester's
deliberate `AiTasks.QUALITY` was discarded for every Gemini user — the entry in their own voice and
their long-term memories ran on the cheap model, exactly inverting the reasoning above. Nothing
failed; the output was just quietly worse, which is why it went unnoticed. Two tests now pin that
each tier reaches the wire.

Temperature is likewise split by purpose — `0.2` for JSON and extraction ("obey the schema, don't
embellish"), `0.7` for anything the user reads as the companion's own voice.

### Streaming

`chatStream` returns a `Flow<ChatStreamEvent>` that **never throws**: it always terminates with
exactly one `Done` or `Failed`, and `Failed` carries whatever partial text streamed before the
failure, because losing a half-written reply is worse than showing one. `Done` also reports
whether the model stopped because it finished or because it hit the token cap.

---

## 6. The companion pipeline

Four cooperating pieces:

- **`CompanionEngine`** — assembles the prompt: persona fragment, voice profile, rolling summary,
  the strongest memories, and journal excerpts retrieved for the current message.
- **`ConversationDigester`** — turns a finished conversation into a journal entry plus memories,
  using the model.
- **`LocalConversationDigester`** — does the same **without a key**.
- **`MemoryExtractor` / `ProactiveMessenger`** — extraction, and check-ins that speak first.

The two digesters are the design decision worth explaining. Before the local one existed, a
keyless user's conversations were saved and then inert: the digest worker called the model, got
`NoKey`, and returned. Talking taught the companion nothing while writing an entry did — an
asymmetry nobody had chosen and nobody had noticed.

The local digester holds one line: **it never writes prose on the user's behalf.** It composes an
entry from what was actually said, classifies mood with the on-device model, and extracts
memories with `CommitmentDetector` and token heuristics. It will assemble and attribute; it will
not invent.

**Replying from the notification shade.** A proactive check-in is answerable without opening the
app. `CompanionReplyReceiver` persists the typed message and hands off to `CompanionReplyWorker`,
because a broadcast receiver has roughly ten seconds and a streamed reply routinely takes longer.
The worker runs the engine with `persistUserTurn = false` — the receiver already saved that turn —
and posts whatever comes back.

Failure there is silent by design. With no key, or offline, the message is still saved and waiting
in the conversation. A notification reading "I couldn't answer that" is worse than the answer
simply being there when they next open the app.

Conversation history is managed by a park/resume watermark rather than by deletion. Opening the
app should feel like arriving, not like walking back into a room mid-sentence, so a fresh launch
parks the thread and shows a blank screen — but "parked" is one tap from resumed for as long as
the retention window allows. Expiry is judged from the newest message's timestamp, never from
when the park happened; anchoring on park time meant every launch re-stamped the clock, so a
conversation glanced at daily outlived any window without a word being added to it.

---

## 7. The memory system

Memories carry `kind`, `text`, `weight`, `timesSeen`, `createdAt`, `lastSeenAt`, provenance
(`sourceType`/`sourceId`), a `userEdited` flag, and an optional `dueAt`.

**Decay is computed at read time, never stored.** Raw weight is a stored 0..1; effective weight
falls off with time since `lastSeenAt`. Storing decayed weights would require a sweep job and
would make the number meaningless the moment the job was late.

**`userEdited` is load-bearing.** A memory the user has corrected is never auto-revised by
extraction — only reinforced. Extraction may not overwrite a human.

**`dueAt` implements open loops.** "Interview on Tuesday" becomes a memory due Wednesday; once
asked, the loop is cleared so the companion never nags twice.

**Consolidation is deliberately asymmetric.** `MemoryConsolidator` merges near-duplicates freely —
the survivor inherits the combined sighting count, so merging makes a memory *stronger* rather
than losing evidence. Pruning is timid to the point of near-inaction: only memories seen once,
never reinforced, never touched by the user, and untouched for six months. Deleting somebody's
memories on their behalf is the one thing that file has to be nervous about.

### One definition of "the same memory"

"Is this the same memory?" was answered in three places, by three different formulas that
disagreed: true Jaccard at 0.6 in `MemoryExtractor`, containment at 0.6 in
`LocalConversationDigester`, and Jaccard-with-a-stoplist at 0.55 in `MemoryConsolidator`.

Containment and Jaccard diverge sharply when one text is much shorter than the other. So a short
memory could clear the extractor's bar as novel, get written, and then be judged a duplicate by
the consolidator a week later and merged away. Nothing crashed and nothing logged. From the
outside it looked like the app quietly forgetting something the user had told it — the single
worst failure mode for a product whose entire promise is that it remembers.

`MemorySimilarity` is now the only answer, with two uses that are different *on purpose*:

- **`jaccard`** — "are these the same memory", where length matters. A one-clause fact and a
  paragraph that happens to contain it are not the same memory. Symmetric; punishes a length
  mismatch. Threshold 0.6.
- **`containment`** — "have we already got this", where length does not matter. Intersection over
  the *smaller* set, asymmetric on purpose: a short new observation wholly inside an existing
  memory adds nothing. Consolidation is slightly keener at 0.55, because it also merges the
  evidence rather than discarding it.

Both run on content words only. Function words dilute overlap without carrying meaning, and so
does the scaffolding the extraction prompt produces — memories are written in the third person, so
`"the user's"` appears in half of them. Left in, it makes unrelated memories look alike *and*
makes a first-person user edit of the same fact look different, which is exactly backwards.

**Decay lives in one place too**, for the same reason. `exp(-days / 90.0)` existed twice — in
`MemoryRepository` and `MemoryConsolidator` — kept in step by a comment saying they should be.
They were two halves of one decision (what the model sees, and what gets pruned), and a change to
one that missed the other would have pruned memories the prompt was still using. `MemoryDecay`
holds the constant: a 90-day time constant, so a half-life of about 62 days — long enough that a
real fact survives a quiet month, short enough that a one-off from last spring stops competing
with this week. Age is measured in fractional days from hours, because whole days would make
everything seen today weigh the same, and that matters in the one conversation where several
memories are reinforced at once.

---

## 8. On-device intelligence

The most interesting engineering in the project, because it is where "no key, no network" forces
real choices.

### Mood classifier — multinomial naive Bayes

Trained on the user's own labelled entries, on their own device. Chosen over anything heavier for
reasons specific to this app: it trains in milliseconds on a few dozen examples, needs no model
file, and — because it learns *this person's* vocabulary rather than a pretrained one — handles
Hindi, Hinglish and private shorthand for free. A general sentiment model knows what "terrible"
means; this one learns what *your* bad days sound like.

It is guarded by thresholds that make it decline to answer rather than guess:
`MIN_TRAINING_SAMPLES = 12`, `MIN_DISTINCT_MOODS = 2` (one-class training data can only predict
that class), `MIN_CONFIDENCE = 0.55`, and `MIN_TOKEN_OCCURRENCES = 2` (words seen once are noise;
requiring two sightings shrinks the model *and* improves it).

### Semantic search — TF-IDF with co-occurrence query expansion

FTS4 answers "which entries contain these words". That fails the question people actually ask a
journal — *"when did I feel like this before?"* — because the words they use today are rarely the
words they used then. `SemanticIndex` scores whole-document cosine similarity instead, so an
entry about being "wiped out after the sprint" surfaces for a query about feeling "exhausted at
work" with no word in common.

**Why not sentence embeddings?** A bundled encoder costs 30–100 MB of APK, cold-starts slowly,
and its multilingual coverage of Hindi and Hinglish is exactly where this app cannot afford to be
weak. TF-IDF learns the vocabulary of the person using it, so code-mixed writing and private
shorthand work by construction rather than by hoping the pretraining included them.

The honest trade-off — and it is stated as such in the source — is that it cannot know two words
are related unless the user's own writing puts them in similar company. `expandQuery` recovers
some of that by walking a co-occurrence neighbour map. Results below `MIN_SCORE` are dropped
entirely: a weak match is worse than no match, because it costs prompt space and puts an
irrelevant memory in front of the user.

### Pattern finder

Four findings, each with a threshold below which it says nothing: hardest weekday, mood trend,
recurring themes, person correlations. `MIN_DAY_SAMPLES = 3`, `MIN_TREND_DAYS = 3`,
`MIN_RECURRENCE = 3`, `MIN_PERSON_MENTIONS = 3`, and a `MEANINGFUL_MOOD_GAP` of 0.6 before a
difference is worth saying aloud. The hardest-day comparison is each weekday against the mean of
*all other* days, so a uniformly low stretch does not make every day look bad.

Person correlation used to live privately inside `PatternsViewModel`, which meant the *companion*
never got it — `CompanionEngine` called `find()` without the map, so "your days read brighter when
Riya comes up" could appear in the Patterns tab and be structurally invisible to the companion
itself. Both callers now share one implementation.

---

## 9. Safety

`DistressSignal` detects plainly-stated self-harm intent. Three deliberate constraints:

**No model, no network, no key.** Someone in crisis at 3am with no API key configured is exactly
the person who must not hit a dead end. It is plain string matching precisely so it cannot fail.

**High precision, deliberately lower recall.** It matches only unambiguous first-person phrasing.
Missing an oblique hint is a real cost, but wrongly telling someone having a bad Tuesday that you
think they might be suicidal is patronising, and it teaches them the app over-reacts — after which
they stop telling it things. A companion that cries wolf is worse than one that stays quiet.

**It never gates the conversation.** The signal adds a gentle offer of help and softens the
companion's instructions. It does not block sending, change the subject, or refuse to engage.

The matcher strips phrasings that merely *contain* an alarming substring before testing — "I don't
want to die" contains "want to die" and means the opposite — and skips statements about someone
else. `CareCard` surfaces Indian helplines (Tele-MANAS, AASRA, iCall) with a dial intent.

---

## 10. Security

API keys live in `EncryptedSharedPreferences` with an AES256-GCM `MasterKey` from the Android
Keystore. **Not DataStore**, and the reason is explicit: DataStore stores values in plaintext on
device, so on a backed-up, rooted or extracted profile the key is recoverable. With a Keystore
master key, the encryption key itself never leaves the secure element on devices that have one.

Keys are stored per vendor rather than as one "the API key", so switching provider does not throw
away the other credential.

### The backup/restore failure this creates

Encrypted preferences and Android's automatic backup interact badly, and it is worth spelling out
because it is the kind of bug that never appears in testing. Auto-backup will happily capture the
encrypted prefs file. The Keystore master key that decrypts it is **never** backed up — hardware-
backed keys are non-exportable by construction. Restore onto a new device and the file is present
but undecryptable, so `EncryptedSharedPreferences.create()` throws — on app launch, because the
prefs are read on the way to answering "is AI switched on".

A fresh install never hits it. Only a restore does.

Two mitigations, because either alone is insufficient:

1. `backup_rules.xml` and `data_extraction_rules.xml` exclude the prefs file from cloud backup
   **and** device transfer. An absent key file means "reconnect your key"; a present, undecryptable
   one means a crash.
2. `SecureKeyStore` catches `GeneralSecurityException`/`IOException` on open and rebuilds the
   store empty — deleting the file *and* the master key alias, since the Keystore entry may be the
   damaged half (a lock-screen change can invalidate it, and then deleting only the file leaves the
   retry failing identically). Dropping the alias is safe precisely because the only file it
   protected has just been deleted.

The Room database is excluded from auto-backup for a related reason: Room runs in WAL mode, so
committed state is split across `.db`, `-wal` and `-shm`. Auto-backup takes no database lock and
can capture the main file without its matching write-ahead log, restoring a journal that quietly
lost its most recent entries. The app ships a proper checkpointing encrypted export instead, and
a silent partial restore is worse than an honest empty start.

---

## 11. Release engineering

R8 with `isMinifyEnabled` and `isShrinkResources` both on. The keep rules cover the three things
R8 genuinely cannot infer:

- **kotlinx-serialization** — generated `$$serializer` classes and `Companion` fields, reached
  reflectively.
- **Ktor** — reflective transport internals, plus `-dontwarn` for the slf4j binding that does not
  exist on Android.
- **Manifest-resolved classes** — Glance receivers, the QS tile service and `@HiltWorker` workers
  are instantiated by name from the manifest, so nothing in the code graph references them.

Also `-dontwarn com.google.errorprone.annotations.**`: Tink (via `security-crypto`) is compiled
against ErrorProne annotations that are compile-time only, and without this R8 hard-fails the
release build.

The file carries its own warning, which is the useful part: **breakage from these rules is
release-only and invisible to `assembleDebug`.** After changing them you have to install the
release build and exercise the AI path, because that is where serialization actually runs.

Signing reads a gitignored `keystore.properties` with an environment-variable fallback for CI.
Absent credentials leave the release build *unsigned* rather than failing, so anyone can run
`assembleRelease` to check size and R8 output without holding the key.

---

## 12. Testing

33 unit test files, all plain JVM. The `domain/` layer having no Android dependencies is what
makes this possible; there is no Robolectric and no emulator in the loop.

Coverage concentrates where correctness is subtle rather than where it is easy:

| Test | Why it exists |
|---|---|
| `AxiomMigrationsTest` | The migration chain is unbroken from baseline to `LATEST_VERSION` |
| `DistressSignalTest` | False positives and negatives on the highest-stakes classifier |
| `MoodClassifierTest` | Threshold behaviour — that it declines rather than guesses |
| `SemanticIndexTest` / `RetrieverTest` | Ranking and the minimum-score cutoff |
| `PatternFinderTest` | Each finding's threshold, including the silent cases |
| `MemoryConsolidatorTest` / `MemoryHygieneTest` | Merge is safe, prune is timid |
| `CommitmentDetectorTest` | Open-loop extraction |
| `BackupCipherTest` / `BackupIdentityTest` | Round-trip integrity of the export |
| `GroqSseParserTest` | Streaming frame parsing, including partial frames |
| `ConversationRetentionTest` | Expiry anchored to last message, not park time |

---

## 13. Known limitations

Stated plainly, because pretending otherwise is worse.

- **`exportSchema` is off** and there is no `MigrationTestHelper` test, both blocked by the same
  Kotlin 2.0.21 serialization-plugin clash. The chain test and manual verification are a weaker
  guarantee. Fix is Kotlin 2.1+, which also moves the Compose compiler.
- **Semantic search cannot generalise beyond the user's own vocabulary.** TF-IDF with
  co-occurrence expansion mitigates but does not solve this.
- **English-only UI.** `locales_config.xml` declares `en`; the Hindi translation is scoped but not
  written, even though the *voice* layer already handles Hindi and Hinglish.
- **Single module.** At 32k lines this is still comfortable; it would not stay that way, and the
  `data`/`domain`/`ui` split is where the module boundaries would fall.
- **Instrumented test coverage is thin** — the unit suite is strong, but UI tests are effectively
  absent.
