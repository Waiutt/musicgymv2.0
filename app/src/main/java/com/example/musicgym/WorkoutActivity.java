package com.example.musicgym;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutActivity extends AppCompatActivity implements AMapLocationListener {

    private MapView mapView;
    private AMap aMap;
    private AMapLocationClient locationClient;

    private TextView tvSportTitle, tvTimer;
    private TextView tvDistance, tvCalories, tvPace;
    private TextView btnAction, btnBack;
    private FrameLayout pauseOverlay;
    private TextView btnResume, btnStop;

    private String sportType;
    private String sportTypeEn;

    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline trajectoryLine;
    private boolean isTracking;
    private boolean isPaused;

    private float totalDistanceMeters;
    private int totalSeconds;
    private double calorieMultiplier = 1.036;
    private LatLng lastLatLng;
    private long lastMovementTime;
    private boolean autoPaused;
    private static final int AUTO_PAUSE_SECONDS = 8;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private ExecutorService executorService;

    private static final int LOCATION_PERMISSION_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        try {
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this, true);
        } catch (Exception e) { e.printStackTrace(); }

        setContentView(R.layout.activity_workout);

        sportType = getIntent().getStringExtra("sport_type");
        if (sportType == null) sportType = "跑步";

        mapView = findViewById(R.id.workout_mapView);
        tvSportTitle = findViewById(R.id.workout_sport_title);
        tvTimer = findViewById(R.id.workout_timer);
        tvDistance = findViewById(R.id.workout_distance);
        tvCalories = findViewById(R.id.workout_calories);
        tvPace = findViewById(R.id.workout_pace);
        btnAction = findViewById(R.id.workout_btn_action);
        btnBack = findViewById(R.id.workout_btn_back);
        pauseOverlay = findViewById(R.id.workout_pause_overlay);
        btnResume = findViewById(R.id.workout_btn_resume);
        btnStop = findViewById(R.id.workout_btn_stop);

        tvSportTitle.setText(sportType);

        switch (sportType) {
            case "跑步": sportTypeEn = "Running"; calorieMultiplier = 1.036; break;
            case "骑行": sportTypeEn = "Cycling"; calorieMultiplier = 0.4; break;
            default:     sportTypeEn = "Walking"; calorieMultiplier = 0.7; break;
        }

        executorService = Executors.newSingleThreadExecutor();

        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        btnBack.setOnClickListener(v -> {
            if (isTracking && !isPaused) {
                pauseTracking();
            } else if (isPaused) {
                Toast.makeText(this, "请先结束或继续运动", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        });

        btnAction.setOnClickListener(v -> {
            if (!isTracking) checkPermissionAndStart();
            else if (!isPaused) pauseTracking();
            else resumeTracking();
        });

        btnResume.setOnClickListener(v -> resumeTracking());
        btnStop.setOnClickListener(v -> stopTracking());

        setupTimer();
        initMapLocationStyle();
        requestLocationAndStart();
    }

    private void initMapLocationStyle() {
        try {
            MyLocationStyle style = new MyLocationStyle();
            style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
            style.strokeColor(Color.argb(80, 56, 189, 248));
            style.radiusFillColor(Color.argb(30, 56, 189, 248));
            aMap.setMyLocationStyle(style);
            aMap.setMyLocationEnabled(true);
            aMap.getUiSettings().setMyLocationButtonEnabled(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void requestLocationAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startOnceLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    private void startOnceLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setOnceLocation(true);
            option.setOnceLocationLatest(true);
            locationClient.setLocationOption(option);
            locationClient.setLocationListener(this);
            locationClient.startLocation();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initMapLocationStyle();
                startOnceLocation();
            } else {
                Toast.makeText(this, "需要位置权限才能记录运动轨迹", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onLocationChanged(AMapLocation loc) {
        if (loc != null && loc.getErrorCode() == 0) {
            LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));

            if (isTracking) {
                if (lastLatLng != null) {
                    float distance = AMapUtils.calculateLineDistance(lastLatLng, latLng);
                    if (distance > 1.0f && distance < 100.0f) {
                        totalDistanceMeters += distance;
                        lastMovementTime = System.currentTimeMillis();
                        if (autoPaused) resumeFromAutoPause();
                    }
                }
                lastLatLng = latLng;
                pathPoints.add(latLng);

                if (pathPoints.size() >= 2) {
                    if (trajectoryLine != null) trajectoryLine.remove();
                    trajectoryLine = aMap.addPolyline(new PolylineOptions()
                            .addAll(pathPoints).width(18f).color(ColorTokens.ACCENT_GREEN).setUseTexture(false));
                }

                float speedMs = loc.getSpeed();
                if (speedMs > 0) {
                    int paceSec = (int) (1000 / speedMs);
                    tvPace.setText(String.format(Locale.getDefault(), "%d'%02d''", paceSec / 60, paceSec % 60));
                }
                updateDashboardUI();
            }
        }
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            startTracking();
        }
    }

    private void startTracking() {
        isTracking = true;
        isPaused = false;
        pauseOverlay.setVisibility(View.GONE);
        btnAction.setBackgroundResource(R.drawable.workout_btn_stop);
        btnAction.setText("⏸");
        btnAction.setTextSize(16f);

        pathPoints.clear();
        totalDistanceMeters = 0f;
        totalSeconds = 0;
        lastLatLng = null;
        updateDashboardUI();

        timerHandler.postDelayed(timerRunnable, 1000);

        try {
            if (locationClient != null) {
                locationClient.stopLocation();
                locationClient.onDestroy();
            }
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setInterval(2000);
            locationClient.setLocationOption(option);
            locationClient.setLocationListener(this);
            locationClient.startLocation();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void pauseTracking() {
        isPaused = true;
        timerHandler.removeCallbacks(timerRunnable);

        if (locationClient != null) {
            locationClient.stopLocation();
        }

        btnAction.setBackgroundResource(R.drawable.workout_btn_start);
        btnAction.setText("▶");
        btnAction.setTextSize(20f);
        pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void resumeTracking() {
        isPaused = false;
        pauseOverlay.setVisibility(View.GONE);

        btnAction.setBackgroundResource(R.drawable.workout_btn_stop);
        btnAction.setText("⏸");
        btnAction.setTextSize(16f);

        timerHandler.postDelayed(timerRunnable, 1000);

        try {
            if (locationClient != null) {
                locationClient.stopLocation();
                locationClient.onDestroy();
            }
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setInterval(2000);
            locationClient.setLocationOption(option);
            locationClient.setLocationListener(this);
            locationClient.startLocation();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopTracking() {
        isTracking = false;
        isPaused = false;
        pauseOverlay.setVisibility(View.GONE);
        btnAction.setBackgroundResource(R.drawable.workout_btn_start);
        btnAction.setText("GO");
        btnAction.setTextSize(20f);

        timerHandler.removeCallbacks(timerRunnable);

        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;
        }

        double distKm = totalDistanceMeters / 1000.0;
        int cal = (int) (distKm * 70.0 * calorieMultiplier);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (totalDistanceMeters > 10 && pathPoints.size() >= 2) {
            WorkoutRecord record = new WorkoutRecord();
            record.setDate(date);
            record.setSportType(sportTypeEn);
            record.setDistanceKm(distKm);
            record.setDurationSeconds(totalSeconds);
            record.setCalories(cal);
            record.setPathPointsJson(serializePathPoints());
            executorService.execute(() ->
                    AppDatabase.getInstance(this).workoutRecordDao().insertRecord(record));
        }

        Intent intent = new Intent(this, WorkoutSummaryActivity.class);
        intent.putExtra("sport_type", sportType);
        intent.putExtra("distance_km", distKm);
        intent.putExtra("duration_sec", totalSeconds);
        intent.putExtra("calories", cal);
        intent.putExtra("date", date);
        startActivity(intent);
        finish();
    }

    private String serializePathPoints() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pathPoints.size(); i++) {
            LatLng p = pathPoints.get(i);
            if (i > 0) sb.append(",");
            sb.append("[").append(p.latitude).append(",").append(p.longitude).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private void setupTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    totalSeconds++;
                    updateDashboardUI();
                    // 自动暂停检测
                    if (!autoPaused && lastMovementTime > 0
                            && System.currentTimeMillis() - lastMovementTime > AUTO_PAUSE_SECONDS * 1000L) {
                        autoPaused = true;
                        Toast.makeText(WorkoutActivity.this,
                                "检测到停止移动，运动已自动暂停", Toast.LENGTH_SHORT).show();
                    }
                    if (!autoPaused) timerHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void resumeFromAutoPause() {
        autoPaused = false;
        timerHandler.postDelayed(timerRunnable, 1000);
        Toast.makeText(this, "检测到移动，运动已恢复", Toast.LENGTH_SHORT).show();
    }

    private void updateDashboardUI() {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));

        float km = totalDistanceMeters / 1000f;
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", km));

        int cal = (int) (km * 70.0 * calorieMultiplier);
        tvCalories.setText(String.valueOf(cal));
    }

    // ========== 生命周期 ==========

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationClient != null && (!isTracking || isPaused)) {
            locationClient.stopLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (locationClient != null) locationClient.onDestroy();
        executorService.shutdown();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
