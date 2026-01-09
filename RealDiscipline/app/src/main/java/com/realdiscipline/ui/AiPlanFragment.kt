package com.realdiscipline.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.ai.client.generativeai.GenerativeModel
import com.realdiscipline.R
import kotlinx.coroutines.*

class AiPlanFragment : Fragment() {
    
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var etGoal: EditText
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnGenerate: Button
    private lateinit var tvPlan: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_plan, container, false)
        
        etAge = view.findViewById(R.id.et_age)
        etWeight = view.findViewById(R.id.et_weight)
        etHeight = view.findViewById(R.id.et_height)
        etGoal = view.findViewById(R.id.et_goal)
        spinnerDuration = view.findViewById(R.id.spinner_duration)
        btnGenerate = view.findViewById(R.id.btn_generate)
        tvPlan = view.findViewById(R.id.tv_plan)
        progressBar = view.findViewById(R.id.progress_bar)
        
        setupSpinner()
        
        btnGenerate.setOnClickListener {
            generateAiPlan()
        }
        
        return view
    }
    
    private fun setupSpinner() {
        val durations = arrayOf("1 Hafta", "2 Hafta", "1 Ay", "3 Ay", "6 Ay")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapter
        spinnerDuration.setSelection(2) // Default: 1 Ay
    }
    
    private fun generateAiPlan() {
        val age = etAge.text.toString()
        val weight = etWeight.text.toString()
        val height = etHeight.text.toString()
        val goal = etGoal.text.toString()
        val duration = spinnerDuration.selectedItem.toString()
        
        if (age.isEmpty() || weight.isEmpty() || goal.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        btnGenerate.isEnabled = false
        tvPlan.text = "AI plan oluşturuluyor..."
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val plan = withContext(Dispatchers.IO) {
                    generatePlanWithGemini(age, weight, height, goal, duration)
                }
                
                tvPlan.text = plan
                
            } catch (e: Exception) {
                tvPlan.text = "Plan oluşturma hatası: ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
            }
        }
    }
    
    private suspend fun generatePlanWithGemini(
        age: String,
        weight: String,
        height: String,
        goal: String,
        duration: String
    ): String {
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "YOUR_API_KEY_HERE" // TODO: Add your Gemini API key
        )
        
        val prompt = """
            Kişisel Bilgiler:
            - Yaş: $age
            - Kilo: $weight kg
            - Boy: $height cm
            - Hedef: $goal
            - Süre: $duration
            
            Lütfen bu kişi için detaylı bir disiplin ve habit tracking planı oluştur:
            
            1. GÜNLÜK RUTINLER (Daily Habits):
               - Sabah rutini
               - Akşam rutini
               - Egzersiz programı
               - Beslenme önerileri
            
            2. HAFTALIK HEDEFLER:
               - Her hafta için spesifik hedefler
               - Takip edilecek metrikler
            
            3. AYLIK İLERLEME:
               - Aylık kilometre taşları
               - Başarı kriterleri
            
            4. TODO LİSTESİ:
               - Hemen yapılacaklar
               - Bu hafta yapılacaklar
               - Bu ay yapılacaklar
            
            Planı Türkçe, detaylı ve motive edici bir şekilde yaz.
        """.trimIndent()
        
        val response = generativeModel.generateContent(prompt)
        return response.text ?: "Plan oluşturulamadı"
    }
}
