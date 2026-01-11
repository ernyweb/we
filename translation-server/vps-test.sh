#!/bin/bash
# Complete VPS setup with all dependencies

echo "📦 Installing jq (JSON parser)..."
apt update && apt install -y jq

echo ""
echo "✅ jq installed!"
echo ""

echo "🧪 Testing translation server..."
echo ""

echo "1️⃣ Health Check:"
curl -s 'http://localhost:3001/?api_key=translation-key-2026-secure-abc123' | jq '.data | {service, version, status, security, languages}'
echo ""

echo "2️⃣ EN -> TR (hello):"
curl -s 'http://localhost:3001/translate-en-hello-to-tr?api_key=translation-key-2026-secure-abc123' | jq '.data | {original, translated}'
echo ""

echo "3️⃣ EN -> RU (good morning):"
curl -s 'http://localhost:3001/translate-en-good.morning-to-ru?api_key=translation-key-2026-secure-abc123' | jq '.data | {original, translated}'
echo ""

echo "4️⃣ EN -> ES (thank you):"
curl -s 'http://localhost:3001/translate-en-thank.you-to-es?api_key=translation-key-2026-secure-abc123' | jq '.data | {original, translated}'
echo ""

echo "5️⃣ EN -> FR (goodbye):"
curl -s 'http://localhost:3001/translate-en-goodbye-to-fr?api_key=translation-key-2026-secure-abc123' | jq '.data | {original, translated}'
echo ""

echo "6️⃣ Languages List:"
curl -s 'http://localhost:3001/languages?api_key=translation-key-2026-secure-abc123' | jq '.data.languages[] | "\(.code): \(.name)"'
echo ""

echo "7️⃣ Download Language Pack (EN-TR sample):"
curl -s 'http://localhost:3001/download/en?target=tr&api_key=translation-key-2026-secure-abc123' | jq '.data | {pack, words, sample: {hello: .dictionary.hello}}'
echo ""

echo "✅ All tests complete!"
echo ""
echo "📊 PM2 Status:"
pm2 status
echo ""
echo "🎯 Your translation server is ready!"
echo "   URL: http://$(hostname -I | awk '{print $1}'):3001"
