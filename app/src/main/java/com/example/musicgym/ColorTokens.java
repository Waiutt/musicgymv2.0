package com.example.musicgym;

import android.graphics.Color;

/**
 * 全局颜色令牌 — 与 colors.xml 保持同步。
 * 避免在 Java 代码中直接调用 Color.parseColor("#...")。
 */
public final class ColorTokens {

    private ColorTokens() {}

    // ── 品牌色 ──
    public static final int BRAND_ORANGE         = Color.parseColor("#FC4C02");

    // ── 强调色 ──
    public static final int ACCENT_AMBER         = Color.parseColor("#f59e0b");
    public static final int ACCENT_GREEN         = Color.parseColor("#22c55e");
    public static final int ACCENT_RED           = Color.parseColor("#ef4444");
    public static final int ACCENT_CYAN          = Color.parseColor("#38bdf8");
    public static final int ACCENT_GREEN_SOFT    = Color.parseColor("#34d399");
    public static final int ACCENT_PURPLE        = Color.parseColor("#a78bfa");
    public static final int ACCENT_ORANGE_STRONG = Color.parseColor("#fb923c");

    // ── 功能色 ──
    public static final int PR_YELLOW            = Color.parseColor("#fbbf24");

    // ── 背景色 ──
    public static final int BG_CARD              = Color.parseColor("#1e293b");
    public static final int BG_INPUT             = Color.parseColor("#334155");
    public static final int BG_OVERLAY           = Color.parseColor("#cc0f172a");
    public static final int CURRENT_TRACK_BG     = Color.parseColor("#1aFC4C02");

    // ── 文字色 ──
    public static final int TEXT_SECONDARY       = Color.parseColor("#9ca3af");
    public static final int TEXT_HINT            = Color.parseColor("#6b7280");
    public static final int TEXT_MUTED           = Color.parseColor("#94a3b8");
    public static final int TEXT_PALE            = Color.parseColor("#cbd5e1");

    // ── 半透明色 ──
    public static final int SELECTED_GREEN_BG    = Color.parseColor("#1a22c55e");
    public static final int TABLE_HEADER_BG      = Color.parseColor("#1a334155");
    public static final int EDIT_FIELD_BG        = Color.parseColor("#2a3a4f");
}
