package com.gamebooster.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var gameDetector: GameDetector
    private lateinit var gameLauncher: GameLauncher
    private lateinit var gameAdapter: GameAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameDetector = GameDetector(this)
        gameLauncher = GameLauncher(this)

        recyclerView = findViewById(R.id.gamesRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        statsText = findViewById(R.id.statsText)

        setupRecyclerView()
        loadGames()
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
            val games = gameDetector.findInstalledGames()
            runOnUiThread {
                gameAdapter.updateGames(games)
                progressBar.visibility = android.view.View.GONE
                statsText.text = "${games.size} oyun bulundu"
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
