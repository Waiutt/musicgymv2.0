package com.example.musicgym;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 数据导出工具 — 将全部记录导出为 CSV 文件到 Downloads 目录 */
public class DataExporter {

    public static boolean exportAll(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "MusicGym_data.csv");
            FileWriter fw = new FileWriter(file);

            // 表头
            fw.write("类型,日期,运动方式,距离(km),时长(秒),卡路里,详情\n");

            // 有氧记录
            List<WorkoutRecord> cardio = db.workoutRecordDao().getAllRecords();
            for (WorkoutRecord r : cardio) {
                fw.write(String.format(Locale.getDefault(),
                        "有氧,%s,%s,%.2f,%d,%d,\"%s\"\n",
                        n(r.getDate()), n(r.getSportType()),
                        r.getDistanceKm(), r.getDurationSeconds(), r.getCalories(),
                        n(r.getPathPointsJson())));
            }

            // 力量记录
            List<StrengthRecord> strength = db.strengthRecordDao().getAllRecords();
            for (StrengthRecord r : strength) {
                fw.write(String.format(Locale.getDefault(),
                        "力量,%s,-,0,%d,0,\"%s\"\n",
                        n(r.getDate()), r.getDurationSeconds(), n(r.getExercisesJson())));
            }

            // 体重记录
            List<WeightRecord> weight = db.weightRecordDao().getAllRecords();
            for (WeightRecord w : weight) {
                fw.write(String.format(Locale.getDefault(),
                        "体重,%s,-,%.1f,-,-,-\n", n(w.getDate()), w.getWeightKg()));
            }

            fw.close();
            return true;
        } catch (Exception e) {
            android.util.Log.e("MusicGym", "Export failed", e);
            return false;
        }
    }

    private static String n(String s) { return s != null ? s : "-"; }
}
