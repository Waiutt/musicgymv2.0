package com.example.musicgym;

import android.content.Context;
import android.view.ViewGroup;

/**
 * UI 通用工具方法。
 */
public final class UiUtils {

    private UiUtils() {}

    // ── 常用布局常量 ──
    public static final int WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    // ── dp → px ──
    /** 将 dp 值转换为像素（基于设备 density）。 */
    public static int dp(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
