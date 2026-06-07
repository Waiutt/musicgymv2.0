package com.example.musicgym;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
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

public class WorkoutActivity extends AppCompatActivity implements AMapLocationListener, TextToSpeech.OnInitListener {

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
    private List<Polyline> trajectoryLines = new ArrayList<>();
    private boolean isTracking;
    private boolean isPaused;

    // 配速着色
    private double lastSegmentDist;
    private List<LatLng> segmentPoints = new ArrayList<>();
    private static final int[] PACE_COLORS = {
            ColorTokens.ACCENT_CYAN,       // 慢: 蓝
            ColorTokens.ACCENT_GREEN,      // 中: 绿
            ColorTokens.ACCENT_AMBER,      // 快: 橙
            ColorTokens.ACCENT_RED,        // 冲刺: 红
    };

    // 公里标记
    private int nextKmMark = 1;
    private int kmStartSeconds;

    private float totalDistanceMeters;
    private int totalSeconds;
    private double calorieMultiplier = 1.036;
    private LatLng lastLatLng;
    private long lastMovementTime;
    private boolean autoPaused;
    private TextToSpeech tts;
    private double lastAnnouncedKm;
    private View musicControlBar;
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

        // 迷你音乐控制条
        buildMiniMusicControl();

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
        aMap = mapView.getMap();

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
        if (aMap == null) return;
        try {
            MyLocationStyle style = new MyLocationStyle();
            style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
            style.strokeColor(Color.argb(80, 56, 189, 248));
            style.radiusFillColor(Color.argb(30, 56, 189, 248));
            aMap.setMyLocationStyle(style);
            aMap.setMyLocationEnabled(true);
            // 地图样式: 暗色模式(匹配App主题) + 交通路况
            aMap.setMapType(AMap.MAP_TYPE_NIGHT);
            aMap.setTrafficEnabled(true);
            aMap.getUiSettings().setMyLocationButtonEnabled(false);
            aMap.getUiSettings().setZoomControlsEnabled(false);
            aMap.getUiSettings().setCompassEnabled(true);
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
                segmentPoints.add(latLng);

                // 配速着色: 每100米画一段
                float segDist = AMapUtils.calculateLineDistance(
                        segmentPoints.get(0), latLng);
                if (segDist > 100 || pathPoints.size() < 2) {
                    drawPaceSegment();
                }

                // 公里标记
                if (totalDistanceMeters >= nextKmMark * 1000) {
                    int kmTime = totalSeconds - kmStartSeconds;
                    aMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(
                                    createKmBitmap(nextKmMark, kmTime)))
                            .anchor(0.5f, 0.5f));
                    kmStartSeconds = totalSeconds;
                    nextKmMark++;
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
        autoPaused = false;
        lastMovementTime = 0;
        lastAnnouncedKm = 0;
        if (tts == null) tts = new TextToSpeech(this, this);
        pauseOverlay.setVisibility(View.GONE);
        btnAction.setBackgroundResource(R.drawable.workout_btn_stop);
        btnAction.setText("⏸");
        btnAction.setTextSize(16f);

        pathPoints.clear();
        totalDistanceMeters = 0f;
        segmentPoints.clear();
        for (Polyline pl : trajectoryLines) pl.remove();
        trajectoryLines.clear();
        nextKmMark = 1;
        kmStartSeconds = 0;
        if (musicControlBar != null) musicControlBar.setVisibility(View.VISIBLE);
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
        if (musicControlBar != null) musicControlBar.setVisibility(View.GONE);
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

        // 自动发布社区动态
        autoPostWorkout(sportType, distKm, totalSeconds, cal);

