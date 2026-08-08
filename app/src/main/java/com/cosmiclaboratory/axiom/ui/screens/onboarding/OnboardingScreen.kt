package com.cosmiclaboratory.axiom.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.notification.CheckInTimes
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.screens.settings.KeyTestState
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlinx.coroutines.launch

/**
 * Seven pages, where page six IS the first entry and page seven is the offer
 * that only makes sense once it has been written.
 *
 * The original four deliberately did not mention an API key, on the grounds
 * that asking during setup implies the app needs the cloud to work and
 * undercuts the privacy position. That reasoning still holds and the ordering
 * here is built around it: nothing is asked for until the app has already
 * proved it works — you have written and saved something real, offline, before
 * the word "key" appears. What changed is that saying nothing at all left
 * people running a journal whose best half was switched off, with no idea it
 * existed or that turning it on was free.
 *
 * The other two new pages exist because the core loop was never stated
 * anywhere: you can talk to this thing about your day and it writes the entry
 * for you. An app whose central feature is a secret is not a private app, just
 * an unused one.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val c = AxiomTheme.colors

    fun next() = scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }

    /*
     * Same shape as SettingsScreen's launcher, and for the same reason: a
     * permission prompt lands far better attached to the feature someone just
     * switched on than fired at launch. The difference is that this one exists
     * at all — onboarding used to set daily_nudge_enabled and never ask, so on
     * API 33+ the whole proactive system was dead on arrival for anyone who
     * never went looking in settings.
     */
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed either way: declining the system dialog is not declining the
        // feature, and the in-app conversation works regardless.
        pendingNotificationAction?.invoke()
        pendingNotificationAction = null
    }
    fun requestNotifications(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationAction = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val lastPage = PAGE_COUNT - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("screen:onboarding")
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(AxiomTheme.space.sm),
            horizontalArrangement = Arrangement.End
        ) {
            // Skippable at every step — a setup flow you cannot escape is a
            // reason to uninstall, not a reason to engage.
            TextButton(onClick = { viewModel.finish(onFinished) }) {
                Text("Skip", style = AxiomTheme.type.uiLabel, color = c.inkFaint)
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AxiomTheme.space.xl),
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> WelcomePage(
                        displayName = state.displayName,
                        onDisplayNameChange = viewModel::setDisplayName
                    )

                    1 -> CoreLoopPage()

                    2 -> YourStoryPage()

                    // The same VoicePreset list settings edits under "How I
                    // talk", so the choice made here is one someone can find
                    // again — and, unlike the persona this replaced, one that
                    // actually reaches the model.
                    3 -> Page(
                        title = "Pick a companion voice",
                        body = "How I talk to you. Every one of these is tunable later, " +
                            "line by line, in Your space."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.base))
                        VoicePreset.entries.forEach { preset ->
                            AxiomCard(
                                tone = if (preset == state.voice) CardTone.Accent else CardTone.Neutral,
                                onClick = { viewModel.setVoice(preset) },
                                modifier = Modifier.padding(vertical = AxiomTheme.space.xs)
                            ) {
                                Text(
                                    preset.displayName,
                                    style = AxiomTheme.type.uiTitleSmall,
                                    color = c.ink
                                )
                                Text(
                                    preset.blurb,
                                    style = AxiomTheme.type.uiBodySmall,
                                    color = c.inkMuted
                                )
                            }
                        }
                    }

                    4 -> Page(
                        title = "A gentle nudge?",
                        body = "One message a day, only on the days you haven't been by, " +
                            "and a look back at your week on Sundays. Off by default."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.lg))
                        // Asking when, here, rather than only in settings: the
                        // yes/no used to hand everyone a silent 21:00 default
                        // with no hint that a time existed to change.
                        Text(
                            "Not before",
                            style = AxiomTheme.type.uiLabel,
                            color = c.inkMuted
                        )
                        Spacer(Modifier.height(AxiomTheme.space.sm))
                        CheckInTimes.ALL.forEach { choice ->
                            AxiomCard(
                                tone = if (choice.minuteOfDay == state.checkInMinuteOfDay) {
                                    CardTone.Accent
                                } else CardTone.Neutral,
                                onClick = { viewModel.setCheckInMinuteOfDay(choice.minuteOfDay) },
                                modifier = Modifier.padding(vertical = AxiomTheme.space.xs)
                            ) {
                                Text(
                                    choice.label,
                                    style = AxiomTheme.type.uiTitleSmall,
                                    color = c.ink
                                )
                            }
                        }
                        Spacer(Modifier.height(AxiomTheme.space.lg))
                        Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                            Button(
                                // Ask for the permission attached to the moment
                                // they say yes. Without this the toggle was
                                // honoured, the worker ran, and every single
                                // notification was dropped by the API 33+ check
                                // in ProactiveMessenger — silently, forever, or
                                // until they happened to open settings.
                                onClick = {
                                    viewModel.setNudge(true)
                                    requestNotifications { next() }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = c.accent, contentColor = c.onAccent
                                ),
                                shape = AxiomTheme.shapes.sm
                            ) { Text("Yes, remind me", style = AxiomTheme.type.uiLabel) }
                            TextButton(onClick = { viewModel.setNudge(false); next() }) {
                                Text("No thanks", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                            }
                        }
                    }

                    5 -> Page(
                        title = "Write your first line",
                        body = "It doesn't have to be good. That's rather the point."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.lg))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(AxiomTheme.shapes.md)
                                .background(c.surfaceSunken)
                                .padding(AxiomTheme.space.base)
                        ) {
                            if (state.firstEntry.isEmpty()) {
                                Text(
                                    "Today I…",
                                    style = AxiomTheme.type.readingBody,
                                    color = c.inkFaint
                                )
                            }
                            BasicTextField(
                                value = state.firstEntry,
                                onValueChange = viewModel::setFirstEntry,
                                textStyle = AxiomTheme.type.readingBody.copy(color = c.ink),
                                cursorBrush = SolidColor(c.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp)
                                    .testTag("field:first_entry")
                            )
                        }
                    }

                    else -> ConnectKeyPage(
                        vendor = state.aiVendor,
                        keyDraft = state.keyDraft,
                        masked = state.keyMasked,
                        keyTest = state.keyTest,
                        onVendorChange = viewModel::setAiVendor,
                        onKeyDraftChange = viewModel::setKeyDraft,
                        onToggleMask = viewModel::toggleKeyMask,
                        onSaveAndTest = viewModel::saveAndTestKey
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(AxiomTheme.space.screenH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageDots(current = pager.currentPage, total = PAGE_COUNT)
            Spacer(Modifier.weight(1f))

            // On the key page the primary action belongs to the card, so the
            // button here is the way past it — deliberately not styled as a
            // lesser choice, because declining has to be as easy as accepting.
            if (pager.currentPage == lastPage) {
                TextButton(onClick = { viewModel.finish(onFinished) }) {
                    Text(
                        if (state.keyTest == KeyTestState.Success) "Start" else "Do this later",
                        style = AxiomTheme.type.uiLabel,
                        color = if (state.keyTest == KeyTestState.Success) c.accent else c.inkMuted
                    )
                }
            } else {
                Button(
                    onClick = {
                        // Saved on the way out of the writing page, not at the
                        // end of the flow, so the next page can honestly say it
                        // already happened.
                        if (pager.currentPage == FIRST_ENTRY_PAGE) viewModel.commitFirstEntry()
                        next()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent, contentColor = c.onAccent
                    ),
                    shape = AxiomTheme.shapes.sm
                ) {
                    Text(
                        if (pager.currentPage == FIRST_ENTRY_PAGE && state.firstEntry.isNotBlank()) {
                            "Save and continue"
                        } else "Continue",
                        style = AxiomTheme.type.uiLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(displayName: String, onDisplayNameChange: (String) -> Unit) {
    val c = AxiomTheme.colors
    Row(Modifier.fillMaxWidth()) {
        AxiomLogo()
    }
    Spacer(Modifier.height(AxiomTheme.space.lg))
    Page(
        title = "Your journal, on your device",
        body = "Everything you write is stored locally. No account, no " +
            "upload, no sync you didn't ask for."
    ) {
        Spacer(Modifier.height(AxiomTheme.space.xl))
        Text("What should we call you?", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Box {
            if (displayName.isEmpty()) {
                Text("Optional", style = AxiomTheme.type.readingSubtitle, color = c.inkFaint)
            }
            BasicTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                textStyle = AxiomTheme.type.readingSubtitle.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("field:name")
            )
        }
    }
}

/** The thing the app does that nothing on screen has ever said out loud. */
@Composable
private fun CoreLoopPage() {
    Page(
        title = "Talk, and I'll write it down",
        body = "The main way to journal here is to have a conversation. When " +
            "you're done, I read it back and turn it into an entry in your " +
            "story — dated, titled, saved. You never press save."
    ) {
        Spacer(Modifier.height(AxiomTheme.space.xl))
        Bullet(
            "Say anything",
            "There's a question waiting each day, but it's a way in, not the " +
                "subject. Tell me about your day, what annoyed you, what you " +
                "don't want to forget."
        )
        Bullet(
            "Or say it out loud",
            "The microphone types for you. Hands-free goes further: I read my " +
                "replies aloud and start listening again, so you can journal " +
                "with the phone face down."
        )
        Bullet(
            "Nothing is lost",
            "Clear a conversation and it gets written up first. What it was " +
                "about survives even when the messages don't."
        )
    }
}

@Composable
private fun YourStoryPage() {
    Page(
        title = "Everything ends up in one story",
        body = "One timeline, in the order it happened, however it got written."
    ) {
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Bullet("Written, Prompted, Voice, Talks", "Filter by how an entry came to be.")
        Bullet("Search and calendar", "Find it by word, or by the week you think it was.")
        Bullet("Patterns", "What your writing keeps circling back to, read on-device.")
        Bullet("What I remember", "The facts I'm holding on to — yours to edit or delete.")
    }
}

@Composable
private fun ConnectKeyPage(
    vendor: AiVendor,
    keyDraft: String,
    masked: Boolean,
    keyTest: KeyTestState,
    onVendorChange: (AiVendor) -> Unit,
    onKeyDraftChange: (String) -> Unit,
    onToggleMask: () -> Unit,
    onSaveAndTest: () -> Unit
) {
    val c = AxiomTheme.colors

    if (keyTest == KeyTestState.Success) {
        Page(
            title = "That's it — we're connected",
            body = "I can write back now, and turn your conversations into entries. " +
                "Everything still lives on this device."
        ) {}
        return
    }

    Page(
        title = "Want me to write back?",
        body = "Axiom works fully offline, and everything you just wrote is already " +
            "saved. A free key adds the half that talks: replies, and turning a " +
            "conversation into a journal entry on its own."
    ) {
        Spacer(Modifier.height(AxiomTheme.space.lg))

        Text(
            "Both of these have a free tier a personal journal won't exhaust. " +
                "No card, no trial that expires.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.md))

        AiVendor.entries.forEach { option ->
            AxiomCard(
                tone = if (option == vendor) CardTone.Accent else CardTone.Neutral,
                onClick = { onVendorChange(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AxiomTheme.space.xs)
            ) {
                Text(option.displayName, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                Spacer(Modifier.height(2.dp))
                Text(option.summary, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
            }
        }

        Spacer(Modifier.height(AxiomTheme.space.md))
        ApiKeyConnectCard(
            vendor = vendor,
            keyDraft = keyDraft,
            masked = masked,
            testing = keyTest == KeyTestState.Testing,
            onKeyDraftChange = onKeyDraftChange,
            onToggleMask = onToggleMask,
            onSaveAndTest = onSaveAndTest,
            // The one place numbered instructions belong: nobody has been asked
            // for an API key thirty seconds into a journalling app before.
            showSteps = true
        )

        if (keyTest is KeyTestState.Failed) {
            Spacer(Modifier.height(AxiomTheme.space.md))
            AxiomErrorSurface(error = keyTest.error, onRetry = onSaveAndTest)
        }

        Spacer(Modifier.height(AxiomTheme.space.md))
        Text(
            "You can do this later in Settings → AI, and nothing here stops " +
                "working if you never do.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkFaint
        )
    }
}

@Composable
private fun Bullet(title: String, body: String) {
    val c = AxiomTheme.colors
    Row(Modifier.padding(bottom = AxiomTheme.space.md)) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(AxiomTheme.shapes.full)
                .background(c.accent)
        )
        Spacer(Modifier.width(AxiomTheme.space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(body, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
        }
    }
}

@Composable
private fun Page(title: String, body: String, content: @Composable () -> Unit) {
    Text(title, style = AxiomTheme.type.uiDisplay, color = AxiomTheme.colors.ink)
    Spacer(Modifier.height(AxiomTheme.space.md))
    Text(body, style = AxiomTheme.type.readingBody, color = AxiomTheme.colors.inkMuted)
    content()
}

@Composable
private fun PageDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(if (index == current) 8.dp else 6.dp)
                    .clip(AxiomTheme.shapes.full)
                    .background(
                        if (index == current) AxiomTheme.colors.accent
                        else AxiomTheme.colors.hairline
                    )
            )
        }
    }
}

private const val PAGE_COUNT = 7

/** Writing the first line. The key page after it depends on this having happened. */
private const val FIRST_ENTRY_PAGE = 5
