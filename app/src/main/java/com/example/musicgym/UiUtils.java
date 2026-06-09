package com.example.musicgym;

import android.content.Context;
import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI 通用工具方法 + 共享线程池。
 */
public final class UiUtils {

    private UiUtils() {}

    // ── 常用布局常量 ──
    public static final int WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    // ── 共享后台线程池（避免每个类创建独立 Executor） ──
    private static final ExecutorService SHARED = Executors.newFixedThreadPool(4);

    public static ExecutorService sharedPool() { return SHARED; }

    public static void runInBackground(Runnable r) { SHARED.execute(r); }

    // ── dp → px ──
    /** 将 dp 值转换为像素（基于设备 density）。 */
    public static int dp(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ── 输入校验 ──

    /** 安全解析 double，失败返回 defaultValue */
    public static double safeParseDouble(String s, double defaultValue) {
        if (s == null || s.trim().isEmpty()) return defaultValue;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    /** 安全解析 int，失败返回 defaultValue */
    public static int safeParseInt(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) return defaultValue;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    /** 钳制 double 值在 [min, max] 范围内 */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 钳制 int 值在 [min, max] 范围内 */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 校验日期格式 yyyy-MM-dd，且不能是未来日期 */
    public static boolean isValidDate(String date) {
        if (date == null || date.length() != 10) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            sdf.setLenient(false);
            java.util.Date d = sdf.parse(date);
            return d != null && !d.after(new java.util.Date());
        } catch (Exception e) { return false; }
    }
}
