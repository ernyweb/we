#include "Player.h"
#include <cmath>

Player::Player() {
    Reset();
}

Player::~Player() = default;

void Player::Update(float deltaTime) {
    UpdateMovement(deltaTime);
    UpdatePhysics(deltaTime);
    runCycle_ += deltaTime * 8.0f;
}

void Player::UpdateMovement(float deltaTime) {
    // Smooth lane transition
    float lerpSpeed = 12.0f;
    x_ += (targetX_ - x_) * lerpSpeed * deltaTime;
}

void Player::UpdatePhysics(float deltaTime) {
    // Apply gravity
    velocityY_ += Config::GRAVITY * deltaTime;
    y_ += velocityY_ * deltaTime;
    
    // Ground collision
    if (y_ <= Config::GROUND_Y) {
        y_ = Config::GROUND_Y;
        velocityY_ = 0.0f;
    }
}

void Player::MoveLeft() {
    if (currentLane_ > 0) {
        currentLane_--;
        targetX_ = Config::LANES[currentLane_];
    }
}

void Player::MoveRight() {
    if (currentLane_ < Config::LANE_COUNT - 1) {
        currentLane_++;
        targetX_ = Config::LANES[currentLane_];
    }
}

void Player::Jump() {
    if (IsOnGround()) {
        velocityY_ = Config::JUMP_SPEED;
    }
}

void Player::Reset() {
    currentLane_ = 1;
    x_ = Config::LANES[currentLane_];
    y_ = Config::GROUND_Y;
    z_ = 0.0f;
    targetX_ = x_;
    velocityY_ = 0.0f;
    runCycle_ = 0.0f;
}

void Player::ChangeCharacter(Config::CharacterType type) {
    characterType_ = type;
}

void Player::Render() {
    // Rendering is handled by Renderer class
    // This would be called by Renderer with player position data
}
