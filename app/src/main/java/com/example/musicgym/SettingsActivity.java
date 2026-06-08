package com.example.musicgym;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 设置页 — 数据管理、隐私政策、关于 */
public class SettingsActivity extends AppCompatActivity {

    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        executor = Executors.newSingleThreadExecutor();

        // 返回
        findViewById(R.id.settings_btn_back).setOnClickListener(v -> finish());

        // 版本号
        TextView tvVersion = findViewById(R.id.settings_tv_version);
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + v);
        } catch (Exception ignored) {}

        // 导出 CSV
        findViewById(R.id.settings_btn_export).setOnClickListener(v -> {
            executor.execute(() -> {
                boolean ok = DataExporter.exportAll(this);
                runOnUiThread(() -> Toast.makeText(this,
                        ok ? "已导出到 Downloads/MusicGym_data.csv" : "导出失败",
                        Toast.LENGTH_SHORT).show());
            });
        });

        // 清除数据
        findViewById(R.id.settings_btn_clear).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("确认清除")
                        .setMessage("将删除所有运动记录、训练数据和体重记录，此操作不可恢复。")
                        .setPositiveButton("确认清除", (d, w) -> {
                            executor.execute(() -> {
                                AppDatabase.getInstance(this).clearAllTables();
                                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
                                getSharedPreferences("music_fav", MODE_PRIVATE).edit().clear().apply();
                                runOnUiThread(() -> Toast.makeText(this,
                                        "数据已清除", Toast.LENGTH_SHORT).show());
                            });
                        })
                        .setNegativeButton("取消", null)
                        .show());

        // 清除缓存
        findViewById(R.id.settings_btn_cache).setOnClickListener(v -> {
            executor.execute(() -> {
                try {
                    java.io.File cache = getCacheDir();
                    deleteDir(cache);
                    java.io.File extCache = getExternalCacheDir();
                    if (extCache != null) deleteDir(extCache);
                } catch (Exception ignored) {}
                runOnUiThread(() -> Toast.makeText(this,
                        "缓存已清除", Toast.LENGTH_SHORT).show());
            });
        });

        // 隐私政策
        findViewById(R.id.settings_btn_privacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        // 主题切换
        findViewById(R.id.settings_btn_theme).setOnClickListener(v -> {
            int current = ThemeManager.getMode(this);
            new AlertDialog.Builder(this)
                    .setTitle("主题切换")
                    .setSingleChoiceItems(new String[]{"跟随系统", "深色模式", "浅色模式"}, current, (d, w) -> {
                        ThemeManager.setMode(this, w);
                        d.dismiss();
                        recreate(); // 立即生效
                    }).show();
        });
        ((TextView) findViewById(R.id.settings_theme_label))
                .setText(new String[]{"跟随系统", "深色模式", "浅色模式"}[ThemeManager.getMode(this)]);

        // 关于
        findViewById(R.id.settings_btn_about).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("关于 MusicGym")
                        .setMessage("MusicGym " + tvVersion.getText() + "\n\n一站式健身记录助手\n运动追踪 · 力量训练 · 音乐播放\n社区社交 · AI 训练计划\n\n© 2026 MusicGym Team")
                        .setPositiveButton("确定", null)
                        .show());
    }

    private void deleteDir(java.io.File dir) {
        if (dir == null || !dir.exists()) return;
        java.io.File[] files = dir.listFiles();
        if (files != null) for (java.io.File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}
