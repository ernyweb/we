package com.gamebooster.launcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private Switch enableOptimization;
    private Switch enableHighPerformance;
    private Switch enableNetworkBoost;
    private SeekBar graphicsQuality;
    private SeekBar refreshRate;
    private SeekBar pingBoost;
    private TextView graphicsLabel;
    private TextView refreshLabel;
    private TextView pingLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("GameBoosterPrefs", MODE_PRIVATE);

        // UI Elements
        enableOptimization = findViewById(R.id.enableOptimization);
        enableHighPerformance = findViewById(R.id.enableHighPerformance);
        enableNetworkBoost = findViewById(R.id.enableNetworkBoost);
        graphicsQuality = findViewById(R.id.graphicsQuality);
        refreshRate = findViewById(R.id.refreshRate);
        pingBoost = findViewById(R.id.pingBoost);
        graphicsLabel = findViewById(R.id.graphicsLabel);
        refreshLabel = findViewById(R.id.refreshLabel);
        pingLabel = findViewById(R.id.pingLabel);

        // Load saved settings
        loadSettings();

        // Optimization Toggle
        enableOptimization.setChecked(prefs.getBoolean("optimization_enabled", true));
        enableOptimization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("optimization_enabled", isChecked).apply();
        });

        // High Performance Toggle
        enableHighPerformance.setChecked(prefs.getBoolean("high_performance", false));
        enableHighPerformance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("high_performance", isChecked).apply();
        });

        // Network Boost Toggle
        enableNetworkBoost.setChecked(prefs.getBoolean("network_boost", false));
        enableNetworkBoost.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("network_boost", isChecked).apply();
        });

        // Graphics Quality (30-100%)
        graphicsQuality.setMax(70);
        graphicsQuality.setProgress(prefs.getInt("graphics_quality", 50));
        graphicsQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int quality = 30 + progress;
                graphicsLabel.setText("Grafik Kalitesi: %" + quality);
                if (fromUser) {
                    prefs.edit().putInt("graphics_quality", progress).apply();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Refresh Rate (30-144 Hz)
        refreshRate.setMax(114);
        refreshRate.setProgress(prefs.getInt("refresh_rate", 30));
        refreshRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int rate = 30 + progress;
                refreshLabel.setText("Refresh Rate: " + rate + "Hz");
                if (fromUser) {
                    prefs.edit().putInt("refresh_rate", progress).apply();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Ping Booster (0-100ms reduction)
        pingBoost.setMax(100);
        pingBoost.setProgress(prefs.getInt("ping_boost", 0));
        pingBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pingLabel.setText("Ping Booster: -" + progress + "ms");
                if (fromUser) {
                    prefs.edit().putInt("ping_boost", progress).apply();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void loadSettings() {
        int graphicsProgress = prefs.getInt("graphics_quality", 50);
        graphicsLabel.setText("Grafik Kalitesi: %" + (30 + graphicsProgress));

        int refreshProgress = prefs.getInt("refresh_rate", 30);
        refreshLabel.setText("Refresh Rate: " + (30 + refreshProgress) + "Hz");

        int pingProgress = prefs.getInt("ping_boost", 0);
        pingLabel.setText("Ping Booster: -" + pingProgress + "ms");
    }
}
