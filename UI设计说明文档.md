# MusicGym v2.0 — 全部界面 UI 设计说明文档

---

## 一、整体架构

| 层级 | 说明 |
|------|------|
| **主框架** | `MainActivity` + `BottomNavigationView` 承载 5 个 Tab（音乐 / GYM / 统计 / 我的 / 分享） |
| **全局配色** | 深色主题，背景 `#0f172a`（深蓝黑）、卡片 `#1e293b`（深灰蓝）、强调色 `#FC4C02`（橙红，Strava 风格） |
| **导航方式** | Fragment 切换（Tab 内） + Intent 跳转 Activity（独立功能页） |

### 配色速查

| 用途 | 色值 | 说明 |
|------|------|------|
| 主背景 | `#0f172a` | Slate 900 深蓝黑 |
| 卡片/顶栏背景 | `#1e293b` | Slate 800 深灰蓝 |
| 输入框背景 | `#334155` | Slate 700 |
| 主强调色 | `#FC4C02` | 橙红，选中态、主按钮 |
| 成功/开始 | `#22c55e` / `#10b981` | 绿色，播放按钮、GPS 轨迹线 |
| 警告/PR | `#f59e0b` | 琥珀黄，PR 标签、卡路里 |
| 信息/次要 | `#38bdf8` | 天蓝，配速、Pro Member |
| 危险/删除 | `#ef4444` | 红色，删除按钮、倒计时 ≤10s |
| 辅文字 1 | `#9ca3af` | 灰色副标题 |
| 辅文字 2 | `#6b7280` | 深灰标签 |
| 辅文字 3 | `#94a3b8` | 浅灰白文字 |
| 弱分割线 | `#334155` | 分割线 |

### 字号规范

| 用途 | 大小 | 样式 |
|------|------|------|
| 页面大标题 | 22-26sp | 粗体 |
| 区域标题/卡片标题 | 18-20sp | 粗体 |
| 数据数值 | 20-36sp | 粗体 |
| 正文内容 | 13-16sp | 常规 |
| 标签/辅助文字 | 10-12sp | 常规 |
| 按钮文字 | 15-18sp | 粗体 |

### 组件规范

| 组件 | 规范 |
|------|------|
| 卡片圆角 | 10-16dp |
| 按钮高度 | 48-60dp |
| 图标区域 | 56dp 圆角方块 + emoji |
| 顶部栏高度 | 44-56dp |
| 底部操作栏 | 52-60dp |
| 标签芯片高度 | 36dp |

---

## 二、各界面逐一详解

### 1. 音乐 Tab — MusicFragment

**文件**: `fragment_music.xml` + `MusicFragment.java`

**布局结构:**

```
┌─────────────────────────────────┐
│  Top Bar: "Music Player"        │  🔀 随机  🔄 扫描
├─────────────────────────────────┤
│                                 │
│        ◯ 180dp 圆形专辑封面      │  ← CardView + ImageView
│                                 │     圆角90dp, 阴影12dp
│                                 │     默认 mipmap/ic_launcher
│                                 │
│          歌曲名称 (20sp 粗体)     │
│          歌手名 (13sp 灰色)       │     灰色 #9ca3af
│                                 │
│     00:00 ═══════●═══════ 00:00 │  ← SeekBar (红色 #f43f5e)
│                                 │     progressTint + thumbTint
│                                 │
│        ⏮        ▶         ⏭    │  ← 三按钮控制栏
│                                 │     ImageButton + tint 着色
│                                 │     播放=绿色 #10b981
│                                 │     前后=白色, 缩放1.6x
│                                 │
├─────────────────────────────────┤
│  ══════ 拖动把手(36dp 药丸) ══════│  ← drag_handle_pill.xml
│  播放列表               0 首     │     半透明灰 #556b7280
│  ┌─────────────────────────────┐│     圆角3dp, 宽36dp×高5dp
│  │ 1  歌曲名称           ✕     ││
│  │    歌手名                    ││  ← 动态生成行
│  │ ▶  歌曲名称(当前)     ✕     ││     当前播放=橙色背景 #1aFC4C02
│  │    歌手名                    ││     ▶ 图标 + 歌曲名 + ✕ 删除
│  │ 3  歌曲名称           ✕     ││
│  └─────────────────────────────┘│
└─────────────────────────────────┘
```

**关键 UI 细节：**

