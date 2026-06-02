package com.example.musicgym;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Locale;

/**
 * 单元测试 — 数据模型和工具方法。
 * 注意：因 Room 实体依赖 android API，使用 Robolectric 作为 Android 模拟层运行。
 */
public class ModelsTest {

    // ── 时间格式化 ──

    @Test
    public void formatTime_correctOutput() {
        assertEquals("01:30", fmt(90));
        assertEquals("60:00", fmt(3600));
        assertEquals("00:00", fmt(0));
        assertEquals("02:05", fmt(125));
    }

    private String fmt(int sec) {
        return String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60);
    }

    // ── 有氧记录 ──

    @Test
    public void workoutRecord_getters() {
        WorkoutRecord r = new WorkoutRecord("2026-05-01", "Running", 5.2, 1800, 340, "[]");
        assertEquals("2026-05-01", r.getDate());
        assertEquals("Running", r.getSportType());
        assertEquals(5.2, r.getDistanceKm(), 0.001);
        assertEquals(1800, r.getDurationSeconds());
        assertEquals(340, r.getCalories());
    }

    @Test
    public void workoutRecord_setters() {
        WorkoutRecord r = new WorkoutRecord();
        r.setDate("2026-06-01");
        r.setSportType("Cycling");
        r.setDistanceKm(15.0);
        r.setDurationSeconds(3600);
        r.setCalories(420);
        assertEquals("2026-06-01", r.getDate());
        assertEquals(15.0, r.getDistanceKm(), 0.01);
    }

    // ── 力量记录 ──

    @Test
    public void strengthRecord_getters() {
        StrengthRecord r = new StrengthRecord("2026-05-10", 2700,
                "[{\"name\":\"卧推\",\"sets\":[{\"weight\":80,\"reps\":8}]}]");
        assertEquals("2026-05-10", r.getDate());
        assertEquals(2700, r.getDurationSeconds());
        assertNotNull(r.getExercisesJson());
    }

    // ── 体重记录 ──

    @Test
    public void weightRecord_getters() {
        WeightRecord w = new WeightRecord("May 15", 73.0);
        assertEquals("May 15", w.getDate());
        assertEquals(73.0, w.getWeightKg(), 0.01);
    }

    // ── 模板 ──

    @Test
    public void workoutTemplate_gettersAndSetters() {
        WorkoutTemplate t = new WorkoutTemplate("推日", "[\"杠铃平板卧推\"]");
        assertEquals("推日", t.getName());
        assertTrue(t.getExercisesJson().contains("杠铃平板卧推"));

        t.setName("拉日");
        t.setExercisesJson("[]");
        assertEquals("拉日", t.getName());
    }
}
