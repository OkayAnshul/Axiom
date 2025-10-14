package com.cosmiclaboratory.axiom.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale

class VoiceRecognitionManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceResults = Channel<VoiceRecognitionResult>()
    val voiceResults: Flow<VoiceRecognitionResult> = _voiceResults.receiveAsFlow()
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceResults.trySend(VoiceRecognitionResult.ReadyForSpeech)
        }
        
        override fun onBeginningOfSpeech() {
            _voiceResults.trySend(VoiceRecognitionResult.SpeechStarted)
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            _voiceResults.trySend(VoiceRecognitionResult.VolumeChanged(rmsdB))
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Not used for this implementation
        }
        
        override fun onEndOfSpeech() {
            _voiceResults.trySend(VoiceRecognitionResult.SpeechEnded)
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }
            _voiceResults.trySend(VoiceRecognitionResult.Error(errorMessage))
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            
            if (!matches.isNullOrEmpty()) {
                val result = matches[0]
                val confidenceScore = confidence?.getOrNull(0) ?: 0f
                _voiceResults.trySend(VoiceRecognitionResult.Success(result, confidenceScore))
            } else {
                _voiceResults.trySend(VoiceRecognitionResult.Error("No speech recognized"))
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _voiceResults.trySend(VoiceRecognitionResult.PartialResult(matches[0]))
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Not used for this implementation
        }
    }
    
    fun startListening(
        language: String = Locale.getDefault().language,
        enablePartialResults: Boolean = true,
        maxResults: Int = 1
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceResults.trySend(VoiceRecognitionResult.Error("Speech recognition not available"))
            return
        }
        
        stopListening() // Stop any ongoing recognition
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            startListening(intent)
        }
    }
    
    fun stopListening() {
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null
    }
    
    fun isListening(): Boolean {
        return speechRecognizer != null
    }
    
    fun destroy() {
        stopListening()
        _voiceResults.close()
    }
}

sealed class VoiceRecognitionResult {
    object ReadyForSpeech : VoiceRecognitionResult()
    object SpeechStarted : VoiceRecognitionResult()
    object SpeechEnded : VoiceRecognitionResult()
    data class VolumeChanged(val volume: Float) : VoiceRecognitionResult()
    data class PartialResult(val text: String) : VoiceRecognitionResult()
    data class Success(val text: String, val confidence: Float) : VoiceRecognitionResult()
    data class Error(val message: String) : VoiceRecognitionResult()
}