package com.gamebooster.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

/**
 * Cihazda yüklü olan oyunları bulur ve yönetir
 */
class GameDetector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val TAG = "GameDetector"

    /**
     * Cihazda yüklü tüm oyunları bul
     */
    fun findInstalledGames(): List<GameInfo> {
        val games = mutableListOf<GameInfo>()
        
        return try {
            val packages = try {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get installed apps: ${e.message}")
                return emptyList()
            }
            
            if (packages == null || packages.isEmpty()) {
                Log.d(TAG, "No packages found")
                return emptyList()
            }
            
            packages.forEach { appInfo ->
                try {
                    // Sistem uygulamalarını hariç tut
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return@forEach
                    }
                    
                    // Google Play'den indirilen uygulamalar oyun olabilir
                    if (isLikelyGame(appInfo)) {
                        try {
                            val label = try {
                                packageManager.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                appInfo.packageName
                            }
                            
                            val icon = try {
                                packageManager.getApplicationIcon(appInfo)
                            } catch (e: Exception) {
                                packageManager.defaultActivityIcon
                            }
                            
                            games.add(GameInfo(
                                name = label,
                                packageName = appInfo.packageName,
                                icon = icon,
                                isSystemApp = false,
                                installTime = System.currentTimeMillis()
                            ))
                        } catch (e: Exception) {
                            Log.d(TAG, "Error adding game ${appInfo.packageName}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error processing app: ${e.message}")
                }
            }
            
            Log.d(TAG, "Found ${games.size} games")
            games.sortedByDescending { it.installTime }
        } catch (e: Exception) {
            Log.e(TAG, "findInstalledGames error: ${e.message}")
            emptyList()
        }
    }

    private fun isLikelyGame(appInfo: ApplicationInfo): Boolean {
        return try {
            // Çok bilinen oyun kategorileri
            val gameKeywords = listOf(
                "game", "candy", "poker", "clash", "royal", "legend", "hero", 
                "fantasy", "adventure", "puzzle", "racing", "shooting", "battle",
                "simulator", "craft", "tycoon", "pvp", "moba", "rpg"
            )

            val appName = appInfo.packageName.lowercase()
            val appLabel = try {
                appInfo.loadLabel(packageManager).toString().lowercase()
            } catch (e: Exception) {
                ""
            }
            
            gameKeywords.any { keyword ->
                appName.contains(keyword) || appLabel.contains(keyword)
            }
        } catch (e: Exception) {
            Log.d(TAG, "isLikelyGame error: ${e.message}")
            false
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