- **提拉面板机制**：
  - `View.OnTouchListener` 监听拖拽，10px 水平/5px 垂直阈值区分点击与拖拽
  - 折叠态 `COLLAPSED_DP = 48dp`，展开态 = 屏幕高度的 55%
  - `ValueAnimator` + `DecelerateInterpolator(2.5f)` 平滑过渡 300ms
  - 拖拽到中点以上→展开，以下→折叠；点击直接切换
  - 按下把手时 alpha 降至 0.7，释放恢复

- **列表行布局（buildPlaylistUI）**：
  - 水平 LinearLayout，padding 12,10,8,10
  - 序号/▶ 图标（32dp 宽，当前播放时橙色 + ▶）
  - 歌曲信息区（权重1）：title 白色 14sp + artist 灰色 11sp，singLine
  - ✕ 删除按钮（红色 `#ef4444`，14sp，padding 12,8）

- **删除逻辑**：
  - 删除当前播放 → 释放 MediaPlayer，清空状态
  - 删除前面的索引 → `currentTrackIndex--`
  - 重新调用 `buildPlaylistUI()` 刷新

- **封面动画**：
  - `ObjectAnimator` 旋转 0→360°，8000ms 周期，`INFINITE` 重复
  - `LinearInterpolator` 匀速旋转
  - 暂停时 `recordAnimator.pause()`，播放时 `resume()` 或 `start()`

- **双路径扫描**：
  - MediaStore 查询 + 文件系统直接扫描（Download / Music / sdcard）
  - `LinkedHashSet<String>` 按路径去重
  - 异步 ExecutorService → UI 线程

---

### 2. GYM Tab — GymFragment

**文件**: `fragment_gym.xml` + `GymFragment.java`

**布局结构:**

```
┌─────────────────────────────────┐
│         选择运动类型               │  22sp 粗体白色
│      开始记录你的每一次运动         │  14sp 灰色 #6b7280
│                                 │
│  ┌─────────────────────────────┐│
│  │ 🏃  跑步                    ││  ← CardView, 16dp圆角
│  │     GPS 追踪·配速记录·卡路里  ││     56dp 图标 + 标题 + 描述 + ›
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │ 🚴  骑行                    ││     图标 GradientDrawable:
│  │     速度追踪·路线记录·卡路里  ││       跑步=绿色 gym_card_icon_bg_run
│  └─────────────────────────────┘│       骑行=蓝色 gym_card_icon_bg_cycle
│  ┌─────────────────────────────┐│       步行=青色 gym_card_icon_bg_walk
│  │ 🚶  步行                    ││       力量=橙色 gym_card_icon_bg_strength
│  │     步数估算·路线记录·卡路里  ││
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │ 🏋️  力量训练                 ││     副标题: "50+ 动作库·部位细分"
│  │     50+ 动作库·部位细分·组数  ││
│  └─────────────────────────────┘│
└─────────────────────────────────┘
```

**关键 UI 细节：**

- **卡片设计**：4 张 `CardView`，16dp 圆角，`#1e293b` 背景，`selectableItemBackground` 前景水波纹
- **图标区域**：56dp × 56dp，4 种不同颜色的 `GradientDrawable`（对角渐变），emoji 居中 28sp
- **内容区**：横向布局，图标 → 标题+描述（权重1）→ › 箭头
- **标题**：18sp 粗体白色；描述：12sp 灰色 `#9ca3af`
- **导航**：前三种 → `WorkoutActivity`（GPS 追踪）；力量训练 → `StrengthActivity`（动作库）

---

### 3. 统计 Tab — StatsFragment

**文件**: `fragment_stats.xml` + `StatsFragment.java` + MPAndroidChart

**布局结构（可垂直滚动）:**

