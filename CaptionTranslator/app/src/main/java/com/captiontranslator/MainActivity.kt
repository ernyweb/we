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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerSourceLang: Spinner
    private lateinit var spinnerTargetLang: Spinner
    private lateinit var toggleService: androidx.appcompat.widget.SwitchCompat
    private lateinit var seekBarTextSize: SeekBar
    private lateinit var textViewTextSize: TextView
    private lateinit var textViewServerStatus: TextView
    private lateinit var textViewServiceStatus: TextView
    private lateinit var btnTestCaption: Button

    private val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    private val MEDIA_PROJECTION_REQUEST_CODE = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupLanguageSpinners()
        checkServerConnection()
        setupListeners()
    }

    private fun initViews() {
        spinnerSourceLang = findViewById(R.id.spinnerSourceLang)
        spinnerTargetLang = findViewById(R.id.spinnerTargetLang)
        toggleService = findViewById(R.id.toggleService)
        seekBarTextSize = findViewById(R.id.seekBarTextSize)
        textViewTextSize = findViewById(R.id.textViewTextSize)
        textViewServerStatus = findViewById(R.id.textViewServerStatus)
        textViewServiceStatus = findViewById(R.id.textViewServiceStatus)
        btnTestCaption = findViewById(R.id.btnTestCaption)

        toggleService.isEnabled = false
    }

    private fun setupLanguageSpinners() {
        val languages = arrayOf("EN (English)", "TR (Türkçe)", "RU (Русский)", "ES (Español)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerSourceLang.adapter = adapter
        spinnerTargetLang.adapter = adapter

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val sourceLang = prefs.getString("source_lang", "EN") ?: "EN"
        val targetLang = prefs.getString("target_lang", "TR") ?: "TR"

        spinnerSourceLang.setSelection(getLanguageIndex(sourceLang))
        spinnerTargetLang.setSelection(getLanguageIndex(targetLang))

        spinnerSourceLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val lang = getLanguageCode(position)
                prefs.edit().putString("source_lang", lang).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTargetLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val lang = getLanguageCode(position)
                prefs.edit().putString("target_lang", lang).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getLanguageIndex(code: String): Int {
        return when (code) {
            "EN" -> 0
            "TR" -> 1
            "RU" -> 2
            "ES" -> 3
            else -> 0
        }
    }

    private fun getLanguageCode(index: Int): String {
        return when (index) {
            0 -> "EN"
            1 -> "TR"
            2 -> "RU"
            3 -> "ES"
            else -> "EN"
        }
    }

    private fun checkServerConnection() {
        textViewServerStatus.text = "● Checking VPS..."
        textViewServerStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val translator = ServerTranslator()
                val connected = translator.testConnection()
                
                withContext(Dispatchers.Main) {
                    if (connected) {
                        textViewServerStatus.text = "● VPS Online (72.60.130.39)"
                        textViewServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                        toggleService.isEnabled = true
                    } else {
                        textViewServerStatus.text = "● VPS Offline"
                        textViewServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                        showServerErrorDialog()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewServerStatus.text = "● Connection Failed"
                    textViewServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                    showServerErrorDialog()
                }
            }
        }
    }

    private fun setupListeners() {
        toggleService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    requestMediaProjection()
                } else {
                    toggleService.isChecked = false
                    showOverlayPermissionDialog()
                }
            } else {
                stopCaptionService()
                textViewServiceStatus.text = "Tap to start"
            }
        }

        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12 + progress
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
            intent.putExtra("test_text", "Testing internal audio capture and translation")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Test caption displayed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Enable 'Display over other apps' to show captions.")
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showServerErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("VPS Connection Failed")
            .setMessage("Cannot connect to 72.60.130.39\n\nCheck:\n• Internet connection\n• VPS server is running")
            .setPositiveButton("Retry") { _, _ ->
                checkServerConnection()
            }
            .setNegativeButton("OK", null)
            .show()
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
                val intent = Intent(this, CaptionService::class.java)
                intent.putExtra("media_projection_result_code", resultCode)
                intent.putExtra("media_projection_data", data)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                textViewServiceStatus.text = "🔊 Capturing audio..."
                Toast.makeText(this, "Internal audio capture started!", Toast.LENGTH_SHORT).show()
            } else {
                toggleService.isChecked = false
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopCaptionService() {
        stopService(Intent(this, CaptionService::class.java))
        Toast.makeText(this, "Caption service stopped", Toast.LENGTH_SHORT).show()
    }
}
