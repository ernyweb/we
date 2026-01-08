package com.gamebooster.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class SystemVoiceService extends Service {
    
    private static final String TAG = "SystemVoiceService";
    private static final String CHANNEL_ID = "VoiceChangerChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread processingThread;
    private boolean isRunning = false;
    
    private static VoiceEffect currentEffect = VoiceEffect.NONE;
    private static boolean isServiceActive = false;
    
    public enum VoiceEffect {
        NONE, ROBOT, WOMAN, MAN, CHILD, MONSTER
    }
    
    public static void setEffect(VoiceEffect effect) {
        currentEffect = effect;
    }
    
    public static VoiceEffect getCurrentEffect() {
        return currentEffect;
    }
    
    public static boolean isActive() {
        return isServiceActive;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "SystemVoiceService created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        
        if ("START".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification("Voice Changer Active"));
            startVoiceProcessing();
        } else if ("STOP".equals(action)) {
            stopVoiceProcessing();
            stopSelf();
        }
        
        return START_STICKY;
    }
    
    private void startVoiceProcessing() {
        if (isRunning) {
            Log.w(TAG, "Already running");
            return;
        }
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            // Create audio record
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 4
            );
            
            // Create audio track
            audioTrack = new AudioTrack(
                android.media.AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT,
                bufferSize * 4,
                AudioTrack.MODE_STREAM
            );
            
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                audioTrack.play();
                isRunning = true;
                isServiceActive = true;
                
                processingThread = new Thread(this::processAudioLoop);
                processingThread.setPriority(Thread.MAX_PRIORITY);
                processingThread.start();
                
                Log.i(TAG, "✅ Voice processing started");
                updateNotification("Voice Changer: " + currentEffect.name());
            } else {
                Log.e(TAG, "AudioRecord initialization failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start voice processing", e);
            stopVoiceProcessing();
        }
    }
    
    private void stopVoiceProcessing() {
        isRunning = false;
        isServiceActive = false;
        
        if (processingThread != null) {
            try {
                processingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            processingThread = null;
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
        
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioTrack = null;
        }
        
        Log.i(TAG, "Voice processing stopped");
    }
    
    private void processAudioLoop() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        short[] buffer = new short[bufferSize];
        
        while (isRunning) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            
            if (read > 0) {
                // Apply voice effect
                short[] processed = applyEffect(buffer, read);
                
                // Output processed audio
                audioTrack.write(processed, 0, read);
            }
        }
    }
    
    private short[] applyEffect(short[] audioData, int length) {
        switch (currentEffect) {
            case ROBOT:
                return applyRobotEffect(audioData, length);
            case WOMAN:
                return applyPitchShift(audioData, length, 1.3f);
            case MAN:
                return applyPitchShift(audioData, length, 0.7f);
            case CHILD:
                return applyPitchShift(audioData, length, 1.5f);
            case MONSTER:
                return applyMonsterEffect(audioData, length);
            case NONE:
            default:
                short[] output = new short[length];
                System.arraycopy(audioData, 0, output, 0, length);
                return output;
        }
    }
    
    private short[] applyRobotEffect(short[] audioData, int length) {
        short[] output = new short[length];
        for (int i = 0; i < length; i++) {
            float sample = audioData[i] / 32768.0f;
            float modulator = (float) Math.sin(2 * Math.PI * 30 * i / SAMPLE_RATE);
            sample = sample * modulator * 0.8f;
            output[i] = (short) (sample * 32768.0f);
        }
        return output;
    }
    
    private short[] applyPitchShift(short[] audioData, int length, float pitchFactor) {
        short[] output = new short[length];
        for (int i = 0; i < length; i++) {
            int sourceIndex = (int) (i / pitchFactor);
            if (sourceIndex < length) {
                output[i] = audioData[sourceIndex];
            } else {
                output[i] = 0;
            }
        }
        return output;
    }
    
    private short[] applyMonsterEffect(short[] audioData, int length) {
        short[] output = new short[length];
        for (int i = 0; i < length; i++) {
            int sourceIndex = (int) (i / 0.5f);
            short sample = 0;
            
            if (sourceIndex < length) {
                sample = audioData[sourceIndex];
            }
            
            if (i > SAMPLE_RATE / 4) {
                sample = (short) ((sample + audioData[i - SAMPLE_RATE / 4] * 0.3f) / 1.3f);
            }
            
            output[i] = sample;
        }
        return output;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Changer Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("System-wide voice changing service");
            
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
                .setContentTitle("Voice Changer")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
    
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVoiceProcessing();
        Log.d(TAG, "SystemVoiceService destroyed");
    }
}
