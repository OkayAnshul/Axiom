package com.cosmiclaboratory.axiom.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The companion's voice, via Android's on-device [TextToSpeech].
 *
 * Why platform TTS and not a cloud voice: it is free, needs no key, and — the
 * decisive part — spoken replies never leave the phone, which keeps the privacy
 * disclosure honest ("generated on-device"). The voice is fine rather than
 * magical; a bundled neural voice is a later-phase upgrade, not a v1 need.
 *
 * Supports sentence-by-sentence queuing so speech can start roughly one
 * sentence after the first streamed token instead of waiting for the full
 * reply. [speaking] tracks the whole queue, not one utterance — hands-free
 * mode watches it to know when to reopen the microphone.
 */
@Singleton
class CompanionSpeaker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var initFailed = false
    private val pendingUtterances = AtomicInteger(0)
    private val utteranceCounter = AtomicInteger(0)

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    /** Idempotent lazy init — TTS engines can take a moment to bind. */
    fun warmUp(language: VoiceLanguage) {
        if (tts != null || initFailed) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyLanguage(language)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _speaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        if (pendingUtterances.decrementAndGet() <= 0) {
                            pendingUtterances.set(0)
                            _speaking.value = false
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (pendingUtterances.decrementAndGet() <= 0) {
                            pendingUtterances.set(0)
                            _speaking.value = false
                        }
                    }
                })
                _available.value = true
            } else {
                initFailed = true
                _available.value = false
            }
        }
    }

    /**
     * Best-voice fallback chain: preferred language → en-IN → engine default.
     * A missing voice hides the speak affordances rather than crashing.
     */
    private fun applyLanguage(language: VoiceLanguage) {
        val engine = tts ?: return
        val candidates = listOfNotNull(
            language.bcp47?.let(Locale::forLanguageTag),
            Locale.forLanguageTag("en-IN"),
            Locale.getDefault()
        )
        for (locale in candidates) {
            val result = engine.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                return
            }
        }
    }

    fun setLanguage(language: VoiceLanguage) {
        if (tts != null) applyLanguage(language)
    }

    /** Queues one sentence. Call [stop] first when starting a fresh reply. */
    fun speakSentence(text: String) {
        val engine = tts ?: return
        val plain = stripMarkdownForSpeech(text)
        if (plain.isBlank()) return
        pendingUtterances.incrementAndGet()
        _speaking.value = true
        engine.speak(plain, TextToSpeech.QUEUE_ADD, null, "axiom-${utteranceCounter.incrementAndGet()}")
    }

    /** Immediate silence — always called before the microphone opens. */
    fun stop() {
        tts?.stop()
        pendingUtterances.set(0)
        _speaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        _available.value = false
        _speaking.value = false
    }
}

/**
 * Splits streamed text into speakable sentences. Returns complete sentences
 * and the leftover tail. Sentence enders: . ! ? … । and newline; a minimum
 * length guard keeps abbreviations like "Dr." from stuttering the voice.
 */
fun extractSpeakableSentences(buffer: String, minLength: Int = 20): Pair<List<String>, String> {
    val sentences = mutableListOf<String>()
    var start = 0
    var lastEnd = 0
    buffer.forEachIndexed { index, char ->
        if (char in SENTENCE_ENDERS) {
            val candidate = buffer.substring(start, index + 1).trim()
            if (candidate.length >= minLength) {
                sentences.add(candidate)
                start = index + 1
                lastEnd = start
            }
        }
    }
    return sentences to buffer.substring(lastEnd)
}

private val SENTENCE_ENDERS = charArrayOf('.', '!', '?', '…', '।', '\n')

/** Markdown reads badly aloud — strip syntax, keep the words. */
fun stripMarkdownForSpeech(text: String): String = text
    .replace(Regex("```[\\s\\S]*?```"), " ")
    .replace(Regex("`([^`]*)`"), "$1")
    .replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
    .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("[*_~]{1,3}([^*_~]+)[*_~]{1,3}"), "$1")
    .replace(Regex("\\s+"), " ")
    .trim()
