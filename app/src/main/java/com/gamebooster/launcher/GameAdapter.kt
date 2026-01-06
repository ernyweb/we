package com.gamebooster.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout

class GameAdapter(
    private var games: List<GameInfo>,
    private val onGameClick: (GameInfo) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    inner class GameViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(android.R.id.icon)
        private val name: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(game: GameInfo) {
            icon.setImageDrawable(game.icon)
            name.text = game.name
            itemView.setOnClickListener {
                onGameClick(game)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_2,
            parent,
            false
        )
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount(): Int = games.size

    fun updateGames(newGames: List<GameInfo>) {
        games = newGames
        notifyDataSetChanged()
    }
}
