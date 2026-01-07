package com.gamebooster.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Oyunları grid layout'ta gösteren adapter
 */
public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private List<GameInfo> gameList;
    private OnGameClickListener onGameClickListener;

    public GameAdapter(List<GameInfo> gameList, OnGameClickListener onGameClickListener) {
        this.gameList = gameList != null ? gameList : new ArrayList<>();
        this.onGameClickListener = onGameClickListener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameInfo game = gameList.get(position);
        holder.gameNameTextView.setText(game.getName());
        holder.gameIconImageView.setImageDrawable(game.getIcon());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameClick(game);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    public void updateGames(List<GameInfo> newGameList) {
        this.gameList = newGameList != null ? newGameList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class GameViewHolder extends RecyclerView.ViewHolder {
        public ImageView gameIconImageView;
        public TextView gameNameTextView;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameIconImageView = itemView.findViewById(R.id.gameIcon);
            gameNameTextView = itemView.findViewById(R.id.gameName);
        }
    }

    public interface OnGameClickListener {
        void onGameClick(GameInfo gameInfo);
    }
}
