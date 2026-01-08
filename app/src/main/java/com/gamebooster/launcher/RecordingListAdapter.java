package com.gamebooster.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingListAdapter extends RecyclerView.Adapter<RecordingListAdapter.ViewHolder> {
    
    private final List<File> recordings;
    private final OnRecordingClickListener playListener;
    private final OnRecordingClickListener deleteListener;
    
    public interface OnRecordingClickListener {
        void onRecordingClick(File file);
    }
    
    public RecordingListAdapter(List<File> recordings, OnRecordingClickListener playListener, OnRecordingClickListener deleteListener) {
        this.recordings = recordings;
        this.playListener = playListener;
        this.deleteListener = deleteListener;
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
        File file = recordings.get(position);
        
        holder.tvFileName.setText(file.getName());
        
        long lastModified = file.lastModified();
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(lastModified));
        long sizeKB = file.length() / 1024;
        holder.tvFileInfo.setText(date + " • " + sizeKB + " KB");
        
        holder.btnPlay.setOnClickListener(v -> playListener.onRecordingClick(file));
        holder.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Sil")
                    .setMessage("Bu kaydı silmek istediğinden emin misin?")
                    .setPositiveButton("Sil", (dialog, which) -> deleteListener.onRecordingClick(file))
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }
    
    @Override
    public int getItemCount() {
        return recordings.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvFileInfo;
        Button btnPlay;
        Button btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
            btnPlay = itemView.findViewById(R.id.btnPlayItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