```
┌─────────────────────────────────┐
│  May 2026              vs Apr   │  标题 24sp 白色粗体
├─────────────────────────────────┤     对比 14sp 灰色
│  [All] [Run] [Ride] [Walk] [Strength] │
│   ↑ 选中=橙红实心  ↑ 未选中=灰边框  │  ← 过滤芯片 (36dp高, padding 16dp)
│                                       stats_filter_selected: #FC4C02 圆角
│                                       stats_filter_unselected: #334155 边框
├─────────────────────────────────┤
│  日 一 二 三 四 五 六            │
│  ■■■■■■■                        │  ← 日历热力图 (CardView #1e293b)
│  ■■■■■■■                        │     星期头: 灰色 10sp
│  ■■■■■■■                        │     单元格: 38dp 高, 宽度=屏幕/7.5
│  ...                             │     绿色=有氧, 橙色=力量
│                                  │     红色=两者都有, 灰色=无数据
├─────────────────────────────────┤
│  ╱╲   ╱╲                        │
│ ╱  ╲╱  ╲    实线=当月           │  ← LineChart (260dp 高)
│╱          ╲  虚线=上月           │     CUBIC_BEZIER 平滑曲线
│─────────────                    │     背景 #1e293b
│                                  │     网格线 #334155 0.5f
│                                  │     Y轴左, X轴下, 无右轴
│                                  │     图例: 右上角水平排列
├─────────────────────────────────┤
│ ┌──────┬──────┬──────┬──────┐   │
│ │ 0.0  │ 0h 0m│  0   │  0   │   │  ← 摘要数据卡片行 (4列)
│ │  km  │duration│workouts│kcal│   │     数值 20sp 粗体白色
│ └──────┴──────┴──────┴──────┘   │     标签 11sp 灰色
├─────────────────────────────────┤
│  HISTORY                         │  ← 分区标题 11sp 灰色大写 letterSpacing
│  ┌─────────────────────────────┐│
│  │ 跑步          2026-04-30    ││  ← item_workout_history.xml
│  │  0.00KM  00:00TIME  0KCAL  ││     CardView 12dp 圆角 2dp 阴影
│  └─────────────────────────────┘│     运动名 16sp 绿色粗体
│  ┌─────────────────────────────┐│     日期 13sp 灰色
│  │ ...更多历史记录...           ││     三个指标 18sp 粗体 + 10sp 标签
│  └─────────────────────────────┘│
└─────────────────────────────────┘
```

**关键 UI 细节：**

- **过滤芯片**：5 个 TextView 作为按钮。点击切换背景 drawable（选中 = 橙色实心白字，未选中 = 灰边框灰字）
- **折线图（MPAndroidChart）**：
  - 当月数据：实线 3dp 宽 + 圆点 4dp 半径 + 填充 30% 透明度 + CUBIC_BEZIER
  - 上月数据：虚线 `DashPathEffect(10f, 6f)` 2dp 宽 + 圆点 3dp + 无填充 + 灰色 `#9ca3af`
  - 力量模式：切换为训练量 (kg) 折线图，摘要卡片标签变为 "sessions"
  - X 轴：`granularity=1f`，`labelCount=7`；Y 轴：`axisMinimum=0f`，上限 = max×1.2
  - 动画 `animateX(600)` 600ms 从左到右展开

- **日历热力图（buildCalendar）**：
  - 完全动态构建，遍历当前月每一天
  - 首行星期头（日一二三四五六），后续行填充日期
  - 月初补空白单元格（`firstDow - 1` 个空 View）
  - 数据来源：同时查 `WorkoutRecord`（有氧）和 `StrengthRecord`（力量）

- **自适应摘要卡片**：有氧模式显示 km / duration / workouts / kcal；力量模式显示 sessions / duration / workouts / —

- **历史列表**：`RecyclerView` + `LinearLayoutManager`，有氧→`WorkoutHistoryAdapter`，力量→`StrengthHistoryAdapter`

- **Mock 数据**：`seedMockData()` 使用 `Random(42)` 确定性生成，确保每次演示一致

---

### 4. 我的 Tab — ProfileFragment

**文件**: `fragment_profile.xml` + `ProfileFragment.java`

**布局结构（可滚动）:**

```
┌─────────────────────────────────┐
│           ◯ 头像 100dp          │  圆形 CardView (#1e293b)
│                                 │  cornerRadius 50dp
│         Sarah Connor            │  24sp 粗体白色
│         Pro Member              │  14sp 蓝色 #38bdf8
│                                 │
│      72              175        │  体重(kg) / 身高(cm)
│   Weight (kg)    Height (cm)    │  数值 28sp 粗体白色
│                                 │  标签 12sp 灰色
│                                 │
│  ┌─────────────────────────────┐│
│  │         编辑资料             ││  ← 50dp 高 Button
│  │         身体围度             ││     背景 #1e293b
│  │         导出数据 CSV         ││     白色文字
│  │         训练提醒             ││     间距 8dp
│  └─────────────────────────────┘│
│                                 │
│  身体围度                        │  ← 16sp 粗体白色分区标题
│  胸围: 90.0 cm                  │  ← 动态加载最新围度
│  腰围: 80.0 cm                  │     蓝色 #38bdf8, 13sp
│  臀围: 95.0 cm                  │
│  臂围: 35.0 cm                  │
│  腿围: 55.0 cm                  │
│                                 │
│  体重记录                        │  ← 16sp 粗体白色分区标题
│  • Jan 15 : 72.0 kg            │  ← 动态加载历史体重
│  • Jan 10 : 71.5 kg            │     绿色 #34d399, 14sp
│  ...                            │     间距 10dp
└─────────────────────────────────┘
```

