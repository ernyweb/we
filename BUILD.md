# Build Detayları

## GitHub Actions Otomatik Build

### Nasıl Çalışır?

1. **Trigger:** `main` veya `develop` branch'e push/PR
2. **Runner:** Ubuntu (Android) + macOS (iOS)
3. **Output:** APK + IPA artifacts
4. **Release:** Main branch'e push'ta otomatik release

### Workflow Adımları:

#### Android Build
```yaml
1. Checkout code
2. Setup JDK 17
3. Setup Android NDK r26b
4. Install CMake & Ninja
5. Create Android project structure
6. Generate build files (AndroidManifest, build.gradle)
7. Build APK with Gradle
8. Upload artifact (30 day retention)
```

#### iOS Build
```yaml
1. Checkout code
2. Install CMake & Ninja (Homebrew)
3. Setup Xcode (latest stable)
4. Configure CMake for iOS
5. Build with Xcode
6. Create IPA package
7. Upload artifact (30 day retention)
```

#### Release (main branch only)
```yaml
1. Download Android + iOS artifacts
2. Create GitHub release
3. Upload APK + IPA files
4. Tag: v1.0.<run_number>
```

## Manuel Build Talimatları

### Android (Linux/macOS/Windows)

#### Gereksinimler:
```bash
# Ubuntu/Debian
sudo apt install cmake ninja-build openjdk-17-jdk

# Download Android NDK
wget https://dl.google.com/android/repository/android-ndk-r26b-linux.zip
unzip android-ndk-r26b-linux.zip -d ~/
export ANDROID_NDK_HOME=~/android-ndk-r26b
```

#### Build:
```bash
mkdir build-android && cd build-android

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 \
  -DANDROID_STL=c++_shared \
  -DANDROID=ON \
  -GNinja

ninja
```

#### APK Oluşturma:
```bash
# Android Studio ile veya:
cd ..
./gradlew assembleRelease
# APK: android/app/build/outputs/apk/release/
```

### iOS (macOS Only)

#### Gereksinimler:
```bash
# Install Xcode from App Store
# Install command line tools
xcode-select --install

# Install CMake
brew install cmake ninja
```

#### Build:
```bash
mkdir build-ios && cd build-ios

cmake .. -GXcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DIOS=ON

# Build in Xcode
open Runner3D.xcodeproj

# Or command line:
cmake --build . --config Release -- -sdk iphoneos
```

#### IPA Oluşturma:
```bash
cd Release-iphoneos
mkdir Payload
cp -r Runner3D.app Payload/
zip -r Runner3D.ipa Payload
```

## Test Etme

### Android Emulator:
```bash
# Create AVD
avdmanager create avd -n test -k "system-images;android-34;google_apis;x86_64"

# Start emulator
emulator -avd test

# Install APK
adb install runner3d.apk
```

### iOS Simulator:
```bash
# List simulators
xcrun simctl list devices

# Boot simulator
xcrun simctl boot "iPhone 15 Pro"

# Install app
xcrun simctl install booted build-ios/Release-iphonesimulator/Runner3D.app
```

### Real Device:
```bash
# Android
adb devices
adb install -r runner3d.apk
adb logcat | grep Runner3D

# iOS (via Xcode)
# Product → Destination → Your Device
# Product → Run (Cmd+R)
```

## Troubleshooting

### Android NDK Errors:
```bash
# Verify NDK path
echo $ANDROID_NDK_HOME
ls $ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake

# Clean and rebuild
rm -rf build-android
mkdir build-android && cd build-android
cmake .. [options]
```

### iOS Signing Errors:
```bash
# In Xcode:
# 1. Select project → Signing & Capabilities
# 2. Select your Team
# 3. Check "Automatically manage signing"

# Or set team in CMake:
cmake .. -DCMAKE_XCODE_ATTRIBUTE_DEVELOPMENT_TEAM="YOUR_TEAM_ID"
```

### CMake Cache Issues:
```bash
# Delete cache
rm -rf build-android build-ios CMakeCache.txt CMakeFiles/

# Reconfigure
cmake .. [options]
```

## Platform-Specific Notes

### Android:
- **Min SDK:** API 24 (Android 7.0)
- **Target SDK:** API 34 (Android 14)
- **ABIs:** arm64-v8a, armeabi-v7a
- **STL:** c++_shared (libc++_shared.so)

### iOS:
- **Min Version:** iOS 13.0
- **Architectures:** arm64 (Apple Silicon + devices)
- **Frameworks:** Foundation, UIKit, OpenGLES

## Performance Optimization

### Release Builds:
```cmake
# CMake Release flags
cmake --build . --config Release

# Strips symbols automatically
# Enables -O3 optimization
# Defines NDEBUG
```

### Size Optimization:
```cmake
# Add to CMakeLists.txt
set(CMAKE_CXX_FLAGS_RELEASE "-Os -DNDEBUG")
```

### Android Shrinking:
```gradle
// build.gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        }
    }
}
```

## Continuous Integration

### GitHub Secrets (iOS):
```
IOS_TEAM_ID: Your Apple Developer Team ID
```

### Artifact Retention:
- Default: 30 days
- Change in workflow: `retention-days: 90`

### Build Matrix (Optional):
```yaml
strategy:
  matrix:
    abi: [arm64-v8a, armeabi-v7a, x86_64]
```

---

**Her push'ta otomatik build alırsın! 🚀**
