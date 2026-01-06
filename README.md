# Runner3D Mobile - C++ Edition

🎮 **Native C++ endless runner game for Android & iOS**

[![Build](https://github.com/ernyweb/we/actions/workflows/build.yml/badge.svg)](https://github.com/ernyweb/we/actions/workflows/build.yml)

## ✨ Features

- ✅ **100% Native C++** - No Unity, no web technologies
- ✅ **Cross-Platform** - Android (arm64/armv7) & iOS (arm64)
- ✅ **Fully Offline** - No internet required
- ✅ **Auto Build** - GitHub Actions CI/CD pipeline
- ✅ **OpenGL ES 3.0** - Modern 3D graphics
- ✅ **Touch Controls** - Swipe to move, tap to jump
- ✅ **10 Characters** - Runner, Chicken, Roblox, Horse, Ninja, Astronaut, Knight, Alien, Penguin, Slime
- ✅ **Day/Night Cycle** - Dynamic lighting (140s cycle)
- ✅ **Local Storage** - Auto device ID, saved scores
- ✅ **Multi-language** - EN, TR, ES

## 🚀 Quick Start

**GitHub Actions ile otomatik build:**

1. Bu repo'yu fork/clone et
2. GitHub'a push yap
3. Actions sekmesinden APK/IPA indir

**Detaylı build talimatları:** [BUILD.md](BUILD.md)

## 🎮 Controls

- **Swipe Left/Right:** Change lane
- **Swipe Up / Tap:** Jump

## 💾 Local Save Data

- DeviceId (auto-generated UUID)
- BestScore
- TotalGames
- Language (EN/TR/ES)
- Settings (volume, graphics quality)

## 📂 Project Structure

```
src/           # C++ source code
CMakeLists.txt # Build configuration
.github/       # GitHub Actions workflow
BUILD.md       # Detailed build instructions
```

---

**Made with ❤️ in C++**