**关键 UI 细节：**

- **编辑资料弹窗**：`AlertDialog` + 动态 `LinearLayout`（3 个 EditText：姓名/体重/身高）。修改体重时自动写入 `WeightRecord` 到数据库
- **身体围度弹窗**：6 个数值输入（体重/胸/腰/臀/臂/腿），包裹在 `ScrollView` 中。输入类型 `TYPE_NUMBER_FLAG_DECIMAL`，默认值从上一次记录读取
- **训练提醒**：`AlertDialog` 单选列表 5 项（每天/周一三五/周二四六/周末/关闭）。通过 `AlarmManager.setRepeating(RTC_WAKEUP)` 在 18:00 触发
- **CSV 导出**：后台线程生成到 `Downloads/MusicGym_export_yyyyMMdd.csv`，包含有氧和力量两种记录
- **SharedPreferences**：存储用户名/体重/身高

---

### 5. 分享 Tab — ShareFragment

**文件**: `fragment_share.xml` + `ShareFragment.java`

**布局结构:**

```
┌─────────────────────────────────┐
│  COMMUNITY FEED                  │  24sp 青色 #38bdf8 粗体
│                                  │  letterSpacing 0.1
│  ┌──────────┐ ┌──────────┐      │
│  │  [图片]  │ │  [图片]  │      │  ← item_blog_post.xml
│  │  标题    │ │  标题    │      │     StaggeredGrid 2列瀑布流
│  │  作者 日期│ │  作者 日期│      │     CardView 12dp圆角 4dp阴影
│  └──────────┘ └──────────┘      │     图片可隐藏(visibility gone)
│  ┌──────────┐ ┌──────────┐      │     标题 14sp 白色粗体 maxLines=2
│  │  [图片]  │ │  [图片]  │      │     作者 11sp 灰色 + 日期 10sp
│  │  标题    │ │  标题    │      │
│  │  作者 日期│ │  作者 日期│      │
│  └──────────┘ └──────────┘      │
│                            [+ ] │  ← FAB 悬浮按钮
└─────────────────────────────────┘    红色 #f43f5e, 白色+号
                                       底部右侧 24dp margin
                                       6dp elevation, 0dp border
```

**关键 UI 细节：**

- **瀑布流**：`StaggeredGridLayoutManager(2, VERTICAL)`，`GAP_HANDLING_NONE`
- **卡片**（`item_blog_post.xml`）：`ImageView`（可选）+ 12dp padding 内容区。标题 ellipsize=end。作者 11sp 灰色 + 日期 10sp 深灰
- **FAB**：`FloatingActionButton`，红色背景 `#f43f5e`，白色 + 号图标，6dp 阴影
- **首次启动**：自动插入 4 条种子数据（R.drawable.pic1-pic4 转 URI）

---

### 6. 帖子详情 — PostDetailActivity

**文件**: `activity_post_detail.xml` + `PostDetailActivity.java`

```
┌─────────────────────────────────┐
│  ┌─────────────────────────────┐│
│  │                             ││
│  │       图片 380dp            ││  ← ImageView centerCrop
│  │                             ││
│  │  [← 返回按钮]               ││  ← ImageButton 48dp, tint=白色
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │  卡片 (marginTop -40dp)     ││  ← CardView 30dp 顶圆角 10dp 阴影
│  │                             ││     叠加效果 (覆盖图片底部)
│  │   标题 26sp 青色粗体         ││
│  │   作者(红) | 日期(灰) 14sp  ││     作者 #f43f5e 粗体
│  │   ─── 分割线 ───            ││     分割线 #3338bdf8 半透明蓝
│  │   正文 16sp 浅白            ││     lineSpacingExtra 10dp
│  │                             ││     #f8fafc
│  └─────────────────────────────┘│
└─────────────────────────────────┘
```

---

### 7. 发布动态 — CreatePostActivity

**文件**: `activity_create_post.xml` + `CreatePostActivity.java`

