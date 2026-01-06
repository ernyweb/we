#pragma once

#include "Config.h"

class Player {
public:
    Player();
    ~Player();
    
    void Update(float deltaTime);
    void Render();
    
    void MoveLeft();
    void MoveRight();
    void Jump();
    
    void Reset();
    void ChangeCharacter(Config::CharacterType type);
    
    // Getters
    float GetX() const { return x_; }
    float GetY() const { return y_; }
    float GetZ() const { return z_; }
    int GetCurrentLane() const { return currentLane_; }
    Config::CharacterType GetCharacterType() const { return characterType_; }
    bool IsOnGround() const { return y_ <= Config::GROUND_Y; }
    
private:
    void UpdateMovement(float deltaTime);
    void UpdatePhysics(float deltaTime);
    
    float x_ = 0.0f;
    float y_ = Config::GROUND_Y;
    float z_ = 0.0f;
    
    float velocityY_ = 0.0f;
    float targetX_ = 0.0f;
    
    int currentLane_ = 1; // Middle lane
    Config::CharacterType characterType_ = Config::CharacterType::RUNNER;
    
    float runCycle_ = 0.0f;
};
