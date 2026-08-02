package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.components.VoiceInputFab
import com.cosmiclaboratory.axiom.ui.theme.AxiomAsh
import com.cosmiclaboratory.axiom.ui.theme.AxiomCyan
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphite
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphiteRaised
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphiteSoft
import com.cosmiclaboratory.axiom.ui.theme.AxiomIvory
import com.cosmiclaboratory.axiom.ui.theme.AxiomMist
import com.cosmiclaboratory.axiom.ui.theme.AxiomOledBlack
import com.cosmiclaboratory.axiom.ui.theme.AxiomSun
import com.cosmiclaboratory.axiom.ui.theme.EditorContentStyle
import com.cosmiclaboratory.axiom.ui.theme.EditorPlaceholderStyle
import com.cosmiclaboratory.axiom.ui.viewmodels.AnswerCaptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerCaptureScreen(
    onBack: () -> Unit,
    onCompleted: (entryId: Long) -> Unit,
    viewModel: AnswerCaptureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val canComplete = !state.isCompleting && state.content.isNotBlank()

    Scaffold(
        containerColor = AxiomOledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reflect",
                        style = MaterialTheme.typography.titleMedium,
                        color = AxiomIvory
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = AxiomMist)
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = canComplete) { viewModel.complete(onCompleted) },
                        color = if (canComplete) AxiomCyan else AxiomGraphiteSoft,
                        contentColor = if (canComplete) AxiomOledBlack else AxiomAsh,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.isCompleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AxiomOledBlack
                                )
                            } else {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            }
                            Text(if (state.isCompleting) "Saving" else "Done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AxiomOledBlack)
            )
        },
        floatingActionButton = {
            VoiceInputFab(onVoiceResult = viewModel::appendVoiceTranscript)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(AxiomOledBlack, Color(0xFF03100F), AxiomOledBlack)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PromptSurface(prompt = state.question?.text ?: "Loading...")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, AxiomGraphiteSoft, RoundedCornerShape(28.dp)),
                    color = AxiomGraphite.copy(alpha = 0.9f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    BasicTextField(
                        value = state.content,
                        onValueChange = viewModel::setContent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        textStyle = EditorContentStyle.copy(color = AxiomIvory),
                        cursorBrush = SolidColor(AxiomCyan),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxSize()) {
                                if (state.content.isBlank()) {
                                    Text(
                                        "Say it naturally. One honest paragraph is enough.",
                                        style = EditorPlaceholderStyle.copy(color = AxiomAsh)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.isSaving) AxiomSun else AxiomCyan)
                    )
                    Text(
                        text = when {
                            state.isSaving -> "Saving"
                            state.savedEntryId != null -> "Autosaved"
                            else -> "Private draft"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AxiomAsh
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptSurface(prompt: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(AxiomCyan.copy(alpha = 0.24f), AxiomSun.copy(alpha = 0.16f))
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        color = AxiomGraphiteRaised.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Today",
                style = MaterialTheme.typography.labelMedium,
                color = AxiomCyan
            )
            Text(
                prompt,
                style = MaterialTheme.typography.titleLarge,
                color = AxiomIvory,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "One question, one clean moment.",
                style = MaterialTheme.typography.bodySmall,
                color = AxiomAsh
            )
        }
    }
}
