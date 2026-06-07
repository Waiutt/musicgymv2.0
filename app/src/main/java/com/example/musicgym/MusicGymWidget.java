package com.example.musicgym;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/** 桌面 Widget — 显示今日运动数据 + 一键开始跑步 */
public class MusicGymWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    private void updateWidget(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_musicgym);

        // 点击打开 App（立即设置，不依赖 DB 数据）
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_stats, pi);

        // Room 必须在后台线程访问
        Executors.newSingleThreadExecutor().execute(() -> {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new java.util.Date());
            float dist = 0; int cal = 0;
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                List<WorkoutRecord> recs = db.workoutRecordDao().getAllRecords();
                for (WorkoutRecord r : recs) {
                    if (today.equals(r.getDate())) {
                        dist += (float) r.getDistanceKm();
                        cal += r.getCalories();
                    }
                }
            } catch (Exception ignored) {}

            final float fDist = dist; final int fCal = cal;
            new Handler(Looper.getMainLooper()).post(() -> {
                views.setTextViewText(R.id.widget_stats,
                        String.format(Locale.getDefault(), "今日 %.1f km  %d kcal", fDist, fCal));
                mgr.updateAppWidget(id, views);
            });
        });
    }
}
