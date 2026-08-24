package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

class AudioRecordManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): Boolean {
        return try {
            val audioFile = File(context.cacheDir, "audio_note_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

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
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error starting record: ${e.message}", e)
            cancelRecording()
            false
        }
    }

    fun stopRecording(): Pair<String, Int>? {
        return try {
            val durationSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (ignored: Exception) {}
                release()
            }
            mediaRecorder = null
            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                Pair(file.absolutePath, durationSeconds)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error stopping record: ${e.message}", e)
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
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
    var currentlyPlayingUri: String? = null
        private set

    fun isPlaying(uri: String? = null): Boolean {
        return try {
            if (uri == null) {
                mediaPlayer?.isPlaying == true
            } else {
                currentlyPlayingUri == uri && mediaPlayer?.isPlaying == true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun playAudio(
        uriString: String,
        onCompletion: () -> Unit,
        onError: () -> Unit
    ) {
        stopAudio()
        try {
            val player = MediaPlayer().apply {
                if (uriString.startsWith("/")) {
                    setDataSource(uriString)
                } else {
                    setDataSource(context, Uri.parse(uriString))
                }
                prepare()
                setOnCompletionListener {
                    currentlyPlayingUri = null
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    currentlyPlayingUri = null
                    onError()
                    true
                }
                start()
            }
            mediaPlayer = player
            currentlyPlayingUri = uriString
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Error playing audio: ${e.message}", e)
            currentlyPlayingUri = null
            onError()
        }
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

    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null
        currentlyPlayingUri = null
    }
}
