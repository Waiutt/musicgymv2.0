package com.example.musicgym;

import android.app.Application;
import android.content.Context;

/** Application 级单例 — 提供全局 Context */
public class MusicGymApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getContext() { return appContext; }
}
