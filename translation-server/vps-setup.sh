#!/bin/bash
# Quick VPS Setup Script

echo "🚀 Installing PM2..."
npm install -g pm2

echo "✅ PM2 installed!"
echo ""

echo "🔧 Creating .env file..."
cat > .env << 'EOF'
PORT=3001
SERVER_SECRET=vps-translation-server-secret-key-2026-change-this
NODE_ENV=production
EOF

echo "✅ .env created!"
echo ""

echo "🚀 Starting translation server..."
pm2 start server-secure.js --name translation-server

echo "💾 Saving PM2 process list..."
pm2 save

echo "🔄 Setting up PM2 auto-start..."
pm2 startup

echo ""
echo "✅ Server started successfully!"
echo ""
echo "📊 Check status:"
echo "   pm2 status"
echo ""
echo "📋 View logs:"
echo "   pm2 logs translation-server"
echo ""
echo "🧪 Test server:"
echo "   curl 'http://localhost:3001/?api_key=translation-key-2026-secure-abc123' | jq"
echo ""
echo "🌐 Your server is ready at: http://YOUR_IP:3001"
echo ""
