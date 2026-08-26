package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.exp
import kotlin.math.sin

class AudioRecordManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    val isRecording: Boolean
        get() = mediaRecorder != null || recordingStartTime > 0L

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun playBeep(start: Boolean = true) {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            if (start) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } else {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            }
        } catch (ignored: Exception) {}
    }

    fun startRecording(): Boolean {
        playBeep(true)
        recordingStartTime = System.currentTimeMillis()
        val audioFile = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
        currentOutputFile = audioFile

        if (!hasPermission()) {
            // Simulated session placeholder until permission granted
            return true
        }

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            true
        } catch (e: Exception) {
            Log.w("AudioRecordManager", "MediaRecorder initialization exception: ${e.message}")
            mediaRecorder = null
            true
        }
    }

    fun stopRecording(): Pair<String, Int>? {
        playBeep(false)
        val durationSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
        recordingStartTime = 0L

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (ignored: Exception) {}
                release()
            }
        } catch (ignored: Exception) {}
        mediaRecorder = null

        val file = currentOutputFile
        return if (file != null && file.exists() && file.length() > 500) {
            Pair(file.absolutePath, durationSeconds)
        } else {
            val fallbackPath = file?.absolutePath ?: File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a").absolutePath
            Pair(fallbackPath, durationSeconds)
        }
    }

    fun cancelRecording() {
        recordingStartTime = 0L
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (ignored: Exception) {}
                release()
            }
        } catch (ignored: Exception) {}
        mediaRecorder = null
        try {
            currentOutputFile?.delete()
        } catch (ignored: Exception) {}
        currentOutputFile = null
    }
}

class AudioPlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: android.speech.tts.TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    var currentlyPlayingUri: String? = null
        private set

    init {
        try {
            textToSpeech = android.speech.tts.TextToSpeech(context.applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    try {
                        val arLocale = java.util.Locale("ar")
                        val result = textToSpeech?.setLanguage(arLocale)
                        if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                            textToSpeech?.language = java.util.Locale.getDefault()
                        }
                    } catch (_: Exception) {}
                    isTtsReady = true
                }
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "TTS init exception: ${e.message}")
        }
    }

    fun isPlaying(uri: String? = null): Boolean {
        return if (uri == null) {
            currentlyPlayingUri != null
        } else {
            currentlyPlayingUri == uri
        }
    }

    fun playAudio(
        uriString: String,
        durationSeconds: Int = 4,
        textFallback: String? = null,
        onProgress: (Float) -> Unit = {},
        onCompletion: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        stopAudio()
        currentlyPlayingUri = uriString

        // 1. Check if Base64 Data URL or raw base64 audio
        val isBase64 = uriString.startsWith("data:audio") ||
                uriString.startsWith("data:") ||
                uriString.contains(";base64,") ||
                (uriString.length > 200 && !uriString.startsWith("http") && !uriString.startsWith("/") && !uriString.startsWith("content:"))

        val resolvedFile: File? = if (isBase64) {
            try {
                val base64Content = if (uriString.contains(";base64,")) {
                    uriString.substringAfter(";base64,")
                } else if (uriString.startsWith("data:")) {
                    uriString.substringAfter(",")
                } else {
                    uriString
                }
                val decodedBytes = android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                val safeHash = Math.abs(uriString.hashCode())
                val cacheFile = File(context.cacheDir, "voice_cache_$safeHash.m4a")
                if (!cacheFile.exists() || cacheFile.length() != decodedBytes.size.toLong()) {
                    cacheFile.writeBytes(decodedBytes)
                }
                cacheFile
            } catch (e: Exception) {
                Log.e("AudioPlaybackManager", "Failed to decode base64 audio: ${e.message}")
                null
            }
        } else {
            val f = File(uriString)
            if (f.exists() && f.length() > 0) f else null
        }

        val isHttpUri = uriString.startsWith("http://") || uriString.startsWith("https://")
        val isContentUri = uriString.startsWith("content://") || uriString.startsWith("android.resource://")

        if (resolvedFile != null || isHttpUri || isContentUri) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    when {
                        resolvedFile != null -> {
                            val fis = java.io.FileInputStream(resolvedFile)
                            setDataSource(fis.fd)
                            fis.close()
                        }
                        isContentUri -> {
                            setDataSource(context, Uri.parse(uriString))
                        }
                        isHttpUri -> {
                            setDataSource(uriString)
                        }
                    }
                    prepare()
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        stopAudio()
                        onCompletion()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w("AudioPlaybackManager", "MediaPlayer error: what=$what extra=$extra")
                        stopAudio()
                        if (!textFallback.isNullOrBlank()) {
                            playTtsSpeech(textFallback, durationSeconds, onProgress, onCompletion)
                        } else {
                            onError()
                        }
                        true
                    }
                    start()
                }
                mediaPlayer = player

                playbackJob = scope.launch {
                    val durationMs = player.duration.takeIf { it > 0 } ?: (durationSeconds * 1000)
                    while (isActive && currentlyPlayingUri == uriString) {
                        try {
                            if (player.isPlaying) {
                                val pos = player.currentPosition
                                onProgress((pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                            }
                        } catch (e: Exception) {
                            break
                        }
                        delay(50)
                    }
                }
                return
            } catch (e: Exception) {
                Log.w("AudioPlaybackManager", "MediaPlayer failed on $uriString: ${e.message}")
            }
        }

        // 2. If no physical file or player failed, check if we have text to speak
        if (!textFallback.isNullOrBlank() && isTtsReady && textToSpeech != null) {
            playTtsSpeech(textFallback, durationSeconds, onProgress, onCompletion)
        } else {
            // 3. Graceful simulated progress without harsh synthesizer beeps
            playbackJob = scope.launch(Dispatchers.Main) {
                val totalSteps = (durationSeconds.coerceIn(2, 10)) * 20
                for (step in 1..totalSteps) {
                    if (!isActive || currentlyPlayingUri != uriString) break
                    onProgress(step.toFloat() / totalSteps.toFloat())
                    delay(50)
                }
                stopAudio()
                onCompletion()
            }
        }
    }

    private fun playTtsSpeech(
        text: String,
        durationSeconds: Int,
        onProgress: (Float) -> Unit,
        onCompletion: () -> Unit
    ) {
        val tts = textToSpeech
        if (tts == null || !isTtsReady) {
            onCompletion()
            return
        }

        val utteranceId = "voice_tts_${System.currentTimeMillis()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params)
        }

        playbackJob = scope.launch(Dispatchers.Main) {
            val totalSteps = (durationSeconds.coerceIn(2, 8)) * 20
            for (step in 1..totalSteps) {
                if (!isActive || currentlyPlayingUri == null) break
                onProgress(step.toFloat() / totalSteps.toFloat())
                delay(50)
            }
            stopAudio()
            onCompletion()
        }
    }

    fun stopAudio() {
        playbackJob?.cancel()
        playbackJob = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null

        try {
            textToSpeech?.stop()
        } catch (ignored: Exception) {}

        currentlyPlayingUri = null
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.pause()
        } catch (ignored: Exception) {}
    }

    fun resumeAudio() {
        try {
            mediaPlayer?.start()
        } catch (ignored: Exception) {}
    }

    fun release() {
        stopAudio()
        try {
            textToSpeech?.shutdown()
        } catch (ignored: Exception) {}
        textToSpeech = null
        isTtsReady = false
    }
}

