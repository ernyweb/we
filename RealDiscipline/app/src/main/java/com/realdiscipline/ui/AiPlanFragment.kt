package com.realdiscipline.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.realdiscipline.R
import com.realdiscipline.ai.DisciplinePlan
import com.realdiscipline.ai.LocalAI
import com.realdiscipline.data.DisciplineDatabase
import kotlinx.coroutines.*

class AiPlanFragment : Fragment() {
    
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var etGoal: EditText
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnGenerate: Button
    private lateinit var btnApplyPlan: Button
    private lateinit var tvPlan: TextView
    private lateinit var progressBar: ProgressBar
    
    private var currentPlan: DisciplinePlan? = null
    private lateinit var db: DisciplineDatabase
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_plan, container, false)
        
        db = DisciplineDatabase.getDatabase(requireContext())
        
        etAge = view.findViewById(R.id.et_age)
        etWeight = view.findViewById(R.id.et_weight)
        etHeight = view.findViewById(R.id.et_height)
        etGoal = view.findViewById(R.id.et_goal)
        spinnerDuration = view.findViewById(R.id.spinner_duration)
        btnGenerate = view.findViewById(R.id.btn_generate)
        btnApplyPlan = view.findViewById(R.id.btn_apply_plan)
        tvPlan = view.findViewById(R.id.tv_plan)
        progressBar = view.findViewById(R.id.progress_bar)
        
        setupSpinner()
        
        btnGenerate.setOnClickListener {
            generateAiPlan()
        }
        
        btnApplyPlan.setOnClickListener {
            applyPlanToTodos()
        }
        
        btnApplyPlan.visibility = View.GONE
        
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
        btnApplyPlan.visibility = View.GONE
        tvPlan.text = "🤖 Kişiselleştirilmiş planınız oluşturuluyor..."
        
        lifecycleScope.launch {
            try {
                val plan = withContext(Dispatchers.IO) {
                    // Simulate AI thinking time
                    delay(1500)
                    LocalAI.generateDisciplinePlan(age, weight, height, goal, duration)
                }
                
                currentPlan = plan
                tvPlan.text = plan.planText
                btnApplyPlan.visibility = View.VISIBLE
                
                // Save profile
                withContext(Dispatchers.IO) {
                    val profile = com.realdiscipline.data.UserProfile(
                        age = age,
                        weight = weight,
                        height = height,
                        goal = goal,
                        aiPlanGenerated = true,
                        lastPlanUpdate = System.currentTimeMillis()
                    )
                    db.userProfileDao().insertProfile(profile)
                }
                
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
    
    private fun applyPlanToTodos() {
        val plan = currentPlan ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("🎯 Planı Uygula")
            .setMessage(
                """
                Bu planı uygulamak ${plan.todos.size} adet görev oluşturacak:
                
                • Günlük rutinler (sabah/akşam)
                • Beslenme takibi
                • Egzersiz programı
                • İlerleme takibi
                
                Eski AI planından gelen görevler silinecek ve yenileriyle değiştirilecek.
                
                Devam edilsin mi?
                """.trimIndent()
            )
            .setPositiveButton("Evet, Uygula") { _, _ ->
                applyPlan(plan)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun applyPlan(plan: DisciplinePlan) {
        progressBar.visibility = View.VISIBLE
        btnApplyPlan.isEnabled = false
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Delete old AI plan todos
                    db.todoDao().deleteAllAiPlanTodos()
                    
                    // Insert new todos
                    db.todoDao().insertTodos(plan.todos)
                }
                
                Toast.makeText(
                    requireContext(),
                    "✅ ${plan.todos.size} görev oluşturuldu! To-Do sekmesine git.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Show success dialog with summary
                showSuccessDialog(plan)
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
                btnApplyPlan.isEnabled = true
            }
        }
    }
    
    private fun showSuccessDialog(plan: DisciplinePlan) {
        val summary = plan.summary
        val message = """
            🎯 Plan Özeti:
            
            📊 Bilgileriniz:
            • Yaş: ${summary.age}
            • BMI: ${String.format("%.1f", summary.bmi)} (${summary.bmiCategory})
            • Hedef: ${summary.goal}
            • Süre: ${summary.weeks} hafta
            
            📅 Günlük Program:
            • Kalori: ${summary.dailyCalories} kcal
            • Protein: ${summary.dailyProtein}g
            • Egzersiz: ${summary.exerciseDaysPerWeek} gün/hafta
            
            ✅ ${plan.todos.size} görev oluşturuldu!
            
            Şimdi To-Do sekmesine gidip görevleri kontrol et!
        """.trimIndent()
        
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Plan Uygulandı!")
            .setMessage(message)
            .setPositiveButton("To-Do'ya Git") { _, _ ->
                // Switch to todo tab
                (requireActivity() as? com.realdiscipline.MainActivity)?.switchToTodoTab()
            }
            .setNegativeButton("Tamam", null)
            .show()
    }
}

