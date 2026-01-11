#!/bin/bash
# VPS Server Güncelleme - API Key Ekle

echo "=== VPS Translation Server Güncelleme ==="

cd /root/translation-server || exit 1

# Yedek al
cp server-secure.js server-secure.js.backup

# GitHub'dan güncel dosyayı çek
echo "Güncel server dosyası indiriliyor..."
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js -o server-secure.js

# PM2'yi restart et
echo "Server yeniden başlatılıyor..."
pm2 restart translation-server

echo ""
echo "✅ Güncelleme tamamlandı!"
echo ""
pm2 logs translation-server --lines 20
