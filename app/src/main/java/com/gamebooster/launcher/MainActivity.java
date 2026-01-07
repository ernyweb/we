package com.gamebooster.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GameDetector gameDetector;
    private GameLauncher gameLauncher;
    private GameAdapter gameAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView statsText;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameDetector = new GameDetector(this);
        gameLauncher = new GameLauncher(this);

        recyclerView = findViewById(R.id.gamesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        statsText = findViewById(R.id.statsText);

        setupRecyclerView();

        // Settings button
        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Android 11+ cihazlarda permission iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            List<String> missingPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.QUERY_ALL_PACKAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_TASKS)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.GET_TASKS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.KILL_BACKGROUND_PROCESSES)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.KILL_BACKGROUND_PROCESSES);
            }

            if (!missingPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        missingPermissions.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE);
            } else {
                loadGames();
            }
        } else {
            loadGames();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadGames();
        }
    }

    private void setupRecyclerView() {
        gameAdapter = new GameAdapter(new ArrayList<>(), new GameAdapter.OnGameClickListener() {
            @Override
            public void onGameClick(GameInfo gameInfo) {
                launchGame(gameInfo.getPackageName());
            }
        });
        recyclerView.setAdapter(gameAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void loadGames() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<GameInfo> games;
                    try {
                        games = gameDetector.findInstalledGames();
                    } catch (Exception e) {
                        Log.e(TAG, "Error finding games: " + e.getMessage(), e);
                        games = new ArrayList<>();
                    }

                    final List<GameInfo> finalGames = games;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                gameAdapter.updateGames(finalGames);
                                progressBar.setVisibility(View.GONE);
                                if (finalGames.isEmpty()) {
                                    statsText.setText("❌ Oyun bulunamadı");
                                } else {
                                    statsText.setText(finalGames.size() + " oyun bulundu");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI: " + e.getMessage());
                                statsText.setText("❌ UI güncelleme hatası");
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in loadGames: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statsText.setText("❌ Beklenmeyen hata");
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    private void launchGame(String packageName) {
        progressBar.setVisibility(View.VISIBLE);
        statsText.setText("Sistem optimizasyonu...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                GameLauncher.LaunchResult result = gameLauncher.launchGameWithBoost(packageName);

                if (result instanceof GameLauncher.LaunchResult.Success) {
                    GameLauncher.LaunchResult.Success success = (GameLauncher.LaunchResult.Success) result;
                    final String message = "📊 Optimizasyon Tamamlandı\n" +
                            "💾 Temizlenen RAM: " + (success.getCleanedRam() / 1024 / 1024) + "MB\n" +
                            "⚡ Beklenen FPS Artış: +" + success.getExpectedFpsBoost() + "%\n" +
                            "🚀 Oyun başlatılıyor...";

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statsText.setText(message);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else if (result instanceof GameLauncher.LaunchResult.Error) {
                    GameLauncher.LaunchResult.Error error = (GameLauncher.LaunchResult.Error) result;
                    final String errorMessage = "❌ Hata: " + error.getMessage();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statsText.setText(errorMessage);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }
}
