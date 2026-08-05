package com.cosmiclaboratory.axiom.ui.screens.companion

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.components.ShowSnackbarOnError
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.design.rememberLocalized
import com.cosmiclaboratory.axiom.ui.navigation.ShelfSheet
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The companion conversation — and now the whole of the home surface.
 *
 * There is no title bar and no bottom navigation. A bar reading "Axiom" told the
 * user something they already knew, and three permanent tabs claimed that Journal
 * and Patterns compete with the conversation for attention. Both are gone: the
 * greeting names where you are, and one shelf glyph leads everywhere else.
 *
 * User turns are bubbles; COMPANION REPLIES ARE NOT. A reply renders as
 * full-width prose with a thin aiTint rule, so it reads like writing rather
 * than like a chat message — but is unmistakably attributed to the model. On a
 * private journal the user must never have to guess who wrote a sentence.
 *
 * Works fully without an API key: the greeting, the daily opener, the mood row,
 * typing and saving to the journal are all local. Only the model reply needs the
 * key, and its absence is an inline card with a way forward, never a dead end.
 */
@Composable
fun CompanionScreen(
    onOpenEntry: (Long) -> Unit,
    onConnectAi: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenTalks: () -> Unit,
    onSaveToJournal: (String) -> Unit,
    onWriteAbout: (String) -> Unit,
    onContinueDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompanionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showDisclosure by remember { mutableStateOf(false) }
    var showShelf by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Mic permission gates both the composer mic and the hands-free toggle.
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceAction?.invoke()
        pendingVoiceAction = null
    }
    fun withMicPermission(action: () -> Unit) {
        pendingVoiceAction = action
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    ShowSnackbarOnError(state.voiceError, viewModel::dismissVoiceError)

    LaunchedEffect(state.messages.size, state.streamingText?.length) {
        val extraRows = if (state.sending) 1 else 0
        val lastIndex = state.messages.lastIndex + extraRows
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    /*
     * The greeting is about now, so it belongs with the newest message: expanded
     * at rest, out of the way once you scroll back into history.
     *
     * This is driven by scroll DELTAS rather than by `listState.canScrollForward`,
     * and that is not a style preference — the obvious version oscillates.
     * Expanding the greeting makes the content taller, which makes the list
     * scrollable, which collapses the greeting, which makes the content shorter,
     * which makes it unscrollable, which expands it again. The loop only shows up
     * once a conversation grows tall enough to sit near the viewport height, and
     * then it flickers forever.
     *
     * A scroll delta is user input, not a layout result, so nothing the greeting
     * does can feed back into it.
     */
    var greetingExpanded by remember { mutableStateOf(true) }
    val collapseOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Positive dy = content moving down = travelling back through
                // history. Negative = returning toward the newest message, which
                // is also what the programmatic scroll after sending produces.
                if (available.y > SCROLL_INTENT_SLOP) greetingExpanded = false
                else if (available.y < -SCROLL_INTENT_SLOP) greetingExpanded = true
                return Offset.Zero
            }
        }
    }

    // Confirm a kept thought where it happened, with a way to go read it.
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(state.keptEntryId) {
        val id = state.keptEntryId ?: return@LaunchedEffect
        viewModel.clearKept()
        val result = snackbar.showSnackbar(
            message = "Kept it in your story",
            actionLabel = "Read it",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) onOpenEntry(id)
    }

    AmbientCanvas(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Swipe right anywhere means "take me to writing". The prompt
                // cards below consume the gesture themselves so that swiping one
                // lands on that question rather than on the timeline.
                .swipeBetween(onSwipeRight = onOpenJournal)
        ) {
            CompanionGreeting(
                greeting = state.greeting,
                lead = state.greetingLead,
                expanded = greetingExpanded,
                entryCount = state.entryCount,
                onOpenShelf = { showShelf = true },
                onOpenJournal = onOpenJournal,
                onClearConversation = if (state.messages.isNotEmpty()) {
                    { confirmClear = true }
                } else null
            )

            CompanionRitualHeader(
                todayMood = state.todayMood,
                todayEmotion = state.todayEmotion,
                todayMoodInferred = state.todayMoodInferred,
                writingDraft = state.writingDraft,
                handsFree = state.handsFree,
                ttsAvailable = state.ttsAvailable,
                onRecordMood = viewModel::recordMood,
                onContinueDraft = onContinueDraft,
                onToggleHandsFree = {
                    if (state.handsFree) {
                        viewModel.setHandsFree(false)
                    } else {
                        withMicPermission { viewModel.setHandsFree(true) }
                    }
                }
            )

            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(collapseOnScroll)
                        .testTag("list:companion"),
                    contentPadding = PaddingValues(
                        horizontal = AxiomTheme.space.screenH,
                        vertical = AxiomTheme.space.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.lg)
                ) {
                    state.resurfaced?.let { old ->
                        item(key = "resurfaced-${old.id}") {
                            ResurfacedCard(entry = old, onOpen = { onOpenEntry(old.id) })
                        }
                    }

                    val newestId = state.messages.lastOrNull()?.id
                    var lastDate: LocalDate? = null
                    state.messages.forEach { message ->
                        val messageDate = message.createdAt.toLocalDate()
                        if (messageDate != lastDate) {
                            lastDate = messageDate
                            item(key = "date-$messageDate") { DateSeparator(messageDate) }
                        }
                        item(key = message.id) {
                            // Only the newest turn arrives. Animating everything
                            // would re-run on every scroll as rows recycle, which
                            // turns reading back through history into a flicker.
                            //
                            // Placement is animated regardless, and separately:
                            // it moves rows that are already on screen when the
                            // thinking row is replaced by the real reply, which
                            // is a different event from a row appearing.
                            Arriving(
                                enabled = message.id == newestId,
                                modifier = axiomItemMotion(fade = false)
                            ) {
                                if (message.isUser) {
                                    CompanionUserBubble(
                                        text = message.text,
                                        onSaveToJournal = { onSaveToJournal(message.text) },
                                        onSwipeRight = onOpenJournal
                                    )
                                } else {
                                    val isSpeaking = state.speakingMessageId == message.id
                                    CompanionReplyBlock(
                                        text = message.text,
                                        citedIds = message.citedEntryIds,
                                        citedEntries = state.citedEntries,
                                        onOpenEntry = onOpenEntry,
                                        onSaveToJournal = { onSaveToJournal(message.text) },
                                        onSwipeRight = onOpenJournal,
                                        speaking = isSpeaking,
                                        onPlay = when {
                                            !state.ttsAvailable -> null
                                            isSpeaking -> viewModel::stopSpeaking
                                            else -> {
                                                { viewModel.speakMessage(message.id, message.text) }
                                            }
                                        },
                                        // Only the app's own prompts get this. A
                                        // reply in conversation is not a question
                                        // waiting to be answered in writing.
                                        onWriteAbout = if (message.isPrompt) {
                                            { onWriteAbout(message.text) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    if (state.sending) {
                        item("thinking") {
                            val streaming = state.streamingText
                            if (streaming.isNullOrEmpty()) {
                                Arriving(enabled = true) {
                                    Row(Modifier.padding(vertical = AxiomTheme.space.sm)) {
                                        ThinkingIndicator()
                                    }
                                }
                            } else {
                                CompanionReplyBlock(
                                    text = streaming,
                                    citedIds = emptyList(),
                                    citedEntries = emptyList(),
                                    onOpenEntry = onOpenEntry,
                                    onSaveToJournal = {},
                                    onPlay = null,
                                    onSwipeRight = onOpenJournal
                                )
                            }
                        }
                    }
                }
            }

            // Below the conversation and above the composer: visible without
            // covering what they just said, and never in the way of saying more.
            state.care?.let { level ->
                Column(Modifier.padding(horizontal = AxiomTheme.space.screenH)) {
                    CareCard(level = level, onDismiss = viewModel::dismissCare)
                }
                Spacer(Modifier.height(AxiomTheme.space.sm))
            }

            state.error?.let { error ->
                Column(Modifier.padding(horizontal = AxiomTheme.space.screenH)) {
                    AxiomErrorSurface(
                        error = error,
                        onRetry = if (error is AxiomError.NoAiKey) null else viewModel::retry,
                        onPrimary = if (error is AxiomError.NoAiKey) onConnectAi else null,
                        onDismiss = viewModel::dismissError
                    )
                    // Their words must survive a missing key or dead network.
                    state.lastFailedText?.let { failed ->
                        TextButton(onClick = {
                            viewModel.dismissError()
                            onSaveToJournal(failed)
                        }) {
                            Text(
                                "Keep this in your journal instead",
                                style = AxiomTheme.type.uiLabel,
                                color = AxiomTheme.colors.accent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(AxiomTheme.space.sm))
            }

            CompanionComposerBar(
                draft = state.draft,
                enabled = !state.sending,
                placeholder = when {
                    state.listening -> "I'm listening…"
                    state.transcribing -> "Getting that down…"
                    else -> rememberInvitation()
                },
                listening = state.listening,
                onChange = viewModel::setDraft,
                onSend = viewModel::send,
                onKeep = viewModel::keepDraft,
                onMic = { withMicPermission(viewModel::toggleListening) }
            )

            /*
             * The bar yields to the keyboard.
             *
             * A permanent bar under the composer would sit between the send
             * button and the bottom of the screen while typing — the one moment
             * the user is certainly not navigating. It leaves when the IME
             * arrives and comes back when it goes, so switching places stays one
             * tap away without ever competing with writing.
             */
            val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
            AnimatedVisibility(
                visible = imeBottom == 0,
                enter = axiomExpand(),
                exit = axiomCollapse()
            ) {
                AxiomPlaceBar(
                    current = AxiomPlace.Conversation,
                    onSelect = { place ->
                        if (place == AxiomPlace.Story) onOpenJournal()
                    }
                )
            }
        }
    }

    if (showShelf) {
        ShelfSheet(
            onDismiss = { showShelf = false },
            onOpenJournal = onOpenJournal,
            onOpenPatterns = onOpenPatterns,
            onOpenTalks = onOpenTalks,
            onOpenMemories = onOpenMemories,
            onOpenSettings = onOpenSettings,
            onDisclosure = { showDisclosure = true },
            onClearConversation = if (state.messages.isNotEmpty()) {
                { confirmClear = true }
            } else null
        )
    }

    if (showDisclosure) {
        WhatGetsSentSheet(onDismiss = { showDisclosure = false })
    }

    // Clearing was wired straight to the viewmodel with no confirmation at all,
    // which for the one irreversible action in the app is the wrong default.
    if (confirmClear) {
        AxiomConfirmDialog(
            title = "Clear this conversation?",
            body = "The messages go. Anything already kept in your story stays, " +
                "and so does what I remember.",
            confirmLabel = "Clear it",
            destructive = true,
            onConfirm = {
                confirmClear = false
                viewModel.clearThread()
            },
            onDismiss = { confirmClear = false }
        )
    }
}

/**
 * Openings the companion offers when the field is empty. Picked once per visit
 * rather than on a timer — text that changes while you are looking at it reads
 * as a machine cycling through options, not as someone asking.
 */
/**
 * Ignore scroll jitter below this. Small enough that a deliberate flick always
 * registers, large enough that settling or a stray pixel never does.
 */
private const val SCROLL_INTENT_SLOP = 2f

private val INVITATIONS = listOf(
    "What happened today?",
    "I'm listening.",
    "Tell me anything.",
    "What's on your mind?"
)

@Composable
private fun rememberInvitation(): String = remember { INVITATIONS.random() }

/**
 * A gentle upward arrival. Messages should settle into the room rather than
 * appear in it, which is the difference between a notification and a reply.
 */
@Composable
private fun Arriving(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // The Box stays even when disabled: [modifier] may carry the list's
    // placement animation, and dropping the wrapper would silently drop that
    // for every message except the newest one.
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val motion = AxiomTheme.motion
    val density = LocalDensity.current
    var landed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = motion.spatialExpressive,
        label = "arrive"
    )
    LaunchedEffect(Unit) { landed = true }
    Box(
        modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * with(density) { 14.dp.toPx() }
        }
    ) { content() }
}

/**
 * Something you wrote a while ago, handed back.
 *
 * Rereading your own past is the strongest reason anyone keeps a journal, and
 * it is the one feature here that needs no key, no network and no model — it is
 * just your own words, returned at a distance.
 */
@Composable
private fun ResurfacedCard(
    entry: com.cosmiclaboratory.axiom.domain.model.Entry,
    onOpen: () -> Unit
) {
    val c = AxiomTheme.colors
    val days = ChronoUnit.DAYS.between(entry.createdAt.toLocalDate(), LocalDate.now())
    val whenPhrase = when {
        days >= 365 -> "About a year ago"
        days >= 60 -> "About ${days / 30} months ago"
        else -> "$days days ago"
    }
    AxiomCard(tone = CardTone.Ai, onClick = onOpen) {
        Text(whenPhrase, style = AxiomTheme.type.uiOverline, color = c.inkFaint)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(
            text = entry.displayTitle,
            style = AxiomTheme.type.readingBody,
            color = c.ink,
            maxLines = 3
        )
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text("You wrote this. Want to read it again?", style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
    }
}

@Composable
private fun DateSeparator(date: LocalDate) {
    val formatter = rememberLocalized { locale ->
        DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    }
    val label = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(formatter)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = AxiomTheme.space.xs),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, style = AxiomTheme.type.uiOverline, color = AxiomTheme.colors.inkFaint)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompanionUserBubble(
    text: String,
    onSaveToJournal: () -> Unit,
    onSwipeRight: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            style = AxiomTheme.type.uiBody,
            color = AxiomTheme.colors.onAccentSoft,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(AxiomTheme.shapes.lg)
                .combinedClickable(onClick = {}, onLongClick = onSaveToJournal)
                // AFTER the clickable, deliberately. Pointer events reach the
                // innermost handler first, so a swipe declared outside the
                // clickable never arrives — the bubble eats it and the gesture
                // only works in the gaps between messages.
                .swipeBetween(onSwipeRight = onSwipeRight)
                .background(AxiomTheme.colors.accentSoft)
                .padding(horizontal = AxiomTheme.space.base, vertical = AxiomTheme.space.md)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompanionReplyBlock(
    text: String,
    citedIds: List<Long>,
    citedEntries: List<com.cosmiclaboratory.axiom.domain.model.Entry>,
    onOpenEntry: (Long) -> Unit,
    onSaveToJournal: () -> Unit,
    onPlay: (() -> Unit)?,
    onSwipeRight: () -> Unit,
    speaking: Boolean = false,
    onWriteAbout: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onSaveToJournal)
            // Declared after the clickable so drags reach it; a prompt swipes to
            // its own question, anything else swipes to the timeline.
            .swipeBetween(onSwipeRight = onWriteAbout ?: onSwipeRight)
    ) {
        // The rule, not a bubble: attribution without turning prose into chat.
        Box(
            Modifier
                .width(2.dp)
                .heightIn(min = 24.dp)
                .clip(AxiomTheme.shapes.full)
                .background(c.aiTint.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(AxiomTheme.space.base))
        Column(Modifier.weight(1f)) {
            Text(text, style = AxiomTheme.type.readingBody, color = c.ink)

            if (onWriteAbout != null) {
                Spacer(Modifier.height(AxiomTheme.space.sm))
                Text(
                    text = "Write about this",
                    style = AxiomTheme.type.uiLabel,
                    color = c.accent,
                    modifier = Modifier
                        .clip(AxiomTheme.shapes.sm)
                        .clickable(onClick = onWriteAbout)
                        .padding(
                            vertical = AxiomTheme.space.sm,
                            horizontal = AxiomTheme.space.xs
                        )
                )
            }

            val cited = citedEntries.filter { it.id in citedIds }
            if (cited.isNotEmpty()) {
                Spacer(Modifier.height(AxiomTheme.space.md))
                Text(
                    if (cited.size == 1) "I looked back at this" else "I looked back at these",
                    style = AxiomTheme.type.uiOverline,
                    color = c.inkFaint
                )
                Spacer(Modifier.height(AxiomTheme.space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                    cited.take(3).forEach { entry ->
                        EntryChip(entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }
        }
        if (onPlay != null) {
            // The same control starts and stops. A play button with no way back
            // is how you end up listening to a paragraph you didn't want.
            AxiomIconButton(
                icon = if (speaking) Icons.Filled.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                label = if (speaking) "Stop reading" else "Read aloud",
                onClick = onPlay,
                tint = if (speaking) c.accent else c.inkFaint
            )
        }
    }
}

/**
 * The invitation to speak. A soft rounded field on a raised surface, whose
 * border warms toward accent on focus — the one place in the app where a control
 * is allowed to look eager.
 */
@Composable
private fun CompanionComposerBar(
    draft: String,
    enabled: Boolean,
    placeholder: String,
    listening: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onKeep: () -> Unit,
    onMic: () -> Unit
) {
    val c = AxiomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 0.55f else 0.0f,
        animationSpec = AxiomTheme.motion.effectsStandard,
        label = "composer-focus"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            /*
             * The keyboard inset only — NOT safeDrawing.
             *
             * The composer is no longer the bottom-most thing on the screen; the
             * place bar is, and it reserves the navigation-bar inset itself.
             * Because the two are siblings rather than nested, nothing consumes
             * that inset once, so asking for it here reserved it a second time
             * and left a visible band of dead space between them.
             *
             * The keyboard is still this row's problem: when it opens the bar
             * leaves, and nothing else is left to lift the composer clear.
             */
            .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom))
            .padding(
                horizontal = AxiomTheme.space.screenH,
                vertical = AxiomTheme.space.sm
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(AxiomTheme.shapes.xl)
                .background(c.surfaceRaised)
                .border(
                    width = 1.dp,
                    color = if (borderAlpha > 0f) {
                        c.accent.copy(alpha = borderAlpha)
                    } else {
                        c.hairline
                    },
                    shape = AxiomTheme.shapes.xl
                )
                .padding(
                    start = AxiomTheme.space.base,
                    end = AxiomTheme.space.xs,
                    top = AxiomTheme.space.xs,
                    bottom = AxiomTheme.space.xs
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .padding(vertical = AxiomTheme.space.md)
            ) {
                // While listening the waveform stands in for the placeholder —
                // the field is not empty so much as waiting, and that reads
                // better as breath than as a sentence asking you to type.
                if (draft.isEmpty()) {
                    if (listening) {
                        BreathingWaveform()
                    } else {
                        Text(placeholder, style = AxiomTheme.type.uiBody, color = c.inkFaint)
                    }
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onChange,
                    enabled = enabled,
                    interactionSource = interaction,
                    textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth().testTag("field:companion")
                )
            }
            AxiomIconButton(
                icon = if (listening) Icons.Filled.StopCircle else Icons.Filled.KeyboardVoice,
                label = if (listening) "Stop listening" else "Speak instead of typing",
                onClick = onMic,
                // Listening is not an error state, so it never turns red.
                tint = if (listening) c.accent else c.inkFaint
            )
        }
        // Two ways out of one box, decided after writing rather than before.
        // Keep needs no API key and never fails — it is the half of Axiom that
        // always works, and it should not be reachable only by sending first
        // and being told no.
        AxiomIconButton(
            icon = Icons.Outlined.EditNote,
            label = "Keep this in your journal",
            onClick = onKeep,
            enabled = draft.isNotBlank(),
            tint = c.inkMuted
        )
        AxiomIconButton(
            icon = Icons.AutoMirrored.Filled.Send,
            label = "Ask the companion",
            onClick = onSend,
            enabled = enabled && draft.isNotBlank(),
            tint = c.accent
        )
    }
}

/** Enumerates exactly what leaves the device. Vague reassurance is worth nothing. */
@Composable
fun WhatGetsSentSheet(onDismiss: () -> Unit) {
    AxiomBottomSheet(title = "What gets sent?", onDismiss = onDismiss) {
        val c = AxiomTheme.colors
        Text(
            "Axiom stores everything on this device. Nothing is uploaded unless you " +
                "connect an AI key yourself.",
            style = AxiomTheme.type.uiBody,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        listOf(
            "Your message, as typed.",
            "Your recent messages in this conversation, and a short running summary of older ones.",
            "Up to 20 remembered details — see and edit them all in \"What I remember\".",
            "Up to 4 short journal excerpts that matched your message, with their dates and moods.",
            "Today's mood, your writing streak, and the time of day.",
            "Your chosen companion voice."
        ).forEach { line ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("•", style = AxiomTheme.type.uiBody, color = c.inkFaint)
                Spacer(Modifier.width(AxiomTheme.space.sm))
                Text(line, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.base))
        Text(
            "All of it goes to your chosen provider with your own key and is never stored " +
                "anywhere but on this device.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        Text("Once a day, in the background", style = AxiomTheme.type.uiOverline, color = c.inkFaint)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(
            "To write the questions it greets you with, and to turn a finished " +
                "conversation into a journal entry, Axiom sends what it remembers and what " +
                "you wrote — without you pressing send. Disconnect the key in Your space to " +
                "stop this entirely; everything else keeps working.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
    }
}
