#include "Game.h"
#include <cstdlib>
#include <ctime>
#include <cmath>

#ifdef PLATFORM_ANDROID
#include <android/log.h>
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "Runner3D", __VA_ARGS__)
#else
#include <iostream>
#define LOG(...) printf(__VA_ARGS__); printf("\n")
#endif

Game::Game() = default;
Game::~Game() = default;

bool Game::Initialize() {
    LOG("Initializing Runner3D...");
    
    // Initialize random seed
    srand(static_cast<unsigned>(time(nullptr)));
    
    // Create core systems
    renderer_ = std::make_unique<Renderer>();
    if (!renderer_->Initialize(Config::DEFAULT_WIDTH, Config::DEFAULT_HEIGHT)) {
        LOG("Failed to initialize renderer");
        return false;
    }
    
    inputManager_ = std::make_unique<InputManager>();
    audioManager_ = std::make_unique<AudioManager>();
    if (!audioManager_->Initialize()) {
        LOG("Warning: Audio initialization failed");
    }
    
    saveManager_ = std::make_unique<SaveManager>();
    if (!saveManager_->Initialize()) {
        LOG("Warning: Save manager initialization failed");
    }
    
    // Load saved data
    saveManager_->Load();
    bestScore_ = saveManager_->GetBestScore();
    
    // Create player
    player_ = std::make_unique<Player>();
    player_->ChangeCharacter(saveManager_->GetSelectedCharacter());
    
    // Set initial distance goal
    distanceGoal_ = Config::DIST_MIN + 
                    static_cast<float>(rand()) / RAND_MAX * 
                    (Config::DIST_MAX - Config::DIST_MIN);
    
    isRunning_ = true;
    LOG("Game initialized successfully");
    return true;
}

void Game::Run() {
    lastFrameTime_ = 0; // Platform-specific time function needed
    
    while (isRunning_) {
        // Calculate delta time
        uint64_t currentTime = 0; // Platform-specific
        float deltaTime = 0.016f; // ~60 FPS fallback
        
        ProcessInput();
        
        if (!isPaused_) {
            Update(deltaTime);
        }
        
        Render();
    }
}

void Game::ProcessInput() {
    inputManager_->Update();
    
    // Lane switching
    if (inputManager_->IsSwipeLeft()) {
        player_->MoveLeft();
        audioManager_->PlayScoreSound();
    }
    if (inputManager_->IsSwipeRight()) {
        player_->MoveRight();
        audioManager_->PlayScoreSound();
    }
    
    // Jump
    if (inputManager_->IsSwipeUp() || inputManager_->IsTouchJustPressed()) {
        if (player_->IsOnGround()) {
            player_->Jump();
            audioManager_->PlayJumpSound();
        }
    }
}

void Game::Update(float deltaTime) {
    if (!isPlaying_) return;
    
    // Increase game speed
    gameSpeed_ += Config::SPEED_INCREMENT * deltaTime * 0.1f;
    if (gameSpeed_ > Config::MAX_SPEED) {
        gameSpeed_ = Config::MAX_SPEED;
    }
    
    // Update distance and score
    float distThisFrame = gameSpeed_ * deltaTime * 10.0f;
    distanceRun_ += distThisFrame;
    score_ = static_cast<int>(distanceRun_);
    
    // Update player
    player_->Update(deltaTime);
    
    // Update obstacles
    SpawnObstacles(deltaTime);
    for (auto& obstacle : obstacles_) {
        if (obstacle->IsActive()) {
            obstacle->Update(deltaTime, gameSpeed_);
            
            // Check collision
            if (obstacle->CheckCollision(player_->GetX(), 
                                        player_->GetY(), 
                                        player_->GetZ())) {
                GameOver();
                return;
            }
            
            // Deactivate if behind player
            if (obstacle->GetZ() > 20.0f) {
                obstacle->SetActive(false);
            }
        }
    }
    
    // Update day/night cycle
    UpdateDayNight(deltaTime);
    
    // Update audio
    audioManager_->Update(deltaTime);
}

