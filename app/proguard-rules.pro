# MusicGym ProGuard Rules

# Room 实体（保持字段名不被混淆，否则数据库列名会变）
-keep class com.example.musicgym.WorkoutRecord { *; }
-keep class com.example.musicgym.StrengthRecord { *; }
-keep class com.example.musicgym.WeightRecord { *; }
-keep class com.example.musicgym.BodyMeasurement { *; }
-keep class com.example.musicgym.WorkoutTemplate { *; }
-keep class com.example.musicgym.BlogPost { *; }

# Room DAO
-keep interface com.example.musicgym.*Dao { *; }

# 高德地图 AMap
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.loc.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep class com.bumptech.glide.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Material Components (BottomSheet)
-keep class com.google.android.material.** { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# 保留行号
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
