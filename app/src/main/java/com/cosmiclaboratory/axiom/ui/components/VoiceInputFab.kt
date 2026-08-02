package com.cosmiclaboratory.axiom.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.utils.VoiceRecognitionManager
import com.cosmiclaboratory.axiom.utils.VoiceRecognitionResult

@Composable
fun VoiceInputFab(
    onVoiceResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val voiceManager = remember { VoiceRecognitionManager(context) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            startListening(voiceManager) { isListening = it }
        } else {
            errorMessage = "Microphone permission required for voice input"
            showError = true
        }
    }
    
    // Listen to voice recognition results
    LaunchedEffect(Unit) {
        voiceManager.voiceResults.collect { result ->
            when (result) {
                is VoiceRecognitionResult.ReadyForSpeech -> {
                    isListening = true
                    showError = false
                }
                is VoiceRecognitionResult.SpeechStarted -> {
                    isListening = true
                }
                is VoiceRecognitionResult.SpeechEnded -> {
                    isListening = false
                }
                is VoiceRecognitionResult.Success -> {
                    isListening = false
                    onVoiceResult(result.text)
                }
                is VoiceRecognitionResult.Error -> {
                    isListening = false
                    errorMessage = result.message
                    showError = true
                }
                is VoiceRecognitionResult.PartialResult -> {
                    // Could show partial results in real-time
                }
                else -> { /* Handle other states if needed */ }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }
    
    // Animation for listening state
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_fab_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact round FAB for better space efficiency
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    voiceManager.stopListening()
                    isListening = false
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = modifier
                .scale(if (isListening) scale else 1.0f)
                .size(48.dp), // Compact size to avoid overlap
            containerColor = if (isListening)
                AxiomTheme.colors.critical else
                AxiomTheme.colors.accent,
            contentColor = if (isListening)
                AxiomTheme.colors.onAccent else
                AxiomTheme.colors.onAccent
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.StopCircle else Icons.Filled.KeyboardVoice,
                contentDescription = if (isListening) "Stop recording" else "Start voice input",
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (isListening) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Listening...",
                style = MaterialTheme.typography.labelSmall,
                color = AxiomTheme.colors.accent
            )
        }
    }
    
    // Show error snackbar
    if (showError) {
        LaunchedEffect(showError) {
            // In a real implementation, you'd use SnackbarHost
            // For now, we'll just clear the error after 3 seconds
            kotlinx.coroutines.delay(3000)
            showError = false
        }
    }
}

private fun startListening(
    voiceManager: VoiceRecognitionManager,
    setListening: (Boolean) -> Unit
) {
    voiceManager.startListening(
        enablePartialResults = true,
        maxResults = 1
    )
    setListening(true)
}
