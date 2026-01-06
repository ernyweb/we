package com.gamebooster.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.ProgressBar
import android.widget.TextView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var gameDetector: GameDetector
    private lateinit var gameLauncher: GameLauncher
    private lateinit var gameAdapter: GameAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statsText: TextView
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameDetector = GameDetector(this)
        gameLauncher = GameLauncher(this)

        recyclerView = findViewById(R.id.gamesRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        statsText = findViewById(R.id.statsText)

        setupRecyclerView()
        
        // Android 11+ cihazlarda permission iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val requiredPermissions = listOf(
                Manifest.permission.QUERY_ALL_PACKAGES,
                Manifest.permission.GET_TASKS,
                Manifest.permission.KILL_BACKGROUND_PROCESSES
            )
            
            val missingPermissions = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                loadGames()
            }
        } else {
            loadGames()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadGames()
        }
    }

    private fun setupRecyclerView() {
        gameAdapter = GameAdapter(emptyList()) { gameInfo ->
            launchGame(gameInfo.packageName)
        }
        recyclerView.adapter = gameAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadGames() {
        progressBar.visibility = android.view.View.VISIBLE
        
        Thread {
            try {
                val games = try {
                    gameDetector.findInstalledGames()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error finding games: ${e.message}", e)
                    emptyList()
                }
                
                runOnUiThread {
                    try {
                        gameAdapter.updateGames(games)
                        progressBar.visibility = android.view.View.GONE
                        if (games.isEmpty()) {
                            statsText.text = "❌ Oyun bulunamadı"
                        } else {
                            statsText.text = "${games.size} oyun bulundu"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error updating UI: ${e.message}")
                        statsText.text = "❌ UI güncelleme hatası"
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Unexpected error in loadGames: ${e.message}", e)
                runOnUiThread {
                    statsText.text = "❌ Beklenmeyen hata"
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }.start()
    }

    private fun launchGame(packageName: String) {
        progressBar.visibility = android.view.View.VISIBLE
        statsText.text = "Sistem optimizasyonu..."

        Thread {
            when (val result = gameLauncher.launchGameWithBoost(packageName)) {
                is Result.Success -> {
                    runOnUiThread {
                        val stats = result.stats
                        statsText.text = """
                            📊 Optimizasyon Tamamlandı
                            💾 Temizlenen RAM: ${stats.cleanedRam / 1024 / 1024}MB
                            ⚡ Beklenen FPS Artış: +${stats.expectedFpsBoost}%
                            🚀 Oyun başlatılıyor...
                        """.trimIndent()
                        progressBar.visibility = android.view.View.GONE
                    }
                }
                is Result.Error -> {
                    runOnUiThread {
                        statsText.text = "❌ Hata: ${result.message}"
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            }
        }.start()
    }
}
