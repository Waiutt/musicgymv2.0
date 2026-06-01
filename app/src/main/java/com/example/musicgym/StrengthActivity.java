package com.example.musicgym;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StrengthActivity extends AppCompatActivity {

    private LinearLayout tabContainer, exerciseContainer;
    private TextView tvSelectedCount, tvBottomCount, btnStart, btnTemplate;
    private EditText etSearch;
    private View btnBack;

    private final Map<String, ExerciseInfo> selectedExercises = new LinkedHashMap<>();
    private final List<String> customExercises = new ArrayList<>();
    private String currentGroup = "chest";
    private String searchQuery = "";
    private ExecutorService executor;

    // 动作库
    private static final List<MuscleGroup> GROUPS = Arrays.asList(
        new MuscleGroup("胸部", "chest", "#FC4C02",
            new SubGroup("上胸", "上斜杠铃卧推","上斜哑铃卧推","上斜哑铃飞鸟","器械上斜推胸"),
            new SubGroup("中胸", "杠铃平板卧推","哑铃平板卧推","哑铃飞鸟","绳索夹胸","器械推胸","斯万推胸"),
            new SubGroup("下胸", "下斜杠铃卧推","双杠臂屈伸","高位绳索夹胸","器械下斜推胸")
        ),
        new MuscleGroup("背部", "back", "#38bdf8",
            new SubGroup("宽度", "引体向上","高位下拉","直臂下压","对握下拉"),
            new SubGroup("厚度", "杠铃划船","哑铃单臂划船","坐姿划船","T杆划船","海豹划船"),
            new SubGroup("下背", "传统硬拉","罗马尼亚硬拉","山羊挺身","早安式体前屈")
        ),
        new MuscleGroup("腿部", "legs", "#34d399",
            new SubGroup("股四头肌", "杠铃深蹲","腿举","哈克深蹲","腿屈伸","前蹲","高脚杯深蹲"),
            new SubGroup("腘绳肌", "腿弯举","罗马尼亚硬拉","北欧弯举","臀桥腿弯举"),
            new SubGroup("臀部", "臀推","保加利亚分腿蹲","弓步蹲","绳索后踢","蚌式开合"),
            new SubGroup("小腿", "站姿提踵","坐姿提踵","驴式提踵")
        ),
        new MuscleGroup("肩部", "shoulders", "#f59e0b",
            new SubGroup("前束", "杠铃推举","哑铃推举","阿诺德推举","前平举","杠铃片前平举"),
            new SubGroup("中束", "侧平举","直立划船","单臂绳索侧平举"),
            new SubGroup("后束", "面拉","俯身飞鸟","蝴蝶机反向飞鸟","绳索面拉")
        ),
        new MuscleGroup("手臂", "arms", "#a78bfa",
            new SubGroup("肱二头肌", "杠铃弯举","哑铃弯举","锤式弯举","牧师凳弯举","集中弯举","上斜哑铃弯举"),
            new SubGroup("肱三头肌", "三头绳索下压","窄距卧推","仰卧臂屈伸","哑铃颈后臂屈伸","双杠臂屈伸","俯身臂屈伸"),
            new SubGroup("前臂", "腕弯举","反向腕弯举","杠铃静力握持")
        ),
        new MuscleGroup("核心", "core", "#ef4444",
            new SubGroup("上腹", "卷腹","绳索卷腹","器械卷腹","仰卧起坐"),
            new SubGroup("下腹", "仰卧抬腿","悬垂举腿","反向卷腹","剪刀腿"),
            new SubGroup("侧腹", "俄罗斯转体","侧平板支撑","伐木式","哑铃侧屈"),
            new SubGroup("稳定", "平板支撑","腹肌轮","死虫式","鸟狗式")
        ),
        new MuscleGroup("全身", "fullbody", "#fb923c",
            new SubGroup("爆发力", "杠铃高翻","哑铃抓举","壶铃摆动","跳箱","战绳"),
            new SubGroup("综合", "波比跳","农夫行走","壶铃土耳其起立","墙球","划船机")
        )
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength);

        tabContainer = findViewById(R.id.strength_tab_container);
        exerciseContainer = findViewById(R.id.strength_exercise_container);
        tvSelectedCount = findViewById(R.id.strength_selected_count);
        tvBottomCount = findViewById(R.id.strength_bottom_count);
        btnStart = findViewById(R.id.strength_btn_start);
        btnBack = findViewById(R.id.strength_btn_back);
        etSearch = findViewById(R.id.strength_search);
        btnTemplate = findViewById(R.id.strength_btn_template);

        executor = Executors.newSingleThreadExecutor();

        btnBack.setOnClickListener(v -> finish());
        btnStart.setOnClickListener(v -> startWorkout());
        btnTemplate.setOnClickListener(v -> showTemplateMenu());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim();
                if (!searchQuery.isEmpty()) {
                    showSearchResults(searchQuery);
                } else {
                    showGroup(currentGroup);
                }
            }
        });

        buildTabs();
        showGroup(currentGroup);
    }

    // ==================== 搜索 ====================
    private void showSearchResults(String q) {
        exerciseContainer.removeAllViews();
        String lower = q.toLowerCase();
        List<String> results = new ArrayList<>();
        for (MuscleGroup g : GROUPS)
            for (SubGroup sub : g.subGroups)
                for (String ex : sub.exercises)
                    if (ex.toLowerCase().contains(lower) && !results.contains(ex))
                        results.add(ex);
        for (String ex : customExercises)
            if (ex.toLowerCase().contains(lower) && !results.contains(ex))
                results.add(ex);

        if (results.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("未找到匹配动作，点击添加自定义动作");
            tv.setTextColor(ColorTokens.ACCENT_AMBER); tv.setTextSize(13f);
            tv.setPadding(8, 20, 8, 20);
            tv.setOnClickListener(v -> showAddCustomDialog());
            exerciseContainer.addView(tv);
            return;
        }

        LinearLayout row = null;
        for (int i = 0; i < results.size(); i++) {
            if (i % 3 == 0) row = newCardRow(exerciseContainer);
            String name = results.get(i);
            row.addView(buildExerciseCard(name, "#FC4C02", selectedExercises.containsKey(name)));
        }
    }

    private LinearLayout newCardRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 4);
        parent.addView(row);
        return row;
    }

    private EditText createStyledEditText(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.GRAY);
        et.setBackgroundColor(ColorTokens.BG_INPUT);
        et.setPadding(20, 16, 20, 16);
        return et;
    }

    // ==================== 模板 ====================
    private void showTemplateMenu() {
        String[] items = {"💾 保存当前选择为模板", "📂 加载已有模板", "➕ 添加自定义动作"};
        new AlertDialog.Builder(this)
                .setTitle("模板管理")
                .setItems(items, (d, which) -> {
                    if (which == 0) saveTemplate();
                    else if (which == 1) loadTemplate();
                    else showAddCustomDialog();
                }).show();
    }

    private void saveTemplate() {
        if (selectedExercises.isEmpty()) { Toast.makeText(this, "请先选择动作", Toast.LENGTH_SHORT).show(); return; }
        EditText et = createStyledEditText("模板名称 (如: 推日)");
        new AlertDialog.Builder(this).setTitle("保存模板").setView(et)
                .setPositiveButton("保存", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) name = "未命名模板";
                    try {
                        JSONArray arr = new JSONArray();
                        for (String ex : selectedExercises.keySet()) arr.put(ex);
                        WorkoutTemplate t = new WorkoutTemplate(name, arr.toString());
                        executor.execute(() -> AppDatabase.getInstance(this).workoutTemplateDao().insert(t));
                        Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { e.printStackTrace(); }
                }).setNegativeButton("取消", null).show();
    }

    private void loadTemplate() {
        executor.execute(() -> {
            List<WorkoutTemplate> temps = AppDatabase.getInstance(this).workoutTemplateDao().getAll();
            runOnUiThread(() -> {
                if (temps.isEmpty()) { Toast.makeText(this, "暂无模板", Toast.LENGTH_SHORT).show(); return; }
                String[] names = new String[temps.size() + 1];
                for (int i = 0; i < temps.size(); i++) names[i] = temps.get(i).getName();
                names[temps.size()] = "🗑 删除所有模板";
                new AlertDialog.Builder(this).setTitle("加载模板")
                        .setItems(names, (d, which) -> {
                            if (which == temps.size()) {
                                for (WorkoutTemplate t : temps)
                                    executor.execute(() -> AppDatabase.getInstance(this).workoutTemplateDao().deleteById(t.getId()));
                                Toast.makeText(this, "模板已清空", Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    selectedExercises.clear();
                                    JSONArray arr = new JSONArray(temps.get(which).getExercisesJson());
                                    for (int i = 0; i < arr.length(); i++) {
                                        String n = arr.getString(i);
                                        selectedExercises.put(n, new ExerciseInfo(n));
                                    }
                                    updateSelectionUI(); showGroup(currentGroup);
                                    Toast.makeText(this, "模板已加载", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }).show();
            });
        });
    }

    private void showAddCustomDialog() {
        EditText et = createStyledEditText("输入动作名称");
        new AlertDialog.Builder(this).setTitle("添加自定义动作").setView(et)
                .setPositiveButton("添加", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty() && !customExercises.contains(name)) {
                        customExercises.add(name);
                        Toast.makeText(this, "已添加: " + name, Toast.LENGTH_SHORT).show();
                        showGroup(currentGroup);
                    }
                }).setNegativeButton("取消", null).show();
    }

    // ==================== 标签 ====================
    private void buildTabs() {
        for (MuscleGroup g : GROUPS) {
            TextView tab = new TextView(this);
            tab.setText(g.name); tab.setTextSize(13f); tab.setPadding(28, 10, 28, 10); tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(WRAP, MATCH));
            final String key = g.key;
            tab.setOnClickListener(v -> {
                currentGroup = key; etSearch.setText(""); searchQuery = "";
                for (int i = 0; i < tabContainer.getChildCount(); i++) {
                    TextView t = (TextView) tabContainer.getChildAt(i);
                    t.setBackgroundColor(Color.TRANSPARENT); t.setTextColor(ColorTokens.TEXT_SECONDARY);
                    t.setTypeface(null, Typeface.NORMAL);
                }
                tab.setBackgroundColor(ColorTokens.BRAND_ORANGE); tab.setTextColor(Color.WHITE);
                tab.setTypeface(null, Typeface.BOLD); showGroup(key);
            });
            tabContainer.addView(tab);
        }
        TextView first = (TextView) tabContainer.getChildAt(0);
        first.setBackgroundColor(ColorTokens.BRAND_ORANGE); first.setTextColor(Color.WHITE);
        first.setTypeface(null, Typeface.BOLD);
    }

    private void showGroup(String key) {
        exerciseContainer.removeAllViews();
        MuscleGroup group = null;
        for (MuscleGroup g : GROUPS) { if (g.key.equals(key)) { group = g; break; } }
        if (group == null) return;

        // 先显示自定义动作
        if (!customExercises.isEmpty()) {
            TextView subTitle = new TextView(this);
            subTitle.setText("自定义"); subTitle.setTextColor(ColorTokens.ACCENT_AMBER); subTitle.setTextSize(12f);
            subTitle.setPadding(8, 12, 8, 8); exerciseContainer.addView(subTitle);
            LinearLayout row = null;
            for (int i = 0; i < customExercises.size(); i++) {
                if (i % 3 == 0) { row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, 0, 0, 4); exerciseContainer.addView(row); }
                String n = customExercises.get(i);
                row.addView(buildExerciseCard(n, "#f59e0b", selectedExercises.containsKey(n)));
            }
        }

        for (SubGroup sub : group.subGroups) {
            TextView subTitle = new TextView(this);
            subTitle.setText(sub.name); subTitle.setTextColor(ColorTokens.TEXT_MUTED); subTitle.setTextSize(12f);
            subTitle.setPadding(8, 16, 8, 8); exerciseContainer.addView(subTitle);

            LinearLayout row = null;
            for (int i = 0; i < sub.exercises.size(); i++) {
                if (i % 3 == 0) { row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, 0, 0, 4); exerciseContainer.addView(row); }
                String n = sub.exercises.get(i);
                row.addView(buildExerciseCard(n, group.color, selectedExercises.containsKey(n)));
            }
        }
    }

    private View buildExerciseCard(String name, String accentColor, boolean isSelected) {
        FrameLayout card = new FrameLayout(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(140), 1f);
        clp.setMargins(3, 0, 3, 6); card.setLayoutParams(clp);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL); inner.setBackgroundColor(ColorTokens.BG_CARD);
        inner.setLayoutParams(new LinearLayout.LayoutParams(MATCH, MATCH)); inner.setPadding(8, 0, 8, 8);

        FrameLayout imgArea = new FrameLayout(this);
        imgArea.setLayoutParams(new LinearLayout.LayoutParams(MATCH, 0, 1f));
        int ci = Color.parseColor(accentColor);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(120, Color.red(ci), Color.green(ci), Color.blue(ci)),
                        Color.argb(40, Color.red(ci), Color.green(ci), Color.blue(ci))});
        imgArea.setBackground(gd);
        TextView ic = new TextView(this); ic.setText(getEmoji(name)); ic.setTextSize(28f); ic.setGravity(Gravity.CENTER);
        ic.setLayoutParams(new FrameLayout.LayoutParams(MATCH, MATCH)); imgArea.addView(ic);
        inner.addView(imgArea);

        TextView tv = new TextView(this);
        tv.setText(name.length() > 5 ? name.substring(0, 4) + ".." : name);
        tv.setTextColor(isSelected ? ColorTokens.BRAND_ORANGE : Color.WHITE);
        tv.setTextSize(11f); tv.setGravity(Gravity.CENTER); tv.setPadding(2, 6, 2, 2);
        inner.addView(tv);

        if (isSelected) {
            TextView chk = new TextView(this); chk.setText("✓"); chk.setTextColor(Color.WHITE); chk.setTextSize(10f); chk.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams chklp = new FrameLayout.LayoutParams(22, 22); chklp.gravity = Gravity.TOP | Gravity.END;
            chklp.setMargins(0, 4, 4, 0);
            GradientDrawable cb = new GradientDrawable(); cb.setShape(GradientDrawable.OVAL); cb.setColor(ColorTokens.BRAND_ORANGE);
            chk.setBackground(cb); card.addView(chk);
        }
        card.addView(inner);
        card.setOnClickListener(v -> showExerciseDetail(name, accentColor));
        return card;
    }

    private void showExerciseDetail(String name, String accentColor) {
        ScrollView sv = new ScrollView(this);
        LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL); detail.setPadding(24, 24, 24, 24);

        FrameLayout demo = new FrameLayout(this);
        demo.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(200)));
        int ci = Color.parseColor(accentColor);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(180, Color.red(ci), Color.green(ci), Color.blue(ci)),
                        Color.argb(60, Color.red(ci), Color.green(ci), Color.blue(ci))});
        gd.setCornerRadius(12); demo.setBackground(gd);
        TextView dt = new TextView(this); dt.setText(getEmoji(name) + "\n" + name);
        dt.setTextColor(Color.WHITE); dt.setTextSize(18f); dt.setGravity(Gravity.CENTER);
        dt.setLayoutParams(new FrameLayout.LayoutParams(MATCH, MATCH)); demo.addView(dt);
        detail.addView(demo);

        TextView tt = new TextView(this); tt.setText(name); tt.setTextColor(Color.WHITE); tt.setTextSize(22f);
        tt.setTypeface(null, Typeface.BOLD); tt.setPadding(0, 20, 0, 8); detail.addView(tt);

        String targets = findTargets(name);
        TextView targ = new TextView(this); targ.setText("目标肌群: " + targets);
        targ.setTextColor(ColorTokens.TEXT_MUTED); targ.setTextSize(14f); targ.setPadding(0, 0, 0, 16); detail.addView(targ);

        TextView desc = new TextView(this);
        desc.setText("标准动作执行时，注意核心收紧、动作幅度完整。建议每组 8-12 次，组间休息 60-90 秒。初学者请用轻重量体会发力感，熟悉后再逐渐加重。");
        desc.setTextColor(ColorTokens.TEXT_PALE); desc.setTextSize(14f); desc.setLineSpacing(4, 1.2f); detail.addView(desc);
        sv.addView(detail);

        boolean sel = selectedExercises.containsKey(name);
        new AlertDialog.Builder(this).setView(sv)
                .setPositiveButton(sel ? "移除动作" : "添加动作", (d, w) -> {
                    if (sel) selectedExercises.remove(name); else selectedExercises.put(name, new ExerciseInfo(name));
                    showGroup(currentGroup); updateSelectionUI();
                }).setNegativeButton("关闭", null).show();
    }

    private String findTargets(String name) {
        for (MuscleGroup g : GROUPS) for (SubGroup s : g.subGroups) if (s.exercises.contains(name)) return g.name + " · " + s.name;
        return "自定义";
    }

    private void updateSelectionUI() {
        int c = selectedExercises.size();
        tvSelectedCount.setText("已选 " + c + " 项"); tvBottomCount.setText(c + " 个动作已选");
        btnStart.setAlpha(c > 0 ? 1f : 0.4f);
    }

    private void startWorkout() {
        if (selectedExercises.isEmpty()) { Toast.makeText(this, "请先选择训练动作", Toast.LENGTH_SHORT).show(); return; }
        Intent i = new Intent(this, StrengthWorkoutActivity.class);
        i.putStringArrayListExtra("exercises", new ArrayList<>(selectedExercises.keySet()));
        startActivity(i);
    }

    private int dp(int d) { return UiUtils.dp(this, d); }
    private String getEmoji(String n) {
        if (n.contains("卧推")||n.contains("推胸")) return "🏋️"; if (n.contains("深蹲")||n.contains("腿举")) return "🦵";
        if (n.contains("硬拉")) return "🏋️‍♂️"; if (n.contains("弯举")||n.contains("臂")) return "💪";
        if (n.contains("推举")||n.contains("平举")) return "🙆"; if (n.contains("划船")||n.contains("下拉")) return "🚣";
        if (n.contains("引体")) return "🧗"; if (n.contains("飞鸟")||n.contains("夹胸")) return "🕊️";
        if (n.contains("卷腹")||n.contains("平板")) return "🔄"; if (n.contains("抬腿")||n.contains("举腿")) return "🦿";
        if (n.contains("提踵")) return "🦶"; if (n.contains("高翻")||n.contains("抓举")) return "🏆";
        if (n.contains("壶铃")||n.contains("波比")) return "🔔"; if (n.contains("绳索")) return "🪢"; if (n.contains("农夫")) return "🚶";
        return "🎯";
    }

    static final int WRAP = UiUtils.WRAP, MATCH = UiUtils.MATCH;

    static class MuscleGroup { String name,key,color; List<SubGroup> subGroups;
        MuscleGroup(String n,String k,String c,SubGroup...ss){name=n;key=k;color=c;subGroups=Arrays.asList(ss);} }
    static class SubGroup { String name; List<String> exercises;
        SubGroup(String n,String...ex){name=n;exercises=Arrays.asList(ex);} }
    static class ExerciseInfo { String name; ExerciseInfo(String n){name=n;} }
}
