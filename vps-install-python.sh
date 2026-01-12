#!/bin/bash
# VPS Python Translation Server Kurulumu

echo "=== Python Translation Server Kurulumu ==="

# Python3 ve pip kontrol
if ! command -v python3 &> /dev/null; then
    echo "Python3 kuruluyor..."
    apt update
    apt install -y python3 python3-pip python3-venv
fi

# Translation server dizini
cd /root/translation-server || exit 1

# Virtual environment oluştur
python3 -m venv venv
source venv/bin/activate

# Bağımlılıkları yükle
pip install -r requirements.txt

# Eski servisleri durdur
pkill -9 node
pm2 kill
systemctl stop nginx
systemctl stop apache2

# Python server'ı başlat
chmod +x server-flask.py

# Systemd service dosyası oluştur
cat > /etc/systemd/system/translation-server.service << 'EOF'
[Unit]
Description=Translation Server (Python Flask)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/translation-server
Environment="PATH=/root/translation-server/venv/bin"
ExecStart=/root/translation-server/venv/bin/python3 /root/translation-server/server-flask.py
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

# Service'i başlat
systemctl daemon-reload
systemctl enable translation-server
systemctl start translation-server

echo ""
echo "✅ Python Translation Server Başlatıldı!"
echo ""
systemctl status translation-server --no-pager
echo ""
echo "Test et:"
echo "curl -X POST http://localhost/translate -H 'Content-Type: application/json' -H 'X-API-Key: mobile-internal-audio-key-2026-xyz789' -d '{\"text\":\"hello\",\"source\":\"EN\",\"target\":\"TR\"}'"
