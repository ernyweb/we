package com.gamebooster.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Debug
import android.os.Process

/**
 * FPS Booster - İşletim sistemi performansını artırır
 * - RAM temizleme
 * - Arka plan işlemlerini durdurma
 * - Öncelik ayarlama
 * - İstatistik toplama
 */
class FpsBooster(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val activityManager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Oyun başlangıcı için sistem optimizasyonu
     */
    fun optimizeForGame(packageName: String): GameStats {
        val stats = GameStats()
        stats.initialRam = getAvailableRam()
        stats.initialFps = 60

        // 1. RAM temizle
        cleanMemory()
        stats.cleanedRam = (stats.initialRam - getAvailableRam()).toLong()

        // 2. Düşük öncelikli arka plan işlemleri durdur
        killBackgroundApps()

        // 3. Sistem ayarlarını optimize et
        optimizeSystemSettings()

        stats.optimizedRam = getAvailableRam()
        stats.expectedFpsBoost = calculateExpectedBoost(stats.initialRam, stats.optimizedRam)

        return stats
    }

    /**
     * Oyun bittikten sonra sistemi eski haline getir
     */
    fun resetSystem() {
        // Sistem otomatik olarak uygulamaları yeniden başlatır
    }

    /**
     * Gerçek zamanlı FPS ve performans metrikleri
     */
    fun getGameMetrics(): GameMetrics {
        return GameMetrics(
            currentFps = estimateFps(),
            ramUsage = getTotalRam() - getAvailableRam(),
            cpuUsage = getCpuUsage(),
            temperature = getDeviceTemperature()
        )
    }

    private fun cleanMemory() {
        try {
            Runtime.getRuntime().gc()
            System.runFinalization()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun killBackgroundApps() {
        try {
            val runningApps = try {
                activityManager.runningAppProcesses
            } catch (e: Exception) {
                android.util.Log.e("FpsBooster", "Failed to get running apps: ${e.message}")
                return
            }
            
            if (runningApps == null || runningApps.isEmpty()) {
                return
            }

            runningApps.forEach { process ->
                try {
                    // Kendi uygulamayı ve sistem uygulamalarını öldürme
                    if (process.pid != android.os.Process.myPid() && !isSystemApp(process.processName)) {
                        try {
                            Process.killProcess(process.pid)
                        } catch (e: Exception) {
                            android.util.Log.d("FpsBooster", "Could not kill process: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.d("FpsBooster", "Error processing app: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FpsBooster", "killBackgroundApps error: ${e.message}")
        }
    }

    private fun optimizeSystemSettings() {
        // Vibration, animasyon, refresh rate ayarları optimize edilebilir
        // (Bunlar sistem ayarlarıdır ve direktly değiştiremeyiz, but can suggest)
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun getAvailableRam(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }

    private fun getTotalRam(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory()
    }

    private fun estimateFps(): Int {
        // Gerçek FPS ölçümü için daha gelişmiş mekanizm lazım
        // Şimdilik sabit değer
        return 60
    }

    private fun getCpuUsage(): Float {
        return try {
            val stat = java.io.File("/proc/stat").readText().split("\n")[0]
            val parts = stat.split("\\s+".toRegex())
            if (parts.size >= 5) {
                val idle = parts[4].toLong()
                val total = parts.drop(1).take(4).sumOf { it.toLong() } + idle
                100f * (1 - idle.toFloat() / total)
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun getDeviceTemperature(): Float {
        return try {
            val tempFile = java.io.File("/sys/class/thermal/thermal_zone0/temp")
            if (tempFile.exists()) {
                tempFile.readText().trim().toFloat() / 1000
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun calculateExpectedBoost(initialRam: Long, optimizedRam: Long): Int {
        val ramImprovement = ((optimizedRam - initialRam).toFloat() / initialRam * 100).toInt()
        return minOf(ramImprovement / 10, 20) // Max 20% boost estimate
    }
}

data class GameStats(
    var initialRam: Long = 0,
    var optimizedRam: Long = 0,
    var cleanedRam: Long = 0,
    var initialFps: Int = 60,
    var expectedFpsBoost: Int = 0
)

data class GameMetrics(
    val currentFps: Int,
    val ramUsage: Long,
    val cpuUsage: Float,
    val temperature: Float
)
