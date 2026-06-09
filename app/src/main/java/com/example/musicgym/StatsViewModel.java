package com.example.musicgym;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 统计页 ViewModel — 管理数据加载、过滤、聚合，Fragment 只负责渲染 */
public class StatsViewModel extends AndroidViewModel {

    private final StatsRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── 日期范围 ──
    private int curYear, curMonth, curDays, lastYear, lastMonth, lastDays;

    // ── 原始数据 ──
    private final List<WorkoutRecord> cardioRecords = new ArrayList<>();
    private final List<StrengthRecord> strengthRecords = new ArrayList<>();

    // ── 当前过滤 ──
    private final MutableLiveData<String> currentFilter = new MutableLiveData<>("All");

    // ── 暴露给 Fragment 的 LiveData ──
    private final MutableLiveData<String> monthTitle = new MutableLiveData<>();
    private final MutableLiveData<String> vsLabel = new MutableLiveData<>();

    // 日历热力图: day → typeBit (1=cardio, 2=strength, 3=both)
    private final MutableLiveData<Map<Integer, Integer>> calendarDays = new MutableLiveData<>();

    // 折线图: 当月 Entry 列表 + 上月 Entry 列表 + 最大值 + 颜色
    private final MutableLiveData<ChartBundle> chartBundle = new MutableLiveData<>();

    // 摘要卡片
    private final MutableLiveData<StatsRepository.SummaryStats> summaryStats = new MutableLiveData<>();

    // 历史列表
    private final MutableLiveData<Boolean> isStrengthMode = new MutableLiveData<>(false);

    // ── 个人纪录
    private final MutableLiveData<String> prText = new MutableLiveData<>();

    // ── 视图模式 (MONTH / WEEK)
    public static final int VIEW_MONTH = 0;
    public static final int VIEW_WEEK = 1;
    private final MutableLiveData<Integer> viewMode = new MutableLiveData<>(VIEW_MONTH);
    private int currentWeekStart;

    private final MutableLiveData<String> goalProgress = new MutableLiveData<>();
    private final android.content.SharedPreferences goalPrefs;

    // ── 周视图 ──
    private final MutableLiveData<String> weekThisLabel = new MutableLiveData<>("");
    private final MutableLiveData<String> weekLastLabel = new MutableLiveData<>("");
    private final MutableLiveData<String> weekComparison = new MutableLiveData<>("");

    // ── 年视图 ──
    private final MutableLiveData<int[]> yearWorkoutDays = new MutableLiveData<>();
    private final MutableLiveData<float[]> yearDistance = new MutableLiveData<>();

    public LiveData<String> getWeekThisLabel() { return weekThisLabel; }
    public LiveData<String> getWeekLastLabel() { return weekLastLabel; }
    public LiveData<String> getWeekComparison() { return weekComparison; }
    public LiveData<int[]> getYearWorkoutDays() { return yearWorkoutDays; }
    public LiveData<float[]> getYearDistance() { return yearDistance; }

    public StatsViewModel(@NonNull Application app) {
        super(app);
        repo = new StatsRepository(app);
        goalPrefs = app.getSharedPreferences("stats_goals", android.content.Context.MODE_PRIVATE);
        StatsRepository.DateRange dr = repo.getDateRange();
        curYear = dr.curYear; curMonth = dr.curMonth; curDays = dr.curDays;
        lastYear = dr.lastYear; lastMonth = dr.lastMonth; lastDays = dr.lastDays;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        currentWeekStart = c.get(java.util.Calendar.DAY_OF_MONTH);
    }

    // ── 公开 Getter ──
    public LiveData<String> getMonthTitle() { return monthTitle; }
    public LiveData<String> getVsLabel() { return vsLabel; }
    public LiveData<Map<Integer, Integer>> getCalendarDays() { return calendarDays; }
    public LiveData<ChartBundle> getChartBundle() { return chartBundle; }
    public LiveData<StatsRepository.SummaryStats> getSummaryStats() { return summaryStats; }
    public LiveData<Boolean> getIsStrengthMode() { return isStrengthMode; }
    public LiveData<String> getPrText() { return prText; }
    public LiveData<String> getCurrentFilter() { return currentFilter; }
    public LiveData<Integer> getViewMode() { return viewMode; }
    public LiveData<String> getGoalProgress() { return goalProgress; }

    // ── 周/月切换 ──
    public void toggleViewMode() {
        int next = viewMode.getValue() == VIEW_MONTH ? VIEW_WEEK : VIEW_MONTH;
        viewMode.setValue(next);
        executor.execute(() -> refreshAll());
    }

