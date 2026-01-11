#!/bin/bash
# Update VPS translation server

echo "🚀 Updating VPS Translation Server..."

# Upload new server code
scp -r /workspaces/we/translation-server/* root@72.60.130.39:/root/translation-server/

# Restart PM2 service
ssh root@72.60.130.39 << 'ENDSSH'
cd /root/translation-server
npm install
pm2 restart translation-server || pm2 start server-secure.js --name translation-server
pm2 save
pm2 logs --lines 20
ENDSSH

echo "✅ VPS updated successfully!"
