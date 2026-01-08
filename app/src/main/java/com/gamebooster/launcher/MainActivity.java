package com.gamebooster.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;
    
    private Button btnRecord;
    private Button btnStop;
    private Button btnRobot;
    private Button btnWoman;
    private Button btnMan;
    private Button btnChild;
    private Button btnMonster;
    private Button btnPlay;
    private Button btnShare;
    private Button btnRealtime;
    private Button btnSystemWide;
    private TextView tvStatus;
    private RecyclerView rvRecordings;
    
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File currentFile;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isRealtimeMode = false;
    private boolean isSystemWideActive = false;
    
    private VoiceEffect currentEffect = VoiceEffect.NONE;
    private RecordingListAdapter adapter;
    private List<File> recordings = new ArrayList<>();
    private RealtimeAudioProcessor realtimeProcessor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        realtimeProcessor = new RealtimeAudioProcessor();
        
        initViews();
        checkPermissions();
        loadRecordings();
        setupRecyclerView();
    }
    
    private void initViews() {
        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);
        btnRobot = findViewById(R.id.btnRobot);
        btnWoman = findViewById(R.id.btnWoman);
        btnMan = findViewById(R.id.btnMan);
        btnChild = findViewById(R.id.btnChild);
        btnMonster = findViewById(R.id.btnMonster);
        btnPlay = findViewById(R.id.btnPlay);
        btnShare = findViewById(R.id.btnShare);
        btnRealtime = findViewById(R.id.btnRealtime);
        btnSystemWide = findViewById(R.id.btnSystemWide);
        tvStatus = findViewById(R.id.tvStatus);
        rvRecordings = findViewById(R.id.rvRecordings);
        
        btnRecord.setOnClickListener(v -> startRecording());
        btnStop.setOnClickListener(v -> stopRecording());
        btnRobot.setOnClickListener(v -> selectEffect(VoiceEffect.ROBOT));
        btnWoman.setOnClickListener(v -> selectEffect(VoiceEffect.WOMAN));
        btnMan.setOnClickListener(v -> selectEffect(VoiceEffect.MAN));
        btnChild.setOnClickListener(v -> selectEffect(VoiceEffect.CHILD));
        btnMonster.setOnClickListener(v -> selectEffect(VoiceEffect.MONSTER));
        btnPlay.setOnClickListener(v -> playWithEffect());
        btnShare.setOnClickListener(v -> shareRecording());
        btnRealtime.setOnClickListener(v -> toggleRealtime());
        btnSystemWide.setOnClickListener(v -> toggleSystemWide());
        
        updateUI();
    }
    
    private void setupRecyclerView() {
        adapter = new RecordingListAdapter(recordings, this::playRecording, this::deleteRecording);
        rvRecordings.setLayoutManager(new LinearLayoutManager(this));
        rvRecordings.setAdapter(adapter);
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    private void startRecording() {
        if (isRecording) return;
        
        try {
            File dir = new File(getExternalFilesDir(null), "recordings");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentFile = new File(dir, "voice_" + timestamp + ".3gp");
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentFile.getAbsolutePath());
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            tvStatus.setText("🎤 Kaydediliyor...");
            updateUI();
            
            Toast.makeText(this, "Kayıt başladı", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Kayıt hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void stopRecording() {
        if (!isRecording) return;
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            
            isRecording = false;
            tvStatus.setText("✅ Kayıt tamamlandı!");
            
            loadRecordings();
            updateUI();
            
            Toast.makeText(this, "Kayıt kaydedildi: " + currentFile.getName(), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Durdurma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void selectEffect(VoiceEffect effect) {
        currentEffect = effect;
        
        // Update realtime processor effect
        if (realtimeProcessor != null) {
            realtimeProcessor.setEffect(convertToRealtimeEffect(effect));
        }
        
        // Reset all button colors
        resetEffectButtons();
        
        // Highlight selected effect
        switch (effect) {
            case ROBOT:
                btnRobot.setBackgroundColor(0xFF4CAF50);
                tvStatus.setText("🤖 Robot sesi seçildi");
                break;
            case WOMAN:
                btnWoman.setBackgroundColor(0xFFE91E63);
                tvStatus.setText("👩 Kadın sesi seçildi");
                break;
            case MAN:
                btnMan.setBackgroundColor(0xFF2196F3);
                tvStatus.setText("👨 Erkek sesi seçildi");
                break;
            case CHILD:
                btnChild.setBackgroundColor(0xFFFF9800);
                tvStatus.setText("👶 Çocuk sesi seçildi");
                break;
            case MONSTER:
                btnMonster.setBackgroundColor(0xFF9C27B0);
                tvStatus.setText("👹 Canavar sesi seçildi");
                break;
        }
    }
    
    private RealtimeAudioProcessor.VoiceEffect convertToRealtimeEffect(VoiceEffect effect) {
        switch (effect) {
            case ROBOT: return RealtimeAudioProcessor.VoiceEffect.ROBOT;
            case WOMAN: return RealtimeAudioProcessor.VoiceEffect.WOMAN;
            case MAN: return RealtimeAudioProcessor.VoiceEffect.MAN;
            case CHILD: return RealtimeAudioProcessor.VoiceEffect.CHILD;
            case MONSTER: return RealtimeAudioProcessor.VoiceEffect.MONSTER;
            default: return RealtimeAudioProcessor.VoiceEffect.NONE;
        }
    }
    
    private void toggleRealtime() {
        if (isRealtimeMode) {
            // Stop realtime mode
            realtimeProcessor.stopRealtime();
            isRealtimeMode = false;
            btnRealtime.setText("🎧 Real-Time: OFF");
            btnRealtime.setBackgroundColor(0xFF666666);
            tvStatus.setText("Real-time modu kapatıldı");
            updateUI();
        } else {
            // Start realtime mode
            realtimeProcessor.setEffect(convertToRealtimeEffect(currentEffect));
            realtimeProcessor.startRealtime();
            isRealtimeMode = true;
            btnRealtime.setText("🎧 Real-Time: ON");
            btnRealtime.setBackgroundColor(0xFF4CAF50);
            tvStatus.setText("🎧 Real-time modu aktif - Konuş ve sesini duy!");
            updateUI();
        }
    }
    
    private void toggleSystemWide() {
        if (isSystemWideActive) {
            // Stop system-wide mode
            stopSystemWideService();
        } else {
            // Check overlay permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // Show explanation dialog
                    new AlertDialog.Builder(this)
                        .setTitle("Overlay İzni Gerekli")
                        .setMessage("Sistem genelinde ses değiştirme için overlay izni gerekiyor. " +
                                "Ayarlara gidip izin verecek misiniz?")
                        .setPositiveButton("Evet", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Hayır", null)
                        .show();
                    return;
                }
            }
            
            // Start system-wide mode
            startSystemWideService();
        }
    }
    
    private void startSystemWideService() {
        try {
            // Set the current effect to the system service
            SystemVoiceService.setEffect(convertToSystemEffect(currentEffect));
            
            // Start the foreground service
            Intent serviceIntent = new Intent(this, SystemVoiceService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            // Start the overlay service
            Intent overlayIntent = new Intent(this, OverlayService.class);
            startService(overlayIntent);
            
            isSystemWideActive = true;
            btnSystemWide.setText("🌐 SİSTEM GENELİ: ON");
            btnSystemWide.setBackgroundColor(0xFFFF5722);
            tvStatus.setText("🌐 Sistem geneli ses değiştirme aktif!\n" +
                    "WhatsApp, oyunlar, aramalar - HER YERDE!");
            
            Toast.makeText(this, "Sistem geneli mod aktif! Overlay butona tıkla.", Toast.LENGTH_LONG).show();
            updateUI();
            
        } catch (Exception e) {
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void stopSystemWideService() {
        try {
            // Stop the services
            stopService(new Intent(this, SystemVoiceService.class));
            stopService(new Intent(this, OverlayService.class));
            
            isSystemWideActive = false;
            btnSystemWide.setText("🌐 SİSTEM GENELİ: OFF");
            btnSystemWide.setBackgroundColor(0xFF666666);
            tvStatus.setText("Sistem geneli mod kapatıldı");
            
            updateUI();
            
        } catch (Exception e) {
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private SystemVoiceService.VoiceEffect convertToSystemEffect(VoiceEffect effect) {
        switch (effect) {
            case ROBOT: return SystemVoiceService.VoiceEffect.ROBOT;
            case WOMAN: return SystemVoiceService.VoiceEffect.WOMAN;
            case MAN: return SystemVoiceService.VoiceEffect.MAN;
            case CHILD: return SystemVoiceService.VoiceEffect.CHILD;
            case MONSTER: return SystemVoiceService.VoiceEffect.MONSTER;
            default: return SystemVoiceService.VoiceEffect.NONE;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "İzin verildi! Tekrar dene.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "İzin verilmedi :(", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void resetEffectButtons() {
        btnRobot.setBackgroundColor(0xFFBBBBBB);
        btnWoman.setBackgroundColor(0xFFBBBBBB);
        btnMan.setBackgroundColor(0xFFBBBBBB);
        btnChild.setBackgroundColor(0xFFBBBBBB);
        btnMonster.setBackgroundColor(0xFFBBBBBB);
    }
    
    private void playWithEffect() {
        if (currentFile == null || !currentFile.exists()) {
            Toast.makeText(this, "Önce kayıt yapın!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        playRecording(currentFile);
    }
    
    private void playRecording(File file) {
        if (isPlaying) {
            stopPlayback();
            return;
        }
        
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            
            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlayback();
                tvStatus.setText("✅ Oynatma tamamlandı");
            });
            
            mediaPlayer.setOnPreparedListener(mp -> {
                // Apply voice effect after preparation
                applyVoiceEffect(mp, currentEffect);
                mp.start();
                isPlaying = true;
                btnPlay.setText("⏸️ Durdur");
                tvStatus.setText("▶️ Oynatılıyor: " + file.getName());
            });
            
            mediaPlayer.prepareAsync();
            
        } catch (Exception e) {
            Toast.makeText(this, "Oynatma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void applyVoiceEffect(MediaPlayer player, VoiceEffect effect) {
        try {
            android.media.audiofx.PresetReverb reverb = new android.media.audiofx.PresetReverb(0, player.getAudioSessionId());
            reverb.setEnabled(true);
            
            switch (effect) {
                case ROBOT:
                    // Robot: Metallic reverb
                    reverb.setPreset(android.media.audiofx.PresetReverb.PRESET_MEDIUMROOM);
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(0.8f).setSpeed(1.0f));
                    break;
                    
                case WOMAN:
                    // Woman: Higher pitch
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(1.3f).setSpeed(1.0f));
                    break;
                    
                case MAN:
                    // Man: Lower pitch
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(0.7f).setSpeed(1.0f));
                    break;
                    
                case CHILD:
                    // Child: Much higher pitch, faster
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(1.5f).setSpeed(1.1f));
                    break;
                    
                case MONSTER:
                    // Monster: Very low pitch, reverb
                    reverb.setPreset(android.media.audiofx.PresetReverb.PRESET_LARGEHALL);
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(0.5f).setSpeed(0.9f));
                    break;
                    
                case NONE:
                default:
                    // Normal playback
                    player.setPlaybackParams(player.getPlaybackParams().setPitch(1.0f).setSpeed(1.0f));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopPlayback() {
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
        isPlaying = false;
        btnPlay.setText("▶️ Oynat");
    }
    
    private void shareRecording() {
        if (currentFile == null || !currentFile.exists()) {
            Toast.makeText(this, "Paylaşılacak kayıt yok!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Uri fileUri = FileProvider.getUriForFile(this, 
                "com.gamebooster.launcher.fileprovider", currentFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Ses kaydını paylaş"));
            
        } catch (Exception e) {
            Toast.makeText(this, "Paylaşma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void loadRecordings() {
        recordings.clear();
        File dir = new File(getExternalFilesDir(null), "recordings");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                recordings.addAll(Arrays.asList(files));
                recordings.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    private void deleteRecording(File file) {
        if (file.delete()) {
            loadRecordings();
            Toast.makeText(this, "Kayıt silindi", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUI() {
        btnRecord.setEnabled(!isRecording && !isRealtimeMode && !isSystemWideActive);
        btnStop.setEnabled(isRecording);
        btnPlay.setEnabled(!isRecording && !isRealtimeMode && !isSystemWideActive && currentFile != null && currentFile.exists());
        btnShare.setEnabled(!isRecording && !isRealtimeMode && !isSystemWideActive && currentFile != null && currentFile.exists());
        btnRealtime.setEnabled(!isRecording && !isSystemWideActive);
        btnSystemWide.setEnabled(!isRecording && !isRealtimeMode);
        
        // Disable effect buttons during recording or system-wide mode
        boolean effectsEnabled = !isRecording && !isSystemWideActive;
        btnRobot.setEnabled(effectsEnabled);
        btnWoman.setEnabled(effectsEnabled);
        btnMan.setEnabled(effectsEnabled);
        btnChild.setEnabled(effectsEnabled);
        btnMonster.setEnabled(effectsEnabled);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopPlayback();
        
        // Stop realtime processing
        if (realtimeProcessor != null) {
            realtimeProcessor.stopRealtime();
        }
        
        // Stop system-wide service if active
        if (isSystemWideActive) {
            stopSystemWideService();
        }
    }
    
    enum VoiceEffect {
        NONE, ROBOT, WOMAN, MAN, CHILD, MONSTER
    }
}
