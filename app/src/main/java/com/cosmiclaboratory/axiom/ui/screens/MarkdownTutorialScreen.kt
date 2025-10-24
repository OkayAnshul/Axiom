package com.cosmiclaboratory.axiom.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.LessonDifficulty
import com.cosmiclaboratory.axiom.domain.model.TutorialLesson
import com.cosmiclaboratory.axiom.domain.model.TutorialStep
import com.cosmiclaboratory.axiom.ui.components.MarkdownPreview
import com.cosmiclaboratory.axiom.ui.viewmodels.TutorialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownTutorialScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TutorialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = if (uiState.currentLesson != null) {
                        uiState.currentLesson!!.title
                    } else {
                        "Markdown Tutorial"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (uiState.currentLesson != null) {
                        viewModel.exitLesson()
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                if (uiState.currentLesson != null) {
                    // Progress indicator
                    Box(
                        modifier = Modifier.padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { uiState.lessonProgress },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${(uiState.lessonProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        when {
            uiState.currentLesson == null -> {
                // Lesson selection screen
                LessonSelectionContent(
                    lessons = uiState.availableLessons,
                    onLessonSelected = viewModel::startLesson,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                // Active lesson screen
                LessonContent(
                    lesson = uiState.currentLesson!!,
                    currentStep = uiState.currentStep,
                    userInput = uiState.userInput,
                    isCorrect = uiState.isCurrentStepCorrect,
                    showHint = uiState.showHint,
                    onUserInputChange = viewModel::updateUserInput,
                    onCheckAnswer = viewModel::checkCurrentStep,
                    onNextStep = viewModel::nextStep,
                    onPreviousStep = viewModel::previousStep,
                    onShowHint = viewModel::showHint,
                    onCompleteLesson = viewModel::completeLesson,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LessonSelectionContent(
    lessons: Map<LessonDifficulty, List<TutorialLesson>>,
    onLessonSelected: (TutorialLesson) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcome section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Master Markdown",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Learn to format your notes beautifully with interactive lessons",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Difficulty sections
        lessons.forEach { (difficulty, lessonList) ->
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(difficulty.color)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = difficulty.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(lessonList) { lesson ->
                            LessonCard(
                                lesson = lesson,
                                onClick = { onLessonSelected(lesson) }
                            )
                        }
                    }
                }
            }
        }
        
        // Reference section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Quick Reference",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Need a quick reminder? Access the markdown cheat sheet anytime while writing notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: TutorialLesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = lesson.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${lesson.estimatedDuration} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lesson.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = lesson.difficulty.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = lesson.difficulty.color.copy(alpha = 0.1f),
                        labelColor = lesson.difficulty.color
                    )
                )
                
                Text(
                    text = "${lesson.steps.size} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LessonContent(
    lesson: TutorialLesson,
    currentStep: TutorialStep?,
    userInput: String,
    isCorrect: Boolean?,
    showHint: Boolean,
    onUserInputChange: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onShowHint: () -> Unit,
    onCompleteLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentStep == null) return
    
    val currentStepIndex = lesson.steps.indexOf(currentStep)
    val isLastStep = currentStepIndex == lesson.steps.size - 1
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lesson.steps.forEachIndexed { index, step ->
                val isActive = index == currentStepIndex
                val isCompleted = step.isCompleted
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }
        
        // Step content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Step ${currentStepIndex + 1} of ${lesson.steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentStep.instruction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Example
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Example:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = currentStep.example,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
        
        // Interactive editor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input area
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Try it yourself:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = userInput,
                    onValueChange = onUserInputChange,
                    placeholder = { Text("Type your markdown here...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when (isCorrect) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.outline
                        }
                    )
                )
                
                // Check button
                Button(
                    onClick = onCheckAnswer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = userInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Answer")
                }
            }
            
            // Preview area
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    if (userInput.isNotBlank()) {
                        MarkdownPreview(
                            content = userInput,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Preview will appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Feedback section
        AnimatedVisibility(
            visible = isCorrect != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect == true) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isCorrect == true) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCorrect == true) "Great job! That's correct!" else "Not quite right. Try again or check the hint.",
                        color = if (isCorrect == true) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
        
        // Hint section
        AnimatedVisibility(
            visible = showHint && currentStep.hints.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hint",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    currentStep.hints.forEach { hint ->
                        Text(
                            text = "• $hint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                if (currentStepIndex > 0) {
                    OutlinedButton(onClick = onPreviousStep) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previous")
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (currentStep.hints.isNotEmpty()) {
                    OutlinedButton(onClick = onShowHint) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hint")
                    }
                }
            }
            
            if (isCorrect == true) {
                Button(
                    onClick = if (isLastStep) onCompleteLesson else onNextStep
                ) {
                    Text(if (isLastStep) "Complete Lesson" else "Next Step")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastStep) Icons.Default.Flag else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}