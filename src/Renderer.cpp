#include "Renderer.h"

#ifdef PLATFORM_ANDROID
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Renderer", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "Renderer", __VA_ARGS__)
#elif defined(PLATFORM_IOS)
#include <OpenGLES/ES3/gl.h>
#else
// Desktop OpenGL headers would go here
#include <iostream>
#define LOGV(...) printf(__VA_ARGS__); printf("\n")
#define LOGE(...) printf(__VA_ARGS__); printf("\n")
#endif

// EGL globals for Android
#ifdef PLATFORM_ANDROID
static EGLDisplay display_ = EGL_NO_DISPLAY;
static EGLContext context_ = EGL_NO_CONTEXT;
static EGLSurface surface_ = EGL_NO_SURFACE;
static ANativeWindow* nativeWindow_ = nullptr;
#endif

Renderer::Renderer() = default;
Renderer::~Renderer() = default;

bool Renderer::Initialize(int width, int height) {
    LOGV("Renderer::Initialize(%d, %d)", width, height);
    width_ = width;
    height_ = height;
    
#ifdef PLATFORM_ANDROID
    if (!InitializeEGL()) {
        LOGE("Failed to initialize EGL");
        return false;
    }
#endif
    
    InitializeGL();
    SetupMatrices();
    
    LOGV("Renderer initialized successfully");
    return true;
}

#ifdef PLATFORM_ANDROID
bool Renderer::InitializeEGL() {
    LOGV("InitializeEGL() called");
    
    // Get the display
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }
    
    // Initialize EGL
    EGLint major, minor;
    if (!eglInitialize(display_, &major, &minor)) {
        LOGE("eglInitialize failed");
        return false;
    }
    LOGV("EGL version: %d.%d", major, minor);
    
    // Get config
    EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 16,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_NONE
    };
    
    EGLConfig config;
    EGLint numConfigs;
    if (!eglChooseConfig(display_, configAttribs, &config, 1, &numConfigs)) {
        LOGE("eglChooseConfig failed");
        return false;
    }
    if (numConfigs == 0) {
        LOGE("No valid EGL config found");
        return false;
    }
    
    // Create context
    EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };
    
    context_ = eglCreateContext(display_, config, EGL_NO_CONTEXT, contextAttribs);
    if (context_ == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed");
        return false;
    }
    
    LOGV("EGL initialized successfully");
    return true;
}

bool Renderer::SetNativeWindow(ANativeWindow* window) {
    LOGV("SetNativeWindow called");
    
    if (!window) {
        LOGE("Null window passed to SetNativeWindow");
        return false;
    }
    
    if (!display_ || display_ == EGL_NO_DISPLAY) {
        LOGE("EGL display not initialized");
        return false;
    }
    
    nativeWindow_ = window;
    
    // Get config again
    EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 16,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_NONE
    };
    
    EGLConfig config;
    EGLint numConfigs;
    if (!eglChooseConfig(display_, configAttribs, &config, 1, &numConfigs)) {
        LOGE("eglChooseConfig failed in SetNativeWindow");
        return false;
    }
    
    // Create surface
    surface_ = eglCreateWindowSurface(display_, config, window, nullptr);
    if (surface_ == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed");
        return false;
    }
    
    // Make current
    if (!eglMakeCurrent(display_, surface_, surface_, context_)) {
        LOGE("eglMakeCurrent failed");
        return false;
    }
    
    LOGV("Native window set successfully");
    return true;
}
#endif

void Renderer::Shutdown() {
#ifdef PLATFORM_ANDROID
    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (surface_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, surface_);
        }
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);
        }
        eglTerminate(display_);
    }
#endif
}

void Renderer::InitializeGL() {
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    
    glViewport(0, 0, width_, height_);
}

void Renderer::SetupMatrices() {
    // Setup projection and view matrices
    // This would use a proper matrix library in production
}

void Renderer::BeginFrame() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
}

void Renderer::EndFrame() {
    // Swap buffers - platform specific
#ifdef PLATFORM_ANDROID
    // eglSwapBuffers(display, surface);
#elif defined(PLATFORM_IOS)
    // [context presentRenderbuffer:GL_RENDERBUFFER];
#endif
}

void Renderer::Clear(float r, float g, float b, float a) {
    glClearColor(r, g, b, a);
}

void Renderer::DrawCube(float x, float y, float z, 
                       float w, float h, float d,
                       float r, float g, float b, float a) {
    // Draw a simple colored cube using OpenGL
    // In production, this would use vertex buffers and shaders
}

void Renderer::DrawGround(float width, float length) {
    // Draw the ground plane
    DrawCube(0, -0.01f, -length/2, width, 0.02f, length,
             0.02f, 0.13f, 0.23f, 1.0f); // Dark blue-gray
}

void Renderer::DrawLaneMarker(float x, float z) {
    // Draw lane marking
    DrawCube(x, 0.01f, z, 0.15f, 0.02f, 1.6f,
             0.9f, 0.93f, 0.97f, 1.0f); // Light gray
}

void Renderer::DrawText(const std::string& text, float x, float y, float size) {
    // Text rendering would use a bitmap font or FreeType
    // Placeholder implementation
}

void Renderer::DrawRect(float x, float y, float w, float h,
                       float r, float g, float b, float a) {
    // 2D rectangle for UI
}

void Renderer::SetCameraPosition(float x, float y, float z) {
    cameraX_ = x;
    cameraY_ = y;
    cameraZ_ = z;
}

void Renderer::SetCameraTarget(float x, float y, float z) {
    // Update camera look-at target
}

void Renderer::UpdateCamera(float playerZ) {
    // Follow player
    cameraZ_ = playerZ + 9.0f;
}

void Renderer::SetSunIntensity(float intensity) {
    sunIntensity_ = intensity;
}

void Renderer::SetMoonIntensity(float intensity) {
    moonIntensity_ = intensity;
}
