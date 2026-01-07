package com.gamebooster.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import java.util.List;

/**
 * Sistem performansını optimize eden sınıf
 */
public class FpsBooster {
    private Context context;
    private ActivityManager activityManager;
    private final String TAG = "FpsBooster";

    public FpsBooster(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * Oyun başlamadan önce sistemi optimize et
     */
    public OptimizationResult optimizeForGame(String packageName) {
        try {
            long cleanedRam = cleanMemory();
            killBackgroundApps();
            GameMetrics metrics = getGameMetrics();

            Log.d(TAG, "Optimization completed for " + packageName);
            return new OptimizationResult(cleanedRam, metrics.getExpectedFpsBoost());
        } catch (Exception e) {
            Log.e(TAG, "Optimization error: " + e.getMessage());
            return new OptimizationResult(0, 0);
        }
    }

    /**
     * RAM temizle
     */
    private long cleanMemory() {
        try {
            long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            System.gc();

            long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long cleanedRam = beforeMemory - afterMemory;

            Log.d(TAG, "Cleaned RAM: " + (cleanedRam / 1024 / 1024) + "MB");
            return Math.max(0, cleanedRam);
        } catch (Exception e) {
            Log.e(TAG, "cleanMemory error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Arka plandaki uygulamaları kapat
     */
    private void killBackgroundApps() {
        try {
            if (activityManager == null) {
                Log.w(TAG, "ActivityManager is null");
                return;
            }

            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                    activityManager.getRunningAppProcesses();

            if (runningAppProcesses == null || runningAppProcesses.isEmpty()) {
                Log.d(TAG, "No running app processes found");
                return;
            }

            int killedCount = 0;
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                try {
                    if (processInfo.pid != android.os.Process.myPid()) {
                        android.os.Process.killProcess(processInfo.pid);
                        killedCount++;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error killing process: " + e.getMessage());
                }
            }

            Log.d(TAG, "Killed " + killedCount + " background processes");
        } catch (Exception e) {
            Log.e(TAG, "killBackgroundApps error: " + e.getMessage());
        }
    }

    /**
     * Oyun performans metriklerini al
     */
    private GameMetrics getGameMetrics() {
        try {
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long ramAvailable = freeMemory / (1024 * 1024);

            // Tahmini FPS artışı
            int expectedFpsBoost = (int) (ramAvailable / 100);
            expectedFpsBoost = Math.min(expectedFpsBoost, 60);

            int fps = 60; // Varsayılan
            if (ramAvailable < 500) {
                fps = 30;
            } else if (ramAvailable < 1000) {
                fps = 45;
            }

            return new GameMetrics(fps, ramAvailable, expectedFpsBoost);
        } catch (Exception e) {
            Log.e(TAG, "getGameMetrics error: " + e.getMessage());
            return new GameMetrics(60, 0, 0);
        }
    }

    /**
     * Optimizasyon sonuçları
     */
    public static class OptimizationResult {
        private long cleanedRam;
        private int expectedFpsBoost;

        public OptimizationResult(long cleanedRam, int expectedFpsBoost) {
            this.cleanedRam = cleanedRam;
            this.expectedFpsBoost = expectedFpsBoost;
        }

        public long getCleanedRam() {
            return cleanedRam;
        }

        public int getExpectedFpsBoost() {
            return expectedFpsBoost;
        }
    }

    /**
     * Oyun metrikler
     */
    private static class GameMetrics {
        private int fps;
        private long ramAvailable;
        private int expectedFpsBoost;

        public GameMetrics(int fps, long ramAvailable, int expectedFpsBoost) {
            this.fps = fps;
            this.ramAvailable = ramAvailable;
            this.expectedFpsBoost = expectedFpsBoost;
        }

        public int getFps() {
            return fps;
        }

        public long getRamAvailable() {
            return ramAvailable;
        }

        public int getExpectedFpsBoost() {
            return expectedFpsBoost;
        }
    }
}
