#pragma once

#include "Config.h"
#include <string>

class SaveManager {
public:
    SaveManager();
    ~SaveManager();
    
    bool Initialize();
    
    // Device ID
    std::string GetDeviceId() const { return deviceId_; }
    
    // Score
    int GetBestScore() const { return bestScore_; }
    void SetBestScore(int score);
    
    int GetTotalGames() const { return totalGames_; }
    void IncrementTotalGames();
    
    // Settings
    Config::Language GetLanguage() const { return language_; }
    void SetLanguage(Config::Language lang);
    
    float GetMusicVolume() const { return musicVolume_; }
    void SetMusicVolume(float volume);
    
    int GetGraphicsQuality() const { return graphicsQuality_; }
    void SetGraphicsQuality(int quality);
    
    // Character
    Config::CharacterType GetSelectedCharacter() const { return selectedCharacter_; }
    void SetSelectedCharacter(Config::CharacterType character);
    
    void Save();
    void Load();
    
private:
    std::string GenerateDeviceId();
    std::string GetSaveFilePath();
    
    std::string deviceId_;
    int bestScore_ = 0;
    int totalGames_ = 0;
    Config::Language language_ = Config::Language::EN;
    float musicVolume_ = 0.6f;
    int graphicsQuality_ = 1; // 0=low, 1=medium, 2=high
    Config::CharacterType selectedCharacter_ = Config::CharacterType::RUNNER;
};
