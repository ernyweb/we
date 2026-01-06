#include "Obstacle.h"
#include <cmath>

Obstacle::Obstacle(float x, float y, float z, int lane)
    : x_(x), y_(y), z_(z), lane_(lane) {
}

Obstacle::~Obstacle() = default;

void Obstacle::Update(float deltaTime, float gameSpeed) {
    // Move obstacle towards player
    z_ += gameSpeed * deltaTime;
}

bool Obstacle::CheckCollision(float px, float py, float pz) const {
    if (!isActive_) return false;
    
    // Simple AABB collision
    float dx = std::abs(px - x_);
    float dy = std::abs(py - y_);
    float dz = std::abs(pz - z_);
    
    float combinedWidth = (width_ + 0.6f) / 2.0f;
    float combinedHeight = (height_ + 1.6f) / 2.0f;
    float combinedDepth = (depth_ + 0.6f) / 2.0f;
    
    return (dx < combinedWidth && 
            dy < combinedHeight && 
            dz < combinedDepth);
}

void Obstacle::Render() {
    // Rendering is handled by Renderer class
}
