package com.realdiscipline.ai

import com.realdiscipline.data.Todo
import kotlin.math.pow
import java.text.SimpleDateFormat
import java.util.*

data class DisciplinePlan(
    val planText: String,
    val todos: List<Todo>,
    val summary: PlanSummary
)

data class PlanSummary(
    val age: Int,
    val weight: Float,
    val height: Float,
    val bmi: Float,
    val bmiCategory: String,
    val goalType: GoalType,
    val goal: String,
    val weeks: Int,
    val dailyCalories: Int,
    val dailyProtein: Int,
    val exerciseDaysPerWeek: Int
)

enum class GoalType {
    WEIGHT_LOSS,
    MUSCLE_GAIN,
    FITNESS,
    ROUTINE,
    STUDY,
    GENERAL
}

object LocalAI {
    
    fun generateDisciplinePlan(
        age: Int,
        weight: Float,
        height: Float,
        goal: String,
        duration: String
    ): DisciplinePlan {
            val bmi = calculateBMI(weight, height)
            val bmiCategory = getBMICategory(bmi)
            val goalType = analyzeGoal(goal)
            val weeks = parseDuration(duration)
            
            val calories = calculateDailyCalories(age, weight, height, goalType)
            val protein = calculateDailyProtein(weight, goalType)
            val exerciseDays = getExerciseDaysPerWeek(goalType)
            
            val summary = PlanSummary(
                age, weight, height, bmi, bmiCategory, goalType, goal, weeks,
                calories, protein, exerciseDays
            )
            
            val planText = buildPlan(age, weight, height, bmi, goalType, goal, weeks, calories, protein)
            val todos = generateTodoList(goalType, summary)
            
            return DisciplinePlan(planText, todos, summary)
        }
        
        private fun calculateBMI(weight: Float, height: Float): Float {
            val heightInMeters = height / 100
            return weight / (heightInMeters.pow(2))
        }
        
        private fun analyzeGoal(goal: String): GoalType {
            val lowerGoal = goal.lowercase()
            return when {
                lowerGoal.contains("kilo ver") || lowerGoal.contains("zayıfla") -> GoalType.WEIGHT_LOSS
                lowerGoal.contains("kas") || lowerGoal.contains("kitle") -> GoalType.MUSCLE_GAIN
                lowerGoal.contains("form") || lowerGoal.contains("fit") -> GoalType.FITNESS
                lowerGoal.contains("uyku") || lowerGoal.contains("düzen") -> GoalType.ROUTINE
                lowerGoal.contains("ders") || lowerGoal.contains("çalış") -> GoalType.STUDY
                else -> GoalType.GENERAL
            }
        }
        
        private fun parseDuration(duration: String): Int {
            return when {
                duration.contains("1 Hafta") -> 1
                duration.contains("2 Hafta") -> 2
                duration.contains("1 Ay") -> 4
                duration.contains("3 Ay") -> 12
                duration.contains("6 Ay") -> 24
                else -> 4
            }
        }
        