    // ── 目标 ──
    public void setGoal(float goalKm) {
        goalPrefs.edit().putFloat("monthly_km_goal", goalKm).apply();
        executor.execute(this::calcGoalProgress);
    }

    private void calcGoalProgress() {
        float goal = goalPrefs.getFloat("monthly_km_goal", 0);
        if (goal <= 0) { goalProgress.postValue(""); return; }
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", curYear, curMonth + 1);
        float total = 0;
        for (WorkoutRecord r : cardioRecords) {
            if (r.getDate() != null && r.getDate().startsWith(prefix))
                total += (float) r.getDistanceKm();
        }
        float pct = Math.min(total / goal * 100, 100);
        goalProgress.postValue(String.format(Locale.getDefault(),
                "月目标 %.0f/%.0fkm (%.0f%%)", total, goal, pct));
    }

    public String getFilter() { return currentFilter.getValue(); }

    public int getCurDays() { return curDays; }
    public int getLastDays() { return lastDays; }

    // ── 加载数据 ──
    public void loadData() {
        executor.execute(() -> {
            StatsRepository.StatsData data = repo.loadAllData(curYear, curMonth, lastYear, lastMonth);
            cardioRecords.clear(); cardioRecords.addAll(data.cardioRecords);
            strengthRecords.clear(); strengthRecords.addAll(data.strengthRecords);
            refreshAll();
        });
    }

    // ── 切换过滤 ──
    public void setFilter(String filter) {
        currentFilter.postValue(filter);
        executor.execute(() -> refreshAll());
    }

    // ── 刷新全部 ──
    private void refreshAll() {
        String filter = currentFilter.getValue();
        if (filter == null) filter = "All";

        // 标题
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(curYear, curMonth, 1);
        String title = new java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        cal.set(lastYear, lastMonth, 1);
        String vs = "vs " + new java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
        monthTitle.postValue(title); vsLabel.postValue(vs);

        // 日历
        calendarDays.postValue(buildCalendar(filter));

        // 折线图
        if ("Strength".equals(filter)) {
            chartBundle.postValue(buildStrengthChart());
        } else if (viewMode.getValue() != null && viewMode.getValue() == VIEW_WEEK) {
            chartBundle.postValue(buildWeekChart(filter));
        } else {
            chartBundle.postValue(buildCardioChart(filter));
        }

        // 摘要
        summaryStats.postValue(repo.calcSummary(cardioRecords, strengthRecords, filter, curYear, curMonth));

        // 模式
        isStrengthMode.postValue("Strength".equals(filter));
        prText.postValue(calcPRs());
        calcGoalProgress();
        calcWeekComparison(filter);
        calcYearView(filter);
    }

    // ── 周视图数据 ──
    private ChartBundle buildWeekChart(String filter) {
        // 本周: 周一到今天
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int today = cal.get(java.util.Calendar.DAY_OF_MONTH);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        int month = curMonth;
        int year = curYear;
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);

