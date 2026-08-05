package com.cosmiclaboratory.axiom.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale

/**
 * Wraps Android's [SpeechRecognizer].
 *
 * Two things here are deliberate and were previously wrong:
 *
 * **The recognizer is not destroyed between sessions.** It used to be torn down
 * on every stop and rebuilt on the next start, which is the textbook way to get
 * `ERROR_SERVER_DISCONNECTED` on the *second* press — the recognition service
 * has not finished unbinding when the new instance tries to bind. The instance
 * now lives until [destroy]; stopping just ends the utterance.
 *
 * **Every documented error code is named.** The unnamed ones fell into "Unknown
 * error", which is what the disconnect surfaced as: an honest bug wearing a
 * meaningless label. When the service really has died we drop the instance so
 * the next start rebuilds cleanly.
 */
class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    /**
     * Buffered and conflating the oldest on overflow. This was a rendezvous
     * channel, where `trySend` silently fails unless a collector happens to be
     * suspended at that instant — so amplitude updates (many per second) could
     * push a real result off a cliff.
     */
    private val _voiceResults = Channel<VoiceRecognitionResult>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
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

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            listening = false
            _voiceResults.trySend(VoiceRecognitionResult.SpeechEnded)
        }

        override fun onError(error: Int) {
            listening = false
            // A dead service cannot be reused. Drop it so the next start binds a
            // fresh one instead of failing identically forever.
            if (error in FATAL_ERRORS) releaseRecognizer()
            _voiceResults.trySend(VoiceRecognitionResult.Error(describe(error)))
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            if (!matches.isNullOrEmpty()) {
                _voiceResults.trySend(
                    VoiceRecognitionResult.Success(matches[0], confidence?.getOrNull(0) ?: 0f)
                )
            } else {
                _voiceResults.trySend(VoiceRecognitionResult.Error("I didn't catch that"))
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _voiceResults.trySend(VoiceRecognitionResult.PartialResult(matches[0]))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun startListening(
        language: String = Locale.getDefault().language,
        enablePartialResults: Boolean = true,
        maxResults: Int = 1
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceResults.trySend(
                VoiceRecognitionResult.Error("Speech recognition isn't available on this device")
            )
            return
        }

        val recognizer = speechRecognizer ?: runCatching {
            SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(recognitionListener)
            }
        }.getOrNull()

        if (recognizer == null) {
            _voiceResults.trySend(VoiceRecognitionResult.Error("Couldn't reach the speech service"))
            return
        }
        speechRecognizer = recognizer

        // Reset any half-finished session on the SAME instance rather than
        // rebuilding — cancel is synchronous from the client's point of view.
        runCatching { recognizer.cancel() }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        listening = true
        runCatching { recognizer.startListening(intent) }.onFailure {
            listening = false
            releaseRecognizer()
            _voiceResults.trySend(VoiceRecognitionResult.Error("Couldn't start listening"))
        }
    }

    /** Ends the utterance and asks for a final result. Keeps the recognizer alive. */
    fun stopListening() {
        listening = false
        runCatching { speechRecognizer?.stopListening() }
    }

    /** Abandons the utterance without asking for a result. */
    fun cancelListening() {
        listening = false
        runCatching { speechRecognizer?.cancel() }
    }

    fun isListening(): Boolean = listening

    fun destroy() {
        listening = false
        releaseRecognizer()
        _voiceResults.close()
    }

    private fun releaseRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    private fun describe(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Trouble with the microphone"
        SpeechRecognizer.ERROR_CLIENT -> "Speech service hiccup — try again"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed"
        SpeechRecognizer.ERROR_NETWORK -> "No connection for speech recognition"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out"
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Still finishing the last one — try again"
        SpeechRecognizer.ERROR_SERVER -> "The speech service had a problem"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything"
        ERROR_TOO_MANY_REQUESTS -> "Too many requests — give it a moment"
        ERROR_SERVER_DISCONNECTED -> "Speech service disconnected — try again"
        ERROR_LANGUAGE_NOT_SUPPORTED -> "That language isn't supported here"
        ERROR_LANGUAGE_UNAVAILABLE -> "That language isn't downloaded yet"
        ERROR_CANNOT_CHECK_SUPPORT -> "Couldn't check language support"
        else -> "Speech recognition failed"
    }

    private companion object {
        // Named locally: these constants only exist from API 31/33, and minSdk is 26.
        const val ERROR_TOO_MANY_REQUESTS = 10
        const val ERROR_SERVER_DISCONNECTED = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
        const val ERROR_CANNOT_CHECK_SUPPORT = 14

        /** Errors that mean this recognizer instance is no longer usable. */
        val FATAL_ERRORS = setOf(
            SpeechRecognizer.ERROR_CLIENT,
            ERROR_SERVER_DISCONNECTED
        )
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
