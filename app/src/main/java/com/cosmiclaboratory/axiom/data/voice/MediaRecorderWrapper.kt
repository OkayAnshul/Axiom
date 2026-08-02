package com.cosmiclaboratory.axiom.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records short voice clips to the app's cache dir for off-device transcription
 * (Whisper). Output is M4A/AAC at 16 kHz mono — Whisper resamples internally,
 * but this keeps the upload small (~30 KB/sec).
 */
@Singleton
class MediaRecorderWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    @Suppress("DEPRECATION")
    fun start(): File {
        stop()
        val dir = File(context.cacheDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice-${System.currentTimeMillis()}.m4a")
        currentFile = file
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(32_000)
            setAudioChannels(1)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stop(): File? {
        val file = currentFile
        runCatching {
            recorder?.stop()
        }
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
        return file?.takeIf { it.exists() && it.length() > 0L }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}
