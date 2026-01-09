package com.captiontranslator

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class CaptionService : Service() {

    private lateinit var windowManager: WindowManager
    private var captionView: android.view.View? = null
    private lateinit var textViewCaption: TextView
    private var translator: Translator? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var internalAudioCapture: InternalAudioCaptureManager? = null

    companion object {
        private const val TAG = "CaptionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "caption_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        initializeTranslator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        // Test mode
        if (intent?.getBooleanExtra("test_mode", false) == true) {
            val testText = intent.getStringExtra("test_text") ?: "Test caption"
            showOverlay()
            translateAndDisplay(testText)
            return START_STICKY
        }

        showOverlay()
        
        // Check audio source preference
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val audioSource = prefs.getString("audio_source", "microphone") ?: "microphone"
        
        if (audioSource == "internal" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Internal audio mode
            val resultCode = intent?.getIntExtra("media_projection_result_code", 0) ?: 0
            val data = intent?.getParcelableExtra<Intent>("media_projection_data")
            
            if (data != null && resultCode != 0) {
                textViewCaption?.text = "🔊 Capturing internal audio..."
                
                internalAudioCapture = InternalAudioCaptureManager(
                    this,
                    translator!!,
                    onTextRecognized = { text ->
                        textViewCaption?.text = text
                    },
                    onTranslation = { original, translated ->
                        // Show translated text on overlay
                        textViewCaption?.text = "✅ $translated"
                        
                        // Auto-hide after 8 seconds
                        android.os.Handler(mainLooper).postDelayed({
                            textViewCaption?.text = ""
                        }, 8000)
                    }
                )
                internalAudioCapture?.startCapture(resultCode, data)
            } else {
                textViewCaption?.text = "❌ Media projection data missing"
            }
        } else {
            // Microphone mode (default)
            textViewCaption?.text = "🎤 Listening for speech..."
            
            android.os.Handler(mainLooper).postDelayed({
                startSpeechRecognition()
            }, 2000)
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Caption Translator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Translating captions in real-time"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Caption Translator")
            .setContentText("Listening and translating...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initializeTranslator() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val sourceLang = prefs.getString("source_lang", TranslateLanguage.ENGLISH) ?: TranslateLanguage.ENGLISH
        val targetLang = prefs.getString("target_lang", TranslateLanguage.TURKISH) ?: TranslateLanguage.TURKISH

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        translator = Translation.getClient(options)

        // Download model if needed
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Translation model downloaded")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Model download failed", e)
            }
    }

    private fun showOverlay() {
        if (captionView != null) return

        val inflater = LayoutInflater.from(this)
        captionView = inflater.inflate(R.layout.overlay_caption, null)
        textViewCaption = captionView!!.findViewById(R.id.textViewCaption)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val textSize = prefs.getInt("text_size", 24)
        textViewCaption.textSize = textSize.toFloat()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 100 // 100px from bottom

        try {
            windowManager.addView(captionView, params)
            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            textViewCaption?.text = "❌ Speech recognition not available on this device"
            return
        }
        
        Log.d(TAG, "Speech recognition is available, creating recognizer...")

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                isListening = true
                textViewCaption?.text = "🎤 Ready - Speak now!"
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
                textViewCaption?.text = "🗣️ Speaking detected..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                isListening = false
                // Restart listening
                android.os.Handler(mainLooper).postDelayed({
                    if (captionView != null) {
                        startListening()
                    }
                }, 500)
            }

            override fun onError(error: Int) {
                val errorMsg = when(error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "❌ No microphone permission!"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Log.e(TAG, "Speech recognition error: $error - $errorMsg")
                textViewCaption?.text = "⚠️ $errorMsg - Retrying..."
                isListening = false
                // Retry after delay
                android.os.Handler(mainLooper).postDelayed({
                    if (captionView != null) {
                        textViewCaption?.text = "🎤 Listening again..."
                        startListening()
                    }
                }, 3000)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d(TAG, "Recognized: $spokenText")
                    textViewCaption?.text = "📝 $spokenText"
                    translateAndDisplay(spokenText)
                } else {
                    Log.d(TAG, "No results found")
                    textViewCaption?.text = "🤷 No speech detected"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partialText = matches[0]
                    // Show partial result without translation
                    textViewCaption?.text = partialText
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startListening()
    }

    private fun startListening() {
        if (isListening) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val sourceLang = prefs.getString("source_lang", TranslateLanguage.ENGLISH) ?: TranslateLanguage.ENGLISH
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocaleFromLanguageCode(sourceLang))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    private fun translateAndDisplay(text: String) {
        if (translator == null) {
            Log.e(TAG, "Translator not initialized")
            textViewCaption?.text = "❌ Translator not ready. Please wait..."
            // Reinitialize
            initializeTranslator()
            return
        }
        
        Log.d(TAG, "Translating: $text")
        textViewCaption?.text = "⏳ Translating..."
        
        translator?.translate(text)
            ?.addOnSuccessListener { translatedText ->
                Log.d(TAG, "Translated: $translatedText")
                textViewCaption?.text = "✅ $translatedText"
                
                // Auto-hide after 8 seconds (longer to read)
                android.os.Handler(mainLooper).postDelayed({
                    textViewCaption?.text = ""
                }, 8000)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Translation failed", e)
                // Show original text if translation fails
                textViewCaption?.text = "⚠️ Translation failed: $text"
                
                // Auto-hide after 6 seconds
                android.os.Handler(mainLooper).postDelayed({
                    textViewCaption?.text = ""
                }, 6000)
            }
    }

    private fun getLocaleFromLanguageCode(code: String): String {
        return when (code) {
            TranslateLanguage.TURKISH -> "tr-TR"
            TranslateLanguage.ENGLISH -> "en-US"
            TranslateLanguage.CHINESE -> "zh-CN"
            TranslateLanguage.SPANISH -> "es-ES"
            TranslateLanguage.FRENCH -> "fr-FR"
            TranslateLanguage.GERMAN -> "de-DE"
            TranslateLanguage.ITALIAN -> "it-IT"
            TranslateLanguage.JAPANESE -> "ja-JP"
            TranslateLanguage.KOREAN -> "ko-KR"
            TranslateLanguage.RUSSIAN -> "ru-RU"
            TranslateLanguage.ARABIC -> "ar-SA"
            TranslateLanguage.PORTUGUESE -> "pt-PT"
            TranslateLanguage.HINDI -> "hi-IN"
            TranslateLanguage.BENGALI -> "bn-BD"
            TranslateLanguage.INDONESIAN -> "id-ID"
            TranslateLanguage.THAI -> "th-TH"
            TranslateLanguage.VIETNAMESE -> "vi-VN"
            TranslateLanguage.DUTCH -> "nl-NL"
            TranslateLanguage.GREEK -> "el-GR"
            else -> "en-US"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        internalAudioCapture?.stopCapture()
        translator?.close()
        captionView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
        captionView = null
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
