package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutTemplateDao {

    @Insert
    long insert(WorkoutTemplate t);

    @Query("SELECT * FROM workout_templates ORDER BY id DESC")
    List<WorkoutTemplate> getAll();

    @Query("DELETE FROM workout_templates WHERE id = :id")
    void deleteById(long id);
}
