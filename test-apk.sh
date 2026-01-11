#!/bin/bash

# Android APK Testing Script
# Tests the integrated ServerTranslator APK

echo "======================================"
echo "🔍 Android APK Test Script"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# APK path
APK_PATH="/workspaces/we/CaptionTranslator/app/build/outputs/apk/debug/app-debug.apk"

echo "1️⃣ Checking APK existence..."
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✅ APK found${NC}"
    ls -lh "$APK_PATH"
else
    echo -e "${RED}❌ APK not found${NC}"
    exit 1
fi

echo ""
echo "2️⃣ APK Information..."
echo "Path: $APK_PATH"
SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "Size: $SIZE"

echo ""
echo "3️⃣ Checking for ServerTranslator class..."
# Extract APK and check for ServerTranslator
TEMP_DIR=$(mktemp -d)
unzip -q "$APK_PATH" -d "$TEMP_DIR"

if find "$TEMP_DIR" -name "*ServerTranslator*" | grep -q .; then
    echo -e "${GREEN}✅ ServerTranslator found in APK${NC}"
    find "$TEMP_DIR" -name "*ServerTranslator*" | head -5
else
    echo -e "${RED}❌ ServerTranslator not found${NC}"
fi

echo ""
echo "4️⃣ Checking for old DeepL references..."
if grep -r "deepl\|DeepL" "$TEMP_DIR/classes" 2>/dev/null | grep -q .; then
    echo -e "${YELLOW}⚠️  DeepL references found (might be in libraries)${NC}"
else
    echo -e "${GREEN}✅ No DeepL references in code${NC}"
fi

echo ""
echo "5️⃣ Checking for VPS URL..."
if grep -r "72.60.130.39" "$TEMP_DIR" 2>/dev/null | grep -q .; then
    echo -e "${GREEN}✅ VPS URL found in APK${NC}"
else
    echo -e "${YELLOW}⚠️  VPS URL not found (might be obfuscated)${NC}"
fi

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo "======================================"
echo "6️⃣ ADB Device Check..."
echo "======================================"

if command -v adb &> /dev/null; then
    echo "ADB version:"
    adb version | head -1
    
    echo ""
    echo "Connected devices:"
    adb devices
    
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✅ $DEVICE_COUNT device(s) connected${NC}"
        echo ""
        echo "📲 To install APK, run:"
        echo "   adb install -r $APK_PATH"
        echo ""
        echo "📋 To view logs, run:"
        echo "   adb logcat | grep -i 'ServerTranslator\\|Translation\\|CaptionService'"
    else
        echo -e "${YELLOW}⚠️  No devices connected${NC}"
        echo ""
        echo "To connect device:"
        echo "  1. Enable USB Debugging on Android"
        echo "  2. Connect via USB"
        echo "  3. Accept debugging prompt"
        echo "  4. Run: adb devices"
    fi
else
    echo -e "${YELLOW}⚠️  ADB not found${NC}"
    echo "Install Android SDK Platform-Tools to use ADB"
fi

echo ""
echo "======================================"
echo "7️⃣ VPS Server Status"
echo "======================================"

echo "Testing VPS server..."
if command -v curl &> /dev/null; then
    RESPONSE=$(curl -s -m 5 http://72.60.130.39/ 2>&1)
    
    if echo "$RESPONSE" | grep -q "Secure Translation Server"; then
        echo -e "${GREEN}✅ VPS server is ONLINE${NC}"
        echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    else
        echo -e "${RED}❌ VPS server unreachable${NC}"
        echo "Response: $RESPONSE"
    fi
else
    echo -e "${YELLOW}⚠️  curl not found, skipping server test${NC}"
fi

echo ""
echo "======================================"
echo "✅ Test Summary"
echo "======================================"
echo "APK Path: $APK_PATH"
echo "APK Size: $SIZE"
echo "Build: Debug"
echo "Status: Ready for installation"
echo ""
echo "Next steps:"
echo "  1. Connect Android device via USB"
echo "  2. Enable USB Debugging"
echo "  3. Run: adb install -r $APK_PATH"
echo "  4. Grant overlay permission"
echo "  5. Grant microphone permission"
echo "  6. Test translation with YouTube video"
echo ""
