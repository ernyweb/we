package com.gamebooster.launcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "CallRecorderPrefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_RECORDING_ENABLED = "recording_enabled";

    private Switch switchNotifications;
    private Switch switchRecording;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        switchNotifications = findViewById(R.id.switchNotifications);
        switchRecording = findViewById(R.id.switchRecording);

        // Load saved settings
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        boolean recordingEnabled = prefs.getBoolean(KEY_RECORDING_ENABLED, true);

        switchNotifications.setChecked(notificationsEnabled);
        switchRecording.setChecked(recordingEnabled);

        // Notifications switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            Toast.makeText(this, 
                isChecked ? "Bildirimler açıldı" : "Bildirimler kapatıldı", 
                Toast.LENGTH_SHORT).show();
        });

        // Recording switch
        switchRecording.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_RECORDING_ENABLED, isChecked).apply();
            Toast.makeText(this, 
                isChecked ? "Kayıt etkin" : "Kayıt devre dışı", 
                Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    public static boolean isNotificationsEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public static boolean isRecordingEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_RECORDING_ENABLED, true);
    }
}
