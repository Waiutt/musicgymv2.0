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
}
