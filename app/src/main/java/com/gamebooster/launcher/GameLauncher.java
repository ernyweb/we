package com.gamebooster.launcher;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Oyunları başlatan ve optimize eden sınıf
 */
public class GameLauncher {
    private Context context;
    private FpsBooster fpsBooster;
    private final String TAG = "GameLauncher";

    public GameLauncher(Context context) {
        this.context = context;
        this.fpsBooster = new FpsBooster(context);
    }

    /**
     * Oyunu FPS optimizasyonu ile başlat
     */
    public LaunchResult launchGameWithBoost(String packageName) {
        try {
            // Sistem optimizasyonu yap
            FpsBooster.OptimizationResult optimizationResult = fpsBooster.optimizeForGame(packageName);

            // Oyunu başlat
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Game launched: " + packageName);
                return new LaunchResult.Success(optimizationResult.getCleanedRam(),
                        optimizationResult.getExpectedFpsBoost());
            } else {
                Log.e(TAG, "Cannot find launch intent for: " + packageName);
                return new LaunchResult.Error("Oyun başlatılamadı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch error: " + e.getMessage());
            return new LaunchResult.Error("Hata: " + e.getMessage());
        }
    }

    /**
     * Oyun başlatma sonucu
     */
    public static abstract class LaunchResult {
        public static class Success extends LaunchResult {
            private long cleanedRam;
            private int expectedFpsBoost;

            public Success(long cleanedRam, int expectedFpsBoost) {
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

        public static class Error extends LaunchResult {
            private String message;

            public Error(String message) {
                this.message = message;
            }

            public String getMessage() {
                return message;
            }
        }
    }
}
