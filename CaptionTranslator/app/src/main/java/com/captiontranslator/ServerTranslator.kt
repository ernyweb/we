package com.captiontranslator

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerTranslator(
    private val serverUrl: String,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "ServerTranslator"
    }
    
    suspend fun translate(
        text: String,
        from: String = "en",
        to: String = "tr"
    ): String = withContext(Dispatchers.IO) {
        try {
            // Clean text for URL (replace spaces with dots)
            val cleanText = text.trim()
                .lowercase()
                .replace(" ", ".")
                .replace("[^a-z0-9.]".toRegex(), "")
            
            // Build URL: /translate-en-hello.world-to-tr?api_key=xxx
            val url = "$serverUrl/translate-$from-$cleanText-to-$to?api_key=$apiKey"
            
            Log.d(TAG, "Translating via server: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Server error: ${response.code}")
            }
            
            val jsonResponse = JSONObject(response.body?.string() ?: "{}")
            val translatedText = jsonResponse.getString("translated")
            
            Log.d(TAG, "Translation: '$text' → '$translatedText'")
            
            translatedText
            
        } catch (e: Exception) {
            Log.e(TAG, "Server translation failed", e)
            throw e
        }
    }
    
    suspend fun downloadLanguagePack(language: String): LanguagePack? = withContext(Dispatchers.IO) {
        try {
            val url = "$serverUrl/download/$language?api_key=$apiKey"
            
            Log.d(TAG, "Downloading language pack: $language")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code}")
            }
            
            val jsonResponse = JSONObject(response.body?.string() ?: "{}")
            
            LanguagePack(
                language = jsonResponse.getString("language"),
                version = jsonResponse.getString("version"),
                wordCount = jsonResponse.getInt("wordCount"),
                dictionaries = jsonResponse.getJSONObject("dictionaries").toString(),
                downloadedAt = jsonResponse.getString("downloadedAt")
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Language pack download failed", e)
            null
        }
    }
}

data class LanguagePack(
    val language: String,
    val version: String,
    val wordCount: Int,
    val dictionaries: String,  // JSON string
    val downloadedAt: String
)
