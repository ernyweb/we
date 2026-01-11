package com.captiontranslator

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CaptionService : Service() {

    private lateinit var windowManager: WindowManager
    private var captionView: android.view.View? = null
    private var textViewCaption: TextView? = null
    private var serverTranslator: ServerTranslator? = null
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
        serverTranslator = ServerTranslator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlay()

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val targetLang = prefs.getString("target_lang", "TR") ?: "TR"

        // Media projection mode - internal audio capture
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode != 0 && data != null) {
            // Start internal audio capture with Vosk
            internalAudioCapture = InternalAudioCaptureManager(
                this,
                serverTranslator!!,
                targetLang,
                onTextRecognized = { text ->
                    Log.d(TAG, "Recognized: $text")
                    textViewCaption?.text = text
                },
                onTranslation = { original, translated ->
                    Log.d(TAG, "Translated: $original -> $translated")
                    textViewCaption?.text = translated
                    android.os.Handler(mainLooper).postDelayed({
                        textViewCaption?.text = ""
                    }, 8000)
                }
            )
            internalAudioCapture?.startCapture(resultCode, data)
        } else {
            textViewCaption?.text = "❌ No media projection data"
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
            .setContentText("VPS: 72.60.130.39")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        if (captionView != null) return

        try {
            val inflater = LayoutInflater.from(this)
            captionView = inflater.inflate(R.layout.overlay_caption, null)
            textViewCaption = captionView!!.findViewById(R.id.textViewCaption)

            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val textSize = prefs.getInt("text_size", 24)
            textViewCaption?.textSize = textSize.toFloat()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 100

            windowManager.addView(captionView, params)
            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            internalAudioCapture?.stopCapture()
            internalAudioCapture = null
            
            if (captionView != null) {
                windowManager.removeView(captionView)
                captionView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
