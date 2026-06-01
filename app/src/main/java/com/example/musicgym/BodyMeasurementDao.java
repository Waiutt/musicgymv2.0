package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BodyMeasurementDao {

    @Insert
    long insert(BodyMeasurement m);

    @Query("SELECT * FROM body_measurements ORDER BY id DESC")
    List<BodyMeasurement> getAll();

    @Query("SELECT * FROM body_measurements ORDER BY id DESC LIMIT 1")
    BodyMeasurement getLatest();
}
