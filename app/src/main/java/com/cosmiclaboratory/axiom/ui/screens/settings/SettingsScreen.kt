package com.cosmiclaboratory.axiom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import androidx.compose.material.icons.outlined.KeyboardVoice

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
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AxiomScaffold(
        title = "Settings",
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
                summary = if (state.keyConnected) "Connected · ${state.activePersona.name.lowercase()}"
                else "Not connected — everything else works offline",
                onClick = onOpenAi
            )
            SettingRow(
                icon = Icons.Outlined.Psychology,
                title = "What Axiom remembers",
                summary = "See, edit or delete every remembered detail",
                onClick = onOpenMemories
            )
            SettingRow(
                icon = Icons.Outlined.KeyboardVoice,
                title = "Voice",
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
                title = "Daily reminder",
                summary = if (state.dailyNudgeEnabled) "On" else "Off",
                onClick = { viewModel.setDailyNudge(!state.dailyNudgeEnabled) },
                trailing = {
                    Switch(
                        checked = state.dailyNudgeEnabled,
                        onCheckedChange = viewModel::setDailyNudge,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AxiomTheme.colors.onAccent,
                            checkedTrackColor = AxiomTheme.colors.accent
                        )
                    )
                }
            )
            SettingRow(
                icon = Icons.Outlined.Person,
                title = "Your name",
                summary = state.displayName.ifBlank { "Not set — used in greetings" },
                onClick = { }
            )
            SettingRow(
                icon = Icons.Outlined.Info,
                title = "About",
                summary = "${state.entryCount} entries · fonts, licences, privacy",
                onClick = { }
            )
        }
    }
}

@Composable
fun SettingsAiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    val uriHandler = LocalUriHandler.current

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

            SectionHeader(if (state.keyConnected) "Connected" else "Connect a key")

            if (state.keyConnected) {
                AxiomCard {
                    Text("A key is saved and encrypted on this device.",
                        style = AxiomTheme.type.uiBody, color = c.ink)
                    Spacer(Modifier.height(AxiomTheme.space.md))
                    TextButton(onClick = viewModel::removeKey) {
                        Text("Remove key", style = AxiomTheme.type.uiLabel, color = c.critical)
                    }
                }
            } else {
                AxiomCard {
                    Box {
                        if (state.keyDraft.isEmpty()) {
                            Text("gsk_…", style = AxiomTheme.type.uiBody, color = c.inkFaint)
                        }
                        BasicTextField(
                            value = state.keyDraft,
                            onValueChange = viewModel::setKeyDraft,
                            textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                            cursorBrush = SolidColor(c.accent),
                            singleLine = true,
                            visualTransformation = if (state.keyMasked) {
                                PasswordVisualTransformation()
                            } else VisualTransformation.None,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field:api_key")
                        )
                    }
                    Spacer(Modifier.height(AxiomTheme.space.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = viewModel::saveAndTestKey,
                            enabled = state.keyDraft.isNotBlank() && state.keyTest != KeyTestState.Testing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.accent, contentColor = c.onAccent
                            ),
                            shape = AxiomTheme.shapes.sm
                        ) {
                            Text(
                                if (state.keyTest == KeyTestState.Testing) "Testing…" else "Save and test",
                                style = AxiomTheme.type.uiLabel
                            )
                        }
                        Spacer(Modifier.width(AxiomTheme.space.sm))
                        AxiomIconButton(
                            icon = if (state.keyMasked) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            label = if (state.keyMasked) "Show key" else "Hide key",
                            onClick = viewModel::toggleKeyMask,
                            tint = c.inkMuted
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { uriHandler.openUri("https://console.groq.com/keys") }) {
                            Text("Get a free key", style = AxiomTheme.type.uiLabel, color = c.accent)
                        }
                    }
                }
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

            SectionHeader("Companion voice")
            state.personas.forEach { persona ->
                AxiomCard(
                    tone = if (persona.key == state.activePersona) CardTone.Accent else CardTone.Neutral,
                    onClick = { viewModel.setPersona(persona.key) }
                ) {
                    Text(persona.displayName, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        persona.systemPromptFragment,
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkMuted,
                        maxLines = 2
                    )
                }
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
            // Live preview: the surrounding screen already re-themes, so the
            // sample simply shows how reading type sits on the chosen surfaces.
            AxiomCard {
                Text("The quick brown fox", style = AxiomTheme.type.readingTitle, color = c.ink)
                Spacer(Modifier.height(AxiomTheme.space.sm))
                Text(
                    "Body text renders in Literata, sized for long reading rather " +
                        "than for controls.",
                    style = AxiomTheme.type.readingBody,
                    color = c.inkMuted
                )
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
