package com.example.musicgym;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Health Connect 入口。
 * 当前：一键跳转系统 Health Connect App
 * 未来：接入 SDK 自动同步体重/心率/步数（需真机调试）
 */
public class HealthConnectManager {

    /** 打开系统 Health Connect App */
    public static void open(Context ctx) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"));
            ctx.startActivity(intent);
        } catch (Exception e) {
            try {
                // 已经安装了 → 直接打开
                ctx.startActivity(ctx.getPackageManager()
                        .getLaunchIntentForPackage("com.google.android.apps.healthdata"));
            } catch (Exception ignored) {}
        }
    }

    /** 设备上是否安装了 Health Connect */
    public static boolean isInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo("com.google.android.apps.healthdata", 0);
            return true;
        } catch (Exception e) { return false; }
    }
}
