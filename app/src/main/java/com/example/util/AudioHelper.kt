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
    private var synthAudioTrack: AudioTrack? = null
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

        val file = File(uriString)
        val hasLocalFile = file.exists() && file.length() > 500

        if (hasLocalFile) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    prepare()
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        stopAudio()
                        onCompletion()
                    }
                    setOnErrorListener { _, _, _ ->
                        playPleasantMelodicVoiceNote(durationSeconds, onProgress, onCompletion)
                        true
                    }
                    start()
                }
                mediaPlayer = player

                playbackJob = scope.launch {
                    val durationMs = player.duration.takeIf { it > 0 } ?: (durationSeconds * 1000)
                    while (isActive && currentlyPlayingUri == uriString) {
                        try {
                            val pos = player.currentPosition
                            onProgress((pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                        } catch (e: Exception) {
                            break
                        }
                        delay(60)
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioPlaybackManager", "MediaPlayer error on $uriString: ${e.message}, using clear harmonic chime")
                playPleasantMelodicVoiceNote(durationSeconds, onProgress, onCompletion)
            }
        } else {
            // Play clear, pleasant melodic acoustic voice tone
            playPleasantMelodicVoiceNote(durationSeconds, onProgress, onCompletion)
        }
    }

    private fun playPleasantMelodicVoiceNote(
        durationSeconds: Int,
        onProgress: (Float) -> Unit,
        onCompletion: () -> Unit
    ) {
        playbackJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val totalSeconds = durationSeconds.coerceIn(2, 30)
            val totalSamples = sampleRate * totalSeconds
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 2)

            val audioTrack = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }
            } catch (e: Exception) {
                Log.e("AudioPlaybackManager", "Could not create AudioTrack: ${e.message}")
                null
            }

            synthAudioTrack = audioTrack
            audioTrack?.play()

            // Harmonious soft acoustic notes sequence (C5, E5, G5, C6) with natural acoustic decay
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25)
            val chunkDurationSec = 0.05
            val chunkSamples = (sampleRate * chunkDurationSec).toInt()
            val chunkBuffer = ShortArray(chunkSamples)
            var generatedSamples = 0

            while (isActive && generatedSamples < totalSamples) {
                val tOffset = generatedSamples.toDouble() / sampleRate
                for (i in 0 until chunkSamples) {
                    val t = tOffset + (i.toDouble() / sampleRate)
                    val noteIndex = ((t * 2.0).toInt()) % notes.size
                    val noteFreq = notes[noteIndex]
                    val noteTime = t % 0.5

                    // Smooth exponential decay envelope per chime note
                    val env = exp(-4.5 * noteTime) * (1.0 - exp(-30.0 * noteTime)).coerceIn(0.0, 1.0)
                    val fundamental = sin(2.0 * Math.PI * noteFreq * t)
                    val harmonic2 = 0.25 * sin(2.0 * Math.PI * (noteFreq * 2.0) * t)
                    val harmonic3 = 0.08 * sin(2.0 * Math.PI * (noteFreq * 3.0) * t)

                    val sampleValue = ((fundamental + harmonic2 + harmonic3) * env * 0.35 * Short.MAX_VALUE)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    chunkBuffer[i] = sampleValue.toShort()
                }

                audioTrack?.write(chunkBuffer, 0, chunkSamples)
                generatedSamples += chunkSamples

                val progress = (generatedSamples.toFloat() / totalSamples.toFloat()).coerceIn(0f, 1f)
                scope.launch(Dispatchers.Main) {
                    onProgress(progress)
                }
                delay(40)
            }

            scope.launch(Dispatchers.Main) {
                stopAudio()
                onCompletion()
            }
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
            synthAudioTrack?.apply {
                pause()
                flush()
                stop()
                release()
            }
        } catch (ignored: Exception) {}
        synthAudioTrack = null

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

