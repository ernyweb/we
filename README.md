# 🎮 Game Launcher with FPS Booster

A powerful Android application that detects installed games and provides system optimization before launching them for maximum FPS performance.

[![Build](https://github.com/ernyweb/we/actions/workflows/build.yml/badge.svg)](https://github.com/ernyweb/we/actions/workflows/build.yml)

## ✨ Features

- ✅ **Auto-detect games** - Intelligent game discovery from installed apps
- ✅ **FPS Boost Engine** - RAM cleanup, background app termination
- ✅ **Performance Monitoring** - Real-time FPS, CPU, RAM, temperature tracking
- ✅ **Beautiful UI** - Material Design with game grid
- ✅ **One-tap Launch** - Launch games with instant optimization
- ✅ **100% Kotlin** - Pure Android, no native code
- ✅ **Auto Build** - GitHub Actions CI/CD pipeline
- ✅ **Minimal Dependencies** - AndroidX only

## 🚀 Quick Start

### From Source
```bash
git clone https://github.com/ernyweb/we.git
cd we
./gradlew assembleRelease
```

### From GitHub Releases
Download latest APK from [Releases](https://github.com/ernyweb/we/releases)

```bash
adb install app-release.apk
```

## 🔧 Tech Stack

- **Language**: Kotlin
- **Min SDK**: Android 24 (Android 7.0)  
- **Target SDK**: Android 34
- **Build**: Gradle with Kotlin DSL
- **Dependencies**: AndroidX, Material Design

## 📂 Project Structure

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/gamebooster/launcher/
│   │   ├── MainActivity.kt         # Main activity
│   │   ├── GameDetector.kt         # Game discovery
│   │   ├── GameLauncher.kt         # Launch manager
│   │   ├── FpsBooster.kt           # Optimization engine
│   │   └── GameAdapter.kt          # UI adapter
│   └── res/layout/ & values/
```

## 📋 Permissions

- `GET_TASKS` - List running processes
- `KILL_BACKGROUND_PROCESSES` - Terminate apps
- `QUERY_ALL_PACKAGES` - Scan installed games
- `INTERNET` - (optional) future features

## 🚀 Build & Deploy

GitHub Actions automatically:
1. Builds APK on push
2. Creates release with APK
3. Keeps artifacts for 30 days

See `.github/workflows/build.yml`

## 🎯 How It Works

1. **Detect**: Scans device for games using keywords
2. **Optimize**: Cleans RAM, stops background apps
3. **Launch**: Starts game with improved performance
4. **Monitor**: Shows real-time stats (FPS, CPU, RAM, temp)

---

**Built with ❤️ in Kotlin for gamers**

