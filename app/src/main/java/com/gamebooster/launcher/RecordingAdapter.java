package com.gamebooster.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    private List<RecordingItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecordingItem item);
    }

    public RecordingAdapter(List<RecordingItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void update(List<RecordingItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordingItem item = items.get(position);
        holder.title.setText(item.title());
        holder.meta.setText(item.meta());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView meta;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordingTitle);
            meta = itemView.findViewById(R.id.recordingMeta);
        }
    }
}
