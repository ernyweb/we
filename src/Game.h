#pragma once

#include "Config.h"
#include "Player.h"
#include "Obstacle.h"
#include "Renderer.h"
#include "InputManager.h"
#include "AudioManager.h"
#include "SaveManager.h"
#include <vector>
#include <memory>

class Game {
public:
    Game();
    ~Game();
    
    bool Initialize();
    void Run();
    void Shutdown();
    
private:
    void ProcessInput();
    void Update(float deltaTime);
    void Render();
    
    void StartGame();
    void GameOver();
    void PauseGame();
    void ResumeGame();
    
    void UpdateDayNight(float deltaTime);
    void SpawnObstacles(float deltaTime);
    
    // Core systems
    std::unique_ptr<Renderer> renderer_;
    std::unique_ptr<InputManager> inputManager_;
    std::unique_ptr<AudioManager> audioManager_;
    std::unique_ptr<SaveManager> saveManager_;
    
    // Game objects
    std::unique_ptr<Player> player_;
    std::vector<std::unique_ptr<Obstacle>> obstacles_;
    
    // Game state
    bool isRunning_ = false;
    bool isPlaying_ = false;
    bool isPaused_ = false;
    
    float gameSpeed_ = Config::INITIAL_SPEED;
    int score_ = 0;
    int bestScore_ = 0;
    float distanceRun_ = 0.0f;
    float distanceGoal_ = 1500.0f;
    
    // Day/night
    float dayTimer_ = 0.0f;
    
    // Spawning
    float spawnTimer_ = 0.0f;
    float spawnInterval_ = 2.0f;
    
    // Performance tracking
    uint64_t lastFrameTime_ = 0;
    float deltaAccumulator_ = 0.0f;
};
