#pragma once

#include <string>

class Renderer {
public:
    Renderer();
    ~Renderer();
    
    bool Initialize(int width, int height);
    void Shutdown();
    
    void BeginFrame();
    void EndFrame();
    
    void Clear(float r, float g, float b, float a);
    
    // 3D rendering
    void DrawCube(float x, float y, float z, float w, float h, float d, 
                  float r, float g, float b, float a);
    void DrawGround(float width, float length);
    void DrawLaneMarker(float x, float z);
    
    // UI rendering
    void DrawText(const std::string& text, float x, float y, float size);
    void DrawRect(float x, float y, float w, float h, 
                  float r, float g, float b, float a);
    
    // Camera
    void SetCameraPosition(float x, float y, float z);
    void SetCameraTarget(float x, float y, float z);
    void UpdateCamera(float playerZ);
    
    // Lighting
    void SetSunIntensity(float intensity);
    void SetMoonIntensity(float intensity);
    
    int GetWidth() const { return width_; }
    int GetHeight() const { return height_; }
    
private:
    void InitializeGL();
    void SetupMatrices();
    
    int width_ = 0;
    int height_ = 0;
    
    float cameraX_ = 0.0f;
    float cameraY_ = 4.5f;
    float cameraZ_ = 9.0f;
    
    float sunIntensity_ = 1.0f;
    float moonIntensity_ = 0.1f;
};
