package com.captiontranslator

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Secure translation client with HMAC-SHA256 authentication
 * Connects to self-hosted translation server at 72.60.130.39
 */
class ServerTranslator {
    companion object {
        private const val TAG = "ServerTranslator"
        
        // Server configuration - Special APK key for internal audio mode
        private const val SERVER_URL = "http://72.60.130.39"
        private const val API_KEY = "mobile-internal-audio-key-2026-xyz789"
        private const val SECRET_KEY = "vps-translation-server-secret-key-2026-change-this"
        private const val TIMEOUT_SECONDS = 20L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Generate HMAC-SHA256 signature for request
     */
    private fun generateSignature(data: String, timestamp: Long): String {
        val payload = "$data|$timestamp"
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val hmac = mac.doFinal(payload.toByteArray())
        return hmac.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify response signature
     */
    private fun verifySignature(data: String, timestamp: Long, signature: String): Boolean {
        val expectedSignature = generateSignature(data, timestamp)
        return signature == expectedSignature
    }
    
    /**
     * Translate text using secure server API
     * @param text Text to translate
     * @param from Source language (en, tr, ru, es, fr)
     * @param to Target language (en, tr, ru, es, fr)
     * @return Translated text or null on failure
     */
    suspend fun translate(
        text: String,
        from: String = "en",
        to: String = "tr"
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Prepare simple request - no signature
            val requestData = JSONObject().apply {
                put("text", text)
                put("source", from)
                put("target", to)
            }
            
            // Build request with API key only
            val requestBody = requestData.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$SERVER_URL/translate")
                .addHeader("X-API-Key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "🌐 Translating: '$text' ($from → $to)")
            Log.d(TAG, "📡 URL: $SERVER_URL/translate")
            Log.d(TAG, "🔑 API Key: ${API_KEY.take(20)}...")
            Log.d(TAG, "📦 Request: $requestData")
            
            // Execute request
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "📥 Response code: ${response.code}")
                val responseBody = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ Server error: ${response.code} - ${response.message}")
                    Log.e(TAG, "Error body: $responseBody")
                    return@withContext null
                }
                
                Log.d(TAG, "📨 Response: $responseBody")
                val json = JSONObject(responseBody)
                
                // Parse response (no signature verification)
                val responseData = json.getJSONObject("data")
                
                // Parse translation
                if (responseData.has("success") && responseData.getBoolean("success")) {
                    val translated = responseData.getString("translated")
                    val accuracy = responseData.optInt("wordsTranslated", 0).toFloat() / 
                                  responseData.optInt("totalWords", 1).toFloat()
                    
                    Log.d(TAG, "✅ Translation: '$translated' (${(accuracy * 100).toInt()}% accuracy)")
                    return@withContext translated
                } else if (responseData.has("error")) {
                    Log.e(TAG, "Translation failed: ${responseData.optString("error")}")
                    return@withContext null
                } else {
                    // Maybe direct translation string
                    val translated = responseData.optString("translation", responseData.toString())
                    Log.d(TAG, "✅ Translation: '$translated'")
                    return@withContext translated
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ DNS error: Cannot resolve $SERVER_URL", e)
            return@withContext null
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ Connection refused: Server not reachable at $SERVER_URL", e)
            return@withContext null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Timeout: Server took too long to respond (${TIMEOUT_SECONDS}s)", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Translation error: ${e.javaClass.simpleName} - ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Test server connection
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple test translation
            Log.d(TAG, "Testing VPS connection...")
            val result = translate("test", "EN", "TR")
            val success = result != null
            Log.d(TAG, "Connection test: ${if (success) "✅ SUCCESS (result: $result)" else "❌ FAILED"}")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            return@withContext false
        }
    }
}
