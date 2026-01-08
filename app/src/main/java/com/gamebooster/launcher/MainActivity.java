package com.gamebooster.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button btnPermissions;
    private Button btnSettings;
    private RecyclerView recordingsList;
    private RecordingAdapter adapter;
    private MediaPlayer mediaPlayer;
    
    // Playback controls
    private LinearLayout playbackControls;
    private TextView playbackFileName;
    private Button btnStopPlayback;
    private SeekBar seekBar;
    private TextView currentTime;
    private TextView totalTime;
    private Handler seekBarHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    statusText.setText("İzinler verildi. Servis başlatılıyor...");
                    startService();
                    loadRecordings();
                } else {
                    statusText.setText("İzinler reddedildi. Uygulama çalışmaz.");
                    Toast.makeText(this, "Tüm izinler gerekli!", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        btnPermissions = findViewById(R.id.btnRequest);
        btnSettings = findViewById(R.id.btnSettings);
        recordingsList = findViewById(R.id.recordingsList);
        
        // Playback controls
        playbackControls = findViewById(R.id.playbackControls);
        playbackFileName = findViewById(R.id.playbackFileName);
        btnStopPlayback = findViewById(R.id.btnStopPlayback);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);

        // Setup RecyclerView
        adapter = new RecordingAdapter(new ArrayList<>(), item -> playRecording(item.path()));
        recordingsList.setLayoutManager(new LinearLayoutManager(this));
        recordingsList.setAdapter(adapter);
        
        // Stop playback button
        btnStopPlayback.setOnClickListener(v -> stopPlayback());
        
        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Settings button
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnPermissions.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                statusText.setText("İzinler zaten verilmiş. Servis çalışıyor.");
                startService();
                loadRecordings();
            } else {
                requestPermissions();
            }
        });

        // Check permissions on start
        if (hasAllPermissions()) {
            statusText.setText("Çağrı kaydedici hazır");
            startService();
            loadRecordings();
        } else {
            statusText.setText("İzin vermek için butona tıklayın");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasAllPermissions()) {
            loadRecordings();
        }
    }

    private boolean hasAllPermissions() {
        String[] permissions = getRequiredPermissions();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.READ_CALL_LOG);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        return permissions.toArray(new String[0]);
    }

    private void requestPermissions() {
        permissionLauncher.launch(getRequiredPermissions());
    }

    private void startService() {
        try {
            Intent intent = new Intent(this, RecordingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            statusText.setText("Servis başlatılamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRecordings() {
        try {
            File dir = new File(getExternalFilesDir(null), "recordings");
            if (!dir.exists()) {
                adapter.update(new ArrayList<>());
                return;
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                adapter.update(new ArrayList<>());
                return;
            }

            List<RecordingItem> items = new ArrayList<>();
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".mp3") || file.getName().endsWith(".m4a"))) {
                    String size = formatSize(file.length());
                    String name = file.getName();
                    items.add(new RecordingItem(name, file.getAbsolutePath(), size));
                }
            }

            adapter.update(items);
            statusText.setText(items.size() + " kayıt bulundu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void playRecording(String path) {
        try {
            // Stop any current playback
            stopPlayback();

            File file = new File(path);
            if (!file.exists()) {
                Toast.makeText(this, "Dosya bulunamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create and prepare media player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            
            mediaPlayer.setOnPreparedListener(mp -> {
                // Show playback controls
                playbackControls.setVisibility(View.VISIBLE);
                playbackFileName.setText("🎧 " + file.getName());
                
                // Set up seekbar
                int duration = mp.getDuration();
                seekBar.setMax(duration);
                totalTime.setText(formatTime(duration));
                
                // Start playback
                mp.start();
                Toast.makeText(MainActivity.this, "Oynatılıyor", Toast.LENGTH_SHORT).show();
                
                // Update seekbar
                updateSeekBar();
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                Toast.makeText(MainActivity.this, "Oynatma tamamlandı", Toast.LENGTH_SHORT).show();
                stopPlayback();
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(MainActivity.this, "Oynatma hatası: " + what, Toast.LENGTH_LONG).show();
                stopPlayback();
                return true;
            });
            
            mediaPlayer.prepareAsync();
            
        } catch (Exception e) {
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            stopPlayback();
        }
    }
    
    private void stopPlayback() {
        // Stop seekbar updates
        seekBarHandler.removeCallbacksAndMessages(null);
        
        // Stop and release media player
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
        
        // Hide playback controls
        if (playbackControls != null) {
            playbackControls.setVisibility(View.GONE);
        }
    }
    
    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                currentTime.setText(formatTime(currentPosition));
                
                // Update every 100ms
                seekBarHandler.postDelayed(this::updateSeekBar, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Optionally stop playback when app goes to background
        // stopPlayback();
    }
}
