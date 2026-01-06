#pragma once

namespace Config {
    // Game settings
    constexpr float LANES[] = {-2.4f, 0.0f, 2.4f};
    constexpr int LANE_COUNT = 3;
    constexpr float INITIAL_SPEED = 8.0f;
    constexpr float MAX_SPEED = 36.0f;
    constexpr float SPEED_INCREMENT = 0.5f;
    
    // Player physics
    constexpr float GRAVITY = -30.0f;
    constexpr float JUMP_SPEED = 8.5f;
    constexpr float GROUND_Y = 0.7f;
    constexpr float JUMP_CLEAR_Y = GROUND_Y + 0.6f;
    
    // Day/night cycle
    constexpr float DAY_LENGTH = 140.0f; // seconds
    
    // Distance tracking
    constexpr float DIST_MIN = 1200.0f;
    constexpr float DIST_MAX = 2200.0f;
    
    // Screen
    constexpr int DEFAULT_WIDTH = 1080;
    constexpr int DEFAULT_HEIGHT = 1920;
    
    // Characters
    enum class CharacterType {
        RUNNER = 0,
        CHICKEN,
        ROBLOX,
        HORSE,
        NINJA,
        ASTRONAUT,
        KNIGHT,
        ALIEN,
        PENGUIN,
        SLIME,
        COUNT
    };
    
    // Languages
    enum class Language {
        EN = 0,
        TR,
        ES,
        COUNT
    };
}
