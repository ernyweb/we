package com.captiontranslator

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.nl.translate.Translator
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioCaptureManager(
    private val context: Context,
    private val translator: Translator,
    private val onTextRecognized: (String) -> Unit
) {
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    
    companion object {
        private const val TAG = "InternalAudioCapture"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    fun startCapture(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build()
            
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
            
            audioRecord?.startRecording()
            Log.d(TAG, "Internal audio capture started")
            
            // Start processing audio in background
            captureJob = CoroutineScope(Dispatchers.IO).launch {
                processAudioStream()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting internal audio capture", e)
            onTextRecognized("❌ Internal audio capture failed: ${e.message}")
        }
    }
    
    private suspend fun processAudioStream() {
        val buffer = ByteArray(1024)
        val audioBuffer = mutableListOf<Short>()
        var silenceCounter = 0
        val SILENCE_THRESHOLD = 500 // Adjust based on testing
        val SPEECH_MIN_LENGTH = SAMPLE_RATE / 2 // 0.5 seconds
        
        while (captureJob?.isActive == true) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            
            if (read > 0) {
                // Convert bytes to shorts (PCM 16-bit)
                val shorts = ByteBuffer.wrap(buffer, 0, read)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                
                var hasSound = false
                
                while (shorts.hasRemaining()) {
                    val sample = shorts.get()
                    val amplitude = Math.abs(sample.toInt())
                    
                    if (amplitude > SILENCE_THRESHOLD) {
                        hasSound = true
                        silenceCounter = 0
                        audioBuffer.add(sample)
                    } else {
                        if (audioBuffer.isNotEmpty()) {
                            silenceCounter++
                            audioBuffer.add(sample)
                        }
                    }
                }
                
                // If we have silence for a while after speech, process it
                if (silenceCounter > 20 && audioBuffer.size > SPEECH_MIN_LENGTH) {
                    // Convert audio to text using speech recognition
                    // Note: Google Speech Recognition works best with microphone
                    // For internal audio, we need to use a different approach
                    
                    Log.d(TAG, "Detected audio segment: ${audioBuffer.size} samples")
                    
                    withContext(Dispatchers.Main) {
                        onTextRecognized("🔊 Audio detected (${audioBuffer.size} samples)")
                    }
                    
                    audioBuffer.clear()
                    silenceCounter = 0
                }
            }
            
            delay(10) // Small delay to prevent busy waiting
        }
    }
    
    fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "Internal audio capture stopped")
    }
}
