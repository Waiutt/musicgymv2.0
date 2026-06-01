package com.example.musicgym;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExercisePageAdapter extends RecyclerView.Adapter<ExercisePageAdapter.PageHolder> {

    private final StrengthWorkoutActivity activity;
    private final List<String> exerciseNames;
    private final Map<String, List<StrengthWorkoutActivity.SetEntry>> workoutData;
    private final Map<String, double[]> prCache;

    ExercisePageAdapter(StrengthWorkoutActivity activity,
                        Map<String, List<StrengthWorkoutActivity.SetEntry>> workoutData,
                        Map<String, double[]> prCache) {
        this.activity = activity;
        this.workoutData = workoutData;
        this.prCache = prCache;
        this.exerciseNames = new ArrayList<>(workoutData.keySet());
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new PageHolder(page);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        String exName = exerciseNames.get(position);
        List<StrengthWorkoutActivity.SetEntry> sets = workoutData.get(exName);
        if (sets == null) return;

        holder.content.removeAllViews();
        holder.content.addView(buildPageContent(position, exName, sets));
    }

    @Override
    public int getItemCount() {
        return exerciseNames.size();
    }

    void refreshNames() {
        exerciseNames.clear();
        exerciseNames.addAll(workoutData.keySet());
    }

    // ==================== 页面内容构建 ====================
    private LinearLayout buildPageContent(int exI, String exName, List<StrengthWorkoutActivity.SetEntry> sets) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(20), dp(24), dp(20));

        // 进度指示器 (如 "2 / 5")
        TextView tvProgress = new TextView(activity);
        tvProgress.setText((exI + 1) + " / " + exerciseNames.size());
        tvProgress.setTextColor(Color.parseColor("#6b7280"));
        tvProgress.setTextSize(12f);
        tvProgress.setGravity(Gravity.CENTER);
        tvProgress.setPadding(0, 0, 0, dp(16));
        page.addView(tvProgress);

        // 动作名
        TextView tvName = new TextView(activity);
        tvName.setText(exName);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(28f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setPadding(0, 0, 0, dp(8));
        page.addView(tvName);

        // PR + 容量
        LinearLayout infoRow = new LinearLayout(activity);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setPadding(0, 0, 0, dp(20));

        double[] pr = prCache.get(exName);
        if (pr != null) {
            infoRow.addView(tagView("PR " + String.format(Locale.getDefault(), "%.0fkg", pr[0]), "#fbbf24"));
        }
        double vol = 0;
        for (StrengthWorkoutActivity.SetEntry se : sets) vol += se.weight * se.reps;
        infoRow.addView(tagView(String.format(Locale.getDefault(), "%.0f kg", vol), "#f59e0b"));
        page.addView(infoRow);

        // 表头
        LinearLayout header = buildHeaderRow();
        page.addView(header);

        // 分隔线
        View divider = new View(activity);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#334155"));
        page.addView(divider);

        // 组行
        double oneRM = pr != null ? pr[0] : 0;
        for (int i = 0; i < sets.size(); i++) {
            StrengthWorkoutActivity.SetEntry se = sets.get(i);
            final int si = i;
            final int fExI = exI;

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));

            // 序号
            TextView tvNum = new TextView(activity);
            tvNum.setText(String.valueOf(i + 1));
            tvNum.setTextColor(Color.WHITE);
            tvNum.setTextSize(16f);
            tvNum.setGravity(Gravity.CENTER);
            tvNum.setLayoutParams(new LinearLayout.LayoutParams(dp(40), WRAP));
            row.addView(tvNum);

            // 重量
            EditText etW = buildEdit(String.valueOf(se.weight), true);
            final int finalSi1 = si;
            etW.setOnFocusChangeListener((v, f) -> {
                if (!f) try {
                    sets.get(finalSi1).weight = Double.parseDouble(etW.getText().toString());
                    activity.onPagerDataChanged(fExI);
                } catch (Exception ignored) {}
            });
            row.addView(etW);

            // 次数
            EditText etR = buildEdit(String.valueOf(se.reps), false);
            final int finalSi2 = si;
            etR.setOnFocusChangeListener((v, f) -> {
                if (!f) try {
                    sets.get(finalSi2).reps = Integer.parseInt(etR.getText().toString());
                    activity.onPagerDataChanged(fExI);
                } catch (Exception ignored) {}
            });
            row.addView(etR);

            // %1RM
            String pct = oneRM > 0 ? String.format(Locale.getDefault(), "%.0f%%", se.weight / oneRM * 100) : "-";
            TextView tvPct = new TextView(activity);
            tvPct.setText(pct);
            tvPct.setTextColor(Color.parseColor("#94a3b8"));
            tvPct.setTextSize(13f);
            tvPct.setGravity(Gravity.CENTER);
            tvPct.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1));
            row.addView(tvPct);

            // 删除
            TextView btnDel = new TextView(activity);
            btnDel.setText("−");
            btnDel.setTextColor(Color.parseColor("#ef4444"));
            btnDel.setTextSize(20f);
            btnDel.setGravity(Gravity.CENTER);
            btnDel.setPadding(dp(12), dp(8), dp(4), dp(8));
            btnDel.setOnClickListener(v -> {
                if (sets.size() > 1) {
                    sets.remove(si);
                    activity.onPagerDataChanged(fExI);
                }
            });
            row.addView(btnDel);

            page.addView(row);

            // 行间分隔
            if (i < sets.size() - 1) {
                View sep = new View(activity);
                sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                sep.setBackgroundColor(Color.parseColor("#1e293b"));
                page.addView(sep);
            }
        }

        // 分隔线
        View divider2 = new View(activity);
        divider2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider2.setBackgroundColor(Color.parseColor("#334155"));
        page.addView(divider2);

        // %1RM 快捷填充
        if (pr != null) {
            LinearLayout pctRow = new LinearLayout(activity);
            pctRow.setOrientation(LinearLayout.HORIZONTAL);
            pctRow.setPadding(0, dp(16), 0, 0);
            for (int pctVal : new int[]{50, 65, 75, 85, 95}) {
                TextView pctBtn = new TextView(activity);
                pctBtn.setText(pctVal + "%");
                pctBtn.setTextColor(Color.parseColor("#38bdf8"));
                pctBtn.setTextSize(13f);
                pctBtn.setBackgroundColor(Color.parseColor("#334155"));
                pctBtn.setPadding(dp(16), dp(8), dp(16), dp(8));
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(WRAP, WRAP);
                plp.setMargins(0, 0, dp(8), 0);
                pctBtn.setLayoutParams(plp);
                pctBtn.setOnClickListener(vv -> {
                    double w = Math.round(pr[0] * pctVal / 100.0 / 2.5) * 2.5;
                    for (StrengthWorkoutActivity.SetEntry se : sets) se.weight = w;
                    activity.onPagerDataChanged(exI);
                });
                pctRow.addView(pctBtn);
            }
            page.addView(pctRow);
        }

        // 操作按钮行
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(20), 0, 0);

        TextView btnAdd = new TextView(activity);
        btnAdd.setText("+ 添加组");
        btnAdd.setTextColor(Color.parseColor("#38bdf8"));
        btnAdd.setTextSize(16f);
        btnAdd.setPadding(0, 0, dp(32), 0);
        btnAdd.setOnClickListener(v -> {
            sets.add(new StrengthWorkoutActivity.SetEntry(0, 10));
            activity.onPagerDataChanged(exI);
        });
        actions.addView(btnAdd);

        TextView btnRest = new TextView(activity);
        btnRest.setText("⏱ 组间休息");
        btnRest.setTextColor(Color.parseColor("#f59e0b"));
        btnRest.setTextSize(16f);
        btnRest.setOnClickListener(v -> activity.startRestTimer());
        actions.addView(btnRest);

        page.addView(actions);
        return page;
    }

    private LinearLayout buildHeaderRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));

        for (String h : new String[]{"组", "重量(kg)", "次数", "%1RM", ""}) {
            TextView th = new TextView(activity);
            th.setText(h);
            th.setTextColor(Color.parseColor("#6b7280"));
            th.setTextSize(11f);

            LinearLayout.LayoutParams hlp;
            if (h.equals("组") || h.equals("")) hlp = new LinearLayout.LayoutParams(dp(40), WRAP);
            else hlp = new LinearLayout.LayoutParams(0, WRAP, 1);
            th.setLayoutParams(hlp);
            th.setGravity(Gravity.CENTER);
            row.addView(th);
        }
        return row;
    }

    private EditText buildEdit(String val, boolean decimal) {
        EditText et = new EditText(activity);
        et.setText(val);
        et.setTextColor(Color.WHITE);
        et.setTextSize(15f);
        et.setInputType(decimal ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL) : InputType.TYPE_CLASS_NUMBER);
        et.setBackgroundColor(Color.parseColor("#334155"));
        et.setPadding(dp(8), dp(8), dp(8), dp(8));
        et.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, WRAP, 1);
        elp.setMargins(0, 0, dp(4), 0);
        et.setLayoutParams(elp);
        return et;
    }

    private TextView tagView(String text, String colorHex) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(12f);
        tv.setBackgroundColor(Color.parseColor("#334155"));
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.setMargins(0, 0, dp(8), 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(int d) {
        return (int) (d * activity.getResources().getDisplayMetrics().density);
    }

    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static class PageHolder extends RecyclerView.ViewHolder {
        LinearLayout content;
        PageHolder(LinearLayout v) {
            super(v);
            content = v;
        }
    }
}
