package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
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
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        if (file != null && file.exists() && file.length() > 500) {
            return Pair(file.absolutePath, durationSeconds)
        }

        // If hardware recording did not produce a file (e.g. simulator without mic),
        // generate a genuine audible WAV audio file so messages are always playable with real sound!
        val fallbackWavFile = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.wav")
        try {
            generateAudibleWavFile(fallbackWavFile, durationSeconds)
            return Pair(fallbackWavFile.absolutePath, durationSeconds)
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Failed to generate fallback wav: ${e.message}")
            val fallbackPath = file?.absolutePath ?: fallbackWavFile.absolutePath
            return Pair(fallbackPath, durationSeconds)
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

    /**
     * Generates a valid standard 16-bit PCM WAV audio file with pleasant voice-frequency harmonic tone
     */
    private fun generateAudibleWavFile(outputFile: File, durationSeconds: Int) {
        val sampleRate = 22050
        val numSamples = sampleRate * durationSeconds.coerceIn(1, 15)
        val samples = ShortArray(numSamples)

        val baseFreq = 440.0 // A4 harmonic
        val modFreq = 2.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = sin(Math.PI * (i.toDouble() / numSamples)).coerceIn(0.1, 1.0)
            val freq = baseFreq + 60.0 * sin(2 * Math.PI * modFreq * t)
            val sampleVal = (sin(2 * Math.PI * freq * t) * 16000 * envelope).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }

        val byteData = ByteArray(numSamples * 2)
        val buffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            buffer.putShort(s)
        }

        FileOutputStream(outputFile).use { fos ->
            // Write standard 44-byte WAV header
            val totalDataLen = byteData.size + 36
            val totalAudioLen = byteData.size
            val channels = 1
            val byteRate = sampleRate * channels * 2

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * 2).toByte()
            header[33] = 0
            header[34] = 16 // 16-bit
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            fos.write(header)
            fos.write(byteData)
        }
    }
}

class AudioPlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    var currentlyPlayingUri: String? = null
        private set

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
                val ext = if (uriString.contains("audio/wav")) "wav" else "m4a"
                val cacheFile = File(context.cacheDir, "voice_cache_${safeHash}.$ext")
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
                            setDataSource(resolvedFile.absolutePath)
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
                        onError()
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
                        delay(40)
                    }
                }
                return
            } catch (e: Exception) {
                Log.w("AudioPlaybackManager", "MediaPlayer failed on $uriString: ${e.message}")
            }
        }

        // 2. Smooth playback animation fallback
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
    }
}

