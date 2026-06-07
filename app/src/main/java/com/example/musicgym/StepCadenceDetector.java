package com.example.musicgym;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * 步频检测器 — 通过加速度传感器实时计算跑步步频(spm)。
 * 用于 BPM 音乐匹配引擎。
 */
public class StepCadenceDetector implements SensorEventListener {

    public interface CadenceListener {
        void onCadenceChanged(int stepsPerMinute);
    }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CadenceListener listener;

    private final List<Float> buffer = new ArrayList<>();
    private long lastPeakTime;
    private int stepCount;
    private int currentCadence;
    private boolean active;

    private static final float PEAK_THRESHOLD = 10.2f; // 垂直加速度峰值阈值
    private static final int MIN_STEP_INTERVAL_MS = 250; // 两步之间最少间隔

    public StepCadenceDetector(Context ctx) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public boolean isAvailable() { return accelerometer != null; }

    public void setListener(CadenceListener l) { this.listener = l; }

    /** 开始检测 */
    public void start() {
        if (accelerometer == null || active) return;
        active = true;
        stepCount = 0;
        lastPeakTime = 0;
        buffer.clear();
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME); // ~20ms 采样

        // 每 3 秒计算一次步频
        handler.postDelayed(cadenceUpdater, 3000);
        // 每 3 秒重置计数
        handler.postDelayed(stepResetter, 3000);
    }

    /** 停止检测 */
    public void stop() {
        active = false;
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(cadenceUpdater);
        handler.removeCallbacks(stepResetter);
    }

    // ── 传感器回调 ──

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!active) return;
        float z = Math.abs(event.values[2]); // 垂直方向加速度绝对值

        buffer.add(z);
        if (buffer.size() >= 30) { // 约 0.6 秒的采样
            detectPeaks();
            buffer.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ── 峰值检测 ──

    private void detectPeaks() {
        for (int i = 1; i < buffer.size() - 1; i++) {
            float prev = buffer.get(i - 1);
            float curr = buffer.get(i);
            float next = buffer.get(i + 1);

            if (curr > prev && curr > next && curr > PEAK_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastPeakTime > MIN_STEP_INTERVAL_MS) {
                    stepCount++;
                    lastPeakTime = now;
                }
            }
        }
    }

    // ── 定时任务 ──

    private final Runnable cadenceUpdater = new Runnable() {
        @Override public void run() {
            if (!active) return;
            currentCadence = stepCount * 20; // 3秒步数 × 20 = 每分钟步频
            if (listener != null) listener.onCadenceChanged(currentCadence);
            handler.postDelayed(this, 3000);
        }
    };

    private final Runnable stepResetter = new Runnable() {
        @Override public void run() {
            if (!active) return;
            stepCount = 0;
            handler.postDelayed(this, 3000);
        }
    };

    public int getCurrentCadence() { return currentCadence; }
}
