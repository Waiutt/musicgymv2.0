package com.example.musicgym;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统计页 — MVVM 重构版。
 * Fragment 只负责 UI 渲染，数据逻辑全部在 StatsViewModel 中。
 */
public class StatsFragment extends Fragment {

    private StatsViewModel vm;

    private TextView tvMonthTitle, tvVsLabel;
    private TextView tvFilterAll, tvFilterRunning, tvFilterCycling, tvFilterWalking, tvFilterStrength;
    private LineChart lineChart;
    private TextView tvCardDistance, tvCardDuration, tvCardWorkouts, tvCardCalories;
    private RecyclerView recyclerView;
    private LinearLayout calendarContainer, prContainer;

    private static final int COLOR_LAST_MONTH = ColorTokens.TEXT_SECONDARY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        prContainer = view.findViewById(R.id.stats_pr_container);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new WorkoutHistoryAdapter(new ArrayList<>()));

        vm = new ViewModelProvider(this).get(StatsViewModel.class);

        initChart();
        initFilters();
        observeViewModel();
        return view;
    }

    // ═══════════ Observe ViewModel ═══════════

    private void observeViewModel() {
        // 标题
        vm.getMonthTitle().observe(getViewLifecycleOwner(), t -> tvMonthTitle.setText(t));
        vm.getVsLabel().observe(getViewLifecycleOwner(), t -> tvVsLabel.setText(t));

        // 折线图
        vm.getChartBundle().observe(getViewLifecycleOwner(), this::renderChart);

        // 摘要卡片
        vm.getSummaryStats().observe(getViewLifecycleOwner(), s -> {
            tvCardDistance.setText(s.distance);
            tvCardDuration.setText(s.duration);
            tvCardWorkouts.setText(s.workouts);
            tvCardCalories.setText(s.calories);
        });

        // 模式切换 → 更新单位标签和过滤 UI
        vm.getIsStrengthMode().observe(getViewLifecycleOwner(), isStrength -> {
            TextView label = (TextView) ((ViewGroup) tvCardDistance.getParent()).getChildAt(1);
            label.setText(isStrength ? "sessions" : "公里");
            updateFilterUI();
        });

        // 日历热力图
        vm.getCalendarDays().observe(getViewLifecycleOwner(), this::buildCalendar);

        // 过滤状态
        vm.getCurrentFilter().observe(getViewLifecycleOwner(), f -> {
            updateHistoryList();
            updateFilterUI();
        });

        // 个人纪录
        vm.getPrText().observe(getViewLifecycleOwner(), t -> {
            prContainer.removeAllViews();
            TextView tvPR = new TextView(requireContext());
            tvPR.setText(t); tvPR.setTextColor(ColorTokens.PR_YELLOW);
            tvPR.setTextSize(13f); tvPR.setLineSpacing(4, 1.1f);
            tvPR.setPadding(16, 10, 16, 6);
            prContainer.addView(tvPR);
        });
    }

    // ═══════════ 折线图 ═══════════

    private void initChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true); lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true); lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(ColorTokens.BG_CARD);
        lineChart.setNoDataText("No data");
        lineChart.setNoDataTextColor(ColorTokens.TEXT_SECONDARY);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.WHITE); legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        XAxis xa = lineChart.getXAxis();
        xa.setTextColor(ColorTokens.TEXT_SECONDARY); xa.setTextSize(10f);
        xa.setPosition(XAxis.XAxisPosition.BOTTOM); xa.setDrawGridLines(false);
        xa.setGranularity(1f); xa.setLabelCount(7, true);

        YAxis ya = lineChart.getAxisLeft();
        ya.setTextColor(ColorTokens.TEXT_SECONDARY); ya.setTextSize(10f);
        ya.setDrawGridLines(true); ya.setGridColor(ColorTokens.BG_INPUT);
        ya.setGridLineWidth(0.5f); ya.setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void renderChart(StatsViewModel.ChartBundle bundle) {
        Calendar cal = Calendar.getInstance();

        LineDataSet curS = new LineDataSet(bundle.curEntries, getMonthLabel(vm.getCurDays()));
        curS.setColor(bundle.accentColor); curS.setCircleColor(bundle.accentColor);
        curS.setLineWidth(3f); curS.setCircleRadius(4f); curS.setDrawCircleHole(false);
        curS.setValueTextSize(0f); curS.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        curS.setDrawFilled(true); curS.setFillColor(bundle.accentColor); curS.setFillAlpha(30);

        LineDataSet lastS = new LineDataSet(bundle.lastEntries, "上月");
        lastS.setColor(COLOR_LAST_MONTH); lastS.setCircleColor(COLOR_LAST_MONTH);
        lastS.setLineWidth(2f); lastS.setCircleRadius(3f); lastS.setDrawCircleHole(false);
        lastS.setValueTextSize(0f); lastS.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lastS.setDrawFilled(false);
        lastS.enableDashedLine(10f, 6f, 0f);
        lastS.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 6f}, 0f));

        List<ILineDataSet> sets = new ArrayList<>(); sets.add(curS); sets.add(lastS);
        lineChart.clear(); lineChart.setData(new LineData(sets));

        int maxDays = Math.max(vm.getCurDays(), vm.getLastDays());
        lineChart.getXAxis().setAxisMinimum(1f);
        lineChart.getXAxis().setAxisMaximum(maxDays);
        lineChart.getAxisLeft().setAxisMaximum(bundle.maxValue * 1.2f + 1f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.animateX(600); lineChart.invalidate();
    }

    private String getMonthLabel(int days) {
        Calendar c = Calendar.getInstance();
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(c.getTime());
    }

    // ═══════════ 日历热力图 ═══════════

    private void buildCalendar(Map<Integer, Integer> activeDays) {
        calendarContainer.removeAllViews();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);
        int totalDays = vm.getCurDays();

        // 星期头
        LinearLayout weekRow = new LinearLayout(getContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL); weekRow.setPadding(0, 0, 0, 4);
        for (String w : new String[]{"日", "一", "二", "三", "四", "五", "六"}) {
            TextView tw = new TextView(getContext());
            tw.setText(w); tw.setTextColor(ColorTokens.TEXT_HINT); tw.setTextSize(10f);
            tw.setGravity(Gravity.CENTER);
            tw.setLayoutParams(new LinearLayout.LayoutParams(0, 40, 1));
            weekRow.addView(tw);
        }
        calendarContainer.addView(weekRow);

        LinearLayout gridRow = null;
        int cellIdx = 0;
        for (int i = 1; i < firstDow; i++) {
            if (cellIdx % 7 == 0) {
                gridRow = new LinearLayout(getContext());
                gridRow.setOrientation(LinearLayout.HORIZONTAL); gridRow.setPadding(0, 0, 0, 2);
                calendarContainer.addView(gridRow);
            }
            View empty = new View(getContext());
            empty.setLayoutParams(new LinearLayout.LayoutParams(0, 38, 1));
            gridRow.addView(empty); cellIdx++;
        }
        for (int day = 1; day <= totalDays; day++) {
            if (cellIdx % 7 == 0) {
                gridRow = new LinearLayout(getContext());
                gridRow.setOrientation(LinearLayout.HORIZONTAL); gridRow.setPadding(0, 0, 0, 2);
                calendarContainer.addView(gridRow);
            }
            Integer flag = activeDays.getOrDefault(day, 0);
            int bg;
            if (flag == 3) bg = ColorTokens.BRAND_ORANGE;
            else if (flag == 1) bg = ColorTokens.ACCENT_GREEN;
            else if (flag == 2) bg = ColorTokens.ACCENT_AMBER;
            else bg = ColorTokens.BG_INPUT;

            TextView tv = new TextView(getContext());
            tv.setText(String.valueOf(day)); tv.setTextColor(Color.WHITE); tv.setTextSize(11f);
            tv.setGravity(Gravity.CENTER); tv.setBackgroundColor(bg);
            ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(0, 38, 1);
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels / 7.5);
            tv.setLayoutParams(lp);

            // 点击日期查看当天详情
            final int dayNum = day;
            final int bgColor = bg;
            tv.setOnClickListener(v -> showDayDetail(dayNum, bgColor));

            gridRow.addView(tv); cellIdx++;
        }
    }

    private void showDayDetail(int day, int bgColor) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        String prefix = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(cal.getTime());
        String date = prefix + "-" + String.format(java.util.Locale.getDefault(), "%02d", day);

        StringBuilder sb = new StringBuilder();

        // 有氧记录
        for (WorkoutRecord r : vm.getCardioRecords()) {
            if (date.equals(r.getDate())) {
                sb.append("🏃 ").append(r.getSportType())
                  .append("  ").append(String.format("%.1f km", r.getDistanceKm()))
                  .append("  ").append(r.getCalories()).append(" kcal\n");
            }
        }

        // 力量记录
        for (StrengthRecord r : vm.getStrengthRecords()) {
            if (date.equals(r.getDate())) {
                int m = r.getDurationSeconds() / 60;
                if (sb.length() > 0) sb.append("\n");
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(r.getExercisesJson());
                    sb.append("🏋️ 力量训练  ").append(arr.length())
                      .append("个动作  ").append(m).append("分钟\n");
                    for (int i = 0; i < Math.min(arr.length(), 5); i++) {
                        org.json.JSONObject ex = arr.getJSONObject(i);
                        org.json.JSONArray sets = ex.getJSONArray("sets");
                        sb.append("  · ").append(ex.optString("name"))
                          .append("  ").append(sets.length()).append("组\n");
                    }
                    if (arr.length() > 5) sb.append("  ... 等").append(arr.length()).append("个动作");
                } catch (Exception e) {
                    sb.append("🏋️ 力量训练  ").append(m).append("分钟");
                }
            }
        }

        if (sb.length() == 0) {
            sb.append("当天没有运动记录\n\n去运动一下吧！💪");
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(date + " 运动记录")
                .setMessage(sb.toString().trim())
                .setPositiveButton("确定", null)
                .show();
    }

    // ═══════════ 过滤 ═══════════

    private void initFilters() {
        View.OnClickListener fl = v -> {
            int id = v.getId();
            String filter;
            if (id == R.id.stats_filter_running) filter = "Running";
            else if (id == R.id.stats_filter_cycling) filter = "Cycling";
            else if (id == R.id.stats_filter_walking) filter = "Walking";
            else if (id == R.id.stats_filter_strength) filter = "Strength";
            else filter = "All";
            vm.setFilter(filter);
        };
        tvFilterAll.setOnClickListener(fl); tvFilterRunning.setOnClickListener(fl);
        tvFilterCycling.setOnClickListener(fl); tvFilterWalking.setOnClickListener(fl);
        tvFilterStrength.setOnClickListener(fl);
    }

    private void updateFilterUI() {
        String f = vm.getFilter();
        for (TextView tv : new TextView[]{tvFilterAll, tvFilterRunning, tvFilterCycling,
                tvFilterWalking, tvFilterStrength}) {
            tv.setBackgroundResource(R.drawable.stats_filter_unselected);
            tv.setTextColor(ColorTokens.TEXT_SECONDARY);
        }
        TextView sel;
        switch (f) {
            case "Running": sel = tvFilterRunning; break;
            case "Cycling": sel = tvFilterCycling; break;
            case "Walking": sel = tvFilterWalking; break;
            case "Strength": sel = tvFilterStrength; break;
            default: sel = tvFilterAll; break;
        }
        sel.setBackgroundResource(R.drawable.stats_filter_selected); sel.setTextColor(Color.WHITE);
    }

    // ═══════════ 历史列表 ═══════════

    private void updateHistoryList() {
        if ("Strength".equals(vm.getFilter())) {
            recyclerView.setAdapter(new WorkoutHistoryAdapter(
                    new ArrayList<>(vm.getStrengthRecords()), true));
        } else {
            recyclerView.setAdapter(new WorkoutHistoryAdapter(
                    new ArrayList<>(vm.getCardioRecords())));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.loadData();
    }
}
