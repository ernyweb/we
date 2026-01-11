package com.captiontranslator

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.json.JSONObject
import java.io.File

class VoskTranscriber(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val sampleRate = 16000f

    companion object {
        private const val TAG = "VoskTranscriber"
        private const val MODEL_PATH = "/sdcard/vosk-model-small-en-us-0.15"
    }

    fun initialize(): Boolean {
        return try {
            val modelFile = File(MODEL_PATH)
            if (!modelFile.exists()) {
                Log.e(TAG, "Vosk model not found at $MODEL_PATH")
                return false
            }

            model = Model(MODEL_PATH)
            recognizer = Recognizer(model, sampleRate)
            Log.d(TAG, "Vosk initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk", e)
            false
        }
    }

    fun processAudioData(audioData: ShortArray): String? {
        return try {
            recognizer?.let { rec ->
                if (rec.acceptWaveForm(audioData, audioData.size)) {
                    val result = rec.result
                    val jsonResult = JSONObject(result)
                    val text = jsonResult.optString("text", "")
                    if (text.isNotEmpty()) {
                        Log.d(TAG, "Recognized: $text")
                        onTextRecognized(text)
                        return text
                    }
                } else {
                    // Partial result
                    val partialResult = rec.partialResult
                    val jsonPartial = JSONObject(partialResult)
                    val partial = jsonPartial.optString("partial", "")
                    if (partial.isNotEmpty()) {
                        Log.d(TAG, "Partial: $partial")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio", e)
            null
        }
    }

    fun getFinalResult(): String? {
        return try {
            recognizer?.let {
                val finalResult = it.finalResult
                val jsonFinal = JSONObject(finalResult)
                jsonFinal.optString("text", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting final result", e)
            null
        }
    }

    fun reset() {
        try {
            recognizer?.let {
                it.reset()
                Log.d(TAG, "Recognizer reset")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting recognizer", e)
        }
    }

    fun release() {
        try {
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
            Log.d(TAG, "Vosk resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Vosk", e)
        }
    }
}
