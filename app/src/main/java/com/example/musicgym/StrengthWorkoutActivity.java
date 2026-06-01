package com.example.musicgym;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StrengthWorkoutActivity extends AppCompatActivity {

    private TextView tvTimer, btnSave, btnBack, tvRestTimer, tvToggleMode;
    private LinearLayout container;
    private LinearLayout restOverlay;
    private ScrollView scrollView;
    private ViewPager2 viewPager;
    private ExercisePageAdapter pagerAdapter;

    final Map<String, List<SetEntry>> workoutData = new LinkedHashMap<>();
    // PR 缓存: 动作名 → (maxWeight, reps)
    final Map<String, double[]> prCache = new LinkedHashMap<>();

    private int totalSeconds, restSeconds;
    private boolean timerRunning, restRunning;
    private boolean isSwipeMode;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength_workout);

        tvTimer = findViewById(R.id.sworkout_timer);
        btnSave = findViewById(R.id.sworkout_btn_save);
        btnBack = findViewById(R.id.sworkout_btn_back);
        container = findViewById(R.id.sworkout_container);
        tvRestTimer = findViewById(R.id.sworkout_rest_timer);
        restOverlay = findViewById(R.id.sworkout_rest_overlay);
        scrollView = findViewById(R.id.sworkout_scroll);
        viewPager = findViewById(R.id.sworkout_pager);
        tvToggleMode = findViewById(R.id.sworkout_toggle_mode);

        createNotificationChannel();
        executor = Executors.newSingleThreadExecutor();

        List<String> names = getIntent().getStringArrayListExtra("exercises");
        if (names != null) {
            for (String n : names) {
                List<SetEntry> sets = new ArrayList<>();
                sets.add(new SetEntry(0, 10));
                workoutData.put(n, sets);
            }
        }

        pagerAdapter = new ExercisePageAdapter(this, workoutData, prCache);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        tvToggleMode.setOnClickListener(v -> {
            isSwipeMode = !isSwipeMode;
            if (isSwipeMode) {
                scrollView.setVisibility(View.GONE);
                viewPager.setVisibility(View.VISIBLE);
                tvToggleMode.setText("☰");
                pagerAdapter.refreshNames();
                pagerAdapter.notifyDataSetChanged();
            } else {
                viewPager.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                tvToggleMode.setText("◉");
                buildUI();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (!workoutData.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("放弃训练?").setMessage("当前有未保存的训练数据")
                        .setPositiveButton("放弃", (d, w) -> finish())
                        .setNegativeButton("继续训练", null).show();
            } else finish();
        });
        btnSave.setOnClickListener(v -> saveWorkout());
        tvRestTimer.setOnClickListener(v -> stopRestTimer());

        loadPRData();
        buildUI();
        startTimer();
    }

    // ==================== PR 数据加载 ====================
    private void loadPRData() {
        executor.execute(() -> {
            List<StrengthRecord> records = AppDatabase.getInstance(this).strengthRecordDao().getAllRecords();
            for (StrengthRecord r : records) {
                try {
                    JSONArray arr = new JSONArray(r.getExercisesJson());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject ex = arr.getJSONObject(i);
                        String name = ex.getString("name");
                        JSONArray sets = ex.getJSONArray("sets");
                        for (int j = 0; j < sets.length(); j++) {
                            JSONObject s = sets.getJSONObject(j);
                            double w = s.optDouble("weight", 0);
                            int reps = s.optInt("reps", 0);
                            if (w > 0 && reps > 0) {
                                double epley = w * (1 + reps / 30.0);
                                double[] cur = prCache.get(name);
                                if (cur == null || epley > cur[0]) {
                                    prCache.put(name, new double[]{epley, w, (double) reps});
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    // ==================== 计时器 ====================
    private void startTimer() {
        timerRunning = true;
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (timerRunning) {
                    totalSeconds++;
                    int m = totalSeconds / 60, s = totalSeconds % 60;
                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    void startRestTimer() {
        restSeconds = 90;
        restRunning = true;
        restOverlay.setVisibility(View.VISIBLE);

        // 通知震动
        sendRestNotification();
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (restRunning && restSeconds > 0) {
                    restSeconds--;
                    int m = restSeconds / 60, s = restSeconds % 60;
                    tvRestTimer.setText(String.format(Locale.getDefault(), "休息 %02d:%02d", m, s));
                    if (restSeconds <= 10) {
                        tvRestTimer.setTextColor(Color.parseColor("#ef4444"));
                    }
                    handler.postDelayed(this, 1000);
                } else if (restRunning) {
                    stopRestTimer();
                }
            }
        }, 1000);
    }

    private void stopRestTimer() {
        restRunning = false;
        restOverlay.setVisibility(View.GONE);
        tvRestTimer.setTextColor(Color.parseColor("#f59e0b"));
        Toast.makeText(this, "休息结束，开始下一组!", Toast.LENGTH_SHORT).show();
    }

    private void sendRestNotification() {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, "strength_rest")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("组间休息").setContentText("90 秒倒计时").setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(200, nb.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("strength_rest", "组间休息", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    // ==================== UI 构建 ====================
    private void buildUI() {
        container.removeAllViews();
        int exIdx = 0;
        for (Map.Entry<String, List<SetEntry>> entry : workoutData.entrySet()) {
            String exName = entry.getKey();
            List<SetEntry> sets = entry.getValue();
            final int exI = exIdx++;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#1e293b"));
            card.setPadding(16, 14, 16, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 12); card.setLayoutParams(lp);

            // 头部: 序号 + 动作名 + PR + 容量
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvNum = new TextView(this);
            tvNum.setText(String.valueOf(exI + 1)); tvNum.setTextColor(Color.parseColor("#FC4C02"));
            tvNum.setTextSize(16f); tvNum.setTypeface(null, Typeface.BOLD); tvNum.setPadding(0, 0, 14, 0);
            header.addView(tvNum);

            TextView tvName = new TextView(this);
            tvName.setText(exName); tvName.setTextColor(Color.WHITE); tvName.setTextSize(16f);
            tvName.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            tvName.setLayoutParams(nameLp);
            header.addView(tvName);

            // PR 显示
            double[] pr = prCache.get(exName);
            if (pr != null) {
                TextView tvPR = new TextView(this);
                tvPR.setText(String.format(Locale.getDefault(), "PR %.0fkg", pr[0]));
                tvPR.setTextColor(Color.parseColor("#fbbf24")); tvPR.setTextSize(11f);
                tvPR.setPadding(0, 0, 10, 0);
                header.addView(tvPR);
            }

            double vol = 0;
            for (SetEntry se : sets) vol += se.weight * se.reps;
            TextView tvVol = addView(header, String.format(Locale.getDefault(), "%.0f kg", vol), Color.parseColor("#f59e0b"), 13f, WRAP);
            card.addView(header);

            // 表头
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.HORIZONTAL); labels.setPadding(0, 12, 0, 4);
            for (String h : new String[]{"组", "重量(kg)", "次数", "%1RM", ""}) {
                TextView th = new TextView(this); th.setText(h); th.setTextColor(Color.parseColor("#6b7280")); th.setTextSize(10f);
                LinearLayout.LayoutParams hlp;
                if (h.equals("重量(kg)") || h.equals("次数") || h.equals("%1RM")) hlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                else if (h.equals("")) hlp = new LinearLayout.LayoutParams(34, ViewGroup.LayoutParams.WRAP_CONTENT);
                else { hlp = new LinearLayout.LayoutParams(26, ViewGroup.LayoutParams.WRAP_CONTENT); th.setGravity(Gravity.CENTER); }
                th.setLayoutParams(hlp); labels.addView(th);
            }
            card.addView(labels);

            // 组行
            for (int i = 0; i < sets.size(); i++) {
                SetEntry se = sets.get(i);
                final int si = i;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, 5, 0, 5);

                addView(row, String.valueOf(i + 1), Color.WHITE, 13f, 26, Gravity.CENTER);

                EditText etW = buildEdit(row, String.valueOf(se.weight), true);
                etW.setOnFocusChangeListener((v, f) -> { if (!f) try {
                    sets.get(si).weight = Double.parseDouble(etW.getText().toString());
                    updateCardDisplay(exI);
                } catch (Exception ignored) {} });

                EditText etR = buildEdit(row, String.valueOf(se.reps), false);
                etR.setOnFocusChangeListener((v, f) -> { if (!f) try {
                    sets.get(si).reps = Integer.parseInt(etR.getText().toString());
                    updateCardDisplay(exI);
                } catch (Exception ignored) {} });

                // %1RM 显示
                double oneRM = pr != null ? pr[0] : 0;
                String pct = oneRM > 0 ? String.format(Locale.getDefault(), "%.0f%%", se.weight / oneRM * 100) : "-";
                addView(row, pct, Color.parseColor("#94a3b8"), 11f, WRAP);

                TextView btnDel = addView(row, "−", Color.parseColor("#ef4444"), 18f, 34, Gravity.CENTER);
                btnDel.setOnClickListener(v -> {
                    if (sets.size() > 1) { sets.remove(si); buildUI(); }
                });
                card.addView(row);
            }

            // 1RM 百分比快捷填充
            LinearLayout pctRow = new LinearLayout(this);
            pctRow.setOrientation(LinearLayout.HORIZONTAL);
            pctRow.setPadding(0, 8, 0, 0);
            final double[] prRef = prCache.get(exName);
            if (prRef != null) {
                for (int pctVal : new int[]{50, 65, 75, 85, 95}) {
                    TextView pctBtn = new TextView(this);
                    pctBtn.setText(pctVal + "%");
                    pctBtn.setTextColor(Color.parseColor("#38bdf8")); pctBtn.setTextSize(11f);
                    pctBtn.setBackgroundColor(Color.parseColor("#334155"));
                    pctBtn.setPadding(12, 6, 12, 6);
                    LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(WRAP, WRAP);
                    plp.setMargins(0, 0, 6, 0); pctBtn.setLayoutParams(plp);
                    final int fExI = exI;
                    pctBtn.setOnClickListener(vv -> {
                        double w = Math.round(prRef[0] * pctVal / 100.0 / 2.5) * 2.5;
                        for (SetEntry se : sets) se.weight = w;
                        fillCardWeights(fExI, w);
                        updateCardDisplay(fExI);
                    });
                    pctRow.addView(pctBtn);
                }
            }
            card.addView(pctRow);

            // 添加组 + 休息按钮
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL); actions.setPadding(0, 10, 0, 0);
            TextView btnAdd = new TextView(this);
            btnAdd.setText("+ 添加组"); btnAdd.setTextColor(Color.parseColor("#38bdf8")); btnAdd.setTextSize(13f);
            btnAdd.setPadding(0, 0, 24, 0);
            btnAdd.setOnClickListener(v -> { sets.add(new SetEntry(0, 10)); buildUI(); });
            actions.addView(btnAdd);

            TextView btnRest = new TextView(this);
            btnRest.setText("⏱ 组间休息"); btnRest.setTextColor(Color.parseColor("#f59e0b")); btnRest.setTextSize(13f);
            btnRest.setOnClickListener(v -> startRestTimer());
            actions.addView(btnRest);

            card.addView(actions);
            container.addView(card);
        }
    }

    // ==================== 增量更新 ====================
    private void updateCardDisplay(int exI) {
        if (exI >= container.getChildCount()) return;
        ViewGroup card = (ViewGroup) container.getChildAt(exI);
        if (card.getChildCount() < 3) return;

        LinearLayout header = (LinearLayout) card.getChildAt(0);
        TextView tvVol = (TextView) header.getChildAt(header.getChildCount() - 1);

        int idx = 0;
        String exName = null;
        List<SetEntry> sets = null;
        for (Map.Entry<String, List<SetEntry>> e : workoutData.entrySet()) {
            if (idx == exI) { exName = e.getKey(); sets = e.getValue(); break; }
            idx++;
        }
        if (sets == null) return;

        double vol = 0;
        for (SetEntry se : sets) vol += se.weight * se.reps;
        tvVol.setText(String.format(Locale.getDefault(), "%.0f kg", vol));

        double[] pr = prCache.get(exName);
        double oneRM = pr != null ? pr[0] : 0;
        for (int i = 0; i < sets.size(); i++) {
            int rowIdx = 2 + i;
            if (rowIdx >= card.getChildCount()) break;
            View row = card.getChildAt(rowIdx);
            if (row instanceof LinearLayout && ((LinearLayout) row).getChildCount() >= 4) {
                TextView tvPct = (TextView) ((LinearLayout) row).getChildAt(3);
                SetEntry se = sets.get(i);
                String pct = oneRM > 0 ? String.format(Locale.getDefault(), "%.0f%%", se.weight / oneRM * 100) : "-";
                tvPct.setText(pct);
            }
        }
    }

    private void fillCardWeights(int exI, double weight) {
        if (exI >= container.getChildCount()) return;
        ViewGroup card = (ViewGroup) container.getChildAt(exI);

        int idx = 0;
        List<SetEntry> sets = null;
        for (Map.Entry<String, List<SetEntry>> e : workoutData.entrySet()) {
            if (idx == exI) { sets = e.getValue(); break; }
            idx++;
        }
        if (sets == null) return;

        for (int i = 0; i < sets.size(); i++) {
            int rowIdx = 2 + i;
            if (rowIdx >= card.getChildCount()) break;
            View row = card.getChildAt(rowIdx);
            if (row instanceof LinearLayout && ((LinearLayout) row).getChildCount() >= 2) {
                EditText etW = (EditText) ((LinearLayout) row).getChildAt(1);
                etW.setText(String.valueOf(weight));
            }
        }
    }

    // Pager 数据变更回调 —— 刷新 pager 当前页
    void onPagerDataChanged(int position) {
        pagerAdapter.notifyItemChanged(position);
    }

    private EditText buildEdit(LinearLayout parent, String val, boolean decimal) {
        EditText et = new EditText(this);
        et.setText(val); et.setTextColor(Color.WHITE); et.setTextSize(13f);
        et.setInputType(decimal ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL) : InputType.TYPE_CLASS_NUMBER);
        et.setBackgroundColor(Color.parseColor("#334155")); et.setPadding(12, 8, 12, 8);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        elp.setMargins(0, 0, 4, 0); et.setLayoutParams(elp);
        parent.addView(et);
        return et;
    }

    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private TextView addView(LinearLayout parent, String text, int color, float size, int width) {
        return addView(parent, text, color, size, width, Gravity.START);
    }
    private TextView addView(LinearLayout parent, String text, int color, float size, int width, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(color); tv.setTextSize(size); tv.setGravity(gravity);
        tv.setLayoutParams(new LinearLayout.LayoutParams(width, WRAP));
        parent.addView(tv); return tv;
    }

    // ==================== 保存 ====================
    private void saveWorkout() {
        if (workoutData.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, List<SetEntry>> e : workoutData.entrySet()) {
                JSONObject ex = new JSONObject(); ex.put("name", e.getKey());
                JSONArray sets = new JSONArray();
                for (SetEntry s : e.getValue()) {
                    JSONObject so = new JSONObject();
                    so.put("weight", s.weight); so.put("reps", s.reps); sets.put(so);
                }
                ex.put("sets", sets); arr.put(ex);
            }
            String json = arr.toString();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            StrengthRecord record = new StrengthRecord(date, totalSeconds, json);
            executor.execute(() -> {
                AppDatabase.getInstance(this).strengthRecordDao().insertRecord(record);
                runOnUiThread(() -> {
                    Toast.makeText(this, "训练已保存 ✓", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } catch (Exception e) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    static class SetEntry { double weight; int reps; SetEntry(double w, int r) { weight = w; reps = r; } }

    @Override protected void onDestroy() {
        super.onDestroy(); timerRunning = false; restRunning = false;
        handler.removeCallbacksAndMessages(null); executor.shutdown();
    }
}
