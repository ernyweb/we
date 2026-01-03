# Avoider — Basit Web Oyun

Runner3D — Basit 3D endless runner

Bu proje şimdi Three.js kullanarak çalışır. Amaç: üç şerit arasında hareket ederek engellerden kaçmak ve mümkün olduğunca yüksek puan almak.

Nasıl çalıştırılır

1. Proje klasörüne girin:

```bash
cd /workspaces/webgame
```

2. Basit bir HTTP sunucusu çalıştırın (ör. Python 3):

```bash
python3 -m http.server 8000
```

3. Tarayıcıda `http://localhost:8000` adresini açın.

Kontroller

- Klavye: Sol/Sağ ok tuşları veya `A`/`D` ile şerit değiştirin.
- Mobil: Ekranın sol/sağ tarafına dokun veya kaydırma ile şerit değiştirin.

Özellikler

- Three.js tabanlı 3D görselleştirme.
- Menü, yeniden başlatma ve "How to" butonu.
- `localStorage` ile en yüksek skor (Best) saklanır.
- Zamanla artan hız ve rastgele engeller.

Geliştirme önerileri

- Ses efektleri ve müzik ekle
- Engeller ve güçlendiriciler çeşitlendir
- Daha gelişmiş kamera/partikül efektleri ekle

Dosyalar

- `index.html`: oyun sayfası ve üç.js CDN bağlantısı
- `style.css`: basit stiller
- `game.js`: Three.js oyun mantığı

Keyifli oynamalar!
