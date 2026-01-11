#!/bin/bash
# Fix Node.js installation conflict and complete VPS setup

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   🔧 Fixing Node.js Installation Conflict                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Remove old Node.js completely
echo "🗑️  Removing old Node.js packages..."
apt remove -y nodejs libnode-dev libnode72 2>/dev/null || true
apt purge -y nodejs libnode-dev libnode72 2>/dev/null || true
apt autoremove -y
apt autoclean

# Clean dpkg
echo "🧹 Cleaning dpkg..."
dpkg --configure -a
apt --fix-broken install -y

# Remove any leftover files
echo "🗑️  Removing leftover files..."
rm -rf /usr/include/node
rm -rf /usr/lib/node_modules
rm -rf /usr/share/doc/nodejs
rm -f /usr/bin/node
rm -f /usr/bin/npm
rm -f /usr/bin/npx

# Update package list
echo "📦 Updating package list..."
apt update

# Install Node.js 20.x
echo "📥 Installing Node.js 20.x..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# Verify installation
echo ""
echo "✅ Node.js installation:"
node -v
npm -v

# Install PM2
echo ""
echo "⚙️  Installing PM2..."
npm install -g pm2

# Create project directory
echo "📁 Creating project directory..."
mkdir -p /root/translation-server
cd /root/translation-server

# Create package.json
cat > package.json << 'PACKAGE_EOF'
{
  "name": "translation-server",
  "version": "2.0.0",
  "description": "Secure translation server with HMAC authentication",
  "main": "server-secure.js",
  "scripts": {
    "start": "node server-secure.js",
    "pm2": "pm2 start server-secure.js --name translation-server"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1"
  }
}
PACKAGE_EOF

# Install dependencies
echo "📦 Installing npm dependencies..."
npm install

# Create .env file
cat > .env << 'ENV_EOF'
PORT=80
SERVER_SECRET=vps-translation-server-secret-key-2026-change-this
NODE_ENV=production
ENV_EOF

# Create directories
mkdir -p lang

# Download server files
echo "⬇️  Downloading server files..."
curl -o server-secure.js https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js
curl -o lang/en-tr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-tr.json
curl -o lang/tr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/tr-en.json
curl -o lang/en-ru.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-ru.json
curl -o lang/ru-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/ru-en.json
curl -o lang/en-es.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-es.json
curl -o lang/es-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/es-en.json
curl -o lang/en-fr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-fr.json
curl -o lang/fr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/fr-en.json

# Stop Apache
echo "🛑 Stopping Apache..."
systemctl stop apache2 2>/dev/null || true
systemctl disable apache2 2>/dev/null || true

# Configure firewall
echo "🔥 Configuring firewall..."
ufw allow 80/tcp 2>/dev/null || true
ufw allow 22/tcp 2>/dev/null || true
echo "y" | ufw enable 2>/dev/null || true

# Start server
echo "🚀 Starting server with PM2..."
pm2 stop translation-server 2>/dev/null || true
pm2 delete translation-server 2>/dev/null || true
pm2 start server-secure.js --name translation-server
pm2 save

# Setup PM2 startup
pm2 startup systemd -u root --hp /root
pm2 save

# Show status
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   ✅ Installation Complete!                                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
pm2 status
echo ""
echo "📡 Server URL: http://$(curl -s ifconfig.me)"
echo "🔧 Test: curl http://localhost/"
echo ""
