package com.gamebooster.launcher;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "call_recorder";
    private static final int NOTIFICATION_ID = 1001;

    private TelephonyManager telephonyManager;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File currentFile;
    private Object callbackObject;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Çağrı kaydedici hazır"));
        
        setupPhoneStateListener();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Çağrı Kaydedici",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Çağrı kayıt servisi");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Çağrı Kaydedici")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void setupPhoneStateListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No READ_PHONE_STATE permission");
            return;
        }

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManager is null");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                TelephonyCallback.CallStateListener callback = new TelephonyCallback.CallStateListener() {
                    @Override
                    public void onCallStateChanged(int state) {
                        handleCallState(state);
                    }
                };
                callbackObject = callback;
                telephonyManager.registerTelephonyCallback(getMainExecutor(), (TelephonyCallback) callbackObject);
                Log.d(TAG, "TelephonyCallback registered (Android 12+)");
            } else {
                PhoneStateListener listener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String phoneNumber) {
                        handleCallState(state);
                    }
                };
                callbackObject = listener;
                telephonyManager.listen((PhoneStateListener) callbackObject, PhoneStateListener.LISTEN_CALL_STATE);
                Log.d(TAG, "PhoneStateListener registered (Android 11-)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register phone state listener", e);
        }
    }

    private void handleCallState(int state) {
        Log.d(TAG, "Call state changed: " + state);
        
        // Check if recording is enabled
        if (!SettingsActivity.isRecordingEnabled(this)) {
            Log.d(TAG, "Recording is disabled in settings");
            return;
        }
        
        switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.d(TAG, "Call started - starting recording");
                startRecording();
                break;
                
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(TAG, "Call ended - stopping recording");
                stopRecording();
                break;
                
            case TelephonyManager.CALL_STATE_RINGING:
                Log.d(TAG, "Phone ringing");
                break;
        }
    }

    private void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No RECORD_AUDIO permission");
            return;
        }

        try {
            File dir = new File(getExternalFilesDir(null), "recordings");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentFile = new File(dir, "call_" + timestamp + ".mp3");

            // Try different audio sources for better compatibility
            boolean recordingStarted = false;
            
            // Try 1: VOICE_COMMUNICATION (best for calls on most devices)
            if (!recordingStarted) {
                try {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                    configureRecorder();
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    recordingStarted = true;
                    Log.d(TAG, "Recording started with VOICE_COMMUNICATION");
                } catch (Exception e) {
                    Log.w(TAG, "VOICE_COMMUNICATION failed: " + e.getMessage());
                    if (mediaRecorder != null) {
                        try { mediaRecorder.release(); } catch (Exception ignored) {}
                        mediaRecorder = null;
                    }
                }
            }
            
            // Try 2: MIC (general microphone, works on most devices)
            if (!recordingStarted) {
                try {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    configureRecorder();
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    recordingStarted = true;
                    Log.d(TAG, "Recording started with MIC");
                } catch (Exception e) {
                    Log.w(TAG, "MIC failed: " + e.getMessage());
                    if (mediaRecorder != null) {
                        try { mediaRecorder.release(); } catch (Exception ignored) {}
                        mediaRecorder = null;
                    }
                }
            }
            
            // Try 3: VOICE_RECOGNITION (backup option)
            if (!recordingStarted) {
                try {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                    configureRecorder();
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    recordingStarted = true;
                    Log.d(TAG, "Recording started with VOICE_RECOGNITION");
                } catch (Exception e) {
                    Log.e(TAG, "VOICE_RECOGNITION failed: " + e.getMessage());
                    if (mediaRecorder != null) {
                        try { mediaRecorder.release(); } catch (Exception ignored) {}
                        mediaRecorder = null;
                    }
                }
            }
            
            if (recordingStarted) {
                isRecording = true;
                updateNotification("Kayıt ediliyor...");
                Log.d(TAG, "Recording started: " + currentFile.getName());
            } else {
                Log.e(TAG, "All audio sources failed");
                if (currentFile != null && currentFile.exists()) {
                    currentFile.delete();
                }
                currentFile = null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            isRecording = false;
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.release();
                } catch (Exception ignored) {}
                mediaRecorder = null;
            }
            if (currentFile != null && currentFile.exists()) {
                currentFile.delete();
            }
        }
    }
    
    private void configureRecorder() {
        // Output format and encoder
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        
        // Audio settings - optimized for voice
        mediaRecorder.setAudioEncodingBitRate(96000);  // 96 kbps (better for voice)
        mediaRecorder.setAudioSamplingRate(44100);     // 44.1 kHz
        mediaRecorder.setAudioChannels(1);             // Mono (better for calls)
        
        // Output file
        mediaRecorder.setOutputFile(currentFile.getAbsolutePath());
        
        // Max file size (100 MB)
        mediaRecorder.setMaxFileSize(100 * 1024 * 1024);
        
        mediaRecorder.setOnInfoListener((mr, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                Log.w(TAG, "Max file size reached");
                stopRecording();
            }
        });
    }

    private void stopRecording() {
        if (!isRecording) {
            return;
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            isRecording = false;
            Log.d(TAG, "Recording stopped");
            
            // Show recording completed notification
            showRecordingCompletedNotification();
            
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                updateNotification("Çağrı kaydedici hazır");
            }, 3000);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
        }
    }

    private void updateNotification(String text) {
        if (!SettingsActivity.isNotificationsEnabled(this)) {
            // Don't show notification if disabled, but keep service running
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }

    private void showRecordingCompletedNotification() {
        if (!SettingsActivity.isNotificationsEnabled(this)) {
            return;
        }
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Ses Kaydedildi")
                    .setContentText(currentFile != null ? currentFile.getName() : "Çağrı kaydı tamamlandı")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build();
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopRecording();
        
        if (telephonyManager != null && callbackObject != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    telephonyManager.unregisterTelephonyCallback((TelephonyCallback) callbackObject);
                } else {
                    telephonyManager.listen((PhoneStateListener) callbackObject, PhoneStateListener.LISTEN_NONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister listener", e);
            }
        }
        
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
