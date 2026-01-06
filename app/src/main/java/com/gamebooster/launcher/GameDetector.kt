package com.gamebooster.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Cihazda yüklü olan oyunları bulur ve yönetir
 */
class GameDetector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Cihazda yüklü tüm oyunları bul
     */
    fun findInstalledGames(): List<GameInfo> {
        val games = mutableListOf<GameInfo>()
        
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        packages.forEach { appInfo ->
            // Google Play'den indirilen uygulamalar oyun olabilir
            // Basit heuristic: kategori kontrol et veya oyun marketi bilgisini kontrol et
            if (isLikelyGame(appInfo)) {
                try {
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    
                    games.add(GameInfo(
                        name = label,
                        packageName = appInfo.packageName,
                        icon = icon,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        installTime = appInfo.firstInstallTime
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        return games.sortedByDescending { it.installTime }
    }

    private fun isLikelyGame(appInfo: ApplicationInfo): Boolean {
        // Sistem uygulamalarını hariç tut
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            return false
        }

        // Çok bilinen oyun kategorileri
        val gameKeywords = listOf(
            "game", "candy", "poker", "clash", "royal", "legend", "hero", 
            "fantasy", "adventure", "puzzle", "racing", "shooting", "battle",
            "simulator", "craft", "tycoon", "pvp", "moba", "rpg"
        )

        val appName = appInfo.packageName.lowercase()
        
        return gameKeywords.any { keyword ->
            appName.contains(keyword) || 
            appInfo.loadLabel(packageManager).toString().lowercase().contains(keyword)
        }
    }
}

data class GameInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    val installTime: Long = 0
)
