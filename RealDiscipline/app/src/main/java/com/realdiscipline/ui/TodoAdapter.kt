package com.realdiscipline.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.realdiscipline.R
import com.realdiscipline.data.Todo

class TodoAdapter(
    private val onTodoChecked: (Todo, Boolean) -> Unit,
    private val onTodoDelete: (Todo) -> Unit,
    private val onTodoClick: (Todo) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardTodo)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val tvRepeat: TextView = itemView.findViewById(R.id.tvRepeat)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(todo: Todo) {
            tvTitle.text = todo.title
            tvDescription.text = todo.description
            tvDescription.visibility = if (todo.description.isNotEmpty()) View.VISIBLE else View.GONE
            
            tvCategory.text = todo.category
            tvCategory.setBackgroundColor(getCategoryColor(todo.category, todo.priority))
            
            if (todo.dueDate.isNotEmpty()) {
                val dateText = formatDueDate(todo.dueDate, todo.dueTime)
                tvDueDate.text = dateText
                tvDueDate.visibility = View.VISIBLE
            } else {
                tvDueDate.visibility = View.GONE
            }
            
            if (todo.repeatType != "None") {
                tvRepeat.text = "🔄 ${todo.repeatType}"
                tvRepeat.visibility = View.VISIBLE
            } else {
                tvRepeat.visibility = View.GONE
            }
            
            cbCompleted.isChecked = todo.completed
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTodoChecked(todo, isChecked)
            }
            
            // Strike through if completed
            if (todo.completed) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.5f
                tvDescription.alpha = 0.3f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1.0f
                tvDescription.alpha = 0.7f
            }
            
            btnDelete.setOnClickListener {
                onTodoDelete(todo)
            }
            
            card.setOnClickListener {
                onTodoClick(todo)
            }
        }
        
        private fun getCategoryColor(category: String, priority: Int): Int {
            val context = itemView.context
            return when {
                priority == 3 -> context.getColor(android.R.color.holo_red_light)
                priority == 2 -> context.getColor(android.R.color.holo_orange_light)
                category == "Exercise" -> context.getColor(android.R.color.holo_blue_light)
                category == "Nutrition" -> context.getColor(android.R.color.holo_green_light)
                category == "Health" -> context.getColor(android.R.color.holo_purple)
                else -> context.getColor(android.R.color.darker_gray)
            }
        }
        
        private fun formatDueDate(date: String, time: String): String {
            val parts = date.split("-")
            if (parts.size != 3) return "📅 $date"
            
            val day = parts[2]
            val month = parts[1]
            val result = "📅 $day.$month"
            
            return if (time.isNotEmpty()) {
                "$result ⏰ $time"
            } else {
                result
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem == newItem
        }
    }
}
