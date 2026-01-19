#!/bin/bash
# Stable Translation Server Setup Script (Node.js, PM2, Port 5000)
# Usage: bash setup.sh

set -e

# 1. Node.js 20.x kurulumu (varsa atla)
if ! command -v node >/dev/null 2>&1; then
  echo "📦 Installing Node.js 20.x..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
else
  echo "✅ Node.js already installed."
fi

# 2. PM2 kurulumu
if ! command -v pm2 >/dev/null 2>&1; then
  echo "⚙️  Installing PM2..."
  sudo npm install -g pm2
else
  echo "✅ PM2 already installed."
fi

# 3. Bağımlılıkları yükle
npm install

# 4. .env dosyasını oluştur (PORT=5000)

cat > .env <<EOF
PORT=5000
SERVER_SECRET=GxKin3To18njIeQJlYzXuZI5zwf05pgE
NODE_ENV=production
EOF

echo "✅ .env created with PORT=5000."

# 5. Server'ı PM2 ile başlat
pm2 stop translation-server 2>/dev/null || true
pm2 delete translation-server 2>/dev/null || true
pm2 start server-secure.js --name translation-server
pm2 save
pm2 startup

echo "\n✅ Translation server is running on port 5000!"
echo "\n80 → 5000 port yönlendirmesi için Apache veya Nginx reverse proxy kullanabilirsin."
echo "\nÖrnek Apache ayarı:"
echo "<VirtualHost *:80>\n  ProxyPreserveHost On\n  ProxyPass / http://localhost:5000/\n  ProxyPassReverse / http://localhost:5000/\n</VirtualHost>"
echo "\nTest için: curl http://localhost:5000/?api_key=translation-key-2026-secure-abc123 | jq"
