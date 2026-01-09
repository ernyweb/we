package com.realdiscipline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "General", // Health, Fitness, Study, Work
    val frequency: String = "Daily", // Daily, Weekly
    val targetDays: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String, // YYYY-MM-DD
    val completed: Boolean = true,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val priority: Int = 1, // 1=Low, 2=Medium, 3=High
    val dueDate: String = "", // YYYY-MM-DD
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Long = 1,
    val age: Int = 0,
    val weight: Float = 0f,
    val height: Float = 0f,
    val goal: String = "",
    val aiPlanGenerated: Boolean = false,
    val lastPlanUpdate: Long = 0
)
