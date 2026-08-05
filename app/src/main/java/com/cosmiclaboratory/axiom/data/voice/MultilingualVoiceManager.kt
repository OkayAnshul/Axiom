package com.cosmiclaboratory.axiom.data.voice

import android.content.Context
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
import com.cosmiclaboratory.axiom.utils.VoiceRecognitionManager
import com.cosmiclaboratory.axiom.utils.VoiceRecognitionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single voice-input entry point for v2. Routes:
 *  - On-device languages → Android `SpeechRecognizer` (offline, fast).
 *  - `HINGLISH_AUTO` → record locally, upload to Groq Whisper (handles code-mix).
 *
 * The Whisper path is the *only* way to get reliable Hinglish transcription on
 * Android — the system recognizer either drops the Hindi half or transliterates
 * it to nonsense Latin characters.
 */
@Singleton
class MultilingualVoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: MediaRecorderWrapper,
    private val ai: AiProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var onDevice: VoiceRecognitionManager? = null
    private val external = Channel<VoiceRecognitionResult>(capacity = Channel.UNLIMITED)
    private var currentLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_IN

    fun results(): Flow<VoiceRecognitionResult> {
        val onDeviceFlow = (onDevice ?: VoiceRecognitionManager(context).also { onDevice = it }).voiceResults
        return merge(onDeviceFlow, external.consumeAsFlow())
    }

    fun start(language: VoiceLanguage) {
        currentLanguage = language
        if (language.onDevice) {
            val mgr = onDevice ?: VoiceRecognitionManager(context).also { onDevice = it }
            mgr.startListening(language = language.bcp47 ?: "en-IN")
        } else {
            external.trySend(VoiceRecognitionResult.ReadyForSpeech)
            external.trySend(VoiceRecognitionResult.SpeechStarted)
            try {
                recorder.start()
            } catch (e: Throwable) {
                external.trySend(VoiceRecognitionResult.Error(e.message ?: "Mic unavailable"))
            }
        }
    }

    fun stop() {
        if (currentLanguage.onDevice) {
            onDevice?.stopListening()
            return
        }
        external.trySend(VoiceRecognitionResult.SpeechEnded)
        val file = recorder.stop()
        if (file == null) {
            external.trySend(VoiceRecognitionResult.Error("No audio captured"))
            return
        }
        scope.launch(Dispatchers.IO) {
            when (val res = ai.transcribeAudio(file, language = null)) {
                is AiResult.Ok -> external.trySend(VoiceRecognitionResult.Success(res.value, 1f))
                is AiResult.NoKey -> external.trySend(VoiceRecognitionResult.Error("Connect a Groq key for Hinglish"))
                is AiResult.RateLimited -> external.trySend(VoiceRecognitionResult.Error("Groq rate-limited; try again"))
                is AiResult.Network -> external.trySend(VoiceRecognitionResult.Error("Network error: ${res.cause.message}"))
                is AiResult.Parse -> external.trySend(VoiceRecognitionResult.Error("Couldn't parse transcript"))
            }
            file.delete()
        }
    }

    fun cancel() {
        if (currentLanguage.onDevice) {
            // Abandon, don't finish: cancelling must not deliver a transcript.
            onDevice?.cancelListening()
        } else {
            recorder.cancel()
        }
    }

    fun isListening(): Boolean = onDevice?.isListening() == true
}
