# Health Connect 一键激活指南

## 前提条件
- 真机 Android 14+ 
- 安装了 [Google Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata)
- 小米手表/手环已配对并同步到 Health Connect

## 激活步骤（3步, 5分钟）

### 1. 复制代码文件
```bash
cp docs/HealthConnectManager.java app/src/main/java/com/example/musicgym/
```

### 2. 恢复 build.gradle 依赖
在 `app/build.gradle` 的 dependencies 中添加：
```groovy
implementation "androidx.health.connect:connect-client:1.1.0"
```

### 3. 恢复 AndroidManifest 权限
在 `<application>` 标签前添加：
```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_WEIGHT" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />

<queries>
    <package android:name="com.google.android.apps.healthdata" />
</queries>
```

## API 兼容说明
Health Connect 1.1.0 移除了 Builder 模式。如果编译失败，需要将：
```java
new ReadRecordsRequest.Builder<>(Record.class).setTimeRangeFilter(...).build()
```
替换为新的 API 调用方式。参考官方文档：
https://developer.android.com/health-and-fitness/guides/health-connect

## 真机调试
编译通过后，在真机上运行 → 个人中心 → 点击"同步健康数据" → 授权 → 自动读取体重/步数/睡眠/心率