        new android.app.AlertDialog.Builder(this)
                .setTitle(sportType + " 完成！")
                .setMessage(String.format(Locale.getDefault(),
                        "距离: %.2f km\n时长: %d:%02d\n卡路里: %d kcal",
                        distKm, totalSeconds / 60, totalSeconds % 60, cal))
                .setPositiveButton("再来一组", (d, w) -> {
                    totalDistanceMeters = 0; totalSeconds = 0;
                    pathPoints.clear(); lastLatLng = null;
                    updateDashboardUI(); checkPermissionAndStart();
                })
                .setNegativeButton("查看总结", (d, w) -> {
                    Intent intent = new Intent(this, WorkoutSummaryActivity.class);
                    intent.putExtra("sport_type", sportType);
                    intent.putExtra("distance_km", distKm);
                    intent.putExtra("duration_sec", totalSeconds);
                    intent.putExtra("calories", cal);
                    intent.putExtra("date", date);
                    intent.putExtra("path_json", serializePathPoints());
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void drawPaceSegment() {
        if (segmentPoints.size() < 2) return;

        // 根据速度选择合适的颜色
        float paceSec = totalSeconds > 0 ? totalSeconds / (totalDistanceMeters / 1000f) : 300;
        int colorIdx = paceSec < 270 ? 3 : paceSec < 330 ? 2 : paceSec < 390 ? 1 : 0;

        Polyline line = aMap.addPolyline(new PolylineOptions()
                .addAll(new ArrayList<>(segmentPoints))
                .width(16f).color(PACE_COLORS[colorIdx])
                .setUseTexture(false));
        trajectoryLines.add(line);
        segmentPoints.clear();
        // 保留最后一个点作为下段起点
        segmentPoints.add(pathPoints.get(pathPoints.size() - 1));
    }

    private android.graphics.Bitmap createKmBitmap(int km, int seconds) {
        int m = seconds / 60, s = seconds % 60;
        String text = km + "km " + m + ":" + String.format("%02d", s);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(28f); paint.setColor(Color.WHITE);
        paint.setAntiAlias(true); paint.setFakeBoldText(true);
        paint.setShadowLayer(3, 1, 1, Color.BLACK);

        float w = paint.measureText(text) + 16;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                (int) w, 40, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        paint.setColor(Color.argb(160, 15, 23, 42));
        canvas.drawRoundRect(0, 0, w, 40, 8, 8, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, 8, 28, paint);
        return bmp;
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

        // 每公里语音播报
        if (tts != null && km >= lastAnnouncedKm + 1.0 && km > 0.1) {
            lastAnnouncedKm = Math.floor(km);
            int paceSec = totalSeconds > 0 ? (int) (totalSeconds / km) : 0;
            String msg = "已跑 " + (int) km + " 公里, 配速 " + (paceSec / 60) + " 分 " + (paceSec % 60) + " 秒";
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "workout_announce");
        }
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
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && tts != null)
            tts.setLanguage(java.util.Locale.CHINESE);
    }

    private void buildMiniMusicControl() {
        ViewGroup root = findViewById(android.R.id.content);
        musicControlBar = new LinearLayout(this);
        ((LinearLayout) musicControlBar).setOrientation(LinearLayout.HORIZONTAL);
        ((LinearLayout) musicControlBar).setGravity(Gravity.CENTER);
        musicControlBar.setBackgroundColor(Color.argb(180, 15, 23, 42));
        musicControlBar.setPadding(16, 8, 16, 8);
        musicControlBar.setVisibility(View.GONE);

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 56);
        flp.gravity = Gravity.TOP;
        flp.topMargin = 56; // below top bar

        String[] labels = {"⏮", "▶", "⏭"};
        for (String label : labels) {
            TextView btn = new TextView(this);
            btn.setText(label); btn.setTextColor(Color.WHITE); btn.setTextSize(20f);
            btn.setGravity(Gravity.CENTER); btn.setPadding(24, 8, 24, 8);
            btn.setOnClickListener(v -> {
                if (label.equals("⏮")) sendMusicCommand("PREV");
                else if (label.equals("▶")) sendMusicCommand("PLAY_PAUSE");
                else sendMusicCommand("NEXT");
            });
            ((LinearLayout) musicControlBar).addView(btn);
        }
        root.addView(musicControlBar, flp);
    }

    private void sendMusicCommand(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("com.example.musicgym." + action);
        startService(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void autoPostWorkout(String sport, double distKm, int sec, int cal) {
        if (distKm < 0.1) return; // 太短不发
        String title = String.format(Locale.getDefault(),
                "完成了 %.1f km %s", distKm, sport);
        String content = String.format(Locale.getDefault(),
                "距离: %.2f km | 时长: %d:%02d | 🔥 %d kcal",
                distKm, sec / 60, sec % 60, cal);
        UserManager.get(this).signIn((userId, nickname) ->
                new CommunityRepository().publishPost(userId, nickname, title, content, null, null));
    }
}
