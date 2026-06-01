package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutRecordDao {

    @Insert
    long insertRecord(WorkoutRecord record);

    @Query("SELECT * FROM workout_records ORDER BY id DESC")
    List<WorkoutRecord> getAllRecords();

    @Query("SELECT * FROM workout_records ORDER BY id DESC LIMIT 1")
    WorkoutRecord getLatestRecord();

    @Query("SELECT * FROM workout_records WHERE date >= :fromDate AND date <= :toDate ORDER BY date ASC")
    List<WorkoutRecord> getRecordsBetween(String fromDate, String toDate);
}
