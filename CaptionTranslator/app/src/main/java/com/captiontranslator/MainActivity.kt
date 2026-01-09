package com.captiontranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.TranslateLanguage

class MainActivity : AppCompatActivity() {

    private lateinit var toggleService: Switch
    private lateinit var spinnerSourceLang: Spinner
    private lateinit var spinnerTargetLang: Spinner
    private lateinit var seekBarTextSize: SeekBar
    private lateinit var textViewTextSize: TextView
    private lateinit var btnTestCaption: Button
    private lateinit var radioMicrophone: RadioButton
    private lateinit var radioInternalAudio: RadioButton

    private val OVERLAY_PERMISSION_REQUEST_CODE = 100
    private val AUDIO_PERMISSION_REQUEST_CODE = 101
    private val MEDIA_PROJECTION_REQUEST_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupLanguageSpinners()
        setupListeners()
        checkPermissions()
    }

    private fun initViews() {
        toggleService = findViewById(R.id.toggleService)
        spinnerSourceLang = findViewById(R.id.spinnerSourceLang)
        spinnerTargetLang = findViewById(R.id.spinnerTargetLang)
        seekBarTextSize = findViewById(R.id.seekBarTextSize)
        textViewTextSize = findViewById(R.id.textViewTextSize)
        btnTestCaption = findViewById(R.id.btnTestCaption)
        radioMicrophone = findViewById(R.id.radioMicrophone)
        radioInternalAudio = findViewById(R.id.radioInternalAudio)
        
        // Load saved audio source preference
        val audioSource = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("audio_source", "microphone") ?: "microphone"
        
        when (audioSource) {
            "microphone" -> radioMicrophone.isChecked = true
            "internal" -> radioInternalAudio.isChecked = true
        }
    }

    private fun setupLanguageSpinners() {
        val languages = listOf(
            Language("Auto Detect", TranslateLanguage.ENGLISH),
            Language("🇹🇷 Turkish (Türkçe)", TranslateLanguage.TURKISH),
            Language("🇺🇸 English", TranslateLanguage.ENGLISH),
            Language("🇨🇳 Chinese (中文)", TranslateLanguage.CHINESE),
            Language("🇪🇸 Spanish (Español)", TranslateLanguage.SPANISH),
            Language("🇫🇷 French (Français)", TranslateLanguage.FRENCH),
            Language("🇩🇪 German (Deutsch)", TranslateLanguage.GERMAN),
            Language("🇮🇹 Italian (Italiano)", TranslateLanguage.ITALIAN),
            Language("🇯🇵 Japanese (日本語)", TranslateLanguage.JAPANESE),
            Language("🇰🇷 Korean (한국어)", TranslateLanguage.KOREAN),
            Language("🇷🇺 Russian (Русский)", TranslateLanguage.RUSSIAN),
            Language("🇦🇪 Arabic (العربية)", TranslateLanguage.ARABIC),
            Language("🇵🇹 Portuguese", TranslateLanguage.PORTUGUESE),
            Language("🇮🇳 Hindi (हिन्दी)", TranslateLanguage.HINDI),
            Language("🇧🇩 Bengali (বাংলা)", TranslateLanguage.BENGALI),
            Language("🇮🇩 Indonesian", TranslateLanguage.INDONESIAN),
            Language("🇹🇭 Thai (ไทย)", TranslateLanguage.THAI),
            Language("🇻🇳 Vietnamese", TranslateLanguage.VIETNAMESE),
            Language("🇳🇱 Dutch (Nederlands)", TranslateLanguage.DUTCH),
            Language("🇬🇷 Greek (Ελληνικά)", TranslateLanguage.GREEK)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerSourceLang.adapter = adapter
        spinnerTargetLang.adapter = adapter

        // Default: Auto → Turkish
        spinnerSourceLang.setSelection(0)
        spinnerTargetLang.setSelection(1)

        // Save language codes
        spinnerSourceLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putString("source_lang", languages[position].code)
                    .apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTargetLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putString("target_lang", languages[position].code)
                    .apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        // Audio source selection
        radioMicrophone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putString("audio_source", "microphone")
                    .apply()
                Toast.makeText(this, "🎤 Microphone mode (for speaking)", Toast.LENGTH_SHORT).show()
            }
        }
        
        radioInternalAudio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getSharedPreferences("settings", MODE_PRIVATE).edit()
                        .putString("audio_source", "internal")
                        .apply()
                    Toast.makeText(this, "🔊 Internal audio mode (for YouTube/videos)", Toast.LENGTH_SHORT).show()
                } else {
                    radioMicrophone.isChecked = true
                    Toast.makeText(this, "Internal audio requires Android 10+", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        toggleService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val audioSource = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("audio_source", "microphone") ?: "microphone"
                
                if (audioSource == "internal" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestMediaProjection()
                } else {
                    if (hasAllPermissions()) {
                        startCaptionService()
                    } else {
                        toggleService.isChecked = false
                        checkPermissions()
                    }
                }
            } else {
                stopCaptionService()
            }
        }

        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12 + progress // 12-42 sp
                textViewTextSize.text = "${size}sp"
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putInt("text_size", size)
                    .apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnTestCaption.setOnClickListener {
            val intent = Intent(this, CaptionService::class.java)
            intent.putExtra("test_mode", true)
            intent.putExtra("test_text", "Hello! This is a test caption. 你好！这是测试字幕。")
            startService(intent)
        }
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                   != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.RECORD_AUDIO), 
                AUDIO_PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasAllPermissions(): Boolean {
        return Settings.canDrawOverlays(this) &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("Please enable 'Display over other apps' permission to show captions.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                    Uri.parse("package:$packageName"))
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startCaptionService() {
        val intent = Intent(this, CaptionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Caption service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopCaptionService() {
        stopService(Intent(this, CaptionService::class.java))
        Toast.makeText(this, "Caption service stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun requestMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Save the result for CaptionService
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putInt("media_projection_result_code", resultCode)
                    .apply()
                
                // Start service with projection data
                val intent = Intent(this, CaptionService::class.java)
                intent.putExtra("media_projection_result_code", resultCode)
                intent.putExtra("media_projection_data", data)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                Toast.makeText(this, "Internal audio capture started!", Toast.LENGTH_SHORT).show()
            } else {
                toggleService.isChecked = false
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Audio permission required for speech recognition", Toast.LENGTH_LONG).show()
            }
        }
    }

    data class Language(val name: String, val code: String)
}
