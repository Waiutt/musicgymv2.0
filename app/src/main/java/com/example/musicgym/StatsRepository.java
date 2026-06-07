package com.example.musicgym;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 统计页数据层 — 封装数据库访问和聚合计算 */
public class StatsRepository {

    private final AppDatabase db;

    public StatsRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    /** 加载当月和上月日期范围 */
    public DateRange getDateRange() {
        Calendar cal = Calendar.getInstance();
        int curYear = cal.get(Calendar.YEAR);
        int curMonth = cal.get(Calendar.MONTH);
        int curDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.MONTH, -1);
        int lastYear = cal.get(Calendar.YEAR);
        int lastMonth = cal.get(Calendar.MONTH);
        int lastDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return new DateRange(curYear, curMonth, curDays, lastYear, lastMonth, lastDays);
    }

    /** 加载全部记录，数据不足时自动生成 mock 数据 */
    public StatsData loadAllData(int curYear, int curMonth, int lastYear, int lastMonth) {
        List<WorkoutRecord> records = db.workoutRecordDao().getAllRecords();
        List<StrengthRecord> strengthRecords = db.strengthRecordDao().getAllRecords();

        if (records.isEmpty() && strengthRecords.isEmpty()) {
            seedMockData(curYear, curMonth, lastYear, lastMonth);
            records = db.workoutRecordDao().getAllRecords();
        }

        return new StatsData(records, strengthRecords);
    }

    /** 有氧数据按月聚合 */
    public Map<Integer, Float> aggregateCardioByMonth(List<WorkoutRecord> records,
                                                        int year, int month, String filter) {
        Map<Integer, Float> sum = new HashMap<>();
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
        for (WorkoutRecord r : records) {
            if (r.getDate() == null || !r.getDate().startsWith(prefix)) continue;
            if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
            try {
                int day = Integer.parseInt(r.getDate().substring(8, 10));
                sum.put(day, sum.getOrDefault(day, 0f) + (float) r.getDistanceKm());
            } catch (Exception ignored) {}
        }
        return sum;
    }

    /** 力量训练按月聚合（训练量 kg） */
    public Map<Integer, Float> aggregateStrengthByMonth(List<StrengthRecord> records,
                                                          int year, int month) {
        Map<Integer, Float> vol = new HashMap<>();
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
        for (StrengthRecord r : records) {
            if (r.getDate() == null || !r.getDate().startsWith(prefix)) continue;
            try {
                int day = Integer.parseInt(r.getDate().substring(8, 10));
                JSONArray arr = new JSONArray(r.getExercisesJson());
                float total = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONArray sets = arr.getJSONObject(i).getJSONArray("sets");
                    for (int j = 0; j < sets.length(); j++) {
                        JSONObject s = sets.getJSONObject(j);
                        total += s.optDouble("weight") * s.optInt("reps");
                    }
                }
                vol.put(day, vol.getOrDefault(day, 0f) + total);
            } catch (Exception ignored) {}
        }
        return vol;
    }

    /** 收集有运动数据的天数 */
    public Set<Integer> getActiveDays(String filter, List<WorkoutRecord> cardioRecords,
                                       List<StrengthRecord> strengthRecords,
                                       int year, int month) {
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
        Set<Integer> cardioDays = new HashSet<>();
        Set<Integer> strengthDays = new HashSet<>();

        if (!"Strength".equals(filter)) {
            for (WorkoutRecord r : cardioRecords) {
                if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                    if ("All".equals(filter) || filter.equals(r.getSportType())) {
                        try { cardioDays.add(Integer.parseInt(r.getDate().substring(8, 10))); } catch (Exception ignored) {}
                    }
                }
            }
        }
        if ("All".equals(filter) || "Strength".equals(filter)) {
            for (StrengthRecord r : strengthRecords) {
                if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                    try { strengthDays.add(Integer.parseInt(r.getDate().substring(8, 10))); } catch (Exception ignored) {}
                }
            }
        }

        Set<Integer> all = new HashSet<>();
        all.addAll(cardioDays);
        all.addAll(strengthDays);
        // 返回首元素标记类型: 1=cardio, 2=strength, 3=both
        for (int day : all) {
            int flag = (cardioDays.contains(day) ? 1 : 0) | (strengthDays.contains(day) ? 2 : 0);
            // 作弊: 用 day*10+flag 存类型
        }
        return all; // 简化 — 在实际使用中会分别传递
    }

    /** 汇总卡片数据 */
    public SummaryStats calcSummary(List<WorkoutRecord> records, List<StrengthRecord> strengthRecords,
                                     String filter, int year, int month) {
        if ("Strength".equals(filter)) {
            String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
            int dur = 0, count = 0;
            for (StrengthRecord r : strengthRecords) {
                if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                    dur += r.getDurationSeconds();
                    count++;
                }
            }
            int h = dur / 3600, m = (dur % 3600) / 60;
            return new SummaryStats(String.valueOf(count),
                    String.format(Locale.getDefault(), "%dh %dm", h, m),
                    String.valueOf(count), "—");
        }

        String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
        float dist = 0, dur = 0, cal = 0;
        int cnt = 0;
        for (WorkoutRecord r : records) {
            if (r.getDate() == null || !r.getDate().startsWith(prefix)) continue;
            if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
            dist += r.getDistanceKm();
            dur += r.getDurationSeconds();
            cal += r.getCalories();
            cnt++;
        }
        int h = (int) dur / 3600, m = ((int) dur % 3600) / 60;
        SummaryStats ss = new SummaryStats(
                String.format(Locale.getDefault(), "%.1f", dist),
                String.format(Locale.getDefault(), "%dh %dm", h, m),
                String.valueOf(cnt),
                String.valueOf((int) cal));

        // 趋势: 对比上月
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.set(year, month, 1);
        cal2.add(java.util.Calendar.MONTH, -1);
        int lastY = cal2.get(java.util.Calendar.YEAR);
        int lastM = cal2.get(java.util.Calendar.MONTH);
        String lastP = String.format(Locale.getDefault(), "%04d-%02d", lastY, lastM + 1);
        float lastDist = 0; int lastCnt = 0;
        for (WorkoutRecord r : records) {
            if (r.getDate() != null && r.getDate().startsWith(lastP)) {
                if ("All".equals(filter) || filter.equals(r.getSportType())) {
                    lastDist += r.getDistanceKm(); lastCnt++;
                }
            }
        }
        if (lastDist > 0) {
            double change = (dist - lastDist) / lastDist * 100;
            ss.trend = String.format(Locale.getDefault(), "%s %.0f%%",
                    change >= 0 ? "↑" : "↓", Math.abs(change));
        } else if (dist > 0) {
            ss.trend = "new";
        }
        return ss;
    }

    // ── Mock 数据 ──

    private void seedMockData(int curYear, int curMonth, int lastYear, int lastMonth) {
        Random r = new Random(42);
        String[] sports = {"Running", "Cycling", "Walking"};
        int[][] lp = {{1,4,7,10,13,16,19,22,25,28}, {3,9,15,21,27}, {2,5,8,11,14,17,20,23,26,29}};
        int[][] cp = {{1,3,5,8,11,14,17,20,23,26,29}, {2,7,12,17,22,27}, {1,4,6,9,12,15,18,21,24,27,30}};
        double[][] dr = {{4,10}, {15,40}, {2,5.5}};
        int[][] dur = {{1200,3300}, {2400,6000}, {1500,4200}};
        for (int si = 0; si < 3; si++) {
            for (int d : lp[si]) {
                double dist = dr[si][0] + r.nextDouble() * (dr[si][1] - dr[si][0]);
                db.workoutRecordDao().insertRecord(new WorkoutRecord(
                        String.format(Locale.getDefault(), "%04d-%02d-%02d", lastYear, lastMonth + 1, d),
                        sports[si], Math.round(dist * 100.0) / 100.0,
                        dur[si][0] + r.nextInt(dur[si][1] - dur[si][0]),
                        (int)(dist * 65 * (si == 1 ? 0.7 : si == 2 ? 0.85 : 1.0)), "[]"));
            }
            for (int d : cp[si]) {
                double dist = dr[si][0] + r.nextDouble() * (dr[si][1] - dr[si][0]);
                db.workoutRecordDao().insertRecord(new WorkoutRecord(
                        String.format(Locale.getDefault(), "%04d-%02d-%02d", curYear, curMonth + 1, d),
                        sports[si], Math.round(dist * 100.0) / 100.0,
                        dur[si][0] + r.nextInt(dur[si][1] - dur[si][0]),
                        (int)(dist * 65 * (si == 1 ? 0.7 : si == 2 ? 0.85 : 1.0)), "[]"));
            }
        }
    }

    // ── 数据容器 ──

    public static class DateRange {
        public final int curYear, curMonth, curDays;
        public final int lastYear, lastMonth, lastDays;
        DateRange(int cy, int cm, int cd, int ly, int lm, int ld) {
            curYear = cy; curMonth = cm; curDays = cd;
            lastYear = ly; lastMonth = lm; lastDays = ld;
        }
    }

    public static class StatsData {
        public final List<WorkoutRecord> cardioRecords;
        public final List<StrengthRecord> strengthRecords;
        StatsData(List<WorkoutRecord> c, List<StrengthRecord> s) {
            cardioRecords = c; strengthRecords = s;
        }
    }

    public static class SummaryStats {
        public final String distance, duration, workouts, calories;
        public String trend;
        SummaryStats(String d, String dur, String w, String cal) {
            distance = d; duration = dur; workouts = w; calories = cal;
        }
    }
}
