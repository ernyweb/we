#!/bin/bash
# VPS Server Tam Güncelleme

echo "=== VPS Translation Server Güncelleme ==="

cd /root/translation-server || exit 1

# PM2 durdur
echo "Server durduruluyor..."
pm2 stop translation-server 2>/dev/null

# Yedek al
if [ -f server-secure.js ]; then
    cp server-secure.js server-secure.js.backup.$(date +%Y%m%d_%H%M%S)
fi

# Tüm dosyaları güncelle
echo "Güncel dosyalar indiriliyor..."
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js -o server-secure.js
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/package.json -o package.json
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/.env.example -o .env

# Bağımlılıkları güncelle
echo "Bağımlılıklar kontrol ediliyor..."
npm install --production

# Server'ı başlat
echo "Server başlatılıyor..."
pm2 delete translation-server 2>/dev/null
pm2 start server-secure.js --name translation-server
pm2 save

echo ""
echo "✅ Güncelleme tamamlandı!"
echo ""
echo "📊 Server durumu:"
pm2 status

echo ""
echo "📜 Son loglar:"
pm2 logs translation-server --lines 15 --nostream
