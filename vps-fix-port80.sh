#!/bin/bash
# Port 80 çakışmasını çöz

echo "=== Port 80 Sorununu Çözme ==="

# Apache'yi durdur
echo "Apache durduruluyor..."
systemctl stop apache2 2>/dev/null
systemctl disable apache2 2>/dev/null

# Port 80'i kullanan process'i bul ve öldür
echo "Port 80'i kullanan process kontrol ediliyor..."
PORT_PID=$(lsof -ti:80)
if [ ! -z "$PORT_PID" ]; then
    echo "Process bulundu (PID: $PORT_PID), öldürülüyor..."
    kill -9 $PORT_PID
    sleep 2
fi

# PM2'deki eski instance'ı sil
echo "PM2 temizleniyor..."
pm2 delete translation-server 2>/dev/null
pm2 kill
sleep 2

# Server'ı yeniden başlat
echo "Server başlatılıyor..."
cd /root/translation-server
pm2 start server-secure.js --name translation-server

echo ""
echo "✅ Tamamlandı!"
pm2 status
