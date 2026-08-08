package com.cosmiclaboratory.axiom.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.ConversationRetention
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
import com.cosmiclaboratory.axiom.domain.notification.CheckInTimes
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Face
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity
import androidx.compose.material.icons.outlined.RecordVoiceOver

/**
 * Settings hub. Every row shows its CURRENT VALUE — nothing is hidden behind a
 * bare chevron, which is the discoverability failure Apple Journal is faulted for.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenHowITalk: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTimeSheet by remember { mutableStateOf(false) }
    var showRetentionSheet by remember { mutableStateOf(false) }
    var showNameSheet by remember { mutableStateOf(false) }
    var showCompanionNameSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }

    /*
     * POST_NOTIFICATIONS is declared in the manifest but was never requested,
     * so on API 33+ every check-in would have been silently dropped. Asked here
     * rather than at launch: permission prompts land far better attached to the
     * feature the user just switched on.
     */
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Enable regardless: a denied permission still leaves the in-app
        // conversation working, and the toggle should reflect their intent.
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

    AxiomScaffold(
        title = "Your space",
        screenTag = "screen:settings",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "AI",
                // The voice, not the derived persona: printing the enum name
                // gave "Connected · calm", which is neither a thing the user
                // picked nor a label they have seen anywhere else.
                summary = if (state.keyConnected) "Connected · ${state.voice.preset.displayName}"
                else "Not connected — everything else works offline",
                onClick = onOpenAi
            )
            SettingRow(
                icon = Icons.Outlined.Psychology,
                title = "What I remember",
                summary = "See, edit or forget anything I've kept",
                onClick = onOpenMemories
            )
            // The manner comes before the microphone: this is the one people
            // are looking for when they tap something called "voice".
            SettingRow(
                icon = Icons.Outlined.RecordVoiceOver,
                title = "How I talk",
                summary = state.voice.preset.displayName + " · " + state.voice.preset.blurb,
                onClick = onOpenHowITalk
            )
            SettingRow(
                icon = Icons.Outlined.KeyboardVoice,
                title = "Speech & sound",
                summary = state.voiceLanguage.displayLabel +
                    if (state.autoSpeakEnabled) " · speaks replies" else "",
                onClick = onOpenVoice
            )
            SettingRow(
                icon = Icons.Outlined.Palette,
                title = "Appearance",
                summary = when (state.themeMode) {
                    ThemeMode.SYSTEM -> "Follow system"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.AMOLED -> "Black (OLED)"
                },
                onClick = onOpenAppearance
            )
            SettingRow(
                icon = Icons.Outlined.Notifications,
                title = "Check in when I'm quiet",
                summary = if (state.dailyNudgeEnabled) {
                    "Not before ${formatMinuteOfDay(state.checkInMinuteOfDay)} · tap to change"
                } else {
                    "Off — the companion only speaks when you open the app"
                },
                onClick = { showTimeSheet = true },
                trailing = {
                    Switch(
                        checked = state.dailyNudgeEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifications { viewModel.setDailyNudge(true) }
                            else viewModel.setDailyNudge(false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AxiomTheme.colors.onAccent,
                            checkedTrackColor = AxiomTheme.colors.accent
                        )
                    )
                }
            )
            SettingRow(
                icon = Icons.Outlined.Notifications,
                title = "Look back with me on Sundays",
                summary = if (state.weeklyRecapEnabled) "Sunday evenings" else "Off",
                onClick = {
                    if (state.weeklyRecapEnabled) viewModel.setWeeklyRecap(false)
                    else requestNotifications { viewModel.setWeeklyRecap(true) }
                },
                trailing = {
                    Switch(
                        checked = state.weeklyRecapEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotifications { viewModel.setWeeklyRecap(true) }
                            else viewModel.setWeeklyRecap(false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AxiomTheme.colors.onAccent,
                            checkedTrackColor = AxiomTheme.colors.accent
                        )
                    )
                }
            )
            // Placed with the conversation settings rather than the
            // notification ones: this is about what you walk back into, not
            // about being interrupted.
            SettingRow(
                icon = Icons.Outlined.History,
                title = "Starting fresh",
                summary = "Every visit opens blank · " +
                    "keep the last one ${state.conversationRetention.label.lowercase()}",
                onClick = { showRetentionSheet = true }
            )
            SettingRow(
                icon = Icons.Outlined.Face,
                title = "What I'm called",
                summary = state.companionName,
                onClick = { showCompanionNameSheet = true }
            )
            SettingRow(
                icon = Icons.Outlined.Person,
                title = "Your name",
                summary = state.displayName.ifBlank { "Not set — it's what I call you" },
                onClick = { showNameSheet = true }
            )
            SettingRow(
                icon = Icons.Outlined.Save,
                title = "Take everything with you",
                summary = "Save your whole journal to a file, or bring one back",
                onClick = onOpenBackup
            )
            SettingRow(
                icon = Icons.Outlined.Info,
                title = "About",
                summary = "${state.entryCount} moments kept · fonts, licences, privacy",
                onClick = { showAboutSheet = true }
            )
        }
    }

    if (showCompanionNameSheet) {
        CompanionNameSheet(
            current = state.companionName,
            onSave = { name ->
                viewModel.setCompanionName(name)
                showCompanionNameSheet = false
            },
            onDismiss = { showCompanionNameSheet = false }
        )
    }

    if (showNameSheet) {
        NameSheet(
            current = state.displayName,
            onSave = { name ->
                viewModel.setDisplayName(name)
                showNameSheet = false
            },
            onDismiss = { showNameSheet = false }
        )
    }

    if (showAboutSheet) {
        AboutSheet(
            entryCount = state.entryCount,
            onDismiss = { showAboutSheet = false }
        )
    }

    if (showRetentionSheet) {
        RetentionSheet(
            selected = state.conversationRetention,
            onPick = { retention ->
                viewModel.setConversationRetention(retention)
                showRetentionSheet = false
            },
            onDismiss = { showRetentionSheet = false }
        )
    }

    if (showTimeSheet) {
        CheckInTimeSheet(
            selected = state.checkInMinuteOfDay,
            onPick = { minute ->
                viewModel.setCheckInMinuteOfDay(minute)
                showTimeSheet = false
            },
            onDismiss = { showTimeSheet = false }
        )
    }
}

