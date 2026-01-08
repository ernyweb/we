package com.gamebooster.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    private static final String CHANNEL_ID = "call_recorder_channel";
    private static final int NOTIF_ID = 44;

    private TelephonyManager telephonyManager;
    private MediaRecorder recorder;
    private boolean recording = false;
    private File currentFile;
    private long startMs = 0L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Object callStateCallback; // TelephonyCallback for Android 12+

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (recording) {
                updateNotification(true);
                handler.postDelayed(this, 5_000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, buildNotification(getString(R.string.notification_idle)));
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        registerListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep running until explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        unregisterListener();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerListener() {
        if (telephonyManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ use TelephonyCallback
                registerCallbackApi31();
            } else {
                // Android 11 and below use PhoneStateListener
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } catch (SecurityException e) {
            android.util.Log.e("RecordingService", "Failed to register listener: " + e.getMessage());
        }
    }

    private void unregisterListener() {
        if (telephonyManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ unregister TelephonyCallback
                unregisterCallbackApi31();
            } else {
                // Android 11 and below unregister PhoneStateListener
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        } catch (Exception ignored) {
        }
    }

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            handleCallStateChange(state);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void registerCallbackApi31() {
        TelephonyCallback.CallStateListener callback = new TelephonyCallback.CallStateListener() {
            @Override
            public void onCallStateChanged(int state) {
                handleCallStateChange(state);
            }
        };
        callStateCallback = callback;
        telephonyManager.registerTelephonyCallback(getMainExecutor(), (TelephonyCallback) callStateCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void unregisterCallbackApi31() {
        if (callStateCallback != null) {
            telephonyManager.unregisterTelephonyCallback((TelephonyCallback) callStateCallback);
            callStateCallback = null;
        }
    }

    private void handleCallStateChange(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                startRecording();
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                stopRecording();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
            default:
                break;
        }
    }

    private void startRecording() {
        stopRecording(); // ensure clean state
        File dir = new File(getExternalFilesDir(null), "recordings");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "call_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".m4a";
        currentFile = new File(dir, fileName);

        recorder = new MediaRecorder();
        try {
            // Prefer VOICE_COMMUNICATION; fall back to MIC if unavailable
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        } catch (Exception e) {
            recorder.reset();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128_000);
        recorder.setAudioSamplingRate(44_100);
        recorder.setOutputFile(currentFile.getAbsolutePath());

        try {
            recorder.prepare();
            recorder.start();
            recording = true;
            startMs = System.currentTimeMillis();
            updateNotification(true);
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(tick, 5_000);
        } catch (IOException | RuntimeException e) {
            recording = false;
            updateNotification(false);
            cleanupFile();
            safeRelease();
        }
    }

    private void stopRecording() {
        if (!recording) return;
        try {
            recorder.stop();
        } catch (RuntimeException ignored) {
        }
        recording = false;
        safeRelease();
        updateNotification(false);
        handler.removeCallbacksAndMessages(null);
    }

    private void safeRelease() {
        if (recorder != null) {
            try {
                recorder.reset();
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
    }

    private void cleanupFile() {
        if (currentFile != null && currentFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            currentFile.delete();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void updateNotification(boolean isRecording) {
        Notification notif = buildNotification(isRecording ? getRecordingText() : getString(R.string.notification_idle));
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notif);
        if (isRecording) {
            startForeground(NOTIF_ID, notif);
        }
    }

    private String getRecordingText() {
        long elapsed = Math.max(0, System.currentTimeMillis() - startMs);
        long min = (elapsed / 1000) / 60;
        long sec = (elapsed / 1000) % 60;
        return getString(R.string.notification_recording) + " • " + String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
