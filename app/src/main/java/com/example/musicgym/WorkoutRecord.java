package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_records")
public class WorkoutRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String date;
    private String sportType;
    private double distanceKm;
    private int durationSeconds;
    private int calories;
    private String pathPointsJson;

    public WorkoutRecord() {}

    public WorkoutRecord(String date, String sportType, double distanceKm, int durationSeconds, int calories, String pathPointsJson) {
        this.date = date;
        this.sportType = sportType;
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.calories = calories;
        this.pathPointsJson = pathPointsJson;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getPathPointsJson() { return pathPointsJson; }
    public void setPathPointsJson(String pathPointsJson) { this.pathPointsJson = pathPointsJson; }
}
