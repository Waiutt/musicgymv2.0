package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** 成就徽章系统 */
public class AchievementManager {

    private final SharedPreferences prefs;

    public AchievementManager(Context ctx) {
        prefs = ctx.getSharedPreferences("achievements", Context.MODE_PRIVATE);
    }

    /** 检查并解锁成就，返回新解锁的成就列表 */
    public List<String> checkAndUnlock(AppDatabase db) {
        List<String> newBadges = new ArrayList<>();
        int totalWorkouts = prefs.getInt("total_workouts", 0);
        int totalStrength = prefs.getInt("total_strength", 0);
        float totalDist = prefs.getFloat("total_dist", 0);

        // 从数据库更新数据
        try {
            List<WorkoutRecord> wRecs = db.workoutRecordDao().getAllRecords();
            totalWorkouts = wRecs.size();
            totalDist = 0;
            for (WorkoutRecord r : wRecs) totalDist += (float) r.getDistanceKm();

            List<StrengthRecord> sRecs = db.strengthRecordDao().getAllRecords();
            totalStrength = sRecs.size();
        } catch (Exception ignored) {}

        prefs.edit().putInt("total_workouts", totalWorkouts)
                .putFloat("total_dist", totalDist)
                .putInt("total_strength", totalStrength).apply();

        // 检查成就
        check("首次运动", totalWorkouts >= 1, newBadges);
        check("运动10次", totalWorkouts >= 10, newBadges);
        check("运动50次", totalWorkouts >= 50, newBadges);
        check("运动100次", totalWorkouts >= 100, newBadges);
        check("总距离10km", totalDist >= 10, newBadges);
        check("总距离100km", totalDist >= 100, newBadges);
        check("总距离500km", totalDist >= 500, newBadges);
        check("力量训练10次", totalStrength >= 10, newBadges);
        check("力量训练50次", totalStrength >= 50, newBadges);

        return newBadges;
    }

    private void check(String name, boolean condition, List<String> badges) {
        if (condition && !prefs.getBoolean("badge_" + name, false)) {
            prefs.edit().putBoolean("badge_" + name, true).apply();
            badges.add(name);
        }
    }

    public List<String> getUnlocked() {
        List<String> list = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("badge_")) list.add(key.substring(6));
        }
        return list;
    }
}
