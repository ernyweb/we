#!/bin/bash
# VPS Translation Server Startup Script

echo "=== Translation Server Başlatma ==="

# Server directory
cd /root/translation-server || exit 1

# PM2 ile server başlat
pm2 start server-secure.js --name translation-server

# Logları göster
pm2 logs translation-server --lines 20

echo ""
echo "✅ Server başlatıldı!"
echo "📊 Durum: pm2 status"
echo "📜 Loglar: pm2 logs translation-server"
echo "🔄 Restart: pm2 restart translation-server"
echo "🛑 Stop: pm2 stop translation-server"
