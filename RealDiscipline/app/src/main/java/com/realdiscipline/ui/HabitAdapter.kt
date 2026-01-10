package com.realdiscipline.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.realdiscipline.R
import com.realdiscipline.data.Habit
import java.text.SimpleDateFormat
import java.util.*

data class HabitWithStats(
    val habit: Habit,
    val streak: Int,
    val completedDays: Int,
    val completedToday: Boolean
)

class HabitAdapter(
    private val onHabitClick: (Habit) -> Unit,
    private val onHabitDelete: (Habit) -> Unit,
    private val onMarkToday: (Habit) -> Unit
) : ListAdapter<HabitWithStats, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvHabitDescription)
        private val tvStreak: TextView = itemView.findViewById(R.id.tvStreak)
        private val tvCompleted: TextView = itemView.findViewById(R.id.tvCompleted)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        private val btnMarkToday: Button = itemView.findViewById(R.id.btnMarkToday)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(habitWithStats: HabitWithStats) {
            val habit = habitWithStats.habit
            
            tvName.text = habit.name
            tvDescription.text = habit.description
            tvDescription.visibility = if (habit.description.isNotEmpty()) View.VISIBLE else View.GONE
            
            tvStreak.text = habitWithStats.streak.toString()
            tvCompleted.text = "${habitWithStats.completedDays}/${habit.targetDays}"
            tvCategory.text = habit.category
            tvFrequency.text = habit.frequency
            
            // Category color
            tvCategory.setBackgroundColor(getCategoryColor(habit.category))
            
            // Today button state
            if (habitWithStats.completedToday) {
                btnMarkToday.text = "✅ Bugün Tamamlandı"
                btnMarkToday.isEnabled = false
                btnMarkToday.alpha = 0.6f
            } else {
                btnMarkToday.text = "✅ Bugün Tamamla"
                btnMarkToday.isEnabled = true
                btnMarkToday.alpha = 1.0f
            }
            
            btnMarkToday.setOnClickListener {
                onMarkToday(habit)
            }
            
            btnDelete.setOnClickListener {
                onHabitDelete(habit)
            }
            
            itemView.setOnClickListener {
                onHabitClick(habit)
            }
        }
        
        private fun getCategoryColor(category: String): Int {
            val context = itemView.context
            return when (category) {
                "Health" -> context.getColor(android.R.color.holo_green_light)
                "Fitness" -> context.getColor(android.R.color.holo_blue_light)
                "Study" -> context.getColor(android.R.color.holo_purple)
                "Work" -> context.getColor(android.R.color.holo_orange_light)
                else -> context.getColor(android.R.color.darker_gray)
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitWithStats>() {
        override fun areItemsTheSame(oldItem: HabitWithStats, newItem: HabitWithStats): Boolean {
            return oldItem.habit.id == newItem.habit.id
        }

        override fun areContentsTheSame(oldItem: HabitWithStats, newItem: HabitWithStats): Boolean {
            return oldItem == newItem
        }
    }
}
