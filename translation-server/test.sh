#!/bin/bash
# Quick test script for translation server

SERVER_URL="http://localhost:3001"
API_KEY="translation-key-2026-secure-abc123"

echo "=== Testing Secure Translation Server ==="
echo ""

# Test 1: Health Check
echo "1. Health Check:"
curl -s "${SERVER_URL}/?api_key=${API_KEY}" | jq -r '.data | "Service: \(.service)\nStatus: \(.status)\nSecurity: \(.security)"'
echo ""

# Test 2: EN -> TR
echo "2. Translation (EN -> TR): 'hello'"
curl -s "${SERVER_URL}/translate-en-hello-to-tr?api_key=${API_KEY}" | jq -r '.data | "Original: \(.original)\nTranslated: \(.translated)\nAccuracy: \((.wordsTranslated / .totalWords * 100) | round)%"'
echo ""

# Test 3: EN -> RU
echo "3. Translation (EN -> RU): 'how are you'"
curl -s "${SERVER_URL}/translate-en-how.are.you-to-ru?api_key=${API_KEY}" | jq -r '.data | "Original: \(.original)\nTranslated: \(.translated)\nCoverage: \(.wordsTranslated)/\(.totalWords)"'
echo ""

# Test 4: EN -> ES
echo "4. Translation (EN -> ES): 'thank you'"
curl -s "${SERVER_URL}/translate-en-thank.you-to-es?api_key=${API_KEY}" | jq -r '.data | "Original: \(.original)\nTranslated: \(.translated)"'
echo ""

# Test 5: EN -> FR
echo "5. Translation (EN -> FR): 'good morning'"
curl -s "${SERVER_URL}/translate-en-good.morning-to-fr?api_key=${API_KEY}" | jq -r '.data | "Original: \(.original)\nTranslated: \(.translated)"'
echo ""

# Test 6: Languages
echo "6. Supported Languages:"
curl -s "${SERVER_URL}/languages?api_key=${API_KEY}" | jq -r '.data.languages[] | "  \(.code): \(.name) (\(.nativeName))"'
echo ""

# Test 7: Download Language Pack
echo "7. Download Language Pack (EN-TR):"
curl -s "${SERVER_URL}/download/en?target=tr&api_key=${API_KEY}" | jq -r '.data | "Pack: \(.pack)\nWords: \(.words)\nSample: \(.dictionary.hello) <- hello"'
echo ""

echo "=== All Tests Complete ==="
