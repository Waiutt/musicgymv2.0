package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/** 主题切换管理 — 深色/浅色/跟随系统 */
public class ThemeManager {

    private static final String KEY = "app_theme";
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_DARK  = 1;
    public static final int MODE_LIGHT = 2;

    /** 应用主题（需在 setContentView 之前调用） */
    public static void apply(Context ctx) {
        int mode = ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)
                .getInt(KEY, MODE_DARK); // 默认深色
        switch (mode) {
            case MODE_LIGHT: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO); break;
            case MODE_SYSTEM: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            default: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES); break;
        }
    }

    /** 持久化主题并立即生效 */
    public static void setMode(Context ctx, int mode) {
        ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)
                .edit().putInt(KEY, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode == MODE_LIGHT
                ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == MODE_SYSTEM
                ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : AppCompatDelegate.MODE_NIGHT_YES);
    }

    public static int getMode(Context ctx) {
        return ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)
                .getInt(KEY, MODE_DARK);
    }
}
