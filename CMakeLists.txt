cmake_minimum_required(VERSION 3.18)
project(Runner3D)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Platform detection
if(ANDROID)
    set(PLATFORM_ANDROID TRUE)
    set(CMAKE_SYSTEM_NAME Android)
    
    # Add Android NDK sources
    set(ANDROID_NDK_SOURCES $ENV{ANDROID_NDK_HOME}/sources)
    if(NOT EXISTS ${ANDROID_NDK_SOURCES})
        message(WARNING "Android NDK sources not found at ${ANDROID_NDK_SOURCES}")
    endif()
    
elseif(IOS)
    set(PLATFORM_IOS TRUE)
    set(CMAKE_SYSTEM_NAME iOS)
endif()

# Source files
set(SOURCES
    src/main.cpp
    src/Game.cpp
    src/Player.cpp
    src/Obstacle.cpp
    src/Renderer.cpp
    src/InputManager.cpp
    src/AudioManager.cpp
    src/SaveManager.cpp
)

set(HEADERS
    src/Game.h
    src/Player.h
    src/Obstacle.h
    src/Renderer.h
    src/InputManager.h
    src/AudioManager.h
    src/SaveManager.h
    src/Config.h
)

# Android specific
if(ANDROID)
    add_library(${PROJECT_NAME} SHARED ${SOURCES} ${HEADERS})
    
    find_library(log-lib log)
    find_library(android-lib android)
    find_library(EGL-lib EGL)
    find_library(GLESv3-lib GLESv3)
    
    target_link_libraries(${PROJECT_NAME}
        ${log-lib}
        ${android-lib}
        ${EGL-lib}
        ${GLESv3-lib}
    )
    
    target_compile_definitions(${PROJECT_NAME} PRIVATE PLATFORM_ANDROID)
    
# iOS specific
elseif(IOS)
    add_executable(${PROJECT_NAME} MACOSX_BUNDLE ${SOURCES} ${HEADERS})
    
    set_target_properties(${PROJECT_NAME} PROPERTIES
        MACOSX_BUNDLE_GUI_IDENTIFIER "com.runner3d.app"
        MACOSX_BUNDLE_BUNDLE_VERSION "1.0"
        MACOSX_BUNDLE_SHORT_VERSION_STRING "1.0"
    )
    
    target_compile_definitions(${PROJECT_NAME} PRIVATE PLATFORM_IOS)
    
    find_library(FOUNDATION_LIBRARY Foundation)
    find_library(UIKIT_LIBRARY UIKit)
    find_library(OPENGLES_LIBRARY OpenGLES)
    
    target_link_libraries(${PROJECT_NAME}
        ${FOUNDATION_LIBRARY}
        ${UIKIT_LIBRARY}
        ${OPENGLES_LIBRARY}
    )
    
# Desktop/Testing
else()
    add_executable(${PROJECT_NAME} ${SOURCES} ${HEADERS})
    target_compile_definitions(${PROJECT_NAME} PRIVATE PLATFORM_DESKTOP)
endif()

# Include directories
target_include_directories(${PROJECT_NAME} PRIVATE src)

# Android NDK native app glue
if(ANDROID)
    target_include_directories(${PROJECT_NAME} PRIVATE 
        $ENV{ANDROID_NDK_HOME}/sources/android/native_app_glue
    )
    target_sources(${PROJECT_NAME} PRIVATE
        $ENV{ANDROID_NDK_HOME}/sources/android/native_app_glue/android_native_app_glue.c
    )
endif()

# Compiler flags
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    target_compile_options(${PROJECT_NAME} PRIVATE
        -Wall -Wextra -Wpedantic
        $<$<CONFIG:Release>:-O3 -DNDEBUG>
        $<$<CONFIG:Debug>:-g -O0>
    )
endif()
