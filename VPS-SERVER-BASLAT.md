# VPS Server Başlatma Rehberi

## 🚀 Hızlı Başlatma

VPS'e SSH ile bağlan:
```bash
ssh root@72.60.130.39
```

Server'ı başlat:
```bash
cd /root/translation-server
pm2 start server-secure.js --name translation-server
```

## 📊 Server Kontrol

**Durum kontrolü:**
```bash
pm2 status
```

**Logları görüntüle:**
```bash
pm2 logs translation-server
```

**Server yeniden başlat:**
```bash
pm2 restart translation-server
```

**Server durdur:**
```bash
pm2 stop translation-server
```

## 🔧 Port Kontrolü

Server'ın 80 portunda çalıştığını kontrol et:
```bash
netstat -tuln | grep :80
```

Başka bir process 80 portunu kullanıyorsa:
```bash
# Apache'yi durdur (eğer çalışıyorsa)
systemctl stop apache2
systemctl disable apache2

# Veya portunu değiştir
export PORT=3000
pm2 restart translation-server
```

## 🧪 Test

**Local test (VPS içinden):**
```bash
curl http://localhost/health
```

**Dışarıdan test:**
```bash
curl http://72.60.130.39/health
```

**Translation testi:**
```bash
curl -X POST http://72.60.130.39/translate \
  -H "Content-Type: application/json" \
  -d '{
    "text": "hello",
    "source": "EN",
    "target": "TR",
    "api_key": "mobile-internal-audio-key-2026-xyz789",
    "timestamp": "'$(date +%s)'",
    "signature": "test123"
  }'
```

## 🔥 Firewall Ayarları

Port 80'i aç:
```bash
ufw allow 80/tcp
ufw status
```

## 📝 Otomatik Başlatma

Sistem yeniden başlatıldığında server'ı otomatik başlat:
```bash
pm2 startup
pm2 save
```

## ⚠️ Sorun Giderme

**Server çalışmıyor:**
```bash
pm2 logs translation-server --err
```

**Port zaten kullanımda:**
```bash
lsof -i :80
# Process'i öldür
kill -9 <PID>
```

**Node.js versiyonu:**
```bash
node --version  # v20.x olmalı
```

## 📱 APK Ayarları

APK şu ayarlarla yapılandırılmıştır:
- **Server URL:** http://72.60.130.39
- **API Key:** mobile-internal-audio-key-2026-xyz789
- **Timeout:** 20 saniye

## 🔐 Güvenlik

API key'leri server-secure.js dosyasında:
```javascript
const API_KEYS = new Set([
  'translation-key-2026-secure-abc123',
  'mobile-app-key-xyz789',
  'mobile-internal-audio-key-2026-xyz789',  // APK için
]);
```

Server secret:
```bash
export SERVER_SECRET="vps-translation-server-secret-key-2026-change-this"
```
