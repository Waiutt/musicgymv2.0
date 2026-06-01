package com.example.musicgym;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.util.List;
import java.util.Locale;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {

    private final List<?> recordList;
    private final boolean isStrength;

    /** 有氧运动记录 */
    public WorkoutHistoryAdapter(List<WorkoutRecord> recordList) {
        this.recordList = recordList;
        this.isStrength = false;
    }

    /** 力量训练记录 */
    public WorkoutHistoryAdapter(List<StrengthRecord> recordList, boolean isStrength) {
        this.recordList = recordList;
        this.isStrength = isStrength;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isStrength) {
            bindStrength(holder, position);
        } else {
            bindCardio(holder, position);
        }
    }

    private void bindCardio(ViewHolder holder, int position) {
        WorkoutRecord record = (WorkoutRecord) recordList.get(position);
        holder.tvSport.setText(record.getSportType());
        holder.tvDate.setText(record.getDate());
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.2f", record.getDistanceKm()));

        int minutes = record.getDurationSeconds() / 60;
        int seconds = record.getDurationSeconds() % 60;
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        holder.tvCalories.setText(String.valueOf(record.getCalories()));
    }

    private void bindStrength(ViewHolder holder, int position) {
        StrengthRecord r = (StrengthRecord) recordList.get(position);
        holder.tvSport.setText("力量训练");
        holder.tvDate.setText(r.getDate());

        int m = r.getDurationSeconds() / 60;
        int s = r.getDurationSeconds() % 60;
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));

        try {
            JSONArray arr = new JSONArray(r.getExercisesJson());
            int totalSets = 0;
            for (int i = 0; i < arr.length(); i++) {
                totalSets += arr.getJSONObject(i).getJSONArray("sets").length();
            }
            holder.tvDistance.setText(arr.length() + " 个动作");
            holder.tvCalories.setText(totalSets + " 组");
        } catch (Exception e) {
            holder.tvDistance.setText("-");
            holder.tvCalories.setText("-");
        }
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSport, tvDate, tvDistance, tvDuration, tvCalories;

        ViewHolder(View v) {
            super(v);
            tvSport = v.findViewById(R.id.tv_workout_sport);
            tvDate = v.findViewById(R.id.tv_workout_date);
            tvDistance = v.findViewById(R.id.tv_workout_distance);
            tvDuration = v.findViewById(R.id.tv_workout_duration);
            tvCalories = v.findViewById(R.id.tv_workout_calories);
        }
    }
}
