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

public class StrengthHistoryAdapter extends RecyclerView.Adapter<StrengthHistoryAdapter.ViewHolder> {

    private final List<StrengthRecord> records;

    public StrengthHistoryAdapter(List<StrengthRecord> records) {
        this.records = records;
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
        StrengthRecord r = records.get(position);
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
        return records.size();
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
