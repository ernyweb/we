package com.realdiscipline.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.realdiscipline.R
import com.realdiscipline.ai.LocalAI
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
        val ageStr = etAge.text.toString()
        val weightStr = etWeight.text.toString()
        val heightStr = etHeight.text.toString()
        val goal = etGoal.text.toString()
        val duration = spinnerDuration.selectedItem.toString()
        
        if (ageStr.isEmpty() || weightStr.isEmpty() || goal.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }
        
        val age = ageStr.toIntOrNull() ?: 0
        val weight = weightStr.toFloatOrNull() ?: 0f
        val height = heightStr.toFloatOrNull() ?: 0f
        
        if (age <= 0 || weight <= 0) {
            Toast.makeText(requireContext(), "Lütfen geçerli değerler girin", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        btnGenerate.isEnabled = false
        tvPlan.text = "🤖 Kişiselleştirilmiş planınız oluşturuluyor..."
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val plan = withContext(Dispatchers.IO) {
                    // Simulate AI thinking time
                    delay(1500)
                    LocalAI.generateDisciplinePlan(age, weight, height, goal, duration)
                }
                
                tvPlan.text = plan
                Toast.makeText(requireContext(), "✅ Plan başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                tvPlan.text = "❌ Plan oluşturma hatası: ${e.message}"
                Toast.makeText(requireContext(), "Hata oluştu", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
            }
        }
    }
}
