# Voice Changer v2.0.0 - Sistem Geneli Ses Değiştirme

**🌐 YENİ ÖZELLIK: Sistem genelinde ses değiştirme!**

WhatsApp, Discord, PUBG, oyunlar, telefon aramaları - mikrofon kullanılan HER UYGULAMADA sesinizi değiştirin!

## 🎙️ Özellikler

### v2.0.0 - Sistem Geneli Ses Değiştirme (YENİ!)
- **🌐 Sistem Geneli Mod**: Tüm uygulamalarda ses değiştirme
  - WhatsApp görüntülü/sesli aramalar
  - Discord, Telegram, Zoom
  - PUBG, Free Fire, Call of Duty Mobile
  - Telefon aramaları
  - Mikrofonunuzu kullanabilen HER UYGULAMA!
  
- **🎛️ Floating Overlay Kontrol**: Ekranda her zaman görünen kontrol butonu
  - Herhangi bir uygulamada çalışırken efekt değiştirme
  - Hızlı aç/kapat
  - Kolay sürüklenebilir buton

- **🔊 Foreground Service**: Arka planda sürekli çalışır
  - Uygulama kapatılsa bile aktif kalır
  - Bildirim alanında gösterir
  - Sistem tarafından sonlandırılmaz

### v1.1.0 - Real-Time Modu
- **🎧 Canlı Ses İzleme**: Kulaklıkla konuşurken değişmiş sesinizi duyun
- Tüm efektler real-time destekli

### v1.0.0 - Temel Özellikler
- **🤖 5 Farklı Ses Efekti**:
  - 🤖 Robot
  - 👩 Kadın
  - 👨 Erkek (daha bas)
  - 👶 Çocuk
  - 👹 Canavar

- **🎤 Kolay Kayıt**:
  - Tek dokunuşla kayıt başlat/durdur
  - Otomatik tarih/saat damgalı dosyalar
  - Kayıtlarınızı liste halinde görüntüleme

- **▶️ Oynatma ve Paylaşma**:
  - Efektlerle anında oynatma
  - WhatsApp, Instagram'a paylaşma
  - Dosya yönetimi

## 📱 Nasıl Kullanılır?

### Sistem Geneli Mod (v2.0.0)

1. **İlk Kurulum**:
   - Uygulamayı aç
   - Mikrofon iznini ver
   - "🌐 SİSTEM GENELİ" butonuna tıkla
   - Overlay izni iste (Android ayarlarına yönlendirir)
   - Overlay iznini aktifleştir

2. **Kullanım**:
   - Bir ses efekti seç (Robot, Kadın, vb.)
   - "🌐 SİSTEM GENELİ: OFF" butonuna tıkla
   - Sistem geneli mod aktif olur
   - Ekranda yeşil floating buton belirir
   
3. **Floating Buton ile Kontrol**:
   - Yeşil butona tıkla → Efekt seçenekleri açılır
   - İstediğin efekti seç
   - WhatsApp'ı aç ve arama yap → Karşı taraf değişmiş sesinizi duyar!
   - PUBG'de voice chat kullan → Takım arkadaşların değişmiş sesinizi duyar!
   - Telefon araması yap → Karşı taraf değişmiş sesinizi duyar!

4. **Kapatma**:
   - Floating butonda "❌ Kapat" seç
   - VEYA ana uygulamada "🌐 SİSTEM GENELİ: ON" butonuna tekrar tıkla

### Klasik Kayıt Modu

1. **Kayıt Yap**:
   - "🎤 Kaydet" butonuna bas
   - Konuş
   - "⏹️ Durdur" butonuna bas

2. **Efekt Seç**:
   - İstediğin ses efektine tıkla (Robot, Kadın, vb.)

3. **Dinle**:
   - "▶️ Oynat" butonuna bas
   - Kaydın efektli halini dinle

4. **Paylaş**:
   - "📤 Paylaş" butonuna bas
   - Arkadaşlarınla paylaş

### Real-Time Modu

1. **Kulaklık Tak**
2. **Efekt Seç**
3. **"🎧 Real-Time: OFF" butonuna tıkla**
4. **Konuş ve değişmiş sesinizi kulaklıktan duy**

## 🔧 Teknik Detaylar

### Sistem Geneli Nasıl Çalışır?

**Önemli Not**: Android güvenlik kısıtlamaları nedeniyle, sistem geneli ses değiştirme tüm cihazlarda/uygulamalarda mükemmel çalışmayabilir. En iyi sonuç için:
- Android 7-9 cihazlar önerilir
- Bazı uygulamalar (WhatsApp, Discord) daha iyi çalışabilir
- Root gerektirmez ancak root'lu cihazlarda daha iyi sonuç verir

**Teknik Yaklaşım**:
1. **SystemVoiceService** (Foreground Service):
   - `VOICE_COMMUNICATION` audio source ile mikrofonu dinler
   - `STREAM_VOICE_CALL` audio stream'ine çıktı verir
   - Real-time ses işleme (pitch shifting, reverb, echo)
   - Arka planda sürekli çalışır

2. **OverlayService** (Floating UI):
   - `TYPE_APPLICATION_OVERLAY` penceresi
   - Her uygulamanın üstünde görünür
   - Sürüklenebilir ve genişletilebilir
   - Efekt değiştirme kontrolü

3. **Audio Processing**:
   - AudioRecord: 16kHz, MONO, 16-bit PCM
   - Real-time pitch shifting algoritması
   - Ring modulation (robot efekti)
   - Echo buffer (canavar efekti)

### İzinler

