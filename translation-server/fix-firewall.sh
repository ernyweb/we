#!/bin/bash

# Fix firewall to allow external access to port 80
# Run this on your VPS as root

echo "🔥 Fixing Firewall for External Access"
echo "========================================"
echo ""

echo "1️⃣ Current iptables status..."
iptables -L INPUT -n -v | head -15
echo ""

echo "2️⃣ Adding INPUT rules for port 80 and 3001..."
# Allow incoming connections to port 80
iptables -I INPUT -p tcp --dport 80 -j ACCEPT
iptables -I INPUT -p tcp --dport 3001 -j ACCEPT

# Allow established connections
iptables -I INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Allow loopback
iptables -I INPUT -i lo -j ACCEPT

echo "✅ Rules added"
echo ""

echo "3️⃣ Saving iptables rules..."
netfilter-persistent save
echo "✅ Saved"
echo ""

echo "4️⃣ New iptables INPUT rules..."
iptables -L INPUT -n -v | head -15
echo ""

echo "5️⃣ Testing external access..."
echo "Testing from localhost..."
curl -s http://localhost/ | jq '.data.service' 2>/dev/null || curl -s http://localhost/
echo ""

echo "Testing from external IP..."
curl -s http://72.60.130.39/ | jq '.data.service' 2>/dev/null || curl -s http://72.60.130.39/
echo ""

echo "======================================"
echo "✅ Firewall configuration complete!"
echo ""
echo "If still not working, check:"
echo "  1. VPS provider firewall (control panel)"
echo "  2. ufw status (run: ufw status)"
echo "  3. Cloud security groups"
echo ""
echo "Test from browser:"
echo "  http://72.60.130.39/"
echo ""
