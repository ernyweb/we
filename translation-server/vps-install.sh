#!/bin/bash
# VPS Translation Server - Complete Installation Script
# Run this on VPS: bash <(curl -s https://raw.githubusercontent.com/ernyweb/we/main/translation-server/vps-install.sh)

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   🔐 VPS Translation Server - Installation Script         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Update system
echo "📦 Updating system packages..."
apt update && apt upgrade -y

# Install Node.js 20.x
echo "📥 Installing Node.js 20.x..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# Install PM2
echo "⚙️  Installing PM2..."
npm install -g pm2

# Create project directory
echo "📁 Creating project directory..."
mkdir -p /root/translation-server
cd /root/translation-server

# Initialize package.json
echo "📝 Initializing project..."
cat > package.json << 'PACKAGE_EOF'
{
  "name": "translation-server",
  "version": "2.0.0",
  "description": "Secure translation server with HMAC authentication",
  "main": "server-secure.js",
  "scripts": {
    "start": "node server-secure.js",
    "dev": "nodemon server-secure.js",
    "pm2": "pm2 start server-secure.js --name translation-server"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1"
  },
  "devDependencies": {
    "nodemon": "^3.0.2"
  }
}
PACKAGE_EOF

# Install dependencies
echo "📦 Installing npm dependencies..."
npm install

# Create .env file
echo "🔐 Creating environment configuration..."
cat > .env << 'ENV_EOF'
PORT=80
SERVER_SECRET=vps-translation-server-secret-key-2026-change-this
NODE_ENV=production
ENV_EOF

# Create directories
echo "📂 Creating directory structure..."
mkdir -p lang

# Download server files from GitHub
echo "⬇️  Downloading server files from GitHub..."
curl -o server-secure.js https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js
curl -o lang/en-tr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-tr.json
curl -o lang/tr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/tr-en.json
curl -o lang/en-ru.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-ru.json
curl -o lang/ru-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/ru-en.json
curl -o lang/en-es.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-es.json
curl -o lang/es-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/es-en.json
curl -o lang/en-fr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-fr.json
curl -o lang/fr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/fr-en.json

# Set permissions
echo "🔒 Setting permissions..."
chmod +x server-secure.js

# Stop Apache if running (port 80 conflict)
echo "🛑 Stopping Apache if running..."
systemctl stop apache2 2>/dev/null || true
systemctl disable apache2 2>/dev/null || true

# Configure firewall
echo "🔥 Configuring firewall..."
ufw allow 80/tcp
ufw allow 22/tcp
ufw --force enable

# Start server with PM2
echo "🚀 Starting server with PM2..."
pm2 stop translation-server 2>/dev/null || true
pm2 delete translation-server 2>/dev/null || true
pm2 start server-secure.js --name translation-server
pm2 save

# Setup PM2 startup
echo "⚡ Configuring PM2 startup..."
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
echo "📡 Server Info:"
echo "   URL: http://$(curl -s ifconfig.me)"
echo "   Port: 80"
echo "   Status: $(pm2 jlist | jq -r '.[0].pm2_env.status')"
echo ""
echo "🔧 Useful Commands:"
echo "   pm2 status              - Check server status"
echo "   pm2 logs                - View logs"
echo "   pm2 restart translation-server - Restart server"
echo "   pm2 stop translation-server    - Stop server"
echo ""
echo "🧪 Test server:"
echo "   curl http://localhost/"
echo ""
