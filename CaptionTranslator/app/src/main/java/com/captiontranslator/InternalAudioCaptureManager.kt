package com.captiontranslator

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class InternalAudioCaptureManager(
    private val context: Context,
    private val serverTranslator: ServerTranslator,
    private val targetLang: String,
    private val onTextRecognized: (String) -> Unit,
    private val onTranslation: (String, String) -> Unit
) {
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var voskTranscriber: VoskTranscriber? = null
    private var captureJob: Job? = null
    private var isCapturing = false

    companion object {
        private const val TAG = "InternalAudioCapture"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    fun startCapture(resultCode: Int, data: Intent?) {
        if (isCapturing) {
            Log.w(TAG, "Already capturing")
            return
        }

        try {
            // Initialize Vosk
            voskTranscriber = VoskTranscriber(context) { text ->
                onTextRecognized(text)
                translateText(text)
            }

            if (!voskTranscriber!!.initialize()) {
                Log.e(TAG, "Failed to initialize Vosk")
                onTextRecognized("❌ Vosk model not found")
                return
            }

            // Get MediaProjection
            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection")
                onTextRecognized("❌ Media projection failed")
                return
            }

            // Setup AudioRecord
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                onTextRecognized("❌ Audio recording failed")
                return
            }

            // Start recording
            audioRecord?.startRecording()
            isCapturing = true

            Log.d(TAG, "Audio capture started")
            onTextRecognized("🎧 Listening to internal audio...")

            // Start capture loop
            captureJob = CoroutineScope(Dispatchers.IO).launch {
                captureAudioLoop()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture", e)
            onTextRecognized("❌ Error: ${e.message}")
            stopCapture()
        }
    }

    private suspend fun captureAudioLoop() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
        val audioBuffer = ShortArray(bufferSize / 2) // 16-bit samples

        try {
            while (isCapturing && captureJob?.isActive == true) {
                val readBytes = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                if (readBytes > 0) {
                    // Process with Vosk
                    voskTranscriber?.processAudioData(audioBuffer)
                } else if (readBytes < 0) {
                    Log.e(TAG, "AudioRecord read error: $readBytes")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in capture loop", e)
        } finally {
            Log.d(TAG, "Capture loop ended")
        }
    }

    private fun translateText(text: String) {
        if (text.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val sourceLang = prefs.getString("source_lang", "EN") ?: "EN"
                
                val translated = serverTranslator.translate(text, sourceLang, targetLang)
                
                if (translated != null) {
                    Log.d(TAG, "Translation: $text -> $translated")
                    onTranslation(text, translated)
                } else {
                    Log.w(TAG, "Translation returned null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
            }
        }
    }

    fun stopCapture() {
        isCapturing = false

        try {
            captureJob?.cancel()
            captureJob = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            voskTranscriber?.release()
            voskTranscriber = null

            mediaProjection?.stop()
            mediaProjection = null

            Log.d(TAG, "Capture stopped and resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        }
    }

    fun isRecording(): Boolean = isCapturing
}
