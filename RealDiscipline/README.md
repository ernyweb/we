# Real Discipline - Habit Tracker & AI Planner 🎯

**Disiplinli yaşam için akıllı asistanınız!**

## 🌟 Özellikler

### 📋 Habit Tracking
- Günlük/haftalık habit takibi
- Kategori bazlı organize (Health, Fitness, Study, Work)
- Streak tracking (kaç gün üst üste)
- Habit geçmişi ve istatistikler

### ✅ Smart To-Do List
- Öncelik bazlı görev yönetimi (Low/Medium/High)
- Kategori ve tarih bazlı filtreleme
- Günlük/haftalık/aylık görevler
- Tamamlanma takibi

### 📊 Progress Charts
- Günlük/haftalık/aylık ilerleme grafikleri
- Habit completion rates
- Streak heatmap
- Kategori bazlı analiz

### 🤖 AI Discipline Planner
**Google Gemini AI ile otomatik plan oluşturma:**

#### Girilen Bilgiler:
- Yaş
- Kilo
- Boy
- Hedef (örn: "10 kilo vermek", "Kas yapmak", "Düzenli uyku")
- Plan süresi (1 hafta - 6 ay)

#### AI Tarafından Oluşturulan:
1. **Günlük Rutinler:**
   - Sabah rutini (uyanma, kahvaltı, egzersiz)
   - Akşam rutini (yemek, gevşeme, uyku)
   - Egzersiz programı
   - Beslenme önerileri

2. **Haftalık Hedefler:**
   - Her hafta için spesifik hedefler
   - Takip edilecek metrikler (kilo, kas kütlesi, uyku saati)

3. **Aylık İlerleme:**
   - Aylık kilometre taşları
   - Başarı kriterleri

4. **Otomatik To-Do List:**
   - Hemen yapılacaklar
   - Bu hafta yapılacaklar
   - Bu ay yapılacaklar

### 🔔 Günlük Hatırlatmalar
- Habit reminder notifications
- To-do deadline alerts
- Motivasyon mesajları

## 🛠️ Teknolojiler

- **Kotlin** - Modern Android development
- **Room Database** - Local data persistence
- **Coroutines** - Async operations
- **Google Gemini AI** - AI plan generation
- **MPAndroidChart** - Beautiful charts
- **WorkManager** - Background notifications
- **Material Design 3** - Modern UI

## 📱 Kurulum

1. Gemini API Key alın: https://makersuite.google.com/app/apikey
2. `AiPlanFragment.kt` dosyasında API key'i güncelleyin:
   ```kotlin
   apiKey = "YOUR_API_KEY_HERE"
   ```
3. Build & Run!

## 🎯 Kullanım

### 1. AI Plan Oluşturma
- AI Plan sekmesine git
- Yaş, kilo, boy ve hedefini gir
- Plan süresini seç (1 hafta - 6 ay)
- "AI Plan Oluştur" butonuna bas
- AI tarafından oluşturulan detaylı planı gör

### 2. Habit Tracking
- Habits sekmesinde yeni habit ekle
- Her gün tamamladığın habitlari işaretle
- Streak'ini koru!

### 3. To-Do Management
- To-Do sekmesinde görev ekle
- Öncelik ve deadline belirle
- Tamamlananları işaretle

### 4. Progress Monitoring
- Progress sekmesinde grafiklerle ilerlemeyi gör
- Haftalık/aylık analiz

## 🚀 Özellik Roadmap

- [ ] Habit ve Todo CRUD operations
- [ ] Room Database integration
- [ ] Charts implementation
- [ ] Notification system
- [ ] Widget support
- [ ] Cloud backup
- [ ] Social features (arkadaşlarla yarış)

## 📄 Lisans

MIT License - Özgürce kullan, düzenle, paylaş!

---

**Disiplinli yaşam için hazır mısın? 💪**
