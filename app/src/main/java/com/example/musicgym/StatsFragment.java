package com.example.musicgym;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private TextView tvMonthTitle, tvVsLabel;
    private TextView tvFilterAll, tvFilterRunning, tvFilterCycling, tvFilterWalking, tvFilterStrength;
    private LineChart lineChart;
    private TextView tvCardDistance, tvCardDuration, tvCardWorkouts, tvCardCalories;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<?> adapter;
    private LinearLayout calendarContainer;

    private AppDatabase db;
    private ExecutorService executorService;
    private Handler mainHandler;

    private List<WorkoutRecord> allRecords = new ArrayList<>();
    private List<StrengthRecord> allStrengthRecords = new ArrayList<>();
    private String currentFilter = "All";

    private int currentYear, currentMonth, currentDayCount;
    private int lastYear, lastMonth, lastDayCount;

    private static final int COLOR_RUN = Color.parseColor("#FC4C02");
    private static final int COLOR_CYCLE = Color.parseColor("#38bdf8");
    private static final int COLOR_WALK = Color.parseColor("#34d399");
    private static final int COLOR_STRENGTH = Color.parseColor("#f59e0b");
    private static final int COLOR_CUR_MONTH = Color.parseColor("#FC4C02");
    private static final int COLOR_LAST_MONTH = Color.parseColor("#9ca3af");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        tvMonthTitle = view.findViewById(R.id.stats_month_title);
        tvVsLabel = view.findViewById(R.id.stats_vs_label);
        tvFilterAll = view.findViewById(R.id.stats_filter_all);
        tvFilterRunning = view.findViewById(R.id.stats_filter_running);
        tvFilterCycling = view.findViewById(R.id.stats_filter_cycling);
        tvFilterWalking = view.findViewById(R.id.stats_filter_walking);
        tvFilterStrength = view.findViewById(R.id.stats_filter_strength);
        lineChart = view.findViewById(R.id.stats_line_chart);
        tvCardDistance = view.findViewById(R.id.stats_card_distance);
        tvCardDuration = view.findViewById(R.id.stats_card_duration);
        tvCardWorkouts = view.findViewById(R.id.stats_card_workouts);
        tvCardCalories = view.findViewById(R.id.stats_card_calories);
        recyclerView = view.findViewById(R.id.stats_recycler_view);
        calendarContainer = view.findViewById(R.id.stats_calendar_container);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkoutHistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initChart();
        initFilters();
        computeDateRange();
        return view;
    }

    private void computeDateRange() {
        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR); currentMonth = cal.get(Calendar.MONTH);
        currentDayCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.MONTH, -1);
        lastYear = cal.get(Calendar.YEAR); lastMonth = cal.get(Calendar.MONTH);
        lastDayCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void initChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true); lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true); lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.parseColor("#1e293b"));
        lineChart.setNoDataText("No data"); lineChart.setNoDataTextColor(Color.parseColor("#9ca3af"));

        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.WHITE); legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        XAxis xa = lineChart.getXAxis();
        xa.setTextColor(Color.parseColor("#9ca3af")); xa.setTextSize(10f);
        xa.setPosition(XAxis.XAxisPosition.BOTTOM); xa.setDrawGridLines(false);
        xa.setGranularity(1f); xa.setLabelCount(7, true);

        YAxis ya = lineChart.getAxisLeft();
        ya.setTextColor(Color.parseColor("#9ca3af")); ya.setTextSize(10f);
        ya.setDrawGridLines(true); ya.setGridColor(Color.parseColor("#334155"));
        ya.setGridLineWidth(0.5f); ya.setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void initFilters() {
        View.OnClickListener fl = v -> {
            int id = v.getId();
            if (id == R.id.stats_filter_all) currentFilter = "All";
            else if (id == R.id.stats_filter_running) currentFilter = "Running";
            else if (id == R.id.stats_filter_cycling) currentFilter = "Cycling";
            else if (id == R.id.stats_filter_walking) currentFilter = "Walking";
            else if (id == R.id.stats_filter_strength) currentFilter = "Strength";
            updateFilterUI(); refreshChartOnly(); updateHistoryList();
        };
        tvFilterAll.setOnClickListener(fl); tvFilterRunning.setOnClickListener(fl);
        tvFilterCycling.setOnClickListener(fl); tvFilterWalking.setOnClickListener(fl);
        tvFilterStrength.setOnClickListener(fl);
    }

    private void updateFilterUI() {
        for (TextView tv : new TextView[]{tvFilterAll, tvFilterRunning, tvFilterCycling, tvFilterWalking, tvFilterStrength}) {
            tv.setBackgroundResource(R.drawable.stats_filter_unselected); tv.setTextColor(Color.parseColor("#9ca3af"));
        }
        TextView sel;
        switch (currentFilter) {
            case "Running": sel = tvFilterRunning; break;
            case "Cycling": sel = tvFilterCycling; break;
            case "Walking": sel = tvFilterWalking; break;
            case "Strength": sel = tvFilterStrength; break;
            default: sel = tvFilterAll; break;
        }
        sel.setBackgroundResource(R.drawable.stats_filter_selected); sel.setTextColor(Color.WHITE);
    }

    @Override public void onResume() { super.onResume(); loadData(); }

    private void loadData() {
        executorService.execute(() -> {
            allRecords = db.workoutRecordDao().getAllRecords();
            allStrengthRecords = db.strengthRecordDao().getAllRecords();
            if (allRecords.isEmpty() && allStrengthRecords.isEmpty()) {
                seedMockData();
                allRecords = db.workoutRecordDao().getAllRecords();
            }
            mainHandler.post(this::refreshAll);
        });
    }

    private void seedMockData() {
        Random r = new Random(42);
        String[] sports = {"Running","Cycling","Walking"};
        int[][] lp = {{1,4,7,10,13,16,19,22,25,28},{3,9,15,21,27},{2,5,8,11,14,17,20,23,26,29}};
        int[][] cp = {{1,3,5,8,11,14,17,20,23,26,29},{2,7,12,17,22,27},{1,4,6,9,12,15,18,21,24,27,30}};
        double[][] dr = {{4,10},{15,40},{2,5.5}};
        int[][] dur = {{1200,3300},{2400,6000},{1500,4200}};
        for (int si=0;si<3;si++) {
            for (int d : lp[si]) {
                double dist = dr[si][0]+r.nextDouble()*(dr[si][1]-dr[si][0]);
                db.workoutRecordDao().insertRecord(new WorkoutRecord(
                    String.format(Locale.getDefault(),"%04d-%02d-%02d",lastYear,lastMonth+1,d),
                    sports[si],Math.round(dist*100.0)/100.0,dur[si][0]+r.nextInt(dur[si][1]-dur[si][0]),
                    (int)(dist*65*(si==1?0.7:si==2?0.85:1.0)),"[]"));
            }
            for (int d : cp[si]) {
                double dist = dr[si][0]+r.nextDouble()*(dr[si][1]-dr[si][0]);
                db.workoutRecordDao().insertRecord(new WorkoutRecord(
                    String.format(Locale.getDefault(),"%04d-%02d-%02d",currentYear,currentMonth+1,d),
                    sports[si],Math.round(dist*100.0)/100.0,dur[si][0]+r.nextInt(dur[si][1]-dur[si][0]),
                    (int)(dist*65*(si==1?0.7:si==2?0.85:1.0)),"[]"));
            }
        }
    }

    private void refreshAll() {
        updateHeader();
        refreshChartOnly();
        buildCalendar();
        updateHistoryList();
    }
    private void refreshChartOnly() {
        if ("Strength".equals(currentFilter)) refreshStrengthChart();
        else refreshCardioChart();
    }

    private void updateHeader() {
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        tvMonthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime()));
        cal.set(lastYear, lastMonth, 1);
        tvVsLabel.setText("vs " + new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime()));
    }

    // ======= 力量图表 =======
    private void refreshStrengthChart() {
        Map<Integer, Float> curVol = new HashMap<>(), lastVol = new HashMap<>();
        String curP = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth + 1);
        String lastP = String.format(Locale.getDefault(), "%04d-%02d", lastYear, lastMonth + 1);

        for (StrengthRecord r : allStrengthRecords) {
            if (r.getDate() == null) continue;
            try {
                int day = Integer.parseInt(r.getDate().substring(8, 10));
                JSONArray arr = new JSONArray(r.getExercisesJson());
                float vol = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONArray sets = arr.getJSONObject(i).getJSONArray("sets");
                    for (int j = 0; j < sets.length(); j++) {
                        JSONObject s = sets.getJSONObject(j);
                        vol += s.optDouble("weight") * s.optInt("reps");
                    }
                }
                if (r.getDate().startsWith(curP)) curVol.put(day, curVol.getOrDefault(day, 0f) + vol);
                else if (r.getDate().startsWith(lastP)) lastVol.put(day, lastVol.getOrDefault(day, 0f) + vol);
            } catch (Exception ignored) {}
        }

        int maxDays = Math.max(currentDayCount, lastDayCount);
        List<Entry> curE = new ArrayList<>(), lastE = new ArrayList<>();
        float curMax = 0;
        for (int d = 1; d <= maxDays; d++) {
            float cv = curVol.getOrDefault(d, 0f), lv = lastVol.getOrDefault(d, 0f);
            curE.add(new Entry(d, cv)); lastE.add(new Entry(d, lv));
            if (cv > curMax) curMax = cv; if (lv > curMax) curMax = lv;
        }
        setChartData(curE, lastE, curMax, COLOR_STRENGTH, "kg");

        float dist = 0; int dur = 0, count = 0;
        for (StrengthRecord r : allStrengthRecords)
            if (r.getDate() != null && r.getDate().startsWith(curP)) { dur += r.getDurationSeconds(); count++; }
        tvCardDistance.setText(String.format(Locale.getDefault(), "%.0f", (double)count));
        int h = dur / 3600, m = (dur % 3600) / 60;
        tvCardDuration.setText(String.format(Locale.getDefault(), "%dh %dm", h, m));
        tvCardWorkouts.setText(String.valueOf(count));
        tvCardCalories.setText("—");
        // change labels
        ((TextView)((ViewGroup)tvCardDistance.getParent()).getChildAt(1)).setText("sessions");
    }

    // ======= 有氧图表 =======
    private void refreshCardioChart() {
        ((TextView)((ViewGroup)tvCardDistance.getParent()).getChildAt(1)).setText("公里");
        Map<Integer, Float> curD = aggMonthly(currentYear, currentMonth, currentDayCount, currentFilter);
        Map<Integer, Float> lastD = aggMonthly(lastYear, lastMonth, lastDayCount, currentFilter);
        int maxDays = Math.max(currentDayCount, lastDayCount);
        List<Entry> curE = new ArrayList<>(), lastE = new ArrayList<>();
        float curMax = 0;
        for (int d = 1; d <= maxDays; d++) {
            float cv = curD.getOrDefault(d, 0f), lv = lastD.getOrDefault(d, 0f);
            curE.add(new Entry(d, cv)); lastE.add(new Entry(d, lv));
            if (cv > curMax) curMax = cv; if (lv > curMax) curMax = lv;
        }
        setChartData(curE, lastE, curMax, getAccentColor(), "km");
        updateSummaryCards(curD, lastD);
    }

    private void setChartData(List<Entry> curE, List<Entry> lastE, float max, int accent, String unit) {
        LineDataSet curS = new LineDataSet(curE, getCurMonthLabel());
        curS.setColor(accent); curS.setCircleColor(accent);
        curS.setLineWidth(3f); curS.setCircleRadius(4f); curS.setDrawCircleHole(false);
        curS.setValueTextSize(0f); curS.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        curS.setDrawFilled(true); curS.setFillColor(accent); curS.setFillAlpha(30);

        LineDataSet lastS = new LineDataSet(lastE, getLastMonthLabel());
        lastS.setColor(COLOR_LAST_MONTH); lastS.setCircleColor(COLOR_LAST_MONTH);
        lastS.setLineWidth(2f); lastS.setCircleRadius(3f); lastS.setDrawCircleHole(false);
        lastS.setValueTextSize(0f); lastS.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lastS.setDrawFilled(false);
        lastS.enableDashedLine(10f, 6f, 0f);
        lastS.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 6f}, 0f));

        List<ILineDataSet> sets = new ArrayList<>(); sets.add(curS); sets.add(lastS);
        lineChart.clear(); lineChart.setData(new LineData(sets));
        lineChart.getXAxis().setAxisMinimum(1f);
        lineChart.getXAxis().setAxisMaximum(Math.max(currentDayCount, lastDayCount));
        lineChart.getAxisLeft().setAxisMaximum(max * 1.2f + 1f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.animateX(600); lineChart.invalidate();
    }

    private Map<Integer, Float> aggMonthly(int y, int m, int days, String filter) {
        Map<Integer, Float> sum = new HashMap<>();
        String p = String.format(Locale.getDefault(), "%04d-%02d", y, m + 1);
        for (WorkoutRecord r : allRecords) {
            if (r.getDate() == null || !r.getDate().startsWith(p)) continue;
            if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
            try { int d = Integer.parseInt(r.getDate().substring(8, 10));
                sum.put(d, sum.getOrDefault(d, 0f) + (float) r.getDistanceKm());
            } catch (Exception ignored) {}
        }
        return sum;
    }

    private int getAccentColor() {
        switch (currentFilter) {
            case "Running": return COLOR_RUN; case "Cycling": return COLOR_CYCLE;
            case "Walking": return COLOR_WALK; default: return COLOR_CUR_MONTH;
        }
    }

    private String getCurMonthLabel() { Calendar c = Calendar.getInstance(); c.set(currentYear, currentMonth, 1);
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(c.getTime()); }
    private String getLastMonthLabel() { Calendar c = Calendar.getInstance(); c.set(lastYear, lastMonth, 1);
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(c.getTime()); }

    private void updateSummaryCards(Map<Integer, Float> curD, Map<Integer, Float> lastD) {
        float dist=0,dur=0,cal=0; int cnt=0;
        String p = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth + 1);
        for (WorkoutRecord r : allRecords) {
            if (r.getDate() == null || !r.getDate().startsWith(p)) continue;
            if (!"All".equals(currentFilter) && !currentFilter.equals(r.getSportType())) continue;
            dist += r.getDistanceKm(); dur += r.getDurationSeconds(); cal += r.getCalories(); cnt++;
        }
        tvCardDistance.setText(String.format(Locale.getDefault(), "%.1f", dist));
        int h = (int) dur / 3600, m = ((int) dur % 3600) / 60;
        tvCardDuration.setText(String.format(Locale.getDefault(), "%dh %dm", h, m));
        tvCardWorkouts.setText(String.valueOf(cnt));
        tvCardCalories.setText(String.valueOf((int) cal));
    }

    // ======= 日历热力图 =======
    private void buildCalendar() {
        calendarContainer.removeAllViews();
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);
        int totalDays = currentDayCount;
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth + 1);

        // 收集有数据的天
        Set<Integer> cardioDays = new HashSet<>(), strengthDays = new HashSet<>();
        for (WorkoutRecord r : allRecords) {
            if (r.getDate() != null && r.getDate().startsWith(prefix))
                try { cardioDays.add(Integer.parseInt(r.getDate().substring(8, 10))); } catch (Exception ignored) {}
        }
        for (StrengthRecord r : allStrengthRecords) {
            if (r.getDate() != null && r.getDate().startsWith(prefix))
                try { strengthDays.add(Integer.parseInt(r.getDate().substring(8, 10))); } catch (Exception ignored) {}
        }

        // 星期头
        LinearLayout weekRow = new LinearLayout(getContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL); weekRow.setPadding(0,0,0,4);
        for (String w : new String[]{"日","一","二","三","四","五","六"}) {
            TextView tw = new TextView(getContext());
            tw.setText(w); tw.setTextColor(Color.parseColor("#6b7280")); tw.setTextSize(10f);
            tw.setGravity(Gravity.CENTER);
            tw.setLayoutParams(new LinearLayout.LayoutParams(0, 40, 1));
            weekRow.addView(tw);
        }
        calendarContainer.addView(weekRow);

        LinearLayout gridRow = null;
        int cellIdx = 0;
        // fill leading empty cells
        for (int i = 1; i < firstDow; i++) {
            if (cellIdx % 7 == 0 || gridRow == null) {
                gridRow = new LinearLayout(getContext());
                gridRow.setOrientation(LinearLayout.HORIZONTAL); gridRow.setPadding(0,0,0,2);
                calendarContainer.addView(gridRow);
            }
            View empty = new View(getContext());
            empty.setLayoutParams(new LinearLayout.LayoutParams(0, 38, 1));
            gridRow.addView(empty);
            cellIdx++;
        }
        for (int day = 1; day <= totalDays; day++) {
            if (cellIdx % 7 == 0 || gridRow == null) {
                gridRow = new LinearLayout(getContext());
                gridRow.setOrientation(LinearLayout.HORIZONTAL); gridRow.setPadding(0,0,0,2);
                calendarContainer.addView(gridRow);
            }
            boolean cardio = cardioDays.contains(day);
            boolean strength = strengthDays.contains(day);
            int bg;
            if (cardio && strength) bg = Color.parseColor("#FC4C02");
            else if (cardio) bg = Color.parseColor("#22c55e");
            else if (strength) bg = Color.parseColor("#f59e0b");
            else bg = Color.parseColor("#334155");

            TextView tv = new TextView(getContext());
            tv.setText(String.valueOf(day)); tv.setTextColor(Color.WHITE); tv.setTextSize(11f);
            tv.setGravity(Gravity.CENTER); tv.setBackgroundColor(bg);
            ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(0, 38, 1);
            lp.width = (int)(getResources().getDisplayMetrics().widthPixels / 7.5);
            tv.setLayoutParams(lp);
            gridRow.addView(tv);
            cellIdx++;
        }
    }

    private void updateHistoryList() {
        if ("Strength".equals(currentFilter)) {
            adapter = new StrengthHistoryAdapter(new ArrayList<>(allStrengthRecords));
        } else {
            adapter = new WorkoutHistoryAdapter(new ArrayList<>(allRecords));
        }
        recyclerView.setAdapter(adapter);
    }

    @Override public void onDestroy() { super.onDestroy(); executorService.shutdown(); }
}