        private fun calculateDailyCalories(age: Int, weight: Float, height: Float, goalType: GoalType): Int {
            // BMR calculation (Mifflin-St Jeor)
            val bmr = 10 * weight + 6.25 * height - 5 * age + 5
            val tdee = (bmr * 1.55).toInt() // Moderate activity
            
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> (tdee * 0.8).toInt() // -20% deficit
                GoalType.MUSCLE_GAIN -> (tdee * 1.1).toInt() // +10% surplus
                else -> tdee
            }
        }
        
        private fun calculateDailyProtein(weight: Float, goalType: GoalType): Int {
            val multiplier = when (goalType) {
                GoalType.WEIGHT_LOSS -> 1.6
                GoalType.MUSCLE_GAIN -> 2.0
                GoalType.FITNESS -> 1.4
                else -> 1.0
            }
            return (weight * multiplier).toInt()
        }
        
        private fun getExerciseDaysPerWeek(goalType: GoalType): Int {
            return when (goalType) {
                GoalType.WEIGHT_LOSS, GoalType.MUSCLE_GAIN -> 6
                GoalType.FITNESS -> 5
                GoalType.ROUTINE -> 3
                else -> 4
            }
        }
        
        private fun buildPlan(
            age: Int,
            weight: Float,
            height: Float,
            bmi: Float,
            goalType: GoalType,
            goal: String,
            weeks: Int,
            calories: Int,
            protein: Int
        ): String {
            val sb = StringBuilder()
            
            // Header
            sb.append("🎯 KİŞİSELLEŞTİRİLMİŞ DİSİPLİN PLANI\n\n")
            sb.append("📊 Profil Analizi:\n")
            sb.append("• Yaş: $age\n")
            sb.append("• Kilo: $weight kg\n")
            sb.append("• Boy: $height cm\n")
            sb.append("• BMI: ${String.format("%.1f", bmi)} ${getBMICategory(bmi)}\n")
            sb.append("• Hedef: $goal\n")
            sb.append("• Süre: $weeks hafta\n\n")
            
            // Daily Routines
            sb.append("═══════════════════════════════\n")
            sb.append("📅 GÜNLÜK RUTİNLER\n")
            sb.append("═══════════════════════════════\n\n")
            
            sb.append(getDailyRoutine(goalType, age, calories, protein))
            
            // Weekly Goals
            sb.append("\n═══════════════════════════════\n")
            sb.append("📈 HAFTALIK HEDEFLER\n")
            sb.append("═══════════════════════════════\n\n")
            
            sb.append(getWeeklyGoals(goalType, weeks, weight))
            
            // Monthly Progress
            sb.append("\n═══════════════════════════════\n")
            sb.append("🎯 AYLIK İLERLEME TAKİBİ\n")
            sb.append("═══════════════════════════════\n\n")
            
            sb.append(getMonthlyProgress(goalType, weeks))
            
            // Todo List
            sb.append("\n═══════════════════════════════\n")
            sb.append("✅ HEMEN BAŞLAMAK İÇİN TO-DO LİST\n")
            sb.append("═══════════════════════════════\n\n")
            
            sb.append(getTodoList(goalType))
            
            // Motivation
            sb.append("\n💪 MOTİVASYON:\n")
            sb.append(getMotivation(goalType))
            
            return sb.toString()
        }
        
        private fun getBMICategory(bmi: Float): String {
            return when {
                bmi < 18.5 -> "(Zayıf)"
                bmi < 25 -> "(Normal)"
                bmi < 30 -> "(Fazla Kilolu)"
                else -> "(Obez)"
            }
        }
        
        private fun getDailyRoutine(goalType: GoalType, age: Int, calories: Int, protein: Int): String {
            val sb = StringBuilder()
            
            // Morning Routine
            sb.append("🌅 SABAH RUTİNİ:\n")
            sb.append("• 06:00-06:30 - Uyanış ve 2 bardak su\n")
            sb.append("• 06:30-07:00 - ${getMorningExercise(goalType)}\n")
            sb.append("• 07:00-07:30 - Duş ve hazırlık\n")
            sb.append("• 07:30-08:00 - ${getBreakfast(goalType)}\n\n")
            
            // Exercise
            sb.append("💪 EGZERSİZ PROGRAMI:\n")
            sb.append(getExercisePlan(goalType, age))
            sb.append("\n")
            
            // Nutrition
            sb.append("🥗 BESLENME ÖNERİLERİ:\n")
            sb.append("• Günlük kalori: ~$calories kcal\n")
            sb.append("• Protein: ${protein}g/gün\n")
            when (goalType) {
                GoalType.WEIGHT_LOSS -> {
                    sb.append("• Kahvaltı: Yumurta + sebze + tam tahıl\n")
                    sb.append("• Ara: Meyve veya kuruyemiş (az)\n")
                    sb.append("• Öğle: Izgara tavuk/balık + salata + bulgur\n")
                    sb.append("• Ara: Yoğurt veya protein bar\n")
                    sb.append("• Akşam: Sebze ağırlıklı + az karbonhidrat\n")
                    sb.append("• KAÇIN: Şeker, fast food, işlenmiş gıda\n")
                }
                GoalType.MUSCLE_GAIN -> {
                    sb.append("• Kahvaltı: 3-4 yumurta + yulaf + muz\n")
                    sb.append("• Ara: Protein shake + kuruyemiş\n")
                    sb.append("• Öğle: 200g et + pilav + sebze\n")
                    sb.append("• Ara: Yoğurt + bal + fıstık ezmesi\n")
                    sb.append("• Akşam: Balık/tavuk + patates + salata\n")
                    sb.append("• Gece: Kazeın protein veya süt\n")
                }
                GoalType.FITNESS -> {
                    sb.append("• Dengeli öğünler: 40% karb, 30% protein, 30% yağ\n")
                    sb.append("• Bol su: 2.5-3L/gün\n")
                    sb.append("• Çeşitli sebze ve meyve\n")
                    sb.append("• İşlenmiş gıdalardan kaçın\n")
                }
                else -> {
                    sb.append("• Düzenli 3 ana + 2 ara öğün\n")
                    sb.append("• Su: 2-3L/gün\n")
                    sb.append("• Dengeli beslenme\n")
                    sb.append("• Az işlenmiş, doğal gıdalar\n")
                }
            }
            sb.append("\n")
            
            // Evening Routine
            sb.append("🌙 AKŞAM RUTİNİ:\n")
            sb.append("• 21:00-21:30 - Günlük değerlendirme ve yarın planı\n")
            sb.append("• 21:30-22:00 - Gevşeme (meditasyon, okuma)\n")
            sb.append("• 22:00-22:30 - Uyku hazırlığı\n")
            sb.append("• 22:30 - UYKU (minimum 7-8 saat)\n")
            
            return sb.toString()
        }
        
        private fun getMorningExercise(goalType: GoalType): String {
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> "30 dk hızlı yürüyüş veya koşu"
                GoalType.MUSCLE_GAIN -> "20 dk esnetme ve mobility"
                GoalType.FITNESS -> "20 dk karışık cardio"
                else -> "15 dk yoga veya esnetme"
            }
        }
        
        private fun getBreakfast(goalType: GoalType): String {
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> "Protein ağırlıklı kahvaltı (yumurta, peynir, sebze)"
                GoalType.MUSCLE_GAIN -> "Yüksek protein kahvaltı (yumurta, yulaf, meyve)"
                else -> "Dengeli kahvaltı (tam tahıl, protein, meyve)"
            }
        }
        
        private fun getExercisePlan(goalType: GoalType, age: Int): String {
            val intensity = if (age < 30) "Yüksek" else if (age < 50) "Orta" else "Düşük-Orta"
            
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> """
                    • Pazartesi: 45 dk cardio (koşu/bisiklet)
                    • Salı: 30 dk strength training (üst vücut)
                    • Çarşamba: 45 dk cardio (yüzme/eliptik)
                    • Perşembe: 30 dk strength training (alt vücut)
                    • Cuma: 45 dk HIIT antrenmanı
                    • Cumartesi: 60 dk açık hava aktivitesi
                    • Pazar: Aktif dinlenme (yoga, yürüyüş)
                    
                    Yoğunluk: $intensity
                    Kalori hedefi: -500 kcal/gün
                """.trimIndent()
                
                GoalType.MUSCLE_GAIN -> """
                    • Pazartesi: Göğüs-Triceps (8-12 tekrar, 4 set)
                    • Salı: Sırt-Biceps (8-12 tekrar, 4 set)
                    • Çarşamba: Dinlenme veya hafif cardio
                    • Perşembe: Omuz-Karın (8-12 tekrar, 4 set)
                    • Cuma: Bacak (8-12 tekrar, 4 set)
                    • Cumartesi: Full body (hafif ağırlıklar)
                    • Pazar: Dinlenme
                    
                    Yoğunluk: $intensity
                    Protein: ${String.format("%.0f", 1.8 * 70)}g/gün
                """.trimIndent()
                
                GoalType.FITNESS -> """
                    • Pazartesi: 30 dk cardio + 20 dk core
                    • Salı: 40 dk functional training
                    • Çarşamba: 30 dk HIIT
                    • Perşembe: 40 dk yoga/pilates
                    • Cuma: 30 dk cardio + 20 dk strength
                    • Cumartesi: Spor/outdoor aktivite
                    • Pazar: Aktif dinlenme
                """.trimIndent()
                
                else -> """
                    • Haftada 3-4 gün 30 dk aktivite
                    • Günlük 10,000 adım hedefi
                    • Hafta sonu açık hava aktivitesi
                """.trimIndent()
            }
        }
        
        private fun getNutritionPlan(goalType: GoalType): String {
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> """
                    • Kalori: -20% açık (yaklaşık 1600-1800 kcal)
                    • Protein: 1.2g/kg vücut ağırlığı
                    • Öğün sayısı: 5-6 küçük öğün
                    • Su: Minimum 2.5 litre/gün
                    • Şeker/işlenmiş gıda: Minimum
                    • Sebze: Her öğünde mutlaka
                """.trimIndent()
                
                GoalType.MUSCLE_GAIN -> """
                    • Kalori: +10-15% fazla (2500-2800 kcal)
                    • Protein: 1.8-2g/kg vücut ağırlığı
                    • Karbonhidrat: Antrenman öncesi/sonrası
                    • Öğün sayısı: 6 öğün (3 ana, 3 ara)
                    • Supplement: Whey protein, creatine (isteğe bağlı)
                """.trimIndent()
                
                else -> """
                    • Dengeli beslenme (40% karb, 30% protein, 30% yağ)
                    • Öğün sayısı: 3 ana + 2 ara öğün
                    • Su: 2-3 litre/gün
                    • Çeşitli sebze ve meyve
                    • İşlenmiş gıdalardan kaçın
                """.trimIndent()
            }
        }
        
        private fun getWeeklyGoals(goalType: GoalType, totalWeeks: Int, currentWeight: Float): String {
            val sb = StringBuilder()
            val weeksToShow = minOf(4, totalWeeks)
            
            for (week in 1..weeksToShow) {
                sb.append("HAFTA $week:\n")
                
                when (goalType) {
                    GoalType.WEIGHT_LOSS -> {
                        val targetWeight = currentWeight - (week * 0.5f)
                        sb.append("• Hedef kilo: ${String.format("%.1f", targetWeight)} kg\n")
                        sb.append("• Cardio: ${150 + week * 30} dakika/hafta\n")
                        sb.append("• Günlük adım: ${8000 + week * 500}\n")
                    }
                    GoalType.MUSCLE_GAIN -> {
                        sb.append("• Ağırlık artışı: +${week * 5}%\n")
                        sb.append("• Protein: ${week * 10}g ekstra\n")
                        sb.append("• Yeni egzersiz: ${week} hareket ekle\n")
                    }
                    else -> {
                        sb.append("• Aktivite: ${20 + week * 5} dk/gün\n")
                        sb.append("• Habit streak: ${week * 7} gün\n")
                    }
                }
                sb.append("\n")
            }
            
            return sb.toString()
        }
        
        private fun getMonthlyProgress(goalType: GoalType, totalWeeks: Int): String {
            val months = (totalWeeks + 3) / 4
            val sb = StringBuilder()
            
            for (month in 1..minOf(3, months)) {
                sb.append("AY $month:\n")
                
                when (goalType) {
                    GoalType.WEIGHT_LOSS -> {
                        sb.append("• ${month * 2}-${month * 3} kg kayıp\n")
                        sb.append("• Beden ölçüsü: -${month * 2}cm\n")
                        sb.append("• Enerji seviyesi: +${month * 20}%\n")
                    }
                    GoalType.MUSCLE_GAIN -> {
                        sb.append("• +${month * 1.5}kg kas kütlesi\n")
                        sb.append("• Kuvvet artışı: +${month * 15}%\n")
                        sb.append("• Hacim artışı: +${month}cm\n")
                    }
                    else -> {
                        sb.append("• Habit completion: ${80 + month * 5}%\n")
                        sb.append("• Disiplin skoru: ${month * 25}/100\n")
                    }
                }
                sb.append("\n")
            }
            
            return sb.toString()
        }
        
        private fun getTodoList(goalType: GoalType): String {
            val common = """
                BUGÜN:
                □ Uygulamaya habit'lerini ekle
                □ İlk sabah rutinini tamamla
                □ Su tüketimini takip et
                □ Akşam değerlendirmesini yap
                
                BU HAFTA:
                □ Egzersiz programını oluştur
                □ Market alışverişi yap (sağlıklı)
                □ Uyku saatlerini düzenle
                □ Progress fotoğrafı çek
                
                BU AY:
                □ Vücut ölçülerini al
                □ Habit tracker'ı doldur
                □ Haftalık ilerlemeyi değerlendir
                □ Gerekirse planı güncelle
            """.trimIndent()
            
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> """
                    HEMEN YAPMALISIN:
                    □ Mutfağı temizle (şekerli/işlenmiş gıda)
                    □ Günlük kalori hedefini belirle
                    □ Kalori sayacı uygulama indir
                    □ Başlangıç kilosunu kaydet
                    
                    $common
                """.trimIndent()
                
                GoalType.MUSCLE_GAIN -> """
                    HEMEN YAPMALISIN:
                    □ Spor salonu kaydı yap
                    □ Protein tozu al (isteğe bağlı)
                    □ Başlangıç ölçülerini al
                    □ Antrenman programını yazdır
                    
                    $common
                """.trimIndent()
                
                else -> common
            }
        }
        
        private fun getMotivation(goalType: GoalType): String {
            return when (goalType) {
                GoalType.WEIGHT_LOSS -> 
                    "\"Her gün küçük adımlar, büyük değişimler yaratır. Sen yapabilirsin! 💪\""
                GoalType.MUSCLE_GAIN -> 
                    "\"Kas kazanmak sabır işidir. Tutarlılık, motivasyondan önemlidir! 🏋️\""
                GoalType.FITNESS -> 
                    "\"Fit olmak bir hedef değil, yaşam tarzıdır. Disiplinini koru! 🎯\""
                GoalType.ROUTINE -> 
                    "\"Düzenli yaşam, mutlu yaşamdır. Her gün biraz daha iyi! ⭐\""
                GoalType.STUDY -> 
                    "\"Öğrenmek bir maraton, sprint değil. Devamlılık anahtardır! 📚\""
                else -> 
                    "\"Başarı = Disiplin × Tutarlılık. Yolculuğunu sev! 🚀\""
            }
        }
        
        // Todo List Generation
        private fun generateTodoList(goalType: GoalType, summary: PlanSummary): List<Todo> {
            val todos = mutableListOf<Todo>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val today = Calendar.getInstance()
            
            // BUGÜN - High Priority
            todos.add(Todo(
                title = "Başlangıç ölçülerini kaydet",
                description = "Kilo: ${summary.weight}kg, Boy: ${summary.height}cm, BMI: ${String.format("%.1f", summary.bmi)}",
                category = "Setup",
                priority = 3,
                dueDate = dateFormat.format(today.time),
                dueTime = "09:00",
                repeatType = "None",
                fromAiPlan = true
            ))
            
            todos.add(Todo(
                title = "İlk sabah rutinini tamamla",
                description = "2 bardak su + ${getMorningExercise(goalType)}",
                category = "Morning",
                priority = 3,
                dueDate = dateFormat.format(today.time),
                dueTime = "06:30",
                repeatType = "Daily",
                fromAiPlan = true
            ))
            
            todos.add(Todo(
                title = "Günlük kalori hedefini belirle",
                description = "Hedef: ${summary.dailyCalories} kcal, Protein: ${summary.dailyProtein}g",
                category = "Nutrition",
                priority = 3,
                dueDate = dateFormat.format(today.time),
                dueTime = "08:00",
                repeatType = "None",
                fromAiPlan = true
            ))
            
            // Goal-specific today tasks
            when (goalType) {
                GoalType.WEIGHT_LOSS -> {
                    todos.add(Todo(
                        title = "Mutfağı temizle (şeker/işlenmiş gıda)",
                        description = "Sağlıksız gıdaları ortadan kaldır",
                        category = "Setup",
                        priority = 3,
                        dueDate = dateFormat.format(today.time),
                        fromAiPlan = true
                    ))
                    todos.add(Todo(
                        title = "Kalori sayma uygulaması kur",
                        description = "MyFitnessPal veya benzeri",
                        category = "Setup",
                        priority = 2,
                        dueDate = dateFormat.format(today.time),
                        fromAiPlan = true
                    ))
                }
                GoalType.MUSCLE_GAIN -> {
                    todos.add(Todo(
                        title = "Spor salonu araştır/kayıt ol",
                        description = "En yakın uygun spor salonunu bul",
                        category = "Setup",
                        priority = 3,
                        dueDate = dateFormat.format(today.time),
                        fromAiPlan = true
                    ))
                    todos.add(Todo(
                        title = "Protein tozu/besin desteği araştır",
                        description = "İhtiyaçlarına uygun ürünleri belirle (isteğe bağlı)",
                        category = "Nutrition",
                        priority = 2,
                        dueDate = dateFormat.format(today.time),
                        fromAiPlan = true
                    ))
                }
                else -> {
                    todos.add(Todo(
                        title = "Hedeflerini not al ve görünür yere as",
                        description = "Hedef: ${summary.goal}",
                        category = "Setup",
                        priority = 2,
                        dueDate = dateFormat.format(today.time),
                        fromAiPlan = true
                    ))
                }
            }
            
            // Daily recurring tasks
            todos.add(Todo(
                title = "Su tüketimini takip et (2-3L)",
                description = "Her saat 1 bardak su",
                category = "Health",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                dueTime = "10:00",
                repeatType = "Daily",
                fromAiPlan = true
            ))
            
            todos.add(Todo(
                title = "Akşam rutini ve günlük değerlendirme",
                description = "Bugünü değerlendir, yarını planla",
                category = "Evening",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                dueTime = "21:00",
                repeatType = "Daily",
                fromAiPlan = true
            ))
            
            todos.add(Todo(
                title = "22:30'da uykuya hazırlan",
                description = "7-8 saat uyku için",
                category = "Sleep",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                dueTime = "22:30",
                repeatType = "Daily",
                fromAiPlan = true
            ))
            
            // BU HAFTA
            today.add(Calendar.DAY_OF_MONTH, 1)
            todos.add(Todo(
                title = "Sağlıklı market alışverişi yap",
                description = "Sebze, meyve, protein kaynakları, tam tahıl",
                category = "Nutrition",
                priority = 3,
                dueDate = dateFormat.format(today.time),
                fromAiPlan = true
            ))
            
            today.add(Calendar.DAY_OF_MONTH, 1)
            todos.add(Todo(
                title = "Haftalık egzersiz programını oluştur",
                description = "${summary.exerciseDaysPerWeek} gün/hafta",
                category = "Exercise",
                priority = 3,
                dueDate = dateFormat.format(today.time),
                fromAiPlan = true
            ))
            
            today.add(Calendar.DAY_OF_MONTH, 2)
            todos.add(Todo(
                title = "İlk progress fotoğrafı çek",
                description = "Ön, yan ve arka açıdan",
                category = "Progress",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                fromAiPlan = true
            ))
            
            today.add(Calendar.DAY_OF_MONTH, 1)
            todos.add(Todo(
                title = "Uyku saatlerini düzenle",
                description = "22:30 yatış, 06:00 kalkış rutini oluştur",
                category = "Sleep",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                fromAiPlan = true
            ))
            
            // İLK HAFTA SONU
            today.add(Calendar.DAY_OF_MONTH, 2)
            todos.add(Todo(
                title = "Haftalık ilerleme değerlendirmesi",
                description = "Neler iyi gitti, neler geliştirilebilir?",
                category = "Progress",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                repeatType = "Weekly",
                repeatDays = "Sun",
                fromAiPlan = true
            ))
            
            // AYLIK
            today.add(Calendar.DAY_OF_MONTH, 23) // ~1 month
            todos.add(Todo(
                title = "Aylık vücut ölçülerini al",
                description = "Kilo, bel, göğüs, bacak ölçüleri",
                category = "Progress",
                priority = 2,
                dueDate = dateFormat.format(today.time),
                repeatType = "Monthly",
                fromAiPlan = true
            ))
            
            if (goalType == GoalType.WEIGHT_LOSS || goalType == GoalType.MUSCLE_GAIN) {
                today.add(Calendar.DAY_OF_MONTH, 0)
                todos.add(Todo(
                    title = "Progress fotoğrafı çek (Aylık)",
                    description = "Önceki fotoğraflarla karşılaştır",
                    category = "Progress",
                    priority = 2,
                    dueDate = dateFormat.format(today.time),
                    repeatType = "Monthly",
                    fromAiPlan = true
                ))
                
                todos.add(Todo(
                    title = "Planı gözden geçir ve güncelle",
                    description = "İhtiyaçlara göre kalori/egzersiz ayarla",
                    category = "Planning",
                    priority = 2,
                    dueDate = dateFormat.format(today.time),
                    repeatType = "Monthly",
                    fromAiPlan = true
                ))
            }
            
            return todos
        }
}
