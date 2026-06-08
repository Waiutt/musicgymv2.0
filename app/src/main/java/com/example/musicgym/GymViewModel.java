package com.example.musicgym;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** GYM 页 ViewModel — 运动卡片上次训练信息 */
public class GymViewModel extends AndroidViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> runInfo = new MutableLiveData<>("");
    private final MutableLiveData<String> cycleInfo = new MutableLiveData<>("");
    private final MutableLiveData<String> walkInfo = new MutableLiveData<>("");
    private final MutableLiveData<String> strengthInfo = new MutableLiveData<>("");

    public GymViewModel(@NonNull Application app) {
        super(app);
    }

    // ── Getters ──
    public LiveData<String> getRunInfo() { return runInfo; }
    public LiveData<String> getCycleInfo() { return cycleInfo; }
    public LiveData<String> getWalkInfo() { return walkInfo; }
    public LiveData<String> getStrengthInfo() { return strengthInfo; }

    // ── 加载上次训练 ──
    public void loadLastWorkouts() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            List<WorkoutRecord> records = db.workoutRecordDao().getAllRecords();
            List<StrengthRecord> sRecs = db.strengthRecordDao().getAllRecords();

            // 有氧运动
            for (WorkoutRecord r : records) {
                String info = "上次 " + (r.getDate() != null ? r.getDate().substring(5) : "")
                        + " · " + String.format(Locale.getDefault(), "%.1fkm", r.getDistanceKm());
                String type = r.getSportType();
                if ("Running".equals(type)) runInfo.postValue(info);
                else if ("Cycling".equals(type)) cycleInfo.postValue(info);
                else if ("Walking".equals(type)) walkInfo.postValue(info);
            }

            // 力量训练
            if (sRecs != null && !sRecs.isEmpty()) {
                StrengthRecord r = sRecs.get(0);
                if (r.getDate() != null) {
                    strengthInfo.postValue("上次 " + r.getDate().substring(5)
                            + " · " + (r.getDurationSeconds() / 60) + "min");
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
