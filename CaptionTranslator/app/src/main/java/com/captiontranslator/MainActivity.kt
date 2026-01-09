package com.captiontranslator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    private val OVERLAY_PERMISSION_REQUEST_CODE = 100
    private val AUDIO_PERMISSION_REQUEST_CODE = 101

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
        toggleService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasAllPermissions()) {
                    startCaptionService()
                } else {
                    toggleService.isChecked = false
                    checkPermissions()
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