```
┌─────────────────────────────────┐
│  UPLOAD NEW DATA                 │  22sp 青色粗体
│                                  │
│  ┌──────────────────────────────┐│
│  │     图片预览区 200dp         ││  ← ImageView #1e293b 背景
│  └──────────────────────────────┘│     centerCrop
│  [  CAMERA  ]    [  GALLERY  ]   │  ← 红 #f43f5e / 紫 #8b5cf6
│                                  │     Button 各权重1
│  Transmission Title              │  ← EditText 青色边框
│                                  │     hint 灰色 #64748b
│  Enter log details...            │  ← EditText 填充剩余空间
│                                  │     gravity=top
│  ┌──────────────────────────────┐│
│  │       INITIATE UPLOAD        ││  ← 60dp 高绿色按钮
│  └──────────────────────────────┘│     18sp 粗体 #10b981
└─────────────────────────────────┘
```

---

### 8. GPS 运动追踪 — WorkoutActivity

**文件**: `activity_workout.xml` + `WorkoutActivity.java`

```
┌─────────────────────────────────┐
│  ✕  跑步                00:00   │  半透明黑顶栏 #cc0f172a (56dp)
│                                 │     ✕ 关闭 → 暂停 / finish()
│                                 │     绿色计时器 #22c55e 22sp 粗体
├─────────────────────────────────┤
│                                 │
│                                 │
│         高德地图全屏             │  ← MapView (全屏)
│         绿色轨迹线               │     Polyline 18f 宽 #22c55e
│         蓝色定位点               │     MyLocationStyle:
│         (街道级 18f 缩放)        │       LOCATION_TYPE_LOCATION_ROTATE
│                                 │       蓝色描边 + 淡蓝填充
│                                 │       禁用内置定位按钮
│                                 │
│                                 │
├─────────────────────────────────┤
│   0.00        0       0'00''    │  底部仪表盘 (距底部100dp)
│   公里       千卡      配速      │  白色 / 橙色 #f59e0b / 蓝色 #38bdf8
│                                  │  28sp粗体 + 11sp标签
│                                  │
│             ⏺ GO                │  ← 80dp × 80dp 圆形按钮
│                                  │     workout_btn_start: 绿色圆形
│                                  │     workout_btn_stop: 红色圆形
│                                  │     文字 GO / ⏸ / ▶
└─────────────────────────────────┘

┌─ 暂停浮层 (#cc0f172a 半透明黑) ─┐
│                                 │
│              ⏸                  │  64sp emoji
│          运动已暂停              │  22sp 粗体白色
│                                 │
│     [  继续  ]  [  结束  ]      │  继续=绿 #22c55e 100dp
│                                 │  结束=红 #ef4444 100dp
│                                  │  48dp高 16sp粗体
└─────────────────────────────────┘
```

**关键 UI 细节：**

- **地图定位**：
  - `AMapLocationClient` 高精度模式
  - `MyLocationStyle`：`LOCATION_TYPE_LOCATION_ROTATE`（跟随方向旋转）
  - 首次单次定位 → 动画移动到用户位置 `CameraUpdateFactory.newLatLngZoom(latlng, 18f)`
  - 追踪中每 2 秒定位一次

- **轨迹线**：
  - 绿色 `#22c55e` 18f 宽度
  - 每次位置更新移除旧线 `trajectoryLine.remove()` 重新绘制
  - 防漂移过滤：1m < 偏移 < 100m 才计入距离

- **仪表盘实时更新**：
  - 距离（公里，保留 2 位小数）
  - 卡路里：`距离 × 70 × 运动系数`（跑步 1.036 / 骑行 0.4 / 步行 0.7）
  - 配速：1000 / 速度(m/s)，格式 `X'XX''`

- **开始/暂停按钮**：
  - `workout_btn_start.xml`：绿色 `GradientDrawable` 圆形
  - `workout_btn_stop.xml`：红色 `GradientDrawable` 圆形
  - GO → 点击开始追踪（定位权限检查），变为 ⏸
  - ⏸ → 暂停追踪，显示浮层，按钮变 ▶
  - 暂停浮层：继续（绿色）或结束（红色，保存记录 + 跳转总结页）

- **前后台生命周期**：`onResume/onPause/onDestroy` 中同步 MapView 和 LocationClient 状态

---

### 9. 运动总结 — WorkoutSummaryActivity

**文件**: `activity_workout_summary.xml` + `WorkoutSummaryActivity.java`

