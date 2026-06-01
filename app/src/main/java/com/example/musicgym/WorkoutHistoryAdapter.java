package com.example.musicgym;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.WorkoutViewHolder> {

    private List<WorkoutRecord> recordList;

    public WorkoutHistoryAdapter(List<WorkoutRecord> recordList) {
        this.recordList = recordList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_history, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutRecord record = recordList.get(position);

        holder.tvSport.setText(record.getSportType());
        holder.tvDate.setText(record.getDate());
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.2f", record.getDistanceKm()));

        int minutes = record.getDurationSeconds() / 60;
        int seconds = record.getDurationSeconds() % 60;
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        holder.tvCalories.setText(String.valueOf(record.getCalories()));
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvSport, tvDate, tvDistance, tvDuration, tvCalories;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSport = itemView.findViewById(R.id.tv_workout_sport);
            tvDate = itemView.findViewById(R.id.tv_workout_date);
            tvDistance = itemView.findViewById(R.id.tv_workout_distance);
            tvDuration = itemView.findViewById(R.id.tv_workout_duration);
            tvCalories = itemView.findViewById(R.id.tv_workout_calories);
        }
    }
}
