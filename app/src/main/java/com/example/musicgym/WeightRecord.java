package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weight_records")
public class WeightRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String date;
    private double weightKg;

    public WeightRecord() {}

    public WeightRecord(String date, double weightKg) {
        this.date = date;
        this.weightKg = weightKg;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
}
