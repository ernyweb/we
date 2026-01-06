#pragma once

class Obstacle {
public:
    Obstacle(float x, float y, float z, int lane);
    ~Obstacle();
    
    void Update(float deltaTime, float gameSpeed);
    void Render();
    
    bool IsActive() const { return isActive_; }
    void SetActive(bool active) { isActive_ = active; }
    
    float GetX() const { return x_; }
    float GetY() const { return y_; }
    float GetZ() const { return z_; }
    int GetLane() const { return lane_; }
    
    bool CheckCollision(float px, float py, float pz) const;
    
private:
    float x_;
    float y_;
    float z_;
    int lane_;
    bool isActive_ = true;
    
    float width_ = 0.8f;
    float height_ = 1.8f;
    float depth_ = 0.8f;
};
