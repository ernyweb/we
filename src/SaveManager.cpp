#include "SaveManager.h"
#include <fstream>
#include <sstream>
#include <random>

#ifdef PLATFORM_ANDROID
#include <android/log.h>
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "SaveManager", __VA_ARGS__)
#else
#include <iostream>
#define LOG(...) printf(__VA_ARGS__); printf("\n")
#endif

SaveManager::SaveManager() = default;
SaveManager::~SaveManager() = default;

bool SaveManager::Initialize() {
    Load();
    
    // Generate device ID if not exists
    if (deviceId_.empty()) {
        deviceId_ = GenerateDeviceId();
        Save();
    }
    
    return true;
}

std::string SaveManager::GenerateDeviceId() {
    // Generate a simple UUID-like string
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 15);
    
    const char* hexChars = "0123456789abcdef";
    std::string uuid;
    uuid.reserve(36);
    
    for (int i = 0; i < 36; ++i) {
        if (i == 8 || i == 13 || i == 18 || i == 23) {
            uuid += '-';
        } else {
            uuid += hexChars[dis(gen)];
        }
    }
    
    LOG("Generated new device ID: %s", uuid.c_str());
    return uuid;
}

std::string SaveManager::GetSaveFilePath() {
#ifdef PLATFORM_ANDROID
    // Use Android internal storage
    return "/data/data/com.runner3d.app/files/save.dat";
#elif defined(PLATFORM_IOS)
    // Use iOS Documents directory
    return "~/Documents/runner3d_save.dat";
#else
    return "save.dat";
#endif
}

void SaveManager::Save() {
    std::ofstream file(GetSaveFilePath());
    if (!file.is_open()) {
        LOG("Failed to open save file for writing");
        return;
    }
    
    file << "deviceId=" << deviceId_ << "\n";
    file << "bestScore=" << bestScore_ << "\n";
    file << "totalGames=" << totalGames_ << "\n";
    file << "language=" << static_cast<int>(language_) << "\n";
    file << "musicVolume=" << musicVolume_ << "\n";
    file << "graphicsQuality=" << graphicsQuality_ << "\n";
    file << "selectedCharacter=" << static_cast<int>(selectedCharacter_) << "\n";
    
    file.close();
    LOG("Game saved successfully");
}

void SaveManager::Load() {
    std::ifstream file(GetSaveFilePath());
    if (!file.is_open()) {
        LOG("No save file found, using defaults");
        return;
    }
    
    std::string line;
    while (std::getline(file, line)) {
        size_t pos = line.find('=');
        if (pos == std::string::npos) continue;
        
        std::string key = line.substr(0, pos);
        std::string value = line.substr(pos + 1);
        
        if (key == "deviceId") {
            deviceId_ = value;
        } else if (key == "bestScore") {
            bestScore_ = std::stoi(value);
        } else if (key == "totalGames") {
            totalGames_ = std::stoi(value);
        } else if (key == "language") {
            language_ = static_cast<Config::Language>(std::stoi(value));
        } else if (key == "musicVolume") {
            musicVolume_ = std::stof(value);
        } else if (key == "graphicsQuality") {
            graphicsQuality_ = std::stoi(value);
        } else if (key == "selectedCharacter") {
            selectedCharacter_ = static_cast<Config::CharacterType>(std::stoi(value));
        }
    }
    
    file.close();
    LOG("Save data loaded successfully");
}

void SaveManager::SetBestScore(int score) {
    bestScore_ = score;
}

void SaveManager::IncrementTotalGames() {
    totalGames_++;
}

void SaveManager::SetLanguage(Config::Language lang) {
    language_ = lang;
}

void SaveManager::SetMusicVolume(float volume) {
    musicVolume_ = volume;
}

void SaveManager::SetGraphicsQuality(int quality) {
    graphicsQuality_ = quality;
}

void SaveManager::SetSelectedCharacter(Config::CharacterType character) {
    selectedCharacter_ = character;
}
