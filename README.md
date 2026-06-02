# MusicGym v2.0 🏃‍♂️🎵

**运动追踪 × 力量训练 × 本地音乐播放器** — 一站式健身记录 Android 应用

[![API](https://img.shields.io/badge/API-34%2B-green)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange)](https://www.java.com)

---

## 架构设计

```
┌─────────────────────────────────────────────────────┐
│                   MainActivity                       │
│          BottomNavigationView (5 Tabs)               │
└──────────┬──────────┬──────────┬──────────┬─────────┘
           │          │          │          │
     ┌─────▼─┐  ┌────▼──┐  ┌────▼──┐  ┌───▼────┐  ┌──┐
     │ GYM  │  │STATS │  │ MUSIC │  │PROFILE│  │社 │
     │运动入口│  │数据统计│  │音乐播放│  │个人中心│  │区 │
     └──┬───┘  └──┬───┘  └───────┘  └──┬───┘  └──┘
        │         │                    │
  ┌─────▼──┐ ┌───▼──────────────┐ ┌───▼──────┐
  │Workout │ │  StatsViewModel  │ │ CSV导出  │
  │Activity│ │  ├ StatsRepository│ │ 训练提醒 │
  │ (GPS)  │ │  └ Room DAOs     │ └──────────┘
  └────────┘ └──────────────────┘
  ┌────────┐
  │Strength│     Architecture: MVVM (Stats) + Repository Pattern
  │Activity│     Database: Room v4 (6 entities, 3 incremental migrations)
  │ 50+动作库│
  │ 训练记录│
  └────────┘
```

### MVVM 示例（Stats 页）

```
StatsFragment  ── observe(LiveData) ──▶  StatsViewModel
    (UI only)                                │
                                        StatsRepository
                                             │
                                        Room Database
```

## 功能概览

| 模块 | 功能 | 技术实现 |
|------|------|----------|
| 🏃 **有氧追踪** | GPS 实时轨迹、距离/配速/卡路里仪表盘 | 高德地图 SDK, AMapLocationClient |
| 🏋️ **力量训练** | 7大肌群 × 17子分组 × 50+ 动作库 | 动态 UI 构建, Room 模板存储 |
| 📊 **数据统计** | 月对比折线图 + 日历热力图 + 历史列表 | **MVVM + LiveData**, MPAndroidChart |
| 🎵 **音乐播放** | 本地扫描、提拉式歌单、删除管理 | MediaPlayer, MediaStore, 手势动画 |
| 👤 **个人中心** | 体重围度记录、CSV导出、训练提醒 | AlarmManager, FileWriter |
| 📝 **社区分享** | 瀑布流帖子、图文发布 | StaggeredGridLayoutManager |

## 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Java 11 |
| **架构** | **MVVM** (ViewModel + LiveData) + Repository Pattern |
| **数据库** | Room (v4, 6 entities, 3 incremental migrations) |
| **地图** | 高德 3D 地图 SDK (AMap) + 定位 |
| **图表** | MPAndroidChart v3.1.0 (CUBIC_BEZIER) |
| **异步** | ExecutorService + Handler (主线程回调) |
| **UI** | Material Components, CardView, RecyclerView, ViewPager2 |
| **图片** | Glide 4.16 |
| **构建** | Gradle KTS, Version Catalog |

## 项目结构

```
app/src/main/java/com/example/musicgym/
├── MainActivity.java          # 5-Tab 主框架
│
├── StatsFragment.java         # 📊 统计 (MVVM)
├── StatsViewModel.java        #     → ViewModel
├── StatsRepository.java       #     → Repository
│
├── GymFragment.java           # 🏃 运动入口
├── WorkoutActivity.java       #     → GPS 追踪
├── WorkoutSummaryActivity.java#     → 运动总结
│
├── StrengthActivity.java      # 🏋️ 力量训练动作库
├── StrengthWorkoutActivity.java#    → 训练记录
├── ExercisePageAdapter.java   #     → ViewPager 适配器
│
├── MusicFragment.java         # 🎵 音乐播放器
├── ProfileFragment.java       # 👤 个人中心
├── ShareFragment.java         # 📝 社区分享
│
├── ColorTokens.java           # 🔧 统一颜色常量
├── UiUtils.java               # 🔧 dp() / WRAP / MATCH
│
├── AppDatabase.java           # Room 数据库 (6 entities, 3 migrations)
├── WorkoutRecord.java / Dao   #     → 有氧记录
├── StrengthRecord.java / Dao  #     → 力量记录
├── WeightRecord.java / Dao    #     → 体重记录
├── BodyMeasurement.java / Dao #     → 围度记录
├── WorkoutTemplate.java / Dao #     → 训练模板
│
└── WorkoutHistoryAdapter.java # RecyclerView 适配器 (有氧+力量)
```

## 构建运行

```bash
# 1. 配置高德地图 API Key
#    在 local.properties 中添加:
#    AMAP_API_KEY=你的Key

# 2. 编译
./gradlew assembleDebug

# 3. 安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 版本历史

| 版本 | 内容 |
|------|------|
| v1.1 | MusicFragment 基础播放 |
| v1.2 | GPS 定位 + 高德地图 |
| v1.3 | 力量训练模块 |
| v1.4 | Stats 折线图 + 日历热力图 |
| v1.5 | 社区分享 + 围度记录 |
| v2.0 | 12项功能增强 + 提拉面板 + 动作库 |
| **v2.0-refactored** | **MVVM 架构 + 颜色统一 + 样式抽取 + API Key 安全** |

---

*2026 | 独立开发者作品*
