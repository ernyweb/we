package com.gamebooster.launcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Cihazda yüklü olan oyunları bulur ve yönetir
 */
public class GameDetector {
    private Context context;
    private PackageManager packageManager;
    private final String TAG = "GameDetector";

    public GameDetector(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * Cihazda yüklü tüm oyunları bul
     */
    public List<GameInfo> findInstalledGames() {
        List<GameInfo> games = new ArrayList<>();

        try {
            List<ApplicationInfo> packages;
            try {
                packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get installed apps: " + e.getMessage());
                return new ArrayList<>();
            }

            if (packages == null || packages.isEmpty()) {
                Log.d(TAG, "No packages found");
                return new ArrayList<>();
            }

            for (ApplicationInfo appInfo : packages) {
                try {
                    // Sistem uygulamalarını hariç tut
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }

                    // Google Play'den indirilen uygulamalar oyun olabilir
                    if (isLikelyGame(appInfo)) {
                        try {
                            String label;
                            try {
                                label = packageManager.getApplicationLabel(appInfo).toString();
                            } catch (Exception e) {
                                label = appInfo.packageName;
                            }

                            Drawable icon;
                            try {
                                icon = packageManager.getApplicationIcon(appInfo);
                            } catch (Exception e) {
                                icon = packageManager.getDefaultActivityIcon();
                            }

                            games.add(new GameInfo(
                                    label,
                                    appInfo.packageName,
                                    icon,
                                    false,
                                    System.currentTimeMillis()
                            ));
                        } catch (Exception e) {
                            Log.d(TAG, "Error adding game " + appInfo.packageName + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error processing app: " + e.getMessage());
                }
            }

            Log.d(TAG, "Found " + games.size() + " games");

            // Yükleme zamanına göre sırala (en yenisi önce)
            Collections.sort(games, new Comparator<GameInfo>() {
                @Override
                public int compare(GameInfo a, GameInfo b) {
                    return Long.compare(b.getInstallTime(), a.getInstallTime());
                }
            });

            return games;
        } catch (Exception e) {
            Log.e(TAG, "findInstalledGames error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isLikelyGame(ApplicationInfo appInfo) {
        try {
            String[] gameKeywords = {
                    "game", "candy", "poker", "clash", "royal", "legend", "hero",
                    "fantasy", "adventure", "puzzle", "racing", "shooting", "battle",
                    "simulator", "craft", "tycoon", "pvp", "moba", "rpg"
            };

            String appName = appInfo.packageName.toLowerCase();
            String appLabel;
            try {
                appLabel = appInfo.loadLabel(packageManager).toString().toLowerCase();
            } catch (Exception e) {
                appLabel = "";
            }

            for (String keyword : gameKeywords) {
                if (appName.contains(keyword) || appLabel.contains(keyword)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.d(TAG, "isLikelyGame error: " + e.getMessage());
            return false;
        }
    }
}
