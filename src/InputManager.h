#pragma once

class InputManager {
public:
    InputManager();
    ~InputManager();
    
    void Update();
    
    // Touch/Mouse
    bool IsTouchDown() const { return isTouchDown_; }
    bool IsTouchJustPressed() const { return touchJustPressed_; }
    float GetTouchX() const { return touchX_; }
    float GetTouchY() const { return touchY_; }
    
    // Swipe detection
    bool IsSwipeLeft() const { return swipeLeft_; }
    bool IsSwipeRight() const { return swipeRight_; }
    bool IsSwipeUp() const { return swipeUp_; }
    
    // Keyboard (for testing)
    bool IsKeyPressed(int keyCode) const;
    
    void OnTouchDown(float x, float y);
    void OnTouchMove(float x, float y);
    void OnTouchUp(float x, float y);
    
private:
    void ProcessSwipe();
    
    bool isTouchDown_ = false;
    bool touchJustPressed_ = false;
    float touchX_ = 0.0f;
    float touchY_ = 0.0f;
    
    float touchStartX_ = 0.0f;
    float touchStartY_ = 0.0f;
    
    bool swipeLeft_ = false;
    bool swipeRight_ = false;
    bool swipeUp_ = false;
    
    constexpr static float SWIPE_THRESHOLD = 50.0f;
};
