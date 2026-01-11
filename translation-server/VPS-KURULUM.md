# 🔐 VPS Translation Server - Kurulum Talimatları

## 📋 Gereksinimler
- Ubuntu 20.04+ veya Debian 11+
- Root erişimi
- Port 80 açık

## 🚀 Otomatik Kurulum (Önerilen)

### VPS'e SSH ile bağlan:
```bash
ssh root@72.60.130.39
```

### Tek komut ile kur:
```bash
bash <(curl -s https://raw.githubusercontent.com/ernyweb/we/main/translation-server/vps-install.sh)
```

Bu script otomatik olarak:
- ✅ Node.js 20.x yükler
- ✅ PM2 process manager yükler
- ✅ Proje dosyalarını GitHub'dan indirir
- ✅ Bağımlılıkları yükler
- ✅ Apache'yi durdurur (port 80 çakışması)
- ✅ Firewall'u yapılandırır
- ✅ Server'ı başlatır ve otomatik başlangıcı ayarlar

---

## 🛠️ Manuel Kurulum (Adım Adım)

### 1. Sistemi Güncelle
```bash
apt update && apt upgrade -y
```

### 2. Node.js 20.x Yükle
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v  # v20.x.x görmelisin
```

### 3. PM2 Yükle
```bash
npm install -g pm2
pm2 -v
```

### 4. Proje Dizini Oluştur
```bash
mkdir -p /root/translation-server
cd /root/translation-server
```

### 5. package.json Oluştur
```bash
cat > package.json << 'EOF'
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
EOF
```

### 6. Dependencies Yükle
```bash
npm install
```

### 7. .env Dosyası Oluştur
```bash
cat > .env << 'EOF'
PORT=80
SERVER_SECRET=vps-translation-server-secret-key-2026-change-this
NODE_ENV=production
EOF
```

### 8. Dizin Yapısını Oluştur
```bash
mkdir -p lang
```

### 9. Server Dosyalarını İndir
```bash
# Ana server dosyası
curl -o server-secure.js https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js

# Dil dosyaları
curl -o lang/en-tr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-tr.json
curl -o lang/tr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/tr-en.json
curl -o lang/en-ru.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-ru.json
curl -o lang/ru-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/ru-en.json
curl -o lang/en-es.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-es.json
curl -o lang/es-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/es-en.json
curl -o lang/en-fr.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/en-fr.json
curl -o lang/fr-en.json https://raw.githubusercontent.com/ernyweb/we/main/translation-server/lang/fr-en.json
```

### 10. Apache'yi Durdur (Port 80 Çakışması)
```bash
systemctl stop apache2
systemctl disable apache2
```

### 11. Firewall Ayarla
```bash
ufw allow 80/tcp
ufw allow 22/tcp
ufw enable
```

### 12. Server'ı PM2 ile Başlat
```bash
pm2 start server-secure.js --name translation-server
pm2 save
pm2 startup systemd -u root --hp /root
```

### 13. Test Et
```bash
# Local test
curl http://localhost/

# External test (başka terminalden)
curl http://72.60.130.39/
```

---

## 📊 Yönetim Komutları

### Server Durumu
```bash
pm2 status
pm2 logs translation-server
pm2 monit
```

### Server Kontrolü
```bash
pm2 restart translation-server  # Yeniden başlat
pm2 stop translation-server     # Durdur
pm2 start translation-server    # Başlat
pm2 delete translation-server   # Sil
```

### Log İzleme
```bash
pm2 logs translation-server --lines 100
pm2 logs translation-server -f  # Real-time
```

### Server Güncelleme
```bash
cd /root/translation-server

# Yeni dosyaları indir
curl -o server-secure.js https://raw.githubusercontent.com/ernyweb/we/main/translation-server/server-secure.js

# Server'ı yeniden başlat
pm2 restart translation-server
```

---

## 🧪 Test Komutları

### 1. Health Check
```bash
curl http://72.60.130.39/
```

### 2. Translation Test
```bash
curl "http://72.60.130.39/test?text=hello&from=en&to=tr&api_key=translation-key-2026-secure-abc123"
```

### 3. History Check
```bash
curl "http://72.60.130.39/history?api_key=translation-key-2026-secure-abc123"
```

### 4. Stats Check
```bash
curl "http://72.60.130.39/stats?api_key=translation-key-2026-secure-abc123"
```

---

## 🔧 Sorun Giderme

### Server başlamıyor
```bash
# Logları kontrol et
pm2 logs translation-server

# Port 80'i kim kullanıyor?
lsof -i :80
netstat -tlnp | grep :80

# Apache çalışıyor mu?
systemctl status apache2
```

### Port 80'e erişilemiyor
```bash
# Firewall kontrolü
ufw status

# Port 80'i aç
ufw allow 80/tcp

# iptables kontrolü
iptables -L -n
```

### Server crash oluyor
```bash
# Hata loglarını oku
pm2 logs translation-server --err --lines 50

# Server'ı debug mode'da çalıştır
pm2 delete translation-server
PORT=80 node server-secure.js
```

### Dosyalar eksik
```bash
# Tüm dosyaları yeniden indir
cd /root/translation-server
rm -rf *
# Yukarıdaki kurulum adımlarını tekrarla
```

---

## 🔐 Güvenlik Notları

### 1. SECRET_KEY'i Değiştir
```bash
nano /root/translation-server/.env
# SERVER_SECRET değerini değiştir
pm2 restart translation-server
```

### 2. Firewall Sadece Gerekli Portları Açık Tut
```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw enable
```

### 3. Güncellemeleri Düzenli Yap
```bash
apt update && apt upgrade -y
npm update
pm2 update
```

---

## 📡 API Endpoints

| Endpoint | Method | Açıklama |
|----------|--------|----------|
| `/` | GET | Server bilgisi |
| `/languages` | GET | Desteklenen diller |
| `/translate` | POST | Çeviri yap (HMAC) |
| `/test` | GET | Test çevirisi |
| `/history` | GET | Çeviri geçmişi |
| `/stats` | GET | İstatistikler |
| `/download/:lang` | GET | Dil paketi indir |

---

## 📞 Destek

Sorun yaşarsanız:
1. `pm2 logs translation-server` ile logları kontrol edin
2. `curl http://localhost/` ile local testi yapın
3. Firewall ve port ayarlarını kontrol edin

**Tüm dosyalar GitHub'da**: https://github.com/ernyweb/we/tree/main/translation-server
