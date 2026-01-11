package com.captiontranslator

import android.util.Log
import com.deepl.api.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeepLTranslator(private val apiKey: String) {
    
    private val translator = Translator(apiKey)
    
    companion object {
        private const val TAG = "DeepLTranslator"
    }
    
    suspend fun translate(text: String, targetLang: String = "TR"): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Translating with DeepL: '$text' → $targetLang")
                
                val result = translator.translateText(text, null, targetLang)
                val translatedText = result.text
                
                Log.d(TAG, "✅ DeepL Success: '$text' → '$translatedText'")
                translatedText
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ DeepL Translation failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    suspend fun translateWithDetection(text: String, targetLang: String = "TR"): TranslationResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Translating with auto-detect: '$text' → $targetLang")
                
                val result = translator.translateText(text, null, targetLang)
                
                TranslationResult(
                    originalText = text,
                    translatedText = result.text,
                    detectedSourceLang = result.detectedSourceLanguage,
                    targetLang = targetLang
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ DeepL Translation failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    fun close() {
        // DeepL translator doesn't need explicit closing
    }
}

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val detectedSourceLang: String,
    val targetLang: String
)
