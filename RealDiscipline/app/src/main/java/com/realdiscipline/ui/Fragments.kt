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

class HabitsFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }
}

class TodosFragment : Fragment() {
    
    private lateinit var db: DisciplineDatabase
    private lateinit var todoDao: TodoDao
    private lateinit var adapter: TodoAdapter
    
    private lateinit var rvTodos: RecyclerView
    private lateinit var fabAddTodo: FloatingActionButton
    private lateinit var tvActiveCount: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var emptyState: View
    
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
        
        db = DisciplineDatabase.getDatabase(requireContext())
        todoDao = db.todoDao()
        
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
                        todoDao.updateTodoCompletion(
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
                        todoDao.deleteTodo(todo)
                    }
                    Toast.makeText(context, "Görev silindi", Toast.LENGTH_SHORT).show()
                }
            },
            onTodoClick = { todo ->
                showTodoDialog(todo)
            }
        )
        
        rvTodos.adapter = adapter
        rvTodos.layoutManager = LinearLayoutManager(context)
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Aktif"))
        tabLayout.addTab(tabLayout.newTab().setText("Tamamlanan"))
        tabLayout.addTab(tabLayout.newTab().setText("Hepsi"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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
        fabAddTodo.setOnClickListener {
            showTodoDialog(null)
        }
    }
    
    private fun observeTodos() {
        val liveData = when (currentFilter) {
            "active" -> todoDao.getAllActiveTodos()
            "completed" -> todoDao.getCompletedTodos()
            else -> todoDao.getAllActiveTodos() // For now, show active for "all"
        }
        
        liveData.observe(viewLifecycleOwner) { todos ->
            adapter.submitList(todos)
            emptyState.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            updateStats()
        }
    }
    
    private fun updateStats() {
        lifecycleScope.launch {
            val active = withContext(Dispatchers.IO) { todoDao.getActiveTodoCount() }
            val completed = withContext(Dispatchers.IO) {
                val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                todoDao.getCompletedTodoCount(weekAgo)
            }
            tvActiveCount.text = active.toString()
            tvCompletedCount.text = completed.toString()
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
                    
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (todo == null) {
                                todoDao.insertTodo(newTodo)
                            } else {
                                todoDao.updateTodo(newTodo)
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
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }
}
