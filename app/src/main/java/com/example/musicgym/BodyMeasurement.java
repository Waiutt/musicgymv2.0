package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "body_measurements")
public class BodyMeasurement {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String date;
    private double weightKg;
    private double chestCm;
    private double waistCm;
    private double hipCm;
    private double armCm;
    private double thighCm;

    public BodyMeasurement() {}

    public BodyMeasurement(String date, double weightKg, double chestCm, double waistCm,
                           double hipCm, double armCm, double thighCm) {
        this.date = date; this.weightKg = weightKg; this.chestCm = chestCm;
        this.waistCm = waistCm; this.hipCm = hipCm; this.armCm = armCm; this.thighCm = thighCm;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double v) { this.weightKg = v; }
    public double getChestCm() { return chestCm; }
    public void setChestCm(double v) { this.chestCm = v; }
    public double getWaistCm() { return waistCm; }
    public void setWaistCm(double v) { this.waistCm = v; }
    public double getHipCm() { return hipCm; }
    public void setHipCm(double v) { this.hipCm = v; }
    public double getArmCm() { return armCm; }
    public void setArmCm(double v) { this.armCm = v; }
    public double getThighCm() { return thighCm; }
    public void setThighCm(double v) { this.thighCm = v; }
}
