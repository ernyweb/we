package com.gamebooster.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Oyunları başlat ve yönet
 */
class GameLauncher(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val fpsBooster = FpsBooster(context)

    /**
     * Oyunu FPS boost ile başlat
     */
    fun launchGameWithBoost(packageName: String): Result {
        return try {
            // Sistem optimizasyonunu başlat
            val stats = fpsBooster.optimizeForGame(packageName)

            // Intent ile oyunu başlat
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.Error("Oyun başlatılamadı")

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)

            Result.Success(stats)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Bilinmeyen hata")
        }
    }

    /**
     * Oyun çalışırken gerçek zamanlı metrikleri al
     */
    fun getGamePerformance(): GameMetrics {
        return fpsBooster.getGameMetrics()
    }

    /**
     * Optimizasyonu sıfırla
     */
    fun resetOptimization() {
        fpsBooster.resetSystem()
    }
}

sealed class Result {
    data class Success(val stats: GameStats) : Result()
    data class Error(val message: String) : Result()
}
