package com.gamebooster.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecordingAdapter.OnItemClickListener {

    private TextView statusText;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private Button btnRequest;
    private RecordingAdapter adapter;
    private MediaPlayer mediaPlayer;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (allPermissionsGranted()) {
                    startRecorderService();
                    loadRecordings();
                } else {
                    statusText.setText(getString(R.string.perm_rationale));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            statusText = findViewById(R.id.statusText);
            emptyView = findViewById(R.id.emptyView);
            recyclerView = findViewById(R.id.recordingsList);
            btnRequest = findViewById(R.id.btnRequest);

            if (recyclerView == null || statusText == null || btnRequest == null) {
                throw new RuntimeException("Layout views not found");
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new RecordingAdapter(new ArrayList<>(), this);
            recyclerView.setAdapter(adapter);

            btnRequest.setOnClickListener(v -> {
                if (allPermissionsGranted()) {
                    startRecorderService();
                    loadRecordings();
                } else {
                    requestPerms();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "onCreate crashed: " + e.getMessage(), e);
            if (statusText != null) {
                statusText.setText("Hata: " + e.getMessage());
            }
            // Don't start service if initialization failed
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Check if views are initialized
            if (statusText == null || recyclerView == null || btnRequest == null) {
                android.util.Log.e("MainActivity", "Views not initialized in onResume");
                return;
            }
            
            if (allPermissionsGranted()) {
                startRecorderService();
                loadRecordings();
            } else {
                statusText.setText(getString(R.string.perm_rationale));
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "onResume crashed: " + e.getMessage(), e);
            if (statusText != null) statusText.setText("Hata: " + e.getMessage());
        }
    }

    private void requestPerms() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.READ_PHONE_STATE);
        perms.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissionLauncher.launch(perms.toArray(new String[0]));
    }

    private boolean allPermissionsGranted() {
        boolean audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean phone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean callLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean notif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return audio && phone && callLog && notif;
    }

    private void startRecorderService() {
        try {
            if (statusText != null) {
                statusText.setText(R.string.notification_idle);
            }
            Intent intent = new Intent(this, RecordingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to start foreground service: " + e.getMessage(), e);
                    // Try regular startService as fallback
                    startService(intent);
                }
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "startRecorderService crashed: " + e.getMessage(), e);
            if (statusText != null) {
                statusText.setText("Servis hatası: " + e.getMessage());
            }
        }
    }

    private void loadRecordings() {
        try {
            File dir = new File(getExternalFilesDir(null), "recordings");
            if (!dir.exists() || !dir.isDirectory()) {
                emptyView.setVisibility(View.VISIBLE);
                adapter.update(new ArrayList<>());
                return;
            }
            File[] files = dir.listFiles();
            List<RecordingItem> items = new ArrayList<>();
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile()) continue;
                    long durationMs = readDuration(f);
                    String title = f.getName();
                    String meta = formatDuration(durationMs) + " • " + formatSize(f.length());
                    items.add(new RecordingItem(title, f.getAbsolutePath(), meta));
                }
            }
            emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.update(items);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "loadRecordings crashed: " + e.getMessage(), e);
            statusText.setText("Kayıt yükleme hatası: " + e.getMessage());
        }
    }

    private long readDuration(File f) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(f.getAbsolutePath());
            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) return Long.parseLong(dur);
        } catch (Exception ignored) {
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    private String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int idx = 0;
        while (size > 1024 && idx < units.length - 1) {
            size /= 1024.0;
            idx++;
        }
        return new DecimalFormat("#.##").format(size) + " " + units[idx];
    }

    @Override
    public void onItemClick(@NonNull RecordingItem item) {
        play(item.path());
    }

    private void play(String path) {
        stopPlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();
            statusText.setText("Oynatılıyor: " + new File(path).getName());
        } catch (Exception e) {
            statusText.setText("Oynatma hatası");
            stopPlayer();
        }
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }
}
