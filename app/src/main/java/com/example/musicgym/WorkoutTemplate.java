package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_templates")
public class WorkoutTemplate {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String exercisesJson; // ["杠铃卧推","哑铃飞鸟",...]

    public WorkoutTemplate() {}

    public WorkoutTemplate(String name, String exercisesJson) {
        this.name = name; this.exercisesJson = exercisesJson;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExercisesJson() { return exercisesJson; }
    public void setExercisesJson(String v) { this.exercisesJson = v; }
}
