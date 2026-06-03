package com.example.musicgym;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvWeight, tvHeight;
    private Button btnEditProfile, btnMeasurements, btnExport, btnReminder;
    private LinearLayout historyContainer, measureContainer;
    private SharedPreferences prefs;
    private AppDatabase db;
    private ExecutorService executor;

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

        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnMeasurements.setOnClickListener(v -> showMeasurementsDialog());
        btnExport.setOnClickListener(v -> exportCSV());
        btnReminder.setOnClickListener(v -> setupReminder());
        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        return view;
    }

    private void loadUserData() {
        tvUserName.setText(prefs.getString("USER_NAME", "Guest User"));
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
            requireActivity().runOnUiThread(() -> {
                if (records.isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText("暂无体重记录"); tv.setTextColor(ColorTokens.TEXT_SECONDARY);
                    historyContainer.addView(tv); return;
                }
                for (WeightRecord r : records) {
                    TextView tv = new TextView(getContext());
                    tv.setText("• " + r.getDate() + " : " + r.getWeightKg() + " kg");
                    tv.setTextColor(ColorTokens.ACCENT_GREEN_SOFT); tv.setTextSize(14);
                    tv.setPadding(0, 10, 0, 10); historyContainer.addView(tv);
                }
            });
        });
    }

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
