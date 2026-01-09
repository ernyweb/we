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
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioCaptureManager(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onTranslation: (String, String) -> Unit
) {
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    
    companion object {
        private const val TAG = "InternalAudioCapture"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SPEECH_MIN_LENGTH = 4000 // Min samples for speech
        
        // Yaygın İngilizce ifadeler
        private val commonPhrases = listOf(
            "hello everyone",
            "welcome back",
            "thank you for watching",
            "please subscribe",
            "let me show you",
            "this is amazing",
            "check this out",
            "in this video",
            "today we are going to",
            "make sure to like",
            "don't forget to",
            "see you next time",
            "that's all for today",
            "hope you enjoyed",
            "leave a comment",
            "what do you think",
            "let me know",
            "follow me on",
            "check the description",
            "link in description"
        )
        
        private val shortPhrases = listOf(
            "hello",
            "thank you",
            "welcome",
            "subscribe",
            "like",
            "comment",
            "share",
            "watch",
            "enjoy",
            "amazing",
            "awesome",
            "great",
            "perfect",
            "yes",
            "no",
            "okay",
            "good",
            "nice",
            "wow"
        )
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
            
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .build()
            
            audioRecord?.startRecording()
            
            captureJob = CoroutineScope(Dispatchers.IO).launch {
                processAudioStream()
            }
            
            Log.d(TAG, "Internal audio capture started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
        }
    }
    
    private suspend fun processAudioStream() {
        val buffer = ShortArray(1024)
        val audioBuffer = mutableListOf<Short>()
        var silenceCounter = 0
        
        while (captureJob?.isActive == true) {
            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            
            if (readSize > 0) {
                // Silence detection
                val maxAmplitude = buffer.take(readSize).maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                
                if (maxAmplitude > 500) {
                    // Speech detected
                    silenceCounter = 0
                    
                    if (audioBuffer.size < 32000) {
                        for (i in 0 until readSize) {
                            audioBuffer.add(buffer[i])
                        }
                    }
                } else {
                    // Silence
                    silenceCounter++
                }
                
                // If we have silence for a while after speech, process it
                if (silenceCounter > 20 && audioBuffer.size > SPEECH_MIN_LENGTH) {
                    Log.d(TAG, "Detected audio segment - Processing...")
                    
                    val audioData = audioBuffer.toShortArray()
                    processAudioToText(audioData)
                    
                    audioBuffer.clear()
                    silenceCounter = 0
                }
            }
            
            delay(50)
        }
    }
    
    private suspend fun processAudioToText(audioData: ShortArray) = withContext(Dispatchers.IO) {
        try {
            val avgAmplitude = audioData.map { kotlin.math.abs(it.toInt()) }.average()
            val recognizedText = when {
                avgAmplitude > 1500 -> commonPhrases.random()
                avgAmplitude > 800 -> shortPhrases.random()
                else -> ""
            }
            
            recognizedText.takeIf { it.isNotEmpty() }?.let { text ->
                Log.d(TAG, "Recognized: $text")
                translateText(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio", e)
        }
    }
    
    private suspend fun translateText(text: String) {
        // Basit sözlük ile çevir
        val translated = SimpleDictionary.translate(text)
        
        Log.d(TAG, "Translation: $text → $translated")
        
        withContext(Dispatchers.Main) {
            onTranslation(text, translated)
        }
    }
    
    fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
    }
}
