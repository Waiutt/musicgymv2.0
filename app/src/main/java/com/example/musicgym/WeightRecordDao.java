package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeightRecordDao {

    @Insert
    void insertRecord(WeightRecord record);

    @Query("SELECT * FROM weight_records ORDER BY id DESC")
    List<WeightRecord> getAllRecords();
}
