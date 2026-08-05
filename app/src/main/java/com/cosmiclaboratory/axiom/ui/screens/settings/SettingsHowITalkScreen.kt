package com.cosmiclaboratory.axiom.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.AxiomScaffold
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * How the companion talks — its own screen at last.
 *
 * [VoiceEditor] used to render at the bottom of the API-key screen, below the
 * key field and the connection test, while the settings row named "Voice" led
 * to microphone languages and text-to-speech. Anyone wanting to change their
 * companion's manner tapped "Voice" and found speech recognition.
 *
 * This is the most distinctive thing the app does, and it was reachable only by
 * scrolling past an API key. Now it has a door with its name on it.
 */
@Composable
fun SettingsHowITalkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AxiomScaffold(
        title = "How I talk",
        screenTag = "screen:how-i-talk",
        modifier = modifier,
        navigationIcon = {
            AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AxiomTheme.space.screenH)
        ) {
            VoiceEditor(
                voice = state.voice,
                onPreset = viewModel::applyVoicePreset,
                onVoice = viewModel::setVoice
            )
            Spacer(Modifier.height(AxiomTheme.space.huge))
        }
    }
}
