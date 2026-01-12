#!/bin/bash
# VPS Python Translation Server - Tam Kurulum

set -e

echo "╔════════════════════════════════════════════════════════╗"
echo "║  🐍 Python Translation Server Kurulumu                ║"
echo "╚════════════════════════════════════════════════════════╝"

# Python3 kontrol
if ! command -v python3 &> /dev/null; then
    echo "📦 Python3 kuruluyor..."
    apt update
    apt install -y python3 python3-pip python3-venv
fi

# Dizin oluştur
mkdir -p /root/translation-server
cd /root/translation-server

# Dosyaları indir
echo "📥 Dosyalar indiriliyor..."
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server.py -o server.py
curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/requirements.txt -o requirements.txt

# Sözlükleri indir
mkdir -p lang
for file in en-tr tr-en en-ru ru-en en-es es-en en-fr fr-en; do
    curl -sL https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/${file}.json -o lang/${file}.json
done

# Virtual environment
echo "🔧 Virtual environment oluşturuluyor..."
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

# Eski servisleri durdur
echo "🛑 Eski servisler durduruluyor..."
pkill -9 node 2>/dev/null || true
pm2 kill 2>/dev/null || true
systemctl stop nginx 2>/dev/null || true
systemctl stop apache2 2>/dev/null || true
systemctl stop translation-server 2>/dev/null || true

# Systemd service
echo "⚙️  Systemd service oluşturuluyor..."
cat > /etc/systemd/system/translation-server.service << 'EOF'
[Unit]
Description=Translation Server (Python Flask)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/translation-server
Environment="PATH=/root/translation-server/venv/bin"
ExecStart=/root/translation-server/venv/bin/python3 /root/translation-server/server.py
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

# Başlat
echo "🚀 Server başlatılıyor..."
chmod +x server.py
systemctl daemon-reload
systemctl enable translation-server
systemctl start translation-server

sleep 2

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║  ✅ KURULUM TAMAMLANDI                                 ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

systemctl status translation-server --no-pager -l

echo ""
echo "📊 TEST:"
echo "curl -X POST http://localhost/translate \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -H 'X-API-Key: mobile-internal-audio-key-2026-xyz789' \\"
echo "  -d '{\"text\":\"hello\",\"source\":\"EN\",\"target\":\"TR\"}'"
echo ""
