package com.example.musicgym;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private TextView btnAction;
    private LinearLayout dotsContainer;

    private static final int[][] PAGES = {
        {0x1F3B5, 0x1F3C3},  // 🎵🏃
        {0x1F3CB, 0x1F4CA}, // 🏋📊
        {0x1F4AA, 0x1F525}, // 💪🔥
    };
    private static final String[] TITLES = {"音乐同步运动", "力量追踪", "AI 智能计划"};
    private static final String[] DESCS = {
        "运动时音乐自动匹配步频\n无需看手机,尽情奔跑",
        "50+ 动作库·肌群细分\n记录每一组,可视化成长",
        "基于你的训练数据\nAI 生成个性化周期计划"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 非首次启动 → 跳过
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("onboarding_done", false)) {
            startMain();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        pager = findViewById(R.id.onboarding_pager);
        btnAction = findViewById(R.id.onboarding_btn);
        dotsContainer = findViewById(R.id.onboarding_dots);

        pager.setAdapter(new PageAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                updateDots(pos);
                btnAction.setText(pos == 2 ? "开始使用" : "下一步");
            }
        });
        updateDots(0);

        btnAction.setOnClickListener(v -> {
            if (pager.getCurrentItem() < 2) {
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            } else {
                // Android 13+ 请求通知权限（前台服务必需）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                        return; // 等回调
                    }
                }
                prefs.edit().putBoolean("onboarding_done", true).apply();
                startMain();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        // 不管用户是否授权，都继续进入主页面
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putBoolean("onboarding_done", true).apply();
        startMain();
    }

    private void updateDots(int current) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < 3; i++) {
            TextView dot = new TextView(this);
            dot.setText(i == current ? "●" : "○");
            dot.setTextColor(i == current ? ColorTokens.BRAND_ORANGE : ColorTokens.TEXT_HINT);
            dot.setTextSize(20f);
            dot.setPadding(8, 0, 8, 0);
            dotsContainer.addView(dot);
        }
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
        @Override public int getItemCount() { return 3; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            h.emoji.setText(new String(Character.toChars(PAGES[pos][0]))
                    + "  " + new String(Character.toChars(PAGES[pos][1])));
            h.title.setText(TITLES[pos]);
            h.desc.setText(DESCS[pos]);
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView emoji, title, desc;
            Holder(View v) {
                super(v);
                emoji = v.findViewById(R.id.onboard_emoji);
                title = v.findViewById(R.id.onboard_title);
                desc = v.findViewById(R.id.onboard_desc);
            }
        }
    }
}
