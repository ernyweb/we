#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <cstring>

#define LOG_TAG "AudioProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Effect types
#define EFFECT_NONE 0
#define EFFECT_ROBOT 1
#define EFFECT_WOMAN 2
#define EFFECT_MAN 3
#define EFFECT_CHILD 4
#define EFFECT_MONSTER 5

// Global state
static int g_sampleRate = 16000;
static float g_robotPhase = 0.0f;
static short* g_echoBuffer = nullptr;
static int g_echoBufferSize = 0;
static int g_echoIndex = 0;

extern "C" {

JNIEXPORT void JNICALL
Java_com_gamebooster_launcher_AudioEffectService_initNativeProcessor(
    JNIEnv* env, jobject thiz, jint sampleRate) {
    
    g_sampleRate = sampleRate;
    
    // Allocate echo buffer (500ms)
    g_echoBufferSize = sampleRate / 2;
    g_echoBuffer = new short[g_echoBufferSize];
    memset(g_echoBuffer, 0, g_echoBufferSize * sizeof(short));
    g_echoIndex = 0;
    
    LOGI("Native audio processor initialized: sampleRate=%d", sampleRate);
}

// Fast pitch shifting using phase vocoder approximation
static void pitchShift(const short* input, short* output, int length, float pitchFactor) {
    for (int i = 0; i < length; i++) {
        float sourceIndex = i / pitchFactor;
        int idx = (int)sourceIndex;
        
        if (idx >= 0 && idx < length - 1) {
            float frac = sourceIndex - idx;
            float s1 = input[idx];
            float s2 = input[idx + 1];
            
            // Linear interpolation
            float sample = s1 + frac * (s2 - s1);
            
            // Clamp to short range
            if (sample > 32767.0f) sample = 32767.0f;
            if (sample < -32768.0f) sample = -32768.0f;
            
            output[i] = (short)sample;
        } else {
            output[i] = 0;
        }
    }
}

// Ring modulation for robot effect
static void robotEffect(const short* input, short* output, int length) {
    const float carrierFreq = 440.0f; // A4 note
    const float phaseIncrement = (2.0f * M_PI * carrierFreq) / g_sampleRate;
    
    for (int i = 0; i < length; i++) {
        float modulator = sinf(g_robotPhase) * 0.7f + 0.3f; // Carrier wave
        float sample = input[i] / 32768.0f;
        sample = sample * modulator;
        
        // Add some harmonics
        float harmonic = sinf(g_robotPhase * 2.0f) * 0.2f;
        sample += sample * harmonic;
        
        output[i] = (short)(sample * 32767.0f);
        
        g_robotPhase += phaseIncrement;
        if (g_robotPhase > 2.0f * M_PI) {
            g_robotPhase -= 2.0f * M_PI;
        }
    }
}

// Monster effect with echo
static void monsterEffect(const short* input, short* output, int length) {
    const float pitchFactor = 0.55f; // Deep voice
    const float echoDecay = 0.4f;
    
    // First apply pitch shift
    short* tempBuffer = new short[length];
    pitchShift(input, tempBuffer, length, pitchFactor);
    
    // Then add echo
    for (int i = 0; i < length; i++) {
        short sample = tempBuffer[i];
        
        // Add echo from buffer
        short echo = g_echoBuffer[g_echoIndex];
        int combined = sample + (int)(echo * echoDecay);
        
        // Clamp
        if (combined > 32767) combined = 32767;
        if (combined < -32768) combined = -32768;
        
        output[i] = (short)combined;
        
        // Store in echo buffer
        g_echoBuffer[g_echoIndex] = (short)combined;
        g_echoIndex = (g_echoIndex + 1) % g_echoBufferSize;
    }
    
    delete[] tempBuffer;
}

// Woman effect (higher pitch)
static void womanEffect(const short* input, short* output, int length) {
    pitchShift(input, output, length, 1.32f);
    
    // Add some brightness
    for (int i = 1; i < length; i++) {
        int enhanced = output[i] + (output[i] - output[i-1]) * 0.15f;
        if (enhanced > 32767) enhanced = 32767;
        if (enhanced < -32768) enhanced = -32768;
        output[i] = (short)enhanced;
    }
}

// Man effect (deeper voice)
static void manEffect(const short* input, short* output, int length) {
    pitchShift(input, output, length, 0.72f);
    
    // Add some bass
    for (int i = 1; i < length; i++) {
        int smoothed = (output[i] + output[i-1]) / 2;
        output[i] = (short)smoothed;
    }
}

// Child effect (much higher pitch)
static void childEffect(const short* input, short* output, int length) {
    pitchShift(input, output, length, 1.48f);
    
    // Make it brighter and clearer
    for (int i = 2; i < length; i++) {
        int enhanced = output[i] * 1.1f + (output[i] - output[i-1]) * 0.2f;
        if (enhanced > 32767) enhanced = 32767;
        if (enhanced < -32768) enhanced = -32768;
        output[i] = (short)enhanced;
    }
}

JNIEXPORT void JNICALL
Java_com_gamebooster_launcher_AudioEffectService_processAudioNative(
    JNIEnv* env, jobject thiz, 
    jshortArray input, jshortArray output, 
    jint length, jint effectType) {
    
    jshort* inputBuffer = env->GetShortArrayElements(input, nullptr);
    jshort* outputBuffer = env->GetShortArrayElements(output, nullptr);
    
    if (inputBuffer == nullptr || outputBuffer == nullptr) {
        LOGE("Failed to get array elements");
        return;
    }
    
    switch (effectType) {
        case EFFECT_ROBOT:
            robotEffect(inputBuffer, outputBuffer, length);
            break;
            
        case EFFECT_WOMAN:
            womanEffect(inputBuffer, outputBuffer, length);
            break;
            
        case EFFECT_MAN:
            manEffect(inputBuffer, outputBuffer, length);
            break;
            
        case EFFECT_CHILD:
            childEffect(inputBuffer, outputBuffer, length);
            break;
            
        case EFFECT_MONSTER:
            monsterEffect(inputBuffer, outputBuffer, length);
            break;
            
        case EFFECT_NONE:
        default:
            memcpy(outputBuffer, inputBuffer, length * sizeof(short));
            break;
    }
    
    env->ReleaseShortArrayElements(input, inputBuffer, 0);
    env->ReleaseShortArrayElements(output, outputBuffer, 0);
}

JNIEXPORT void JNICALL
Java_com_gamebooster_launcher_AudioEffectService_releaseNativeProcessor(
    JNIEnv* env, jobject thiz) {
    
    if (g_echoBuffer != nullptr) {
        delete[] g_echoBuffer;
        g_echoBuffer = nullptr;
    }
    
    g_echoBufferSize = 0;
    g_echoIndex = 0;
    g_robotPhase = 0.0f;
    
    LOGI("Native audio processor released");
}

} // extern "C"
