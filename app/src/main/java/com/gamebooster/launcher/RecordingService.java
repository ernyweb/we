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

            // Try ALL possible audio sources aggressively
            int[] audioSources = {
                MediaRecorder.AudioSource.VOICE_CALL,           // Deprecated but works on some devices
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,  // Best for calls
                MediaRecorder.AudioSource.VOICE_DOWNLINK,       // Remote party only
                MediaRecorder.AudioSource.VOICE_UPLINK,         // Your voice only
                MediaRecorder.AudioSource.MIC,                  // General mic
                MediaRecorder.AudioSource.VOICE_RECOGNITION,    // Voice recognition
                MediaRecorder.AudioSource.CAMCORDER,            // Camcorder mic
                MediaRecorder.AudioSource.DEFAULT               // Default source
            };
            
            String[] sourceNames = {
                "VOICE_CALL",
                "VOICE_COMMUNICATION", 
                "VOICE_DOWNLINK",
                "VOICE_UPLINK",
                "MIC",
                "VOICE_RECOGNITION",
                "CAMCORDER",
                "DEFAULT"
            };

            boolean recordingStarted = false;
            
            for (int i = 0; i < audioSources.length && !recordingStarted; i++) {
                try {
                    Log.d(TAG, "Trying audio source: " + sourceNames[i]);
                    
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(audioSources[i]);
                    
                    // Use 3GP format - more compatible for calls
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    
                    // Output file
                    mediaRecorder.setOutputFile(currentFile.getAbsolutePath());
                    
                    // Prepare and start
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    
                    // Wait a bit and check if it's actually recording
                    Thread.sleep(100);
                    
                    try {
                        int amplitude = mediaRecorder.getMaxAmplitude();
                        Log.d(TAG, sourceNames[i] + " amplitude: " + amplitude);
                    } catch (Exception e) {
                        Log.w(TAG, sourceNames[i] + " getMaxAmplitude failed: " + e.getMessage());
                    }
                    
                    recordingStarted = true;
                    isRecording = true;
                    updateNotification("Kaydediliyor (" + sourceNames[i] + ")");
                    Log.i(TAG, "✅ Recording STARTED with: " + sourceNames[i]);
                    
                } catch (Exception e) {
                    Log.w(TAG, "❌ " + sourceNames[i] + " failed: " + e.getMessage());
                    if (mediaRecorder != null) {
                        try { 
                            mediaRecorder.reset();
                            mediaRecorder.release(); 
                        } catch (Exception ignored) {}
                        mediaRecorder = null;
                    }
                }
            }
            
            if (!recordingStarted) {
                Log.e(TAG, "🚨 ALL AUDIO SOURCES FAILED!");
                if (currentFile != null && currentFile.exists()) {
                    currentFile.delete();
                }
                currentFile = null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "startRecording crashed: " + e.getMessage(), e);
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

    private void stopRecording() {
        if (!isRecording) {
            return;
        }

        try {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.w(TAG, "Stop failed: " + e.getMessage());
                }
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            isRecording = false;
            
            // Check if file has actual content
            boolean fileHasContent = false;
            if (currentFile != null && currentFile.exists()) {
                long fileSize = currentFile.length();
                Log.d(TAG, "Recording file size: " + fileSize + " bytes");
                
                // If file is smaller than 1KB, it's probably empty
                if (fileSize < 1024) {
                    Log.w(TAG, "⚠️ File too small (" + fileSize + " bytes), deleting...");
                    currentFile.delete();
                } else {
                    fileHasContent = true;
                    Log.i(TAG, "✅ Recording saved: " + currentFile.getName() + " (" + fileSize + " bytes)");
                }
            }
            
            if (fileHasContent) {
                // Show recording completed notification
                showRecordingCompletedNotification();
            }
            
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