/**
 * How long a conversation stays available after you close the app.
 *
 * The blank-on-open behaviour is not optional and is stated in the sheet's own
 * subtitle rather than offered as a switch — arriving to a clean screen is what
 * the conversation is now, and this only governs how long the last one is still
 * within reach behind it.
 */
@Composable
private fun RetentionSheet(
    selected: ConversationRetention,
    onPick: (ConversationRetention) -> Unit,
    onDismiss: () -> Unit
) {
    AxiomBottomSheet(title = "Keep the last conversation", onDismiss = onDismiss) {
        Text(
            "Every visit starts blank. Until this runs out, the last one is one " +
                "tap away; after it, I write it into your story and let it go.",
            style = AxiomTheme.type.uiBodySmall,
            color = AxiomTheme.colors.inkMuted,
            modifier = Modifier.padding(bottom = AxiomTheme.space.sm)
        )
        ConversationRetention.entries.forEach { option ->
            AxiomCard(
                tone = if (option == selected) CardTone.Accent else CardTone.Neutral,
                onClick = { onPick(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AxiomTheme.space.xs)
            ) {
                Text(option.label, style = AxiomTheme.type.uiTitleSmall, color = AxiomTheme.colors.ink)
                Text(
                    option.blurb,
                    style = AxiomTheme.type.uiBodySmall,
                    color = AxiomTheme.colors.inkMuted
                )
            }
        }
    }
}

/**
 * Four times rather than a clock face: this is "when is it welcome to hear
 * from me", not an alarm. Delivery is approximate anyway — the companion
 * speaks within [CheckInTimes.WINDOW_MINUTES] of this time, once the device
 * is awake.
 *
 * The options come from [CheckInTimes] because onboarding asks the same
 * question, and the two used to be able to drift apart.
 */
@Composable
private fun CheckInTimeSheet(
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AxiomBottomSheet(title = "Not before", onDismiss = onDismiss) {
        CheckInTimes.ALL.forEach { (minute, label) ->
            AxiomCard(
                tone = if (minute == selected) CardTone.Accent else CardTone.Neutral,
                onClick = { onPick(minute) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AxiomTheme.space.xs)
            ) {
                Text(label, style = AxiomTheme.type.uiTitleSmall, color = AxiomTheme.colors.ink)
                Text(
                    formatMinuteOfDay(minute),
                    style = AxiomTheme.type.uiBodySmall,
                    color = AxiomTheme.colors.inkMuted
                )
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.base))
        Text(
            "You'll only hear from the companion on days you haven't written or talked.",
            style = AxiomTheme.type.uiBodySmall,
            color = AxiomTheme.colors.inkMuted
        )
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

@Composable
fun SettingsAiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors

    AxiomScaffold(
        title = "AI",
        screenTag = "screen:settings_ai",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AxiomTheme.space.screenH),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            // Privacy first, before asking for anything.
            AxiomCard(tone = CardTone.Ai) {
                Text("Your journal stays on this device", style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                Spacer(Modifier.height(AxiomTheme.space.xs))
                Text(
                    "Axiom works fully offline. A key only enables the optional AI " +
                        "features, and even then only the entries relevant to a question " +
                        "are sent — never your whole journal.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkMuted
                )
            }

            SectionHeader("Provider")
            AiVendor.entries.forEach { vendor ->
                AxiomCard(
                    tone = if (vendor == state.aiVendor) CardTone.Accent else CardTone.Neutral,
                    onClick = { viewModel.setAiVendor(vendor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AxiomTheme.space.xs)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            vendor.displayName,
                            style = AxiomTheme.type.uiTitleSmall,
                            color = c.ink,
                            modifier = Modifier.weight(1f)
                        )
                        // Shows at a glance which one you already have set up.
                        // This rendered an empty string, so the marker it was
                        // meant to be never appeared at all.
                        if (state.aiVendor != vendor && state.keyConnected) {
                            Text("Key saved", style = AxiomTheme.type.uiMeta, color = c.inkFaint)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(vendor.summary, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
                }
            }

            SectionHeader(
                if (state.vendorKeyConnected) "${state.aiVendor.displayName} connected"
                else "Connect a ${state.aiVendor.displayName} key"
            )

            if (state.vendorKeyConnected) {
                AxiomCard {
                    Text("A key is saved and encrypted on this device.",
                        style = AxiomTheme.type.uiBody, color = c.ink)
                    Spacer(Modifier.height(AxiomTheme.space.md))
                    TextButton(onClick = viewModel::removeKey) {
                        Text("Remove key", style = AxiomTheme.type.uiLabel, color = c.critical)
                    }
                }
            } else {
                // Shared with onboarding's key page — see ApiKeyConnectCard.
                ApiKeyConnectCard(
                    vendor = state.aiVendor,
                    keyDraft = state.keyDraft,
                    masked = state.keyMasked,
                    testing = state.keyTest == KeyTestState.Testing,
                    onKeyDraftChange = viewModel::setKeyDraft,
                    onToggleMask = viewModel::toggleKeyMask,
                    onSaveAndTest = viewModel::saveAndTestKey
                )
            }

            // Every outcome of the live check is visible; a silent test is useless.
            when (val test = state.keyTest) {
                KeyTestState.Idle, KeyTestState.Testing -> Unit
                KeyTestState.Success -> AxiomInlineNotice(
                    "Connected. AI features are available.",
                    tone = CardTone.Accent
                )
                is KeyTestState.Failed -> AxiomErrorSurface(
                    error = test.error,
                    onRetry = viewModel::saveAndTestKey
                )
            }

            Spacer(Modifier.height(AxiomTheme.space.huge))
        }
    }
}

@Composable
fun SettingsAppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors

    AxiomScaffold(
        title = "Appearance",
        screenTag = "screen:settings_appearance",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AxiomTheme.space.screenH),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            SectionHeader("Theme")
            AxiomSegmented(
                options = listOf("System", "Light", "Dark", "Black"),
                selectedIndex = when (state.themeMode) {
                    ThemeMode.SYSTEM -> 0
                    ThemeMode.LIGHT -> 1
                    ThemeMode.DARK -> 2
                    ThemeMode.AMOLED -> 3
                },
                onSelect = { index ->
                    viewModel.setThemeMode(
                        when (index) {
                            1 -> ThemeMode.LIGHT
                            2 -> ThemeMode.DARK
                            3 -> ThemeMode.AMOLED
                            else -> ThemeMode.SYSTEM
                        }
                    )
                }
            )
            SettingRow(
                icon = Icons.Outlined.Palette,
                title = "Let the light change with the day",
                summary = if (state.adaptiveLight) {
                    "Warm in the morning, quieter by afternoon, amber at night"
                } else {
                    "Off — the same colours at every hour"
                },
                onClick = { viewModel.setAdaptiveLight(!state.adaptiveLight) },
                trailing = {
                    Switch(
                        checked = state.adaptiveLight,
                        onCheckedChange = viewModel::setAdaptiveLight,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AxiomTheme.colors.onAccent,
                            checkedTrackColor = AxiomTheme.colors.accent
                        )
                    )
                }
            )

            // Live preview: the surrounding screen already re-themes, so the
            // sample simply shows how reading type sits on the chosen surfaces.
            AxiomCard {
                Text("Good evening", style = AxiomTheme.type.greeting, color = c.ink)
                Spacer(Modifier.height(AxiomTheme.space.sm))
                Text(
                    "Greetings are set in Fraunces, softened. What you read and write is " +
                        "Literata, sized for long reading rather than for controls.",
                    style = AxiomTheme.type.readingBody,
                    color = c.inkMuted
                )
            }
        }
    }
}

