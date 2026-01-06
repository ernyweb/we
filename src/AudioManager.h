#pragma once

#include <string>

class AudioManager {
public:
    AudioManager();
    ~AudioManager();
    
    bool Initialize();
    void Shutdown();
    
    void Update(float deltaTime);
    
    void PlayMusic();
    void StopMusic();
    void PauseMusic();
    void ResumeMusic();
    
    void PlayJumpSound();
    void PlayCollisionSound();
    void PlayScoreSound();
    
    void SetMusicVolume(float volume);
    void SetSFXVolume(float volume);
    
    float GetMusicVolume() const { return musicVolume_; }
    float GetSFXVolume() const { return sfxVolume_; }
    
private:
    bool isInitialized_ = false;
    bool isMusicPlaying_ = false;
    float musicVolume_ = 0.6f;
    float sfxVolume_ = 0.8f;
    
    // Platform-specific audio handles would go here
#ifdef PLATFORM_ANDROID
    // Android AudioTrack/OpenSL ES
#elif defined(PLATFORM_IOS)
    // iOS AVAudioPlayer
#endif
};
