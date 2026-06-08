package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/** 首次安装预置数据 — 让 App 打开就有内容可展示 */
public class SeedDataManager {

    private static final String KEY_SEEDED = "seeded_v5.2";
    private static final Random R = new Random();

    public static void seedIfNeeded(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SEEDED, false)) return;

        AppDatabase db = AppDatabase.getInstance(ctx);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // ═══ 7天有氧运动 ═══
        String[] sports = {"Running","Running","Running","Cycling","Running","Walking","Running"};
        double[] dists = {5.2, 4.8, 6.1, 15.3, 3.5, 4.2, 7.0};
        int[] secs    = {1560, 1440, 1860, 2700, 1200, 3600, 2100};
        int[] cals    = {320, 290, 380, 450, 180, 200, 420};
        for (int i = 0; i < 7; i++) {
            cal.add(Calendar.DAY_OF_YEAR, -6 + i);
            WorkoutRecord r = new WorkoutRecord();
            r.setDate(sdf.format(cal.getTime()));
            r.setSportType(sports[i]);
            r.setDistanceKm(dists[i] + R.nextDouble() * 0.5);
            r.setDurationSeconds(secs[i] + R.nextInt(60));
            r.setCalories(cals[i] + R.nextInt(20));
            r.setPathPointsJson(buildMockPath(dists[i]));
            db.workoutRecordDao().insertRecord(r);
        }

        // ═══ 隔天力量训练 ═══
        for (int day : new int[]{1,3,5,7}) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, -day);
            int duration = 2400 + R.nextInt(600);
            db.strengthRecordDao().insertRecord(
                    new StrengthRecord(sdf.format(cal.getTime()), duration,
                            buildStrengthJson(day)));
        }

        // ═══ 体重下行趋势 ═══
        double weight = 78.5;
        for (int i = 0; i < 7; i++) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, -6 + i);
            if (i > 0) weight -= R.nextDouble() * 0.3;
            db.weightRecordDao().insertRecord(
                    new WeightRecord(sdf.format(cal.getTime()),
                            Math.round(weight * 10) / 10.0));
        }

        // ═══ 围度 ═══
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        db.bodyMeasurementDao().insert(new BodyMeasurement(
                sdf.format(cal.getTime()), 76.5, 98, 82, 96, 36, 56));

        // ═══ 社区 mock 帖子 ═══
        String[] titles = {"晨跑 5km 打卡","今天推胸日","周末骑行 15km","深蹲 PR 100kg"};
        String[] snips  = {"配速 5:12/km，心率稳定在 150",
                           "杠铃平板卧推 80kg×8×4组，状态不错",
                           "沿江骑行道，风景超好，下次组队",
                           "终于突破 100kg 了！努力训练 3 个月"};
        String[] auths  = {"跑步达人","健身小王","骑行爱好者","力量举新手"};
        String[] fulls  = {"晨跑 5km 打卡 ☀️\n配速 5:12/km，心率稳定在 150\n用时 26 分，消耗 320 kcal",
                           "今天推胸日 💪\n卧推 80kg×8×4 组\n上斜 30kg×10×4 组\n飞鸟 14kg×12×3 组",
                           "周末骑行 15km 🚴\n沿江骑行道风景超好\n用时 45 分，均速 20km/h",
                           "深蹲 PR 100kg 🎉\n终于突破 100kg 大关\n从 60kg→100kg，感谢 MusicGym"};
        for (int i = 0; i < titles.length; i++) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.HOUR_OF_DAY, -i * 6);
            BlogPost p = new BlogPost(titles[i], sdf.format(cal.getTime()),
                    snips[i], auths[i], fulls[i], "");
            db.blogPostDao().insertPost(p);
        }

        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    private static String buildMockPath(double dist) {
        StringBuilder sb = new StringBuilder("[");
        int n = (int)(dist * 20);
        double lat = 30.55 + R.nextDouble() * 0.05;
        double lng = 104.05 + R.nextDouble() * 0.05;
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            lat += (R.nextDouble() - 0.5) * 0.0008;
            lng += (R.nextDouble() - 0.5) * 0.0008;
            sb.append("[").append(String.format(Locale.US,"%.6f",lat))
              .append(",").append(String.format(Locale.US,"%.6f",lng)).append("]");
        }
        return sb.append("]").toString();
    }

    private static String buildStrengthJson(int day) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            String[][] exs = (day == 1 || day == 5) ? new String[][]{
                {"杠铃平板卧推","80","8,8,8,7"}, {"上斜哑铃卧推","30","10,10,9,8"},
                {"哑铃飞鸟","14","12,12,10"}, {"三头绳索下压","25","12,12,12,10"}
            } : new String[][]{
                {"杠铃深蹲","100","5,5,5,4,4"}, {"罗马尼亚硬拉","80","8,8,8"},
                {"腿举","160","10,10,10"}, {"保加利亚分腿蹲","20","12,12,10"}
            };
            for (String[] ex : exs) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("name", ex[0]);
                org.json.JSONArray sets = new org.json.JSONArray();
                double w = Double.parseDouble(ex[1]);
                for (String rep : ex[2].split(",")) {
                    org.json.JSONObject s = new org.json.JSONObject();
                    s.put("weight", w * (0.9 + R.nextDouble() * 0.2));
                    s.put("reps", Integer.parseInt(rep.trim()));
                    sets.put(s);
                }
                obj.put("sets", sets); arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) { return "[]"; }
    }
}
