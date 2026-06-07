package com.example.musicgym;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvWeight, tvHeight;
    private Button btnEditProfile, btnMeasurements, btnExport, btnReminder;
    private LinearLayout historyContainer, measureContainer;
    private LineChart weightChart;
    private SharedPreferences prefs;
    private AppDatabase db;
    private ExecutorService executor;
    private ImageView ivAvatar;
    private Uri avatarUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap bmp = (Bitmap) extras.get("data");
                    if (bmp != null) {
                        ivAvatar.setImageBitmap(bmp);
                        saveAvatarToCache(bmp);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(), uri);
                        ivAvatar.setImageBitmap(bmp);
                        saveAvatarToCache(bmp);
                    } catch (Exception ignored) {}
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvWeight = view.findViewById(R.id.tv_weight);
        tvHeight = view.findViewById(R.id.tv_height);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnMeasurements = view.findViewById(R.id.btn_measurements);
        btnExport = view.findViewById(R.id.btn_export);
        btnReminder = view.findViewById(R.id.btn_reminder);
        historyContainer = view.findViewById(R.id.history_container);
        measureContainer = view.findViewById(R.id.measure_container);

        prefs = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();

        executor.execute(this::migrateWeightHistoryIfNeeded);
        loadUserData();

        // 头像点击
        ivAvatar = view.findViewById(R.id.iv_avatar);
        ivAvatar.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("设置头像")
                    .setItems(new String[]{"📷 拍照", "🖼 从相册选择"}, (d, which) -> {
                        if (which == 0) cameraLauncher.launch(
                                new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                        else galleryLauncher.launch("image/*");
                    }).show();
        });
        // 加载已缓存的头像
        loadCachedAvatar();

        // 成就检查
        executor.execute(() -> {
            AchievementManager am = new AchievementManager(requireContext());
            List<String> newBadges = am.checkAndUnlock(db);
            if (!newBadges.isEmpty()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "🏆 解锁成就: " + newBadges.get(0) + "!",
                                Toast.LENGTH_LONG).show());
            }
        });

        // 体重趋势图 — 动态插入到按钮上方
        ViewGroup rootLayout = (ViewGroup) view.findViewById(R.id.profile_root);
        View btnFirst = view.findViewById(R.id.btn_edit_profile);
        int btnIdx = rootLayout.indexOfChild(btnFirst);

        weightChart = new LineChart(requireContext());
        weightChart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp2px(180)));
        weightChart.setPadding(dp2px(4), dp2px(12), dp2px(4), 0);
        weightChart.getDescription().setEnabled(false);
        weightChart.setTouchEnabled(false);
        weightChart.setBackgroundColor(ColorTokens.BG_CARD);
        weightChart.getLegend().setTextColor(Color.WHITE);
        weightChart.getLegend().setTextSize(10f);
        weightChart.getXAxis().setTextColor(ColorTokens.TEXT_SECONDARY);
        weightChart.getXAxis().setTextSize(9f);
        weightChart.getXAxis().setPosition(
                com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        weightChart.getAxisLeft().setTextColor(ColorTokens.TEXT_SECONDARY);
        weightChart.getAxisLeft().setTextSize(9f);
        weightChart.getAxisRight().setEnabled(false);
        rootLayout.addView(weightChart, btnIdx);

        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnMeasurements.setOnClickListener(v -> showMeasurementsDialog());
        btnExport.setOnClickListener(v -> exportCSV());
        btnReminder.setOnClickListener(v -> setupReminder());
        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        return view;
    }

    private void loadUserData() {
        tvUserName.setText(prefs.getString("USER_NAME", "点击设置昵称"));
        tvWeight.setText(prefs.getString("USER_WEIGHT", "70.0"));
        tvHeight.setText(prefs.getString("USER_HEIGHT", "170.0"));
        loadHistoryUI();
        loadMeasurementsUI();
    }

    private void migrateWeightHistoryIfNeeded() {
        String hs = prefs.getString("WEIGHT_HISTORY", "");
        if (hs.isEmpty()) return;
        for (String e : hs.split(",")) {
            e = e.trim(); if (e.isEmpty()) continue;
            String[] p = e.split(":");
            if (p.length >= 2) try {
                db.weightRecordDao().insertRecord(new WeightRecord(p[0].trim(), Double.parseDouble(p[1].trim().replace("kg","").trim())));
            } catch (Exception ignored) {}
        }
        prefs.edit().remove("WEIGHT_HISTORY").apply();
    }

    private void loadHistoryUI() {
        historyContainer.removeAllViews();
        executor.execute(() -> {
            List<WeightRecord> records = db.weightRecordDao().getAllRecords();
            // 计算连续打卡天数
            int streak = calcStreak();
            List<Entry> wtEntries = new ArrayList<>();
            for (int i = 0; i < Math.min(records.size(), 60); i++) {
                WeightRecord r = records.get(i);
                wtEntries.add(new Entry(i, (float) r.getWeightKg()));
            }
            java.util.Collections.reverse(wtEntries);

            requireActivity().runOnUiThread(() -> {
                // 连续打卡
                TextView tvStreak = new TextView(getContext());
                tvStreak.setText("🔥 连续运动 " + streak + " 天");
                tvStreak.setTextColor(streak >= 7 ? ColorTokens.BRAND_ORANGE : ColorTokens.TEXT_SECONDARY);
                tvStreak.setTextSize(15f);
                tvStreak.setPadding(0, 0, 0, dp2px(8));
                historyContainer.addView(tvStreak);

                // 体重折线图
                if (wtEntries.size() >= 2) {
                    LineDataSet ds = new LineDataSet(wtEntries, "体重(kg)");
                    ds.setColor(ColorTokens.ACCENT_CYAN);
                    ds.setCircleColor(ColorTokens.ACCENT_CYAN);
                    ds.setLineWidth(2.5f);
                    ds.setCircleRadius(4f);
                    ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    ds.setValueTextSize(0f);
                    ds.setDrawFilled(true);
                    ds.setFillColor(ColorTokens.ACCENT_CYAN);
                    ds.setFillAlpha(30);

                    LineData data = new LineData(ds);
                    weightChart.setData(data);
                    weightChart.getXAxis().setAxisMinimum(0f);
                    weightChart.getXAxis().setAxisMaximum(wtEntries.size() - 1f);
                    weightChart.getAxisLeft().setAxisMinimum(
                            (float) (getMinWeight(wtEntries) - 2));
                    weightChart.animateX(500);
                    weightChart.invalidate();
                    weightChart.setVisibility(View.VISIBLE);
                } else {
                    weightChart.setVisibility(View.GONE);
                }

                // 最近记录文字
                if (records.size() >= 2) {
                    WeightRecord latest = records.get(0);
                    WeightRecord prev = records.get(1);
                    double diff = latest.getWeightKg() - prev.getWeightKg();
                    String trend = diff < 0 ? "↓ " + String.format("%.1f", Math.abs(diff)) + "kg"
                            : diff > 0 ? "↑ +" + String.format("%.1f", diff) + "kg" : "→ 持平";
                    TextView tvTrend = new TextView(getContext());
                    tvTrend.setText("最近: " + latest.getWeightKg() + "kg  " + trend);
                    tvTrend.setTextColor(diff < 0 ? ColorTokens.ACCENT_GREEN_SOFT :
                            diff > 0 ? ColorTokens.ACCENT_AMBER : ColorTokens.TEXT_SECONDARY);
                    tvTrend.setTextSize(14f);
                    tvTrend.setPadding(0, dp2px(4), 0, 0);
                    historyContainer.addView(tvTrend);
                } else if (!records.isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText("最新: " + records.get(0).getWeightKg() + " kg (至少需2条记录才能显示趋势)");
                    tv.setTextColor(ColorTokens.TEXT_SECONDARY);
                    tv.setTextSize(13f);
                    tv.setPadding(0, dp2px(4), 0, 0);
                    historyContainer.addView(tv);
                }
            });
        });
    }

    private float getMinWeight(List<Entry> entries) {
        float min = Float.MAX_VALUE;
        for (Entry e : entries) if (e.getY() < min) min = e.getY();
        return min == Float.MAX_VALUE ? 60 : min;
    }

    private int calcStreak() {
        try {
            HashSet<String> activeDays = new HashSet<>();
            for (WorkoutRecord r : db.workoutRecordDao().getAllRecords())
                if (r.getDate() != null) activeDays.add(r.getDate());
            for (StrengthRecord r : db.strengthRecordDao().getAllRecords())
                if (r.getDate() != null) activeDays.add(r.getDate());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            int streak = 0;
            while (streak < 365) {
                String day = sdf.format(cal.getTime());
                if (activeDays.contains(day)) { streak++; cal.add(Calendar.DAY_OF_YEAR, -1); }
                else break;
            }
            return streak;
        } catch (Exception e) { return 0; }
    }

    private void saveAvatarToCache(Bitmap bmp) {
        try {
            File dir = new File(requireContext().getCacheDir(), "avatars");
            dir.mkdirs();
            File f = new File(dir, "avatar.png");
            FileOutputStream os = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, os);
            os.close();
            prefs.edit().putString("avatar_path", f.getAbsolutePath()).apply();
        } catch (Exception ignored) {}
    }

    private void loadCachedAvatar() {
        String path = prefs.getString("avatar_path", "");
        if (!path.isEmpty()) {
            try {
                ivAvatar.setImageBitmap(BitmapFactory.decodeFile(path));
            } catch (Exception ignored) {}
        }
    }

    private int dp2px(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    private void loadMeasurementsUI() {
        measureContainer.removeAllViews();
        executor.execute(() -> {
            BodyMeasurement latest = db.bodyMeasurementDao().getLatest();
            requireActivity().runOnUiThread(() -> {
                if (latest == null) {
                    TextView tv = new TextView(getContext());
                    tv.setText("暂无围度数据"); tv.setTextColor(ColorTokens.TEXT_SECONDARY);
                    measureContainer.addView(tv); return;
                }
                String[] labels = {"胸围","腰围","臀围","臂围","腿围"};
                double[] vals = {latest.getChestCm(),latest.getWaistCm(),latest.getHipCm(),latest.getArmCm(),latest.getThighCm()};
                for (int i=0;i<labels.length;i++) {
                    TextView tv = new TextView(getContext());
                    tv.setText(labels[i] + ": " + vals[i] + " cm");
                    tv.setTextColor(ColorTokens.ACCENT_CYAN); tv.setTextSize(13);
                    tv.setPadding(0,6,0,6); measureContainer.addView(tv);
                }
            });
        });
    }

    private void showEditDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(60,40,60,10);
        EditText en = addEdit(layout, "Name", tvUserName.getText().toString());
        EditText ew = addEdit(layout, "Weight (kg)", tvWeight.getText().toString()); ew.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText eh = addEdit(layout, "Height (cm)", tvHeight.getText().toString()); eh.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(getContext()).setTitle("编辑资料").setView(layout)
            .setPositiveButton("保存", (d,w)->{
                String nw = ew.getText().toString();
                if (!nw.equals(tvWeight.getText().toString()) && !nw.isEmpty()) {
                    String date = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date());
                    executor.execute(()-> db.weightRecordDao().insertRecord(new WeightRecord(date, Double.parseDouble(nw))));
                }
                prefs.edit().putString("USER_NAME",en.getText().toString()).putString("USER_WEIGHT",nw).putString("USER_HEIGHT",eh.getText().toString()).apply();
                loadUserData(); Toast.makeText(getContext(), "已更新", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("取消",null).show();
    }

    private void showMeasurementsDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(60,40,60,10);
        EditText[] eds = new EditText[6];
        String[] hints = {"体重(kg)","胸围(cm)","腰围(cm)","臀围(cm)","臂围(cm)","腿围(cm)"};
        executor.execute(()->{
            BodyMeasurement last = db.bodyMeasurementDao().getLatest();
            String[] defs = last!=null ? new String[]{String.valueOf(last.getWeightKg()),String.valueOf(last.getChestCm()),String.valueOf(last.getWaistCm()),String.valueOf(last.getHipCm()),String.valueOf(last.getArmCm()),String.valueOf(last.getThighCm())} : new String[]{"70","90","80","95","35","55"};
            requireActivity().runOnUiThread(()->{
                for (int i=0;i<6;i++) { eds[i]=addEdit(layout,hints[i],defs[i]); eds[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); }
                new AlertDialog.Builder(getContext()).setTitle("记录围度").setView(new ScrollView(getContext()){{addView(layout);}})
                    .setPositiveButton("保存",(d,w)->{
                        try { double[] v=new double[6]; for(int i=0;i<6;i++)v[i]=Double.parseDouble(eds[i].getText().toString());
                            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
                            executor.execute(()->db.bodyMeasurementDao().insert(new BodyMeasurement(date,v[0],v[1],v[2],v[3],v[4],v[5])));
                            loadMeasurementsUI(); Toast.makeText(getContext(),"围度已保存",Toast.LENGTH_SHORT).show();
                        } catch(Exception e){ Toast.makeText(getContext(),"请输入有效数字",Toast.LENGTH_SHORT).show(); }
                    }).setNegativeButton("取消",null).show();
            });
        });
    }

    private void exportCSV() {
        executor.execute(()->{
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File f = new File(dir, "MusicGym_export_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".csv");
                FileWriter fw = new FileWriter(f);
                fw.write("type,date,sport,distance_km,duration_sec,calories\n");
                for (WorkoutRecord r : db.workoutRecordDao().getAllRecords())
                    fw.write(String.format(Locale.getDefault(),"cardio,%s,%s,%.2f,%d,%d\n",r.getDate(),r.getSportType(),r.getDistanceKm(),r.getDurationSeconds(),r.getCalories()));
                for (StrengthRecord r : db.strengthRecordDao().getAllRecords())
                    fw.write(String.format(Locale.getDefault(),"strength,%s,,,%d,\n",r.getDate(),r.getDurationSeconds()));
                fw.close();
                requireActivity().runOnUiThread(()-> Toast.makeText(getContext(), "已导出到 Downloads/\n" + f.getName(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                requireActivity().runOnUiThread(()-> Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupReminder() {
        String[] days = {"每天","周一三五","周二四六","周末","关闭提醒"};
        new AlertDialog.Builder(getContext()).setTitle("训练提醒").setItems(days, (d,w)->{
            if (w==4) { cancelReminder(); return; }
            int[] daysOfWeek;
            switch(w) {
                case 0: daysOfWeek = new int[]{Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY}; break;
                case 1: daysOfWeek = new int[]{Calendar.MONDAY,Calendar.WEDNESDAY,Calendar.FRIDAY}; break;
                case 2: daysOfWeek = new int[]{Calendar.TUESDAY,Calendar.THURSDAY,Calendar.SATURDAY}; break;
                default: daysOfWeek = new int[]{Calendar.SATURDAY,Calendar.SUNDAY}; break;
            }
            setWeeklyReminder(daysOfWeek);
        }).show();
    }

    private void setWeeklyReminder(int[] days) {
        AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(requireContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(requireContext(), 999, i, PendingIntent.FLAG_IMMUTABLE);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 18); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long baseTime = cal.getTimeInMillis();
        for (int dow : days) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_WEEK, dow);
            c.set(Calendar.HOUR_OF_DAY, 18); c.set(Calendar.MINUTE, 0);
            if (c.getTimeInMillis() < System.currentTimeMillis()) c.add(Calendar.WEEK_OF_YEAR, 1);
            if (am != null) am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pi);
        }
        Toast.makeText(getContext(), "提醒已设置 (18:00)", Toast.LENGTH_SHORT).show();
    }

    private void cancelReminder() {
        AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(requireContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(requireContext(), 999, i, PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        Toast.makeText(getContext(), "提醒已关闭", Toast.LENGTH_SHORT).show();
    }

    private EditText addEdit(LinearLayout layout, String hint, String text) {
        EditText et = new EditText(getContext());
        et.setHint(hint); et.setText(text); layout.addView(et);
        return et;
    }

    @Override public void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
