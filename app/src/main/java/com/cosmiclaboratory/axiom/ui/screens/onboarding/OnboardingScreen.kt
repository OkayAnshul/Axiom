package com.cosmiclaboratory.axiom.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlinx.coroutines.launch

/**
 * Four pages, where page four IS the first entry.
 *
 * Deliberately does NOT ask for an API key. Asking during setup implies the app
 * needs the cloud to work, which undercuts the entire privacy position — AI is
 * offered later, at the moment it would actually be used.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val c = AxiomTheme.colors

    fun next() = scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }

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
                    .padding(horizontal = AxiomTheme.space.xl),
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> Page(
                        title = "Your journal, on your device",
                        body = "Everything you write is stored locally. No account, no " +
                            "upload, no sync you didn't ask for."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.xl))
                        Text("What should we call you?", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                        Spacer(Modifier.height(AxiomTheme.space.sm))
                        Box {
                            if (state.displayName.isEmpty()) {
                                Text("Optional", style = AxiomTheme.type.readingSubtitle, color = c.inkFaint)
                            }
                            BasicTextField(
                                value = state.displayName,
                                onValueChange = viewModel::setDisplayName,
                                textStyle = AxiomTheme.type.readingSubtitle.copy(color = c.ink),
                                cursorBrush = SolidColor(c.accent),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("field:name")
                            )
                        }
                    }

                    1 -> Page(
                        title = "Pick a companion voice",
                        body = "This only changes how the optional AI features talk to you. " +
                            "You can switch any time."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.base))
                        state.personas.forEach { persona ->
                            AxiomCard(
                                tone = if (persona.key == state.persona) CardTone.Accent else CardTone.Neutral,
                                onClick = { viewModel.setPersona(persona.key) },
                                modifier = Modifier.padding(vertical = AxiomTheme.space.xs)
                            ) {
                                Text(persona.displayName, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                            }
                        }
                    }

                    2 -> Page(
                        title = "A gentle nudge?",
                        body = "One reminder a day, only if you haven't written. Off by " +
                            "default, and you can change it later."
                    ) {
                        Spacer(Modifier.height(AxiomTheme.space.xl))
                        Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                            Button(
                                onClick = { viewModel.setNudge(true); next() },
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

                    else -> Page(
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
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(AxiomTheme.space.screenH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageDots(current = pager.currentPage, total = 4)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (pager.currentPage < 3) next() else viewModel.finish(onFinished)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent, contentColor = c.onAccent
                ),
                shape = AxiomTheme.shapes.sm
            ) {
                Text(
                    when {
                        pager.currentPage < 3 -> "Continue"
                        state.firstEntry.isNotBlank() -> "Save and start"
                        else -> "Start"
                    },
                    style = AxiomTheme.type.uiLabel
                )
            }
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