```
┌─────────────────────────────────┐
│  跑步 完成 ✓                     │  26sp 粗体白色
│  2026-05-06                      │  14sp 灰色 #9ca3af
│                                 │
│    0.00      00:00       0      │  三列数据
│    公里       时长      千卡     │  数值 36sp 粗体 (绿/白/橙)
│                                  │  标签 13sp 灰色 #94a3b8
│                                  │
│  平均配速              0'00''   │  左 14sp 灰, 右 18sp 白粗体
│  ─────────────────────────────  │  分割线 #334155 1dp
│                                 │
│  ┌─────────────────────────────┐│
│  │            完成              ││  ← 48dp 绿色 #22c55e 按钮
│  └─────────────────────────────┘│     16sp 粗体白色
└─────────────────────────────────┘     点击 finish()
```

---

### 10. 力量训练动作库 — StrengthActivity

**文件**: `activity_strength.xml` + `StrengthActivity.java`

```
┌─────────────────────────────────┐
│  ✕  力量训练            已选 0 项│  深灰 #1e293b 顶栏 (56dp)
│                                 │     ✕ 关闭按钮 + 橙色 #FC4C02 选中计数
├─────────────────────────────────┤
│  ← [胸部][背部][腿部][肩部] →   │  HorizontalScrollView (44dp)
│      [手臂][核心][全身]          │  每个标签 13sp padding 28×10
│                                  │  选中=#FC4C02 实心粗体白字
│                                  │  未选中=透明灰字
├─────────────────────────────────┤
│  🔍 搜索动作...             📋  │  搜索栏 (40dp)
│                                  │  EditText: #334155 背景, 13sp
│                                  │  📋 模板按钮: 36dp×32dp
├─────────────────────────────────┤
│  自定义                          │  ← 黄色 #f59e0b 12sp 子标题
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ 🎯   │ │ 🎯   │ │ 🎯   │    │  ← 3列卡片 (每卡140dp高)
│  │动作名 │ │动作名 │ │动作名 │    │     背景 #1e293b
│  └──────┘ └──────┘ └──────┘    │     顶部 GradientDrawable 对角渐变
│  ┌──────┐ ┌──────┐ ┌──────┐    │     emoji 28sp 居中
│  │ ...  │ │ ...  │ │ ...  │    │     名称 11sp (超4字截断+"..")
│  └──────┘ └──────┘ └──────┘    │     选中=右上角 ✓ 圆标 (22dp)
│  上胸 (子分组标题)               │     点击→动作详情弹窗
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ 🏋️  │ │ 🏋️  │ │ 🏋️  │    │
│  │上斜..│ │上斜..│ │上斜..│    │  ← 名称超长截断为4字
│  └──────┘ └──────┘ └──────┘    │
│  中胸                            │
│  (更多 3列卡片...)               │
├─────────────────────────────────┤
│  0 个动作已选       [开始训练]   │  底部操作栏 60dp #1e293b
└─────────────────────────────────┘     按钮: #FC4C02 40dp 高 padding 28dp
                                        未选时 alpha=0.4
```

**关键 UI 细节：**

- **7 大肌群 + 17 子分组 + 50+ 动作**：
  - 胸部 `#FC4C02`（上胸/中胸/下胸）
  - 背部 `#38bdf8`（宽度/厚度/下背）
  - 腿部 `#34d399`（股四头肌/腘绳肌/臀部/小腿）
  - 肩部 `#f59e0b`（前束/中束/后束）
  - 手臂 `#a78bfa`（肱二头肌/肱三头肌/前臂）
  - 核心 `#ef4444`（上腹/下腹/侧腹/稳定）
  - 全身 `#fb923c`（爆发力/综合）

- **动作卡片**（`buildExerciseCard`）：
  - `FrameLayout` 包裹，140dp 高，权重 1（3 列等宽）
  - 上半部分：`GradientDrawable` TL_BR 对角渐变（accent 色 120/40 alpha），emoji 居中
  - 下半部分：动作名 11sp，居中对齐
  - 选中标记：右上角 22dp 橙色圆形 + ✓ 白色 10sp

- **动作详情弹窗**（`showExerciseDetail`）：
  - `AlertDialog` + `ScrollView`
  - 200dp 演示区：对角渐变 + emoji + 动作名
  - 22sp 粗体标题 + 目标肌群描述（如"胸部 · 上胸"）
  - 标准动作描述文本 14sp 浅色 #cbd5e1
  - 按钮：动态文字"添加动作"/"移除动作"

