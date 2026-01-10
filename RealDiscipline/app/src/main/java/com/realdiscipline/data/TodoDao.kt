package com.realdiscipline.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE completed = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getAllActiveTodos(): LiveData<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE completed = 1 ORDER BY completedAt DESC LIMIT 50")
    fun getCompletedTodos(): LiveData<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE dueDate = :date AND completed = 0")
    fun getTodosForDate(date: String): LiveData<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE id = :todoId")
    suspend fun getTodo(todoId: Long): Todo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<Todo>)
    
    @Update
    suspend fun updateTodo(todo: Todo)
    
    @Delete
    suspend fun deleteTodo(todo: Todo)
    
    @Query("UPDATE todos SET completed = :completed, completedAt = :completedAt WHERE id = :todoId")
    suspend fun updateTodoCompletion(todoId: Long, completed: Boolean, completedAt: Long)
    
    @Query("DELETE FROM todos WHERE fromAiPlan = 1")
    suspend fun deleteAllAiPlanTodos()
    
    @Query("SELECT COUNT(*) FROM todos WHERE completed = 0")
    suspend fun getActiveTodoCount(): Int
    
    @Query("SELECT COUNT(*) FROM todos WHERE completed = 1 AND completedAt >= :startTime")
    suspend fun getCompletedTodoCount(startTime: Long): Int
}
