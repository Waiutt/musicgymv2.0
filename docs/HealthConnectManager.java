package com.example.musicgym;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.records.SleepSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Google Health Connect — 统一健康数据入口（所有品牌手表通用） */
public class HealthConnectManager {

    static final int REQUEST_CODE = 9999;

    private static final Set<String> HC_PERMS = new HashSet<>();
    static {
        HC_PERMS.add("android.permission.health.READ_STEPS");
        HC_PERMS.add("android.permission.health.READ_WEIGHT");
        HC_PERMS.add("android.permission.health.READ_SLEEP");
        HC_PERMS.add("android.permission.health.READ_HEART_RATE");
    }

    private HealthConnectClient client;

    public HealthConnectManager(Context ctx) {
        try { client = HealthConnectClient.getOrCreate(ctx); }
        catch (Exception e) { client = null; }
    }

    public boolean isReady() { return client != null; }

    public void requestPermissions(Activity activity) {
        if (client == null) { openPlayStore(activity); return; }
        try {
            activity.startActivityForResult(
                    client.getPermissionController()
                            .createRequestPermissionResultContract()
                            .createIntent(activity, HC_PERMS), REQUEST_CODE);
        } catch (Exception e) { openPlayStore(activity); }
    }

    private void openPlayStore(Context ctx) {
        try { ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.apps.healthdata")));
        } catch (Exception ignored) {}
    }

    public float getLatestWeight() {
        try {
            Instant end = Instant.now();
            List<WeightRecord> list = client.readRecords(
                    new ReadRecordsRequest.Builder<>(WeightRecord.class)
                            .setTimeRangeFilter(TimeRangeFilter.between(
                                    end.minus(90, ChronoUnit.DAYS), end))
                            .setPageSize(1).setAscending(false).build()
            ).getRecords();
            return list.isEmpty() ? -1 : (float) list.get(0).getWeight().getInKilograms();
        } catch (Exception e) { return -1; }
    }

    public float getLastSleepHours() {
        try {
            Instant end = Instant.now();
            List<SleepSessionRecord> list = client.readRecords(
                    new ReadRecordsRequest.Builder<>(SleepSessionRecord.class)
                            .setTimeRangeFilter(TimeRangeFilter.between(
                                    end.minus(2, ChronoUnit.DAYS), end))
                            .setPageSize(1).setAscending(false).build()
            ).getRecords();
            if (list.isEmpty()) return -1;
            SleepSessionRecord r = list.get(0);
            return (r.getEndTime().toEpochMilli() - r.getStartTime().toEpochMilli()) / 3600000f;
        } catch (Exception e) { return -1; }
    }

    public int getTodaySteps() {
        try {
            Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
            List<StepsRecord> list = client.readRecords(
                    new ReadRecordsRequest.Builder<>(StepsRecord.class)
                            .setTimeRangeFilter(TimeRangeFilter.between(start, Instant.now()))
                            .build()
            ).getRecords();
            int t = 0;
            for (StepsRecord r : list) t += (int) r.getCount();
            return t;
        } catch (Exception e) { return -1; }
    }

    public int getRestingHeartRate() {
        try {
            Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
            List<HeartRateRecord> list = client.readRecords(
                    new ReadRecordsRequest.Builder<>(HeartRateRecord.class)
                            .setTimeRangeFilter(TimeRangeFilter.between(start, Instant.now()))
                            .build()
            ).getRecords();
            if (list.isEmpty()) return -1;
            int sum = 0, count = 0;
            for (HeartRateRecord r : list)
                for (HeartRateRecord.Sample s : r.getSamples()) {
                    sum += (int) s.getBeatsPerMinute(); count++;
                }
            return count > 0 ? sum / count : -1;
        } catch (Exception e) { return -1; }
    }

    public HealthSnapshot fetchAll() {
        HealthSnapshot s = new HealthSnapshot();
        s.weightKg = getLatestWeight();
        s.sleepHours = getLastSleepHours();
        s.steps = getTodaySteps();
        s.restingHr = getRestingHeartRate();
        s.hasData = s.weightKg > 0 || s.steps > 0 || s.sleepHours > 0;
        return s;
    }

    public static class HealthSnapshot {
        public float weightKg = -1, sleepHours = -1;
        public int steps = -1, restingHr = -1;
        public boolean hasData;
    }
}
