package com.realdiscipline.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.realdiscipline.R
import com.realdiscipline.data.DisciplineDatabase
import com.realdiscipline.data.Todo
import com.realdiscipline.data.TodoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class HabitsFragment : Fragment() {
    
    private var db: DisciplineDatabase? = null
    private var habitDao: com.realdiscipline.data.HabitDao? = null
    private var adapter: HabitAdapter? = null
    
    private var rvHabits: RecyclerView? = null
    private var fabAddHabit: FloatingActionButton? = null
    private var tvTotalHabits: TextView? = null
    private var tvTodayCompleted: TextView? = null
    private var emptyState: View? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val database = DisciplineDatabase.getDatabase(requireContext())
        db = database
        habitDao = database.habitDao()
        
        initViews(view)
        setupRecyclerView()
        setupFab()
        observeHabits()
    }
    
    private fun initViews(view: View) {
        rvHabits = view.findViewById(R.id.rvHabits)
        fabAddHabit = view.findViewById(R.id.fabAddHabit)
        tvTotalHabits = view.findViewById(R.id.tvTotalHabits)
        tvTodayCompleted = view.findViewById(R.id.tvTodayCompleted)
        emptyState = view.findViewById(R.id.emptyState)
    }
    
    private fun setupRecyclerView() {
        adapter = HabitAdapter(
            onHabitClick = { habit ->
                showHabitDialog(habit)
            },
            onHabitDelete = { habit ->
                deleteHabit(habit)
            },
            onMarkToday = { habit ->
                markTodayComplete(habit)
            }
        )
        
        rvHabits?.adapter = adapter
        rvHabits?.layoutManager = LinearLayoutManager(context)
    }
    
    private fun setupFab() {
        fabAddHabit?.setOnClickListener {
            showHabitDialog(null)
        }
    }
    
    private fun observeHabits() {
        val dao = habitDao ?: return
        
        dao.getAllActiveHabits().observe(viewLifecycleOwner) { habits ->
            lifecycleScope.launch {
                val habitsWithStats = mutableListOf<HabitWithStats>()
                val today = getToday()
                
                for (habit in habits) {
                    val completedDays = withContext(Dispatchers.IO) {
                        dao.getCompletedDaysCount(habit.id)
                    }
                    val streak = withContext(Dispatchers.IO) {
                        calculateStreak(habit.id, dao)
                    }
                    val completedToday = withContext(Dispatchers.IO) {
                        dao.getLogForDate(habit.id, today) != null
                    }
                    
                    habitsWithStats.add(
                        HabitWithStats(habit, streak, completedDays, completedToday)
                    )
                }
                
                adapter?.submitList(habitsWithStats)
                emptyState?.visibility = if (habits.isEmpty()) View.VISIBLE else View.GONE
                updateStats(habitsWithStats)
            }
        }
    }
    
    private fun updateStats(habits: List<HabitWithStats>) {
        tvTotalHabits?.text = habits.size.toString()
        val todayCompleted = habits.count { it.completedToday }
        tvTodayCompleted?.text = "$todayCompleted/${habits.size}"
    }
    
    private suspend fun calculateStreak(habitId: Long, dao: com.realdiscipline.data.HabitDao): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        
        for (i in 0 until 365) {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.time)
            val log = dao.getLogForDate(habitId, date)
            
            if (log != null && log.completed) {
                streak++
            } else {
                break
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return streak
    }
    
    private fun getToday(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
    }
    
    private fun markTodayComplete(habit: com.realdiscipline.data.Habit) {
        val dao = habitDao ?: return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val log = com.realdiscipline.data.HabitLog(
                    habitId = habit.id,
                    date = getToday(),
                    completed = true
                )
                dao.insertLog(log)
            }
            Toast.makeText(context, "✅ ${habit.name} tamamlandı!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteHabit(habit: com.realdiscipline.data.Habit) {
        val dao = habitDao ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Alışkanlığı Sil")
            .setMessage("\"${habit.name}\" alışkanlığını silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dao.deleteHabit(habit)
                    }
                    Toast.makeText(context, "Alışkanlık silindi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun showHabitDialog(habit: com.realdiscipline.data.Habit?) {
        val dialog = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etHabitDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerHabitCategory)
        val spinnerFrequency = dialogView.findViewById<Spinner>(R.id.spinnerFrequency)
        val etTargetDays = dialogView.findViewById<EditText>(R.id.etTargetDays)
        
        val categories = arrayOf("Health", "Fitness", "Study", "Work", "General")
        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        
        val frequencies = arrayOf("Daily", "Weekly")
        spinnerFrequency.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, frequencies)
        
        habit?.let {
            etName.setText(it.name)
            etDescription.setText(it.description)
            spinnerCategory.setSelection(categories.indexOf(it.category))
            spinnerFrequency.setSelection(frequencies.indexOf(it.frequency))
            etTargetDays.setText(it.targetDays.toString())
        }
        
        dialog.setView(dialogView)
            .setTitle(if (habit == null) "Yeni Alışkanlık" else "Alışkanlığı Düzenle")
            .setPositiveButton("Kaydet") { _, _ ->
                val name = etName.text.toString()
                if (name.isNotEmpty()) {
                    val newHabit = com.realdiscipline.data.Habit(
                        id = habit?.id ?: 0,
                        name = name,
                        description = etDescription.text.toString(),
                        category = spinnerCategory.selectedItem.toString(),
                        frequency = spinnerFrequency.selectedItem.toString(),
                        targetDays = etTargetDays.text.toString().toIntOrNull() ?: 30
                    )
                    
                    val dao = habitDao ?: return@setPositiveButton
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (habit == null) {
                                dao.insertHabit(newHabit)
                            } else {
                                dao.updateHabit(newHabit)
                            }
                        }
                        Toast.makeText(context, "Alışkanlık kaydedildi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}

class TodosFragment : Fragment() {
    
    private var db: DisciplineDatabase? = null
    private var todoDao: TodoDao? = null
    private var adapter: TodoAdapter? = null
    
    private var rvTodos: RecyclerView? = null
    private var fabAddTodo: FloatingActionButton? = null
    private var tvActiveCount: TextView? = null
    private var tvCompletedCount: TextView? = null
    private var tabLayout: TabLayout? = null
    private var emptyState: View? = null
    
    private var currentFilter = "active" // active, completed, all
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_todos, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val database = DisciplineDatabase.getDatabase(requireContext())
        db = database
        todoDao = database.todoDao()
        
        initViews(view)
        setupRecyclerView()
        setupTabs()
        setupFab()
        observeTodos()
    }
    
    private fun initViews(view: View) {
        rvTodos = view.findViewById(R.id.rvTodos)
        fabAddTodo = view.findViewById(R.id.fabAddTodo)
        tvActiveCount = view.findViewById(R.id.tvActiveCount)
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount)
        tabLayout = view.findViewById(R.id.tabLayout)
        emptyState = view.findViewById(R.id.emptyState)
    }
    
    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onTodoChecked = { todo, isChecked ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        todoDao?.updateTodoCompletion(
                            todo.id,
                            isChecked,
                            if (isChecked) System.currentTimeMillis() else 0
                        )
                    }
                }
            },
            onTodoDelete = { todo ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        todoDao?.deleteTodo(todo)
                    }
                    Toast.makeText(context, "Görev silindi", Toast.LENGTH_SHORT).show()
                }
            },
            onTodoClick = { todo ->
                showTodoDialog(todo)
            }
        )
        
        rvTodos?.adapter = adapter
        rvTodos?.layoutManager = LinearLayoutManager(context)
    }
    
    private fun setupTabs() {
        tabLayout?.addTab(tabLayout?.newTab()?.setText("Aktif") ?: return)
        tabLayout?.addTab(tabLayout?.newTab()?.setText("Tamamlanan") ?: return)
        tabLayout?.addTab(tabLayout?.newTab()?.setText("Hepsi") ?: return)
        
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentFilter = "active"
                    1 -> currentFilter = "completed"
                    2 -> currentFilter = "all"
                }
                observeTodos()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupFab() {
        fabAddTodo?.setOnClickListener {
            showTodoDialog(null)
        }
    }
    
    private fun observeTodos() {
        val dao = todoDao ?: return
        val liveData = when (currentFilter) {
            "active" -> dao.getAllActiveTodos()
            "completed" -> dao.getCompletedTodos()
            else -> dao.getAllActiveTodos() // For now, show active for "all"
        }
        
        liveData.observe(viewLifecycleOwner) { todos ->
            adapter?.submitList(todos)
            emptyState?.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            updateStats()
        }
    }
    
    private fun updateStats() {
        val dao = todoDao ?: return
        lifecycleScope.launch {
            val active = withContext(Dispatchers.IO) { dao.getActiveTodoCount() }
            val completed = withContext(Dispatchers.IO) {
                val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                dao.getCompletedTodoCount(weekAgo)
            }
            tvActiveCount?.text = active.toString()
            tvCompletedCount?.text = completed.toString()
        }
    }
    
    private fun showTodoDialog(todo: Todo?) {
        val dialog = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val spinnerRepeat = dialogView.findViewById<Spinner>(R.id.spinnerRepeat)
        val etDueDate = dialogView.findViewById<EditText>(R.id.etDueDate)
        val etDueTime = dialogView.findViewById<EditText>(R.id.etDueTime)
        
        // Setup spinners
        val categories = arrayOf("General", "Exercise", "Nutrition", "Health", "Sleep", "Work", "Study")
        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        
        val priorities = arrayOf("Düşük", "Orta", "Yüksek")
        spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        
        val repeats = arrayOf("None", "Daily", "Weekly", "Monthly")
        spinnerRepeat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, repeats)
        
        // Fill if editing
        todo?.let {
            etTitle.setText(it.title)
            etDescription.setText(it.description)
            spinnerCategory.setSelection(categories.indexOf(it.category))
            spinnerPriority.setSelection(it.priority - 1)
            spinnerRepeat.setSelection(repeats.indexOf(it.repeatType))
            etDueDate.setText(it.dueDate)
            etDueTime.setText(it.dueTime)
        }
        
        dialog.setView(dialogView)
            .setTitle(if (todo == null) "Yeni Görev" else "Görevi Düzenle")
            .setPositiveButton("Kaydet") { _, _ ->
                val title = etTitle.text.toString()
                if (title.isNotEmpty()) {
                    val newTodo = Todo(
                        id = todo?.id ?: 0,
                        title = title,
                        description = etDescription.text.toString(),
                        category = spinnerCategory.selectedItem.toString(),
                        priority = spinnerPriority.selectedItemPosition + 1,
                        repeatType = spinnerRepeat.selectedItem.toString(),
                        dueDate = etDueDate.text.toString(),
                        dueTime = etDueTime.text.toString(),
                        completed = todo?.completed ?: false,
                        completedAt = todo?.completedAt ?: 0,
                        fromAiPlan = false
                    )
                    
                    val dao = todoDao ?: return@setPositiveButton
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (todo == null) {
                                dao.insertTodo(newTodo)
                            } else {
                                dao.updateTodo(newTodo)
                            }
                        }
                        Toast.makeText(context, "Görev kaydedildi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}

class ProgressFragment : Fragment() {
    
    private var db: DisciplineDatabase? = null
    private var tvTotalHabits: TextView? = null
    private var tvActiveTodos: TextView? = null
    private var tvCompletedTodos: TextView? = null
    private var tvWeeklySummary: TextView? = null
    private var tvProfileInfo: TextView? = null
    private var tvMotivation: TextView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = DisciplineDatabase.getDatabase(requireContext())
        
        tvTotalHabits = view.findViewById(R.id.tvTotalHabits)
        tvActiveTodos = view.findViewById(R.id.tvActiveTodos)
        tvCompletedTodos = view.findViewById(R.id.tvCompletedTodos)
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary)
        tvProfileInfo = view.findViewById(R.id.tvProfileInfo)
        tvMotivation = view.findViewById(R.id.tvMotivation)
        
        loadStats()
    }
    
    private fun loadStats() {
        val database = db ?: return
        
        lifecycleScope.launch {
            // Habits
            val habitCount = withContext(Dispatchers.IO) {
                database.habitDao().getAllActiveHabits().value?.size ?: 0
            }
            tvTotalHabits?.text = habitCount.toString()
            
            // Todos
            val activeTodos = withContext(Dispatchers.IO) {
                database.todoDao().getActiveTodoCount()
            }
            tvActiveTodos?.text = activeTodos.toString()
            
            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val completedTodos = withContext(Dispatchers.IO) {
                database.todoDao().getCompletedTodoCount(weekAgo)
            }
            tvCompletedTodos?.text = completedTodos.toString()
            
            // Weekly summary
            val weeklyHabits = withContext(Dispatchers.IO) {
                // Count habit logs from this week
                var count = 0
                database.habitDao().getAllActiveHabits().value?.forEach { habit ->
                    count += database.habitDao().getWeeklyCompletionCount(habit.id)
                }
                count
            }
            
            tvWeeklySummary?.text = "Bu hafta $weeklyHabits alışkanlık ve $completedTodos görevi tamamladın.\n" +
                    if (completedTodos > 0 || weeklyHabits > 0) {
                        "Harika gidiyorsun! Devam et! 💪"
                    } else {
                        "Haydi başlayalım! 🚀"
                    }
            
            // Profile
            val profile = withContext(Dispatchers.IO) {
                database.userProfileDao().getUserProfileSync()
            }
            
            if (profile != null && profile.aiPlanGenerated) {
                val bmi = profile.weight / ((profile.height / 100) * (profile.height / 100))
                tvProfileInfo?.text = """
                    📊 Profil:
                    • Yaş: ${profile.age}
                    • Kilo: ${profile.weight} kg
                    • Boy: ${profile.height} cm
                    • BMI: ${String.format("%.1f", bmi)}
                    • Hedef: ${profile.goal}
                    • Son plan: ${formatDate(profile.lastPlanUpdate)}
                """.trimIndent()
            } else {
                tvProfileInfo?.text = "Henüz AI planı oluşturmadınız.\nAI Plan sekmesine gidip planınızı oluşturun!"
            }
            
            // Random motivation
            val motivations = listOf(
                "Başarı = Disiplin × Tutarlılık\nHer gün biraz daha iyiye! 💪",
                "Küçük adımlar, büyük değişimler yaratır.\nDevam et! 🚀",
                "Hedeflerine ulaşmanın tek yolu:\nSabır + Disiplin + Tutarlılık = Başarı! 🎯",
                "Vazgeçmek yok! Her gün bir adım daha yakınsın! 💫",
                "Bugün yapamadıklarını yarın yapabilirsin.\nAma bugünü de atlamadan! ⭐"
            )
            tvMotivation?.text = motivations.random()
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val days = diff / (24 * 60 * 60 * 1000)
        
        return when {
            days == 0L -> "Bugün"
            days == 1L -> "Dün"
            days < 7 -> "$days gün önce"
            days < 30 -> "${days / 7} hafta önce"
            else -> "${days / 30} ay önce"
        }
    }
}