- **搜索功能**：`TextWatcher` 实时过滤 50+ 动作名 + 自定义动作。结果 3 列展示。无匹配时显示"点击添加自定义动作"黄色提示

- **模板系统**：
  - 保存：`AlertDialog` 输入模板名 → JSON 序列化 → Room 插入
  - 加载：列表选择 → JSON 反序列化 → 填充 `selectedExercises`
  - 添加自定义动作：输入名称 → 加入 `customExercises` 列表
  - 删除所有模板：确认后逐条 `deleteById`

- **底部栏**：15sp 计数白字 + 40dp 橙色开始按钮。未选动作时按钮 alpha 0.4 禁用

---

### 11. 力量训练记录 — StrengthWorkoutActivity

**文件**: `activity_strength_workout.xml` + `StrengthWorkoutActivity.java` + `ExercisePageAdapter.java`

```
┌─────────────────────────────────┐
│  ✕  训练记录          ◉  00:00  │  顶栏 56dp #1e293b
│                                 │     ✕ 返回 → 放弃确认 (AlertDialog)
│                                 │     ◉ 模式切换 (列表 / 滑动)
│                                 │     计时器 #f59e0b 橙色 20sp
├─────────────────────────────────┤
│  ┌─────────────────────────────┐│
│  │ 1 杠铃卧推  PR 100kg   80kg││  ← 动作卡片 (#1e293b)
│  │ 组  重量(kg)  次数  %1RM   ││     序号 #FC4C02 粗体 16sp
│  │────────────────────────────││     动作名 白色 粗体 16sp
│  │ 1  [  60  ] [ 10 ] 60%  − ││     PR #fbbf24 黄色 11sp
│  │ 2  [  80  ] [  5 ] 80%  − ││     容量 #f59e0b 橙色 13sp
│  │                             ││
│  │ 表头: 组(26dp) 重量(权重1)  ││  ← 表头 10sp 灰色 #6b7280
│  │       次数(权重1) %1RM −    ││
│  │                             ││
│  │ 组行: 序号白13sp|EditText|  ││  ← EditText: #334155 背景
│  │       EditText|%显示|−删除  ││     TYPE_NUMBER_FLAG_DECIMAL
│  │                             ││     失焦自动保存 + 刷新显示
│  │ [50%] [65%] [75%] [85%] [95%]││  ← 1RM 快捷按钮
│  │                             ││     蓝色 #38bdf8 11sp
│  │ + 添加组      ⏱ 组间休息    ││     #334155 背景 padding 12×6
│  └─────────────────────────────┘│     2.5kg 取整
│  ┌─────────────────────────────┐│
│  │ 2 哑铃飞鸟           PR... ││  ← 下一个动作卡片
│  │ ... (同上结构)              ││     (同上结构)
│  └─────────────────────────────┘│
│                                 │
│  ═══════════════════════════    │
│  ⏱                              │  ← 组间休息浮层 (半透明黑遮罩)
│     休息 01:30                  │     #cc0f172a 全屏
│     点击跳过                     │     橙色计时器 32sp 粗体
│  ═══════════════════════════    │     ≤10秒 文字变红 #ef4444
│                                  │     点击跳过 / 倒计时结束自动隐藏
├─────────────────────────────────┤
│  ┌─────────────────────────────┐│
│  │         保存训练             ││  ← 底部保存按钮
│  └─────────────────────────────┘│     52dp #FC4C02
└─────────────────────────────────┘     16sp 粗体白色
                                        16dp margin
```

**关键 UI 细节：**

- **双模式切换**：
  - 列表模式（默认）：`ScrollView` + 动态构建卡片（`buildUI()`）
  - 滑动模式：`ViewPager2` + `ExercisePageAdapter`，每个动作独立一页
  - 切换按钮：◉（列表）/ ☰（滑动）

- **PR 系统**：
  - Epley 公式：`weight × (1 + reps / 30)`
  - 从所有历史 `StrengthRecord` 中遍历计算每个动作的最大 1RM 估算值
  - 显示为黄色 `#fbbf24` 标签 "PR XXkg"

- **1RM 百分比快捷填充**：
  - 5 个蓝色按钮 [50%] [65%] [75%] [85%] [95%]
  - 点击后：`PR × 百分比 / 100`，2.5kg 取整
  - 所有组统一填充该重量 + 刷新 EditText

