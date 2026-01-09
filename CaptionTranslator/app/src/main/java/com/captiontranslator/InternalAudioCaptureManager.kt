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
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioCaptureManager(
    private val context: Context,
    private val translator: Translator,
    private val onTextRecognized: (String) -> Unit,
    private val onTranslation: (String, String) -> Unit  // (original, translated)
) {
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var recognitionJob: Job? = null
    private var voskModel: Model? = null
    private var voskRecognizer: Recognizer? = null
    
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
                    // Convert audio buffer to WAV and recognize
                    Log.d(TAG, "Detected audio segment: ${audioBuffer.size} samples - Processing...")
                    
                    val audioData = audioBuffer.toShortArray()
                    
                    withContext(Dispatchers.Main) {
                        onTextRecognized("🎧 Processing ${audioBuffer.size} samples...")
                    }
                    
                    // Process speech recognition
                    processAudioToText(audioData)
                    
                    audioBuffer.clear()
                    silenceCounter = 0
                }
            }
            
            delay(10) // Small delay to prevent busy waiting
        }
    }
    
    private suspend fun processAudioToText(audioData: ShortArray) {
        try {
            // Save audio to temporary WAV file
            val wavFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
            saveAsWav(audioData, wavFile)
            
            Log.d(TAG, "WAV file saved: ${wavFile.absolutePath}, size: ${wavFile.length()} bytes")
            
            // Use Vosk for speech recognition
            recognizeAudioFile(wavFile)
            
            // Clean up
            wavFile.delete()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio to text", e)
        }
    }
    
    private suspend fun recognizeAudioFile(wavFile: File) {
        withContext(Dispatchers.IO) {
            try {
                // Initialize Vosk if not already done
                if (voskModel == null) {
                    initializeVosk()
                }
                
                if (voskRecognizer == null) {
                    Log.e(TAG, "Vosk recognizer not initialized")
                    return@withContext
                }
                
                // Read WAV file (skip 44-byte header)
                val audioData = wavFile.readBytes().copyOfRange(44, wavFile.readBytes().size)
                
                // Process audio with Vosk
                voskRecognizer?.acceptWaveForm(audioData, audioData.size)
                val result = voskRecognizer?.result
                
                Log.d(TAG, "Vosk result: $result")
                
                // Parse JSON result
                val recognizedText = parseVoskResult(result ?: "")
                
                if (recognizedText.isNotEmpty()) {
                    Log.d(TAG, "Recognized text: $recognizedText")
                    
                    // Translate the text
                    translateText(recognizedText)
                } else {
                    Log.d(TAG, "No speech detected in audio")
                }
                
                // Reset recognizer for next use
                voskRecognizer?.reset()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error recognizing audio", e)
            }
        }
    }
    
    private fun parseVoskResult(jsonResult: String): String {
        return try {
            // Simple JSON parsing - extract "text" field
            val textStart = jsonResult.indexOf("\"text\" : \"")
            if (textStart == -1) return ""
            
            val textContentStart = textStart + "\"text\" : \"".length
            val textContentEnd = jsonResult.indexOf("\"", textContentStart)
            
            if (textContentEnd == -1) return ""
            
            jsonResult.substring(textContentStart, textContentEnd).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk result", e)
            ""
        }
    }
    
    private fun initializeVosk() {
        try {
            Log.d(TAG, "Initializing Vosk model...")
            
            // Unpack model from assets if needed
            val modelPath = File(context.filesDir, "vosk-model-en")
            if (!modelPath.exists()) {
                copyAssetFolder("vosk-model-en", modelPath.absolutePath)
            }
            
            voskModel = Model(modelPath.absolutePath)
            voskRecognizer = Recognizer(voskModel, SAMPLE_RATE.toFloat())
            
            Log.d(TAG, "Vosk initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk", e)
        }
    }
    
    private fun copyAssetFolder(srcName: String, dstName: String) {
        try {
            val assetManager = context.assets
            val files = assetManager.list(srcName) ?: emptyArray()
            
            val outDir = File(dstName)
            if (!outDir.exists()) {
                outDir.mkdirs()
            }
            
            for (filename in files) {
                val src = "$srcName/$filename"
                val dst = "$dstName/$filename"
                
                if (assetManager.list(src)?.isNotEmpty() == true) {
                    // It's a directory
                    copyAssetFolder(src, dst)
                } else {
                    // It's a file
                    copyAssetFile(src, dst)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset folder", e)
        }
    }
    
    private fun copyAssetFile(srcName: String, dstName: String) {
        try {
            val inStream: InputStream = context.assets.open(srcName)
            val outFile = File(dstName)
            val outStream = FileOutputStream(outFile)
            
            val buffer = ByteArray(4096)
            var length: Int
            while (inStream.read(buffer).also { length = it } > 0) {
                outStream.write(buffer, 0, length)
            }
            
            inStream.close()
            outStream.flush()
            outStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset file: $srcName", e)
        }
    }
    
    private suspend fun translateText(text: String) {
        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                Log.d(TAG, "Translation: $text → $translatedText")
                onTranslation(text, translatedText)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Translation failed", e)
                onTranslation(text, "⚠️ Translation failed")
            }
    }
    
    private fun saveAsWav(audioData: ShortArray, outputFile: File) {
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2 // 16-bit = 2 bytes
        
        FileOutputStream(outputFile).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(36 + audioData.size * 2), 0, 4)
            fos.write("WAVE".toByteArray())
            
            // fmt chunk
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16), 0, 4) // chunk size
            fos.write(shortToByteArray(1), 0, 2) // audio format (PCM)
            fos.write(shortToByteArray(channels.toShort()), 0, 2)
            fos.write(intToByteArray(SAMPLE_RATE), 0, 4)
            fos.write(intToByteArray(byteRate), 0, 4)
            fos.write(shortToByteArray((channels * 2).toShort()), 0, 2) // block align
            fos.write(shortToByteArray(16), 0, 2) // bits per sample
            
            // data chunk
            fos.write("data".toByteArray())
            fos.write(intToByteArray(audioData.size * 2), 0, 4)
            
            // audio data
            val buffer = ByteBuffer.allocate(audioData.size * 2)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            for (sample in audioData) {
                buffer.putShort(sample)
            }
            fos.write(buffer.array())
        }
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
    
    fun stopCapture() {
        captureJob?.cancel()
        recognitionJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "Internal audio capture stopped")
    }
}
