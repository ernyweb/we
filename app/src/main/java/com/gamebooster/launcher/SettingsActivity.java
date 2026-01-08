package com.gamebooster.launcher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "CallRecorderPrefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_RECORDING_ENABLED = "recording_enabled";

    private Switch switchNotifications;
    private Switch switchRecording;
    private SharedPreferences prefs;
    private Spinner spinnerFormat;
    private Spinner spinnerBitrate;
    private Spinner spinnerSampleRate;
    private Spinner spinnerChannels;

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

        // Audio format spinner
        spinnerFormat = findViewById(R.id.spinnerFormat);
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, new String[]{"3GP", "MP4"}
        );
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(formatAdapter);
        spinnerFormat.setSelection(prefs.getString("audio_format", "3GP").equals("MP4") ? 1 : 0);
        spinnerFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String format = position == 0 ? "3GP" : "MP4";
                prefs.edit().putString("audio_format", format).apply();
                Toast.makeText(SettingsActivity.this, "Format: " + format, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Bitrate spinner
        spinnerBitrate = findViewById(R.id.spinnerBitrate);
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, new String[]{"48 kbps", "96 kbps", "128 kbps"}
        );
        bitrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBitrate.setAdapter(bitrateAdapter);
        int savedBitrate = prefs.getInt("bitrate", 96000);
        int bitratePos = savedBitrate == 48000 ? 0 : savedBitrate == 128000 ? 2 : 1;
        spinnerBitrate.setSelection(bitratePos);
        spinnerBitrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int bitrate = position == 0 ? 48000 : position == 2 ? 128000 : 96000;
                prefs.edit().putInt("bitrate", bitrate).apply();
                Toast.makeText(SettingsActivity.this, "Bitrate: " + (bitrate/1000) + " kbps", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sample Rate spinner
        spinnerSampleRate = findViewById(R.id.spinnerSampleRate);
        ArrayAdapter<String> sampleRateAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, new String[]{"8 kHz", "16 kHz", "44.1 kHz"}
        );
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSampleRate.setAdapter(sampleRateAdapter);
        int savedSampleRate = prefs.getInt("sample_rate", 44100);
        int sampleRatePos = savedSampleRate == 8000 ? 0 : savedSampleRate == 16000 ? 1 : 2;
        spinnerSampleRate.setSelection(sampleRatePos);
        spinnerSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int sampleRate = position == 0 ? 8000 : position == 1 ? 16000 : 44100;
                prefs.edit().putInt("sample_rate", sampleRate).apply();
                String displayRate = position == 2 ? "44.1 kHz" : (sampleRate/1000) + " kHz";
                Toast.makeText(SettingsActivity.this, "Sample Rate: " + displayRate, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Channels spinner
        spinnerChannels = findViewById(R.id.spinnerChannels);
        ArrayAdapter<String> channelsAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, new String[]{"Mono", "Stereo"}
        );
        channelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChannels.setAdapter(channelsAdapter);
        spinnerChannels.setSelection(prefs.getInt("channels", 1) == 2 ? 1 : 0);
        spinnerChannels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int channels = position == 0 ? 1 : 2;
                prefs.edit().putInt("channels", channels).apply();
                Toast.makeText(SettingsActivity.this, channels == 1 ? "Mono" : "Stereo", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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

    public static String getAudioFormat(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("audio_format", "3GP");
    }

    public static int getBitrate(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("bitrate", 96000);
    }

    public static int getSampleRate(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("sample_rate", 44100);
    }

    public static int getChannels(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("channels", 1);
    }
}
