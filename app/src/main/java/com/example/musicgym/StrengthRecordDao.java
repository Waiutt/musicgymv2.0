package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StrengthRecordDao {

    @Insert
    long insertRecord(StrengthRecord record);

    @Query("SELECT * FROM strength_records ORDER BY id DESC")
    List<StrengthRecord> getAllRecords();
}
