package com.realdiscipline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.realdiscipline.ui.*

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(HabitsFragment())
        }
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits -> {
                    loadFragment(HabitsFragment())
                    true
                }
                R.id.nav_todos -> {
                    loadFragment(TodosFragment())
                    true
                }
                R.id.nav_progress -> {
                    loadFragment(ProgressFragment())
                    true
                }
                R.id.nav_ai -> {
                    loadFragment(AiPlanFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
