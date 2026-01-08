package com.gamebooster.launcher;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

public class RealtimeAudioProcessor {
    
    private static final String TAG = "RealtimeAudioProcessor";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread processingThread;
    private boolean isProcessing = false;
    
    private VoiceEffect currentEffect = VoiceEffect.NONE;
    
    public enum VoiceEffect {
        NONE, ROBOT, WOMAN, MAN, CHILD, MONSTER
    }
    
    public void setEffect(VoiceEffect effect) {
        this.currentEffect = effect;
    }
    
    public void startRealtime() {
        if (isProcessing) return;
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            audioTrack = new AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT,
                bufferSize,
                AudioTrack.MODE_STREAM
            );
            
            isProcessing = true;
            audioRecord.startRecording();
            audioTrack.play();
            
            processingThread = new Thread(this::processAudio);
            processingThread.start();
            
            Log.d(TAG, "Real-time audio processing started");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start real-time processing", e);
            stopRealtime();
        }
    }
    
    public void stopRealtime() {
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
        
        Log.d(TAG, "Real-time audio processing stopped");
    }
    
    private void processAudio() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        short[] buffer = new short[bufferSize];
        
        while (isProcessing) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            
            if (read > 0) {
                // Apply voice effect
                short[] processed = applyEffect(buffer, read);
                
                // Play processed audio
                audioTrack.write(processed, 0, read);
            }
        }
    }
    
    private short[] applyEffect(short[] audioData, int length) {
        short[] output = new short[length];
        
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
                System.arraycopy(audioData, 0, output, 0, length);
                return output;
        }
    }
    
    private short[] applyRobotEffect(short[] audioData, int length) {
        short[] output = new short[length];
        
        // Simple ring modulation for robot effect
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
        
        // Simple pitch shifting using time stretching
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
        
        // Very low pitch + echo
        for (int i = 0; i < length; i++) {
            int sourceIndex = (int) (i / 0.5f);
            short sample = 0;
            
            if (sourceIndex < length) {
                sample = audioData[sourceIndex];
            }
            
            // Add echo
            if (i > SAMPLE_RATE / 4) {
                sample = (short) ((sample + audioData[i - SAMPLE_RATE / 4] * 0.3f) / 1.3f);
            }
            
            output[i] = sample;
        }
        
        return output;
    }
    
    public boolean isProcessing() {
        return isProcessing;
    }
}
