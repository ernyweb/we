package com.gamebooster.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.VH> {

    interface OnItemClickListener {
        void onItemClick(@NonNull RecordingItem item);
    }

    private List<RecordingItem> data;
    private final OnItemClickListener listener;

    RecordingAdapter(List<RecordingItem> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    void update(List<RecordingItem> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RecordingItem item = data.get(position);
        holder.title.setText(item.title());
        holder.meta.setText(item.meta());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            meta = itemView.findViewById(R.id.txtMeta);
        }
    }
}