/**
 * What you call the companion.
 *
 * It reaches the bottom bar and the system prompt together, so the name on the
 * tab is the name it answers to. This is not the launcher label: Android fixes
 * that in the manifest at build time, and the only way to vary it is a set of
 * `<activity-alias>` entries declared in advance — which can only offer names we
 * picked, and which drops and re-adds the icon, losing its place on the home
 * screen. An arbitrary name on the launcher is not something the platform
 * allows, so this renames the companion everywhere inside the app instead.
 */
@Composable
private fun CompanionNameSheet(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    AxiomBottomSheet(title = "What should you call me?", onDismiss = onDismiss) {
        Text(
            "It's what you'll see at the bottom of the screen, and what I'll know " +
                "myself as. Leave it empty to go back to ${CompanionIdentity.DEFAULT_NAME}. " +
                "The name on your home screen is set by Android and can't change.",
            style = AxiomTheme.type.uiBodySmall,
            color = AxiomTheme.colors.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(CompanionIdentity.MAX_LENGTH) },
            singleLine = true,
            placeholder = { Text(CompanionIdentity.DEFAULT_NAME, style = AxiomTheme.type.uiBody) },
            textStyle = AxiomTheme.type.uiBody,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        Button(
            onClick = { onSave(draft.trim()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = AxiomTheme.colors.accent,
                contentColor = AxiomTheme.colors.onAccent
            ),
            shape = AxiomTheme.shapes.sm
        ) { Text("Save", style = AxiomTheme.type.uiLabel) }
    }
}

/** What the companion calls you. Used in every greeting, so it is worth asking for. */
@Composable
private fun NameSheet(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    AxiomBottomSheet(title = "What should I call you?", onDismiss = onDismiss) {
        Text(
            "It only ever appears in greetings on this device.",
            style = AxiomTheme.type.uiBodySmall,
            color = AxiomTheme.colors.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(40) },
            singleLine = true,
            placeholder = { Text("Optional", style = AxiomTheme.type.uiBody) },
            textStyle = AxiomTheme.type.uiBody,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        Button(
            onClick = { onSave(draft.trim()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = AxiomTheme.colors.accent,
                contentColor = AxiomTheme.colors.onAccent
            ),
            shape = AxiomTheme.shapes.sm
        ) { Text("Save", style = AxiomTheme.type.uiLabel) }
    }
}

/** Honest about what this is, what it counts, and whose device it lives on. */
@Composable
private fun AboutSheet(entryCount: Int, onDismiss: () -> Unit) {
    val c = AxiomTheme.colors
    AxiomBottomSheet(title = "About Axiom", onDismiss = onDismiss) {
        Text(
            "$entryCount moments kept, all of them on this device.",
            style = AxiomTheme.type.readingBody,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        listOf(
            "Everything you write is stored locally. There is no account and no sync.",
            "Nothing is uploaded unless you connect an AI key yourself, and you can " +
                "disconnect it at any time without losing anything.",
            "Set in Fraunces, Literata and Figtree — all open-source under the SIL " +
                "Open Font License."
        ).forEach { line ->
            Row(Modifier.padding(vertical = AxiomTheme.space.xs)) {
                Text("•", style = AxiomTheme.type.uiBody, color = c.inkFaint)
                Spacer(Modifier.width(AxiomTheme.space.sm))
                Text(line, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
            }
        }
    }
}

/**
 * Voice settings: input language routing and spoken replies. The privacy line
 * per option is explicit because the Hinglish path is the one place voice
 * audio leaves the device — the user should choose that knowingly.
 */
@Composable
fun SettingsVoiceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors

    AxiomScaffold(
        title = "Voice",
        screenTag = "screen:settings-voice",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AxiomTheme.space.screenH)
        ) {
            SectionHeader("Speech recognition")
            Spacer(Modifier.height(AxiomTheme.space.sm))
            VoiceLanguage.entries.forEach { language ->
                AxiomCard(
                    tone = if (language == state.voiceLanguage) CardTone.Accent else CardTone.Neutral,
                    onClick = { viewModel.setVoiceLanguage(language) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AxiomTheme.space.xs)
                ) {
                    Text(language.displayLabel, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (language.onDevice) "Recognized on this device — audio never leaves your phone"
                        else "Audio is uploaded to Groq with your key for transcription",
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkMuted
                    )
                }
            }

            Spacer(Modifier.height(AxiomTheme.space.lg))
            SectionHeader("Spoken replies")
            Spacer(Modifier.height(AxiomTheme.space.sm))
            SettingRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "Speak replies aloud",
                summary = "On-device voice — spoken words never leave your phone",
                onClick = { viewModel.setAutoSpeak(!state.autoSpeakEnabled) },
                trailing = {
                    Switch(
                        checked = state.autoSpeakEnabled,
                        onCheckedChange = viewModel::setAutoSpeak,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.onAccent,
                            checkedTrackColor = c.accent
                        )
                    )
                }
            )
        }
    }
}
