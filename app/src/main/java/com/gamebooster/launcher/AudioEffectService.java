package com.gamebooster.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Advanced Audio Effect Service
 * Uses VOICE_CALL source + AudioEffect API for system-wide voice changing
 * This works better on calls and VoIP apps
 */
public class AudioEffectService extends Service {
    
    private static final String TAG = "AudioEffectService";
    private static final String CHANNEL_ID = "audio_effect_channel";
    private static final int NOTIFICATION_ID = 2001;
    
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread processingThread;
    private volatile boolean isProcessing = false;
    
    // Audio effects
    private AcousticEchoCanceler echoCanceler;
    private AutomaticGainControl gainControl;
    private NoiseSuppressor noiseSuppressor;
    
    // Voice effect
    private static VoiceEffect currentEffect = VoiceEffect.NONE;
    
    // Native audio processing
    static {
        try {
            System.loadLibrary("audioprocessor");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }
    
    // Native methods for audio processing
    private native void initNativeProcessor(int sampleRate);
    private native void processAudioNative(short[] input, short[] output, int length, int effectType);
    private native void releaseNativeProcessor();
    
    public enum VoiceEffect {
        NONE(0), ROBOT(1), WOMAN(2), MAN(3), CHILD(4), MONSTER(5);
        
        private final int value;
        VoiceEffect(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Initialize native processor
        try {
            initNativeProcessor(SAMPLE_RATE);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native processor not available, using Java processing", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isProcessing) {
            startAudioProcessing();
        }
        return START_STICKY;
    }
    
    private void startAudioProcessing() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            // Try VOICE_CALL source first (best for calls)
            try {
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_CALL,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 4
                );
            } catch (Exception e) {
                Log.w(TAG, "VOICE_CALL not available, trying VOICE_COMMUNICATION", e);
                // Fallback to VOICE_COMMUNICATION
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 4
                );
            }
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }
            
            // Setup audio effects
            int sessionId = audioRecord.getAudioSessionId();
            setupAudioEffects(sessionId);
            
            // Create AudioTrack for output - use VOICE_CALL stream
            audioTrack = new AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT,
                bufferSize * 4,
                AudioTrack.MODE_STREAM
            );
            
            // Set audio track to communication mode
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false);
            }
            
            audioRecord.startRecording();
            audioTrack.play();
            
            isProcessing = true;
            
            // Start processing thread
            processingThread = new Thread(this::processAudioLoop);
            processingThread.setPriority(Thread.MAX_PRIORITY);
            processingThread.start();
            
            updateNotification("🎙️ Aktif - Aramalar için ses değiştirme çalışıyor");
            Log.i(TAG, "Audio processing started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio processing", e);
            stopSelf();
        }
    }
    
    private void setupAudioEffects(int sessionId) {
        try {
            // Disable echo cancellation to preserve our effects
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId);
                if (echoCanceler != null) {
                    echoCanceler.setEnabled(false);
                }
            }
            
            // Disable automatic gain control
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId);
                if (gainControl != null) {
                    gainControl.setEnabled(false);
                }
            }
            
            // Keep noise suppressor enabled but minimal
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId);
                if (noiseSuppressor != null) {
                    noiseSuppressor.setEnabled(true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup audio effects", e);
        }
    }
    
    private void processAudioLoop() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
        short[] inputBuffer = new short[bufferSize];
        short[] outputBuffer = new short[bufferSize];
        
        while (isProcessing) {
            try {
                int samplesRead = audioRecord.read(inputBuffer, 0, bufferSize);
                
                if (samplesRead > 0) {
                    // Try native processing first
                    try {
                        processAudioNative(inputBuffer, outputBuffer, samplesRead, currentEffect.getValue());
                    } catch (UnsatisfiedLinkError e) {
                        // Fallback to Java processing
                        applyEffectJava(inputBuffer, outputBuffer, samplesRead);
                    }
                    
                    // Write to output
                    audioTrack.write(outputBuffer, 0, samplesRead);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in audio processing loop", e);
                break;
            }
        }
    }
    
    private void applyEffectJava(short[] input, short[] output, int length) {
        switch (currentEffect) {
            case ROBOT:
                applyRobotEffect(input, output, length);
                break;
            case WOMAN:
                applyPitchShift(input, output, length, 1.3f);
                break;
            case MAN:
                applyPitchShift(input, output, length, 0.7f);
                break;
            case CHILD:
                applyPitchShift(input, output, length, 1.5f);
                break;
            case MONSTER:
                applyMonsterEffect(input, output, length);
                break;
            default:
                System.arraycopy(input, 0, output, 0, length);
                break;
        }
    }
    
    private void applyRobotEffect(short[] input, short[] output, int length) {
        // Ring modulation for robot effect
        float carrierFreq = 440.0f;
        float phase = 0;
        float phaseIncrement = (2.0f * (float)Math.PI * carrierFreq) / SAMPLE_RATE;
        
        for (int i = 0; i < length; i++) {
            float modulator = (float)Math.sin(phase);
            float sample = input[i] / 32768.0f;
            sample = sample * modulator * 0.7f;
            output[i] = (short)(sample * 32767.0f);
            
            phase += phaseIncrement;
            if (phase > 2.0f * Math.PI) {
                phase -= 2.0f * (float)Math.PI;
            }
        }
    }
    
    private void applyPitchShift(short[] input, short[] output, int length, float pitchFactor) {
        // Simple pitch shifting using resampling
        for (int i = 0; i < length; i++) {
            float sourceIndex = i / pitchFactor;
            int index = (int)sourceIndex;
            
            if (index >= 0 && index < length - 1) {
                float fraction = sourceIndex - index;
                float sample1 = input[index];
                float sample2 = input[index + 1];
                output[i] = (short)(sample1 + fraction * (sample2 - sample1));
            } else {
                output[i] = 0;
            }
        }
    }
    
    private void applyMonsterEffect(short[] input, short[] output, int length) {
        // Low pitch + echo
        float pitchFactor = 0.5f;
        int echoDelay = SAMPLE_RATE / 4; // 250ms echo
        float echoDecay = 0.3f;
        
        for (int i = 0; i < length; i++) {
            float sourceIndex = i / pitchFactor;
            int index = (int)sourceIndex;
            
            short sample = 0;
            if (index >= 0 && index < length) {
                sample = input[index];
            }
            
            // Add echo
            if (i >= echoDelay) {
                sample += (short)(output[i - echoDelay] * echoDecay);
            }
            
            output[i] = sample;
        }
    }
    
    public static void setEffect(VoiceEffect effect) {
        currentEffect = effect;
        Log.i(TAG, "Effect changed to: " + effect);
    }
    
    public static VoiceEffect getCurrentEffect() {
        return currentEffect;
    }
    
    private void stopAudioProcessing() {
        isProcessing = false;
        
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
        
        // Release audio effects
        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }
        if (gainControl != null) {
            gainControl.release();
            gainControl = null;
        }
        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
        
        // Release native processor
        try {
            releaseNativeProcessor();
        } catch (UnsatisfiedLinkError e) {
            // Native library not loaded
        }
        
        // Reset audio mode
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Audio Effect Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Sistem geneli ses değiştirme servisi");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ Voice Changer - Audio Effect")
            .setContentText("Aramalar için ses değiştirme aktif")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ Voice Changer - Audio Effect")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
        
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAudioProcessing();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
