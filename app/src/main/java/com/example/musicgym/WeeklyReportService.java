package com.example.musicgym;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 周一自动周报推送 — 总结上周运动数据 */
public class WeeklyReportService extends BroadcastReceiver {

    private static final String CHANNEL_ID = "weekly_report";
    private static final int NOTIFY_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "运动周报", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("每周运动数据总结");
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<WorkoutRecord> wRecs = db.workoutRecordDao().getAllRecords();
                List<StrengthRecord> sRecs = db.strengthRecordDao().getAllRecords();

                // 计算上周数据
                Calendar cal = Calendar.getInstance();
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                cal.add(Calendar.DAY_OF_YEAR, -(dow == 1 ? 13 : dow + 5)); // 上周一
                String from = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                cal.add(Calendar.DAY_OF_YEAR, 6);
                String to = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

                double dist = 0; int count = 0, sec = 0, calBurn = 0;
                for (WorkoutRecord r : wRecs) {
                    if (r.getDate() != null && r.getDate().compareTo(from) >= 0
                            && r.getDate().compareTo(to) <= 0) {
                        dist += r.getDistanceKm(); count++; sec += r.getDurationSeconds();
                        calBurn += r.getCalories();
                    }
                }
                int sCount = 0;
                for (StrengthRecord r : sRecs) {
                    if (r.getDate() != null && r.getDate().compareTo(from) >= 0
                            && r.getDate().compareTo(to) <= 0) sCount++;
                }

                if (count + sCount == 0) return; // 上周没运动，不推送

                String title = "🏃 上周运动周报";
                String content = String.format(Locale.getDefault(),
                        "%d次有氧 · %.1fkm · %dmin · 🔥%dkcal | %d次力量",
                        count, dist, sec / 60, calBurn, sCount);

                NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title).setContentText(content)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

                Intent open = new Intent(context, MainActivity.class);
                b.setContentIntent(PendingIntent.getActivity(context, 0, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                        .notify(NOTIFY_ID, b.build());
            } catch (Exception ignored) {}
        });
    }

    /** 注册周一早9点周报闹钟 */
    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WeeklyReportService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.before(Calendar.getInstance())) cal.add(Calendar.WEEK_OF_YEAR, 1);

        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, pi);
    }

    /** 取消周报 */
    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2,
                new Intent(context, WeeklyReportService.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