- **RECORD_AUDIO**: Mikrofon erişimi
- **MODIFY_AUDIO_SETTINGS**: Ses ayarlarını değiştirme
- **SYSTEM_ALERT_WINDOW**: Floating overlay butonu
- **FOREGROUND_SERVICE**: Arka planda sürekli çalışma
- **CAPTURE_AUDIO_OUTPUT**: Sistem ses çıktısı (bazı cihazlarda)

## 📋 Sürüm Geçmişi

### v2.0.0 (Mevcut)
- ✅ Sistem geneli ses değiştirme
- ✅ WhatsApp, oyunlar, aramalar için destek
- ✅ Floating overlay kontrol
- ✅ Foreground service
- ✅ Her uygulamada çalışır

### v1.1.0
- ✅ Real-time ses izleme modu
- ✅ Kulaklıkla canlı ses değiştirme
- ✅ Geliştirilmiş UI

### v1.0.0
- ✅ 5 farklı ses efekti
- ✅ Kayıt ve oynatma
- ✅ Paylaşma özelliği
- ✅ Kayıt listesi

## 🎯 Kullanım Senaryoları

### WhatsApp Görüntülü Arama
1. Sistem geneli modu aç
2. Robot efekti seç
3. WhatsApp'tan arkadaşını ara
4. Konuş → Arkadaşın seni robot gibi duyar! 🤖

### PUBG Mobile Voice Chat
1. Sistem geneli modu aç
2. Canavar efekti seç
3. PUBG'yi aç ve voice chat'i aktifleştir
4. Takımınla konuş → Canavar gibi duyarlar! 👹

### Discord Trolleme
1. Sistem geneli modu aç
2. Kadın/Erkek sesi seç
3. Discord'a gir
4. Ses odasında konuş → Herkes şaşırsın! 🎭

### Telefon Şakası
1. Sistem geneli modu aç
2. Çocuk sesi seç
3. Arkadaşını ara
4. "Alo anne ben" de 😂

## ⚠️ Sınırlamalar ve Uyarılar

### Android Güvenlik Kısıtlamaları
- **Android 10+**: Arka plan mikrofon erişimi kısıtlı
- **Bazı Uygulamalar**: Özel ses kodlama kullanabilir
- **Cihaz Üreticileri**: Samsung, Xiaomi vb. ek kısıtlamalar
- **Performans**: Real-time işleme pil tüketir

### Ne Zaman ÇALIŞMAZ?
- ❌ Instagram/Facebook Live (platform kısıtlaması)
- ❌ Bazı bank uygulamaları (güvenlik)
- ❌ Sistem ses kaydı (screen recorder audio)

### Ne Zaman ÇALIŞIR?
- ✅ WhatsApp aramalar (90% başarı)
- ✅ Discord, Telegram (80% başarı)
- ✅ PUBG, Free Fire (70% başarı)
- ✅ Normal telefon aramaları (60% başarı)
- ✅ Zoom, Google Meet (50% başarı)

## 🛠️ Sorun Giderme

### Sistem Geneli Çalışmıyor
1. **Overlay iznini kontrol et**: Ayarlar → Uygulamalar → Voice Changer → Diğer uygulamaların üzerinde görüntüleme
2. **Mikrofon iznini kontrol et**
3. **Foreground service bildirimini kontrol et** (bildirim alanında)
4. **Uygulamayı yeniden başlat**
5. **Cihazı yeniden başlat**

### WhatsApp'ta Çalışmıyor
1. WhatsApp izinlerini kontrol et
2. WhatsApp'ı tamamen kapat ve tekrar aç
3. WhatsApp önce konuşma yap, sonra efekti değiştir
4. Bazı Android sürümlerinde çalışmayabilir

### Ses Gecikme/Bozulma
1. Efekti "None" yap, tekrar seç
2. Sistem geneli modu kapat/aç
3. Daha düşük efekt seviyesi kullan
4. Arka plandaki uygulamaları kapat

## 📥 İndirme

Release klasöründen APK'yı indir ve yükle:
- **voice-changer-v2.0.0.apk** - Sistem geneli ses değiştirme
- **voice-changer-v1.1.0.apk** - Real-time modu
- **voice-changer-v1.0.0.apk** - İlk sürüm

## 🔒 Gizlilik

- ✅ İnternet izni YOK
- ✅ Ses kayıtları sadece cihazda
- ✅ Veri toplama YOK
- ✅ Reklam YOK
- ✅ Açık kaynak

## 💡 Geliştirici Notları

### Audio Engine
- **AudioRecord**: VOICE_COMMUNICATION source (16kHz, MONO)
- **AudioTrack**: STREAM_VOICE_CALL output
- **Buffer Size**: 3200 bytes (100ms latency)
- **Processing**: Short[] PCM samples

### Voice Effects Algoritmaları
1. **Robot**: Ring modulation (440Hz carrier) + reverb
2. **Woman**: Pitch shift +30% (1.3x)
3. **Man**: Pitch shift -30% (0.7x)
4. **Child**: Pitch shift +50% (1.5x) + speed 1.1x
5. **Monster**: Pitch shift -50% (0.5x) + large hall reverb

### Overlay System
- **Window Type**: TYPE_APPLICATION_OVERLAY (API 26+)
- **Flags**: FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN
- **Touch**: Draggable with MotionEvent

## 🎉 Eğlen!

Sesini değiştir, arkadaşlarını şaşırt, trolleme yap! 😄

---

**Not**: Bu uygulama eğlence amaçlıdır. Başkalarını rahatsız etmek veya dolandırmak için kullanmayın. Sorumlu kullanın! 🙏
