package com.realdiscipline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Habit::class, HabitLog::class, Todo::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
abstract class DisciplineDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: DisciplineDatabase? = null
        
        fun getDatabase(context: Context): DisciplineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DisciplineDatabase::class.java,
                    "discipline_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
