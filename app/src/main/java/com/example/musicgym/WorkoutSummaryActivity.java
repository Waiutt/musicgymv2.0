package com.example.musicgym;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class WorkoutSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_summary);

        String sport = getIntent().getStringExtra("sport_type");
        double distKm = getIntent().getDoubleExtra("distance_km", 0);
        int durSec = getIntent().getIntExtra("duration_sec", 0);
        int cal = getIntent().getIntExtra("calories", 0);
        String date = getIntent().getStringExtra("date");
        String pathJson = getIntent().getStringExtra("path_json");

        // 添加"查看路线"按钮
        TextView btnRoute = findViewById(R.id.summary_btn_route);
        if (pathJson != null && !pathJson.isEmpty() && !pathJson.equals("[]")) {
            btnRoute.setVisibility(View.VISIBLE);
            btnRoute.setOnClickListener(v -> {
                Intent i = new Intent(this, RoutePlaybackActivity.class);
                i.putExtra("path_json", pathJson);
                i.putExtra("sport_type", sport);
                i.putExtra("distance_km", (float) distKm);
                i.putExtra("duration_sec", durSec);
                startActivity(i);
            });
        }

        TextView tvSport = findViewById(R.id.summary_sport);
        tvSport.setText(sport + " 完成 ✓");

        TextView tvDate = findViewById(R.id.summary_date);
        tvDate.setText(date);

        ((TextView) findViewById(R.id.summary_distance)).setText(String.format(Locale.getDefault(), "%.2f", distKm));

        int min = durSec / 60, sec = durSec % 60;
        ((TextView) findViewById(R.id.summary_duration)).setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));

        ((TextView) findViewById(R.id.summary_calories)).setText(String.valueOf(cal));

        if (distKm > 0 && durSec > 0) {
            double paceSec = durSec / distKm;
            int pm = (int) paceSec / 60, ps = (int) paceSec % 60;
            ((TextView) findViewById(R.id.summary_pace)).setText(String.format(Locale.getDefault(), "%d'%02d''", pm, ps));
        }

        findViewById(R.id.summary_btn_done).setOnClickListener(v -> finish());
    }
}
