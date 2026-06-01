package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "strength_records")
public class StrengthRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String date;
    private int durationSeconds;
    private String exercisesJson;

    public StrengthRecord() {}

    @Ignore
    public StrengthRecord(String date, int durationSeconds, String exercisesJson) {
        this.date = date;
        this.durationSeconds = durationSeconds;
        this.exercisesJson = exercisesJson;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getExercisesJson() { return exercisesJson; }
    public void setExercisesJson(String exercisesJson) { this.exercisesJson = exercisesJson; }
}
