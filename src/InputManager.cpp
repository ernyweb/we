#include "InputManager.h"
#include <cmath>

InputManager::InputManager() = default;
InputManager::~InputManager() = default;

void InputManager::Update() {
    // Reset per-frame flags
    touchJustPressed_ = false;
    swipeLeft_ = false;
    swipeRight_ = false;
    swipeUp_ = false;
}

void InputManager::OnTouchDown(float x, float y) {
    isTouchDown_ = true;
    touchJustPressed_ = true;
    touchX_ = x;
    touchY_ = y;
    touchStartX_ = x;
    touchStartY_ = y;
}

void InputManager::OnTouchMove(float x, float y) {
    if (isTouchDown_) {
        touchX_ = x;
        touchY_ = y;
    }
}

void InputManager::OnTouchUp(float x, float y) {
    if (isTouchDown_) {
        ProcessSwipe();
    }
    
    isTouchDown_ = false;
    touchX_ = x;
    touchY_ = y;
}

void InputManager::ProcessSwipe() {
    float dx = touchX_ - touchStartX_;
    float dy = touchY_ - touchStartY_;
    
    float absDx = std::abs(dx);
    float absDy = std::abs(dy);
    
    // Horizontal swipe
    if (absDx > SWIPE_THRESHOLD && absDx > absDy) {
        if (dx > 0) {
            swipeRight_ = true;
        } else {
            swipeLeft_ = true;
        }
    }
    // Vertical swipe
    else if (absDy > SWIPE_THRESHOLD && absDy > absDx) {
        if (dy < 0) { // Swipe up (Y inverted on some platforms)
            swipeUp_ = true;
        }
    }
}

bool InputManager::IsKeyPressed(int keyCode) const {
    // Platform-specific keyboard handling
    return false;
}