void Game::Render() {
    renderer_->BeginFrame();
    renderer_->Clear(0.027f, 0.063f, 0.133f, 1.0f); // Dark blue sky
    
    // Update camera to follow player
    renderer_->UpdateCamera(player_->GetZ());
    
    // Draw ground and lane markers
    renderer_->DrawGround(30.0f, 1200.0f);
    for (int i = 0; i < 120; ++i) {
        renderer_->DrawLaneMarker(0.0f, static_cast<float>(i) * -4.0f);
    }
    
    // Draw player
    player_->Render();
    
    // Draw obstacles
    for (auto& obstacle : obstacles_) {
        if (obstacle->IsActive()) {
            obstacle->Render();
        }
    }
    
    // Draw UI
    if (isPlaying_) {
        renderer_->DrawText("Score: " + std::to_string(score_), 20, 20, 24);
        renderer_->DrawText("Best: " + std::to_string(bestScore_), 20, 60, 20);
        renderer_->DrawText("Speed: " + std::to_string(static_cast<int>(gameSpeed_ * 10)) + " km/h", 
                          20, 100, 20);
    } else {
        renderer_->DrawText("Runner3D Mobile", 
                          renderer_->GetWidth() / 2 - 100, 
                          renderer_->GetHeight() / 2, 
                          32);
        renderer_->DrawText("Tap to Start", 
                          renderer_->GetWidth() / 2 - 80, 
                          renderer_->GetHeight() / 2 + 50, 
                          24);
    }
    
    renderer_->EndFrame();
}

void Game::StartGame() {
    isPlaying_ = true;
    isPaused_ = false;
    score_ = 0;
    distanceRun_ = 0.0f;
    gameSpeed_ = Config::INITIAL_SPEED;
    dayTimer_ = 0.0f;
    
    // Clear obstacles
    obstacles_.clear();
    
    // Reset player
    player_->Reset();
    
    // New distance goal
    distanceGoal_ = Config::DIST_MIN + 
                    static_cast<float>(rand()) / RAND_MAX * 
                    (Config::DIST_MAX - Config::DIST_MIN);
    
    audioManager_->PlayMusic();
    LOG("Game started");
}

void Game::GameOver() {
    isPlaying_ = false;
    audioManager_->StopMusic();
    audioManager_->PlayCollisionSound();
    
    // Update best score
    if (score_ > bestScore_) {
        bestScore_ = score_;
        saveManager_->SetBestScore(bestScore_);
    }
    
    saveManager_->IncrementTotalGames();
    saveManager_->Save();
    
    LOG("Game Over - Score: %d, Best: %d", score_, bestScore_);
}

void Game::PauseGame() {
    isPaused_ = true;
    audioManager_->PauseMusic();
}

void Game::ResumeGame() {
    isPaused_ = false;
    audioManager_->ResumeMusic();
}

void Game::UpdateDayNight(float deltaTime) {
    dayTimer_ += deltaTime * 0.6f;
    float phase = fmod(dayTimer_, Config::DAY_LENGTH) / Config::DAY_LENGTH;
    float angle = phase * 2.0f * 3.14159f;
    
    float sunIntensity = 0.25f + std::sin(angle) * 0.55f;
    sunIntensity = std::max(0.12f, std::min(1.0f, sunIntensity));
    
    float moonIntensity = 0.18f + std::max(0.0f, -std::sin(angle)) * 0.35f;
    
    renderer_->SetSunIntensity(sunIntensity);
    renderer_->SetMoonIntensity(moonIntensity);
}

void Game::SpawnObstacles(float deltaTime) {
    spawnTimer_ += deltaTime;
    
    // Adjust spawn interval based on speed
    float speedFactor = gameSpeed_ / Config::MAX_SPEED;
    spawnInterval_ = 2.0f - speedFactor * 1.2f;
    spawnInterval_ = std::max(0.8f, spawnInterval_);
    
    if (spawnTimer_ >= spawnInterval_) {
        spawnTimer_ = 0.0f;
        
        // Random lane
        int lane = rand() % Config::LANE_COUNT;
        float x = Config::LANES[lane];
        
        // Spawn ahead of player
        float z = player_->GetZ() - 50.0f;
        
        auto obstacle = std::make_unique<Obstacle>(x, 0.0f, z, lane);
        obstacles_.push_back(std::move(obstacle));
    }
}

void Game::Shutdown() {
    LOG("Shutting down game...");
    
    obstacles_.clear();
    player_.reset();
    audioManager_->Shutdown();
    renderer_->Shutdown();
    
    isRunning_ = false;
}
