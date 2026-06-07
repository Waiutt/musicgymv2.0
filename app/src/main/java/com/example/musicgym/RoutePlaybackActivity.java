package com.example.musicgym;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** 路线回放 — 在地图上动画展示历史运动轨迹 */
public class RoutePlaybackActivity extends AppCompatActivity {

    private MapView mapView;
    private AMap aMap;
    private TextView tvTitle, btnPlay, tvInfo;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Marker playMarker;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean playing, isSatellite;
    private int playIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_playback);

        mapView = findViewById(R.id.route_map_view);
        mapView.onCreate(savedInstanceState);
        tvTitle = findViewById(R.id.route_tv_title);
        btnPlay = findViewById(R.id.route_btn_play);
        tvInfo = findViewById(R.id.route_info_text);

        findViewById(R.id.route_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.route_btn_satellite).setOnClickListener(v -> {
            isSatellite = !isSatellite;
            aMap.setMapType(isSatellite ? AMap.MAP_TYPE_SATELLITE : AMap.MAP_TYPE_NIGHT);
        });
        btnPlay.setOnClickListener(v -> togglePlayback());

        aMap = mapView.getMap();
        if (aMap != null) loadAndDrawRoute();
    }

    private void loadAndDrawRoute() {
        // 暗色地图 + 交通
        aMap.setMapType(AMap.MAP_TYPE_NIGHT);
        aMap.setTrafficEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);

        String json = getIntent().getStringExtra("path_json");
        String sport = getIntent().getStringExtra("sport_type");
        float dist = getIntent().getFloatExtra("distance_km", 0);
        int sec = getIntent().getIntExtra("duration_sec", 0);

        tvTitle.setText(sport != null ? sport + " 路线" : "路线回放");
        tvInfo.setText(String.format(java.util.Locale.getDefault(),
                "总距离 %.2f km | 时长 %d:%02d", dist, sec / 60, sec % 60));

        if (json == null || json.isEmpty() || json.equals("[]")) {
            tvInfo.setText("无路线数据");
            return;
        }

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray pt = arr.getJSONArray(i);
                pathPoints.add(new LatLng(pt.getDouble(0), pt.getDouble(1)));
            }
        } catch (Exception e) { return; }

        if (pathPoints.size() < 2 || aMap == null) return;

        // 绘制完整路线
        aMap.addPolyline(new PolylineOptions()
                .addAll(pathPoints)
                .width(14f).color(Color.parseColor("#22c55e"))
                .useGradient(true));

        // 缩放到包含全路线
        LatLngBounds.Builder bounds = LatLngBounds.builder();
        for (LatLng p : pathPoints) bounds.include(p);
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));

        // 起点标记
        aMap.addMarker(new MarkerOptions()
                .position(pathPoints.get(0))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("起点"));
        // 终点标记
        aMap.addMarker(new MarkerOptions()
                .position(pathPoints.get(pathPoints.size() - 1))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title("终点"));
    }

    private void togglePlayback() {
        if (pathPoints.size() < 2) return;
        if (playing) {
            stopPlayback();
            btnPlay.setText("▶ 播放");
        } else {
            startPlayback();
            btnPlay.setText("⏸ 暂停");
        }
    }

    private void startPlayback() {
        if (playing) return;
        playing = true;
        if (playMarker != null) playMarker.remove();

        playMarker = aMap.addMarker(new MarkerOptions()
                .position(pathPoints.get(0))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .anchor(0.5f, 0.5f));

        playIndex = 0;
        handler.post(playRunnable);
    }

    private void stopPlayback() {
        playing = false;
        handler.removeCallbacks(playRunnable);
    }

    private final Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (!playing || playIndex >= pathPoints.size()) {
                playing = false;
                btnPlay.setText("▶ 播放");
                return;
            }
            LatLng p = pathPoints.get(playIndex);
            playMarker.setPosition(p);
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 17f));

            // 每帧更新进度
            if (pathPoints.size() > 1) {
                float progress = (float) playIndex / (pathPoints.size() - 1) * 100;
                tvInfo.setText(String.format(java.util.Locale.getDefault(),
                        "回放中 %.0f%%  |  点 %d/%d", progress, playIndex + 1, pathPoints.size()));
            }

            playIndex += 3; // 每次跳3个点(加速回放)
            handler.postDelayed(this, 80); // 80ms/帧
        }
    };

    // ── 生命周期 ──
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        stopPlayback();
        mapView.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mapView.onSaveInstanceState(out);
    }
}