- **组行编辑**：
  - 重量 EditText：`TYPE_NUMBER_FLAG_DECIMAL`（支持小数）
  - 次数 EditText：`TYPE_CLASS_NUMBER`（整数）
  - `OnFocusChangeListener`：失焦时自动保存到 `workoutData` + 调用 `updateCardDisplay` 刷新容量和 %1RM
  - 删除按钮 "−"（红色 18sp）：至少保留 1 组（`sets.size() > 1`）

- **组间休息**：
  - 90 秒倒计时，`restOverlay` 浮层覆盖全屏
  - 剩余 ≤10 秒时文字变红 `#ef4444`
  - 同时发送 Android `Notification`（`NotificationChannel "strength_rest"`）
  - 点击计时器文字跳过休息

- **放弃确认**：返回时检查 `workoutData` 非空 → `AlertDialog` 确认

- **保存**：JSON 序列化所有动作×组数据 → `StrengthRecord` → Room 插入

---

## 三、全局设计规范速查

```
配色:
  背景  #0f172a    卡片  #1e293b    输入框  #334155
  强调  #FC4C02    成功  #22c55e    警告  #f59e0b
  信息  #38bdf8    危险  #ef4444    紫色  #a78bfa
  灰1   #9ca3af    灰2   #6b7280    灰3   #94a3b8

字号:
  页面标题  22-26sp bold    数据数值  20-36sp bold
  卡片标题  18-20sp bold    正文      13-16sp
  标签      10-12sp         按钮      15-18sp bold

圆角:
  卡片  10-16dp    按钮  按需    头像  50dp (圆形)

尺寸:
  顶栏    44-56dp    底栏    52-60dp
  按钮高  48-60dp    图标区  56dp×56dp
  标签高  36dp       FAB     默认
```

---

## 四、动态 UI 构建清单

| 界面 | 动态构建内容 | 构建方法 |
|------|-------------|----------|
| MusicFragment | 播放列表行（序号+歌曲+删除） | `buildPlaylistUI()` 遍历 playlist |
| StatsFragment | 日历热力图（星期头+日期格） | `buildCalendar()` 遍历当月天数 |
| StatsFragment | 折线图数据（当月/上月对比） | `refreshCardioChart()` / `refreshStrengthChart()` |
| ProfileFragment | 体重历史列表 | `loadHistoryUI()` 遍历 WeightRecord |
| ProfileFragment | 围度数据列表 | `loadMeasurementsUI()` 读取最新 BodyMeasurement |
| StrengthActivity | 部位标签栏 | `buildTabs()` 遍历 GROUPS |
| StrengthActivity | 动作 3 列卡片网格 | `showGroup()` / `showSearchResults()` 遍历 SubGroups |
| StrengthActivity | 动作详情弹窗 | `showExerciseDetail()` 动态 AlertDialog |
| StrengthWorkoutActivity | 动作卡片（表头+组行+快捷栏） | `buildUI()` 遍历 workoutData |
| StrengthWorkoutActivity | PR 数据缓存 | `loadPRData()` 遍历历史记录计算 Epley |

---

## 五、交互模式汇总

| 模式 | 所在界面 | 实现方式 |
|------|---------|---------|
| 提拉拖拽面板 | Music | `OnTouchListener` + `ValueAnimator` + 10px 阈值 |
| 芯片过滤切换 | Stats | 5 个 TextView + `setOnClickListener` + drawable 切换 |
| 瀑布流列表 | Share | `StaggeredGridLayoutManager(2, VERTICAL)` |
| 标签页水平滚动 | Strength | `HorizontalScrollView` + 动态 TextView 标签 |
| 3 列卡片网格 | Strength | 动态 `LinearLayout` 行内 3 个 `FrameLayout` 卡片 |
| 列表/滑动双模式 | StrengthWorkout | `ScrollView` ↔ `ViewPager2` 一键切换 |
| 浮层遮罩 | Workout / StrengthWorkout | `FrameLayout` 全屏半透明 + 居中内容 |
| 百分比快捷填充 | StrengthWorkout | 按钮计算 `PR × pct%` 填充所有组 |
| 失焦自动保存 | StrengthWorkout | `EditText.OnFocusChangeListener` |
| 倒计时 + 系统通知 | StrengthWorkout | `Handler.postDelayed` + `NotificationCompat` |

---

*文档生成日期: 2026-06-01 | MusicGym v2.0 (versionCode 7)*