        // 7天聚合
        float[] weekData = new float[7];
        for (WorkoutRecord r : cardioRecords) {
            if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
                try {
                    int day = Integer.parseInt(r.getDate().substring(8, 10));
                    int dow = getDayOfWeek(year, month, day); // 0=Mon..6=Sun
                    if (dow >= 0 && dow < 7 && day >= today - 6) {
                        weekData[dow] += (float) r.getDistanceKm();
                    }
                } catch (Exception ignored) {}
            }
        }

        List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
        float max = 0;
        for (int i = 0; i < 7; i++) {
            entries.add(new com.github.mikephil.charting.data.Entry(i, weekData[i]));
            if (weekData[i] > max) max = weekData[i];
        }
        return new ChartBundle(entries, new ArrayList<>(), max, getAccentColor(filter), "km");
    }

    private int getDayOfWeek(int year, int month, int day) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(year, month, day);
        int dow = c.get(java.util.Calendar.DAY_OF_WEEK);
        return dow == java.util.Calendar.SUNDAY ? 6 : dow - 2; // Mon=0..Sun=6
    }

    // ── 日历数据 ──
    private Map<Integer, Integer> buildCalendar(String filter) {
        Map<Integer, Integer> map = new HashMap<>();
        String prefix = String.format(Locale.getDefault(), "%04d-%02d", curYear, curMonth + 1);

        if (!"Strength".equals(filter)) {
            for (WorkoutRecord r : cardioRecords) {
                if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                    if ("All".equals(filter) || filter.equals(r.getSportType())) {
                        try {
                            int day = Integer.parseInt(r.getDate().substring(8, 10));
                            map.put(day, map.getOrDefault(day, 0) | 1);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        if ("All".equals(filter) || "Strength".equals(filter)) {
            for (StrengthRecord r : strengthRecords) {
                if (r.getDate() != null && r.getDate().startsWith(prefix)) {
                    try {
                        int day = Integer.parseInt(r.getDate().substring(8, 10));
                        map.put(day, map.getOrDefault(day, 0) | 2);
                    } catch (Exception ignored) {}
                }
            }
        }
        return map;
    }

    // ── 有氧折线图 ──
    private ChartBundle buildCardioChart(String filter) {
        Map<Integer, Float> curD = repo.aggregateCardioByMonth(cardioRecords, curYear, curMonth, filter);
        Map<Integer, Float> lastD = repo.aggregateCardioByMonth(cardioRecords, lastYear, lastMonth, filter);
        int maxDays = Math.max(curDays, lastDays);
        List<com.github.mikephil.charting.data.Entry> curE = new ArrayList<>();
        List<com.github.mikephil.charting.data.Entry> lastE = new ArrayList<>();
        float max = 0;
        for (int d = 1; d <= maxDays; d++) {
            float cv = curD.getOrDefault(d, 0f), lv = lastD.getOrDefault(d, 0f);
            curE.add(new com.github.mikephil.charting.data.Entry(d, cv));
            lastE.add(new com.github.mikephil.charting.data.Entry(d, lv));
            if (cv > max) max = cv; if (lv > max) max = lv;
        }
        return new ChartBundle(curE, lastE, max, getAccentColor(filter), "km");
    }

    // ── 力量折线图 ──
    private ChartBundle buildStrengthChart() {
        Map<Integer, Float> curVol = repo.aggregateStrengthByMonth(strengthRecords, curYear, curMonth);
        Map<Integer, Float> lastVol = repo.aggregateStrengthByMonth(strengthRecords, lastYear, lastMonth);
        int maxDays = Math.max(curDays, lastDays);
        List<com.github.mikephil.charting.data.Entry> curE = new ArrayList<>();
        List<com.github.mikephil.charting.data.Entry> lastE = new ArrayList<>();
        float max = 0;
        for (int d = 1; d <= maxDays; d++) {
            float cv = curVol.getOrDefault(d, 0f), lv = lastVol.getOrDefault(d, 0f);
            curE.add(new com.github.mikephil.charting.data.Entry(d, cv));
            lastE.add(new com.github.mikephil.charting.data.Entry(d, lv));
            if (cv > max) max = cv; if (lv > max) max = lv;
        }
        return new ChartBundle(curE, lastE, max, ColorTokens.ACCENT_AMBER, "kg");
    }

    private int getAccentColor(String filter) {
        switch (filter) {
            case "Running": return ColorTokens.BRAND_ORANGE;
            case "Cycling": return ColorTokens.ACCENT_CYAN;
            case "Walking": return ColorTokens.ACCENT_GREEN_SOFT;
            default: return ColorTokens.BRAND_ORANGE;
        }
    }

    // ── 数据容器 ──
    public static class ChartBundle {
        public final List<com.github.mikephil.charting.data.Entry> curEntries, lastEntries;
        public final float maxValue;
        public final int accentColor;
        public final String unit;
        ChartBundle(List<com.github.mikephil.charting.data.Entry> c, List<com.github.mikephil.charting.data.Entry> l,
                    float m, int a, String u) {
            curEntries = c; lastEntries = l; maxValue = m; accentColor = a; unit = u;
        }
    }

    public List<WorkoutRecord> getCardioRecords() { return new ArrayList<>(cardioRecords); }
    public List<StrengthRecord> getStrengthRecords() { return new ArrayList<>(strengthRecords); }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }


    private String calcPRs() {
        StringBuilder sb = new StringBuilder();
        // 有氧 PRs
        double maxDist = 0; int maxCal = 0; double bestPace = 999;
        for (WorkoutRecord r : cardioRecords) {
            if (r.getDistanceKm() > maxDist) maxDist = r.getDistanceKm();
            if (r.getCalories() > maxCal) maxCal = r.getCalories();
            if (r.getDurationSeconds() > 0 && r.getDistanceKm() > 0) {
                double pace = (double) r.getDurationSeconds() / r.getDistanceKm();
                if (r.getDistanceKm() > 0.5 && pace < bestPace) bestPace = pace;
            }
        }
        if (maxDist > 0) {
            int pMin = (int) bestPace / 60, pSec = (int) bestPace % 60;
            sb.append("🏃 最远: ").append(String.format(Locale.getDefault(), "%.1fkm", maxDist))
              .append("  |  🔥 最多: ").append(maxCal).append("kcal")
              .append("  |  ⚡ 最快: ").append(pMin).append("'").append(String.format("%02d", pSec)).append("\"\n");
        }
        // 力量 PRs
        double maxWeight = 0; String maxWtEx = ""; double maxVol = 0; String maxVolEx = "";
        try {
            for (StrengthRecord r : strengthRecords) {
                org.json.JSONArray arr = new org.json.JSONArray(r.getExercisesJson());
                double totalVol = 0;
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject ex = arr.getJSONObject(i);
                    org.json.JSONArray sets = ex.getJSONArray("sets");
                    for (int j = 0; j < sets.length(); j++) {
                        org.json.JSONObject s = sets.getJSONObject(j);
                        double w = s.optDouble("weight");
                        if (w > maxWeight) { maxWeight = w; maxWtEx = ex.optString("name"); }
                    }
                    for (int j = 0; j < sets.length(); j++) {
                        org.json.JSONObject s = sets.getJSONObject(j);
                        totalVol += s.optDouble("weight") * s.optInt("reps");
                    }
                }
                if (totalVol > maxVol) { maxVol = totalVol; maxVolEx = arr.getJSONObject(0).optString("name"); }
            }
        } catch (Exception ignored) {}
        if (maxWeight > 0) {
            sb.append("🏋️ 最大重量: ").append(maxWtEx).append(" ").append((int)maxWeight).append("kg")
              .append("  |  📊 最大容量: ").append(maxVolEx).append(" ").append((int)maxVol).append("kg");
        }
        return sb.length() > 0 ? sb.toString() : "暂无纪录，快去运动吧！💪";
    }

    // ── 周视图对比 ──
    private void calcWeekComparison(String filter) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        // 本周一
        cal.add(java.util.Calendar.DAY_OF_YEAR, -(dow == 1 ? 6 : dow - 2));
        String thisMon = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        // 上周一
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
        String lastMon = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_YEAR, 6);
        String lastSun = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        double thisDist = 0, lastDist = 0;
        int thisCount = 0, lastCount = 0, thisSec = 0, lastSec = 0;
        for (WorkoutRecord r : cardioRecords) {
            if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
            if (r.getDate() == null) continue;
            if (r.getDate().compareTo(thisMon) >= 0) {
                thisDist += r.getDistanceKm(); thisCount++; thisSec += r.getDurationSeconds();
            } else if (r.getDate().compareTo(lastMon) >= 0 && r.getDate().compareTo(lastSun) <= 0) {
                lastDist += r.getDistanceKm(); lastCount++; lastSec += r.getDurationSeconds();
            }
        }
        weekThisLabel.postValue(String.format(Locale.getDefault(), "本周 %d次 · %.1fkm · %dmin", thisCount, thisDist, thisSec / 60));
        weekLastLabel.postValue(String.format(Locale.getDefault(), "上周 %d次 · %.1fkm · %dmin", lastCount, lastDist, lastSec / 60));
        if (lastDist > 0) {
            double chg = (thisDist - lastDist) / lastDist * 100;
            weekComparison.postValue(String.format(Locale.getDefault(), "%.0f%%", Math.abs(chg)));
        } else weekComparison.postValue("-");
    }

    // ── 年视图 ──
    private void calcYearView(String filter) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int thisYear = cal.get(java.util.Calendar.YEAR);
        int[] days = new int[12];
        float[] dist = new float[12];
        for (WorkoutRecord r : cardioRecords) {
            if (!"All".equals(filter) && !filter.equals(r.getSportType())) continue;
            if (r.getDate() == null) continue;
            try {
                int y = Integer.parseInt(r.getDate().substring(0, 4));
                int m = Integer.parseInt(r.getDate().substring(5, 7)) - 1;
                if (y == thisYear && m >= 0 && m < 12) {
                    days[m]++; dist[m] += (float) r.getDistanceKm();
                }
            } catch (Exception ignored) {}
        }
        // 力量训练也计入天数
        java.util.HashSet<String> strengthDays = new java.util.HashSet<>();
        for (StrengthRecord r : strengthRecords) {
            if (r.getDate() != null) try {
                int y = Integer.parseInt(r.getDate().substring(0, 4));
                int m = Integer.parseInt(r.getDate().substring(5, 7)) - 1;
                if (y == thisYear && m >= 0 && m < 12) {
                    strengthDays.add(r.getDate());
                }
            } catch (Exception ignored) {}
        }
        for (String d : strengthDays) {
            try {
                int m = Integer.parseInt(d.substring(5, 7)) - 1;
                if (m >= 0 && m < 12) days[m] = Math.max(days[m], 1);
            } catch (Exception ignored) {}
        }
        yearWorkoutDays.postValue(days);
        yearDistance.postValue(dist);
    }
}
