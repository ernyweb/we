package com.realdiscipline.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): LiveData<List<Habit>>
    
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabit(habitId: Long): Habit?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long
    
    @Update
    suspend fun updateHabit(habit: Habit)
    
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitLogs(habitId: Long): LiveData<List<HabitLog>>
    
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Long, date: String): HabitLog?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)
    
    @Query("SELECT COUNT(DISTINCT date) FROM habit_logs WHERE habitId = :habitId AND completed = 1")
    suspend fun getCompletedDaysCount(habitId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM habit_logs 
        WHERE habitId = :habitId 
        AND completed = 1 
        AND date >= date('now', '-7 days')
    """)
    suspend fun getWeeklyCompletionCount(habitId: Long): Int
}
