# MusicGym v5.2 阶段性总结

> 日期：2026-06-07 | versionCode: 37 | versionName: 5.2

---

## 一、项目概览

MusicGym 是一款面向健身爱好者的 Android 运动追踪 App，核心差异化定位是**运动与音乐深度融合**——根据步频实时匹配 BPM，让用户跑步时无需操作手机。

| 指标 | 数值 |
|------|------|
| Java 文件 | 50 个 |
| XML 布局 | 22 个 |
| Java 代码行 | ~7,500 |
| XML 代码行 | ~2,850 |
| 编译 SDK | 36 (Android 16 Preview) |
| 最低 SDK | 34 (Android 14) |
| APK 体积 | Debug 78MB / Release 45MB |

---

## 二、功能模块矩阵

### 🎵 音乐模块 (MusicFragment + MusicService)
| 功能 | 状态 |
|------|------|
| 本地 MP3 扫描（MediaStore + 文件系统） | ✅ |
| 播放/暂停/上下曲 + SeekBar | ✅ |
| 3 种播放模式（顺序/单曲/随机） | ✅ |
| 红心收藏（SharedPreferences） | ✅ |
| 5 段均衡器（跑步/举铁/拉伸/人声/自定义） | ✅ |
| 前台服务 + 通知栏播放控制 | ✅ |
| AudioFocus 音频焦点管理 | ✅ |
| 后台线程扫描 | ✅ |
| **BPM 步频匹配**（加速度传感器 → 自动切歌） | ✅ |
| Spotify SDK 集成 | ❌ 待 v2.5 |
| 歌词显示 | ❌ 待 v2.5 |
| MediaSession 蓝牙/线控 | ❌ 兼容性问题待调试 |

### 🏋️ 力量训练 (StrengthActivity + StrengthWorkoutActivity)
| 功能 | 状态 |
|------|------|
| 7 大肌群 × 17 子群 × 50+ 动作 | ✅ |
| 动作搜索 + 多选 | ✅ |
| 双模式训练（列表 / ViewPager2 滑动） | ✅ |
| 组数×重量×次数 + 热身标记 + RPE + 备注 | ✅ |
| %1RM 一键填充胶囊 | ✅ |
| 组间休息计时器（60/90/120/180s）+ 通知 | ✅ |
| 训练模板保存/加载 | ✅ |
| 渐进超负荷建议（上次训练数据回显） | ✅ |
| 肌肉恢复状态显示（🔴🟡🟢） | ✅ |
| 动作详情折线图（重量历史趋势） | ✅ |
| **🤖 DeepSeek AI 4 周计划生成** | ✅ |
| 动作演示 GIF | ❌ 待下载 |

### 🗺️ 运动追踪 (WorkoutActivity)
| 功能 | 状态 |
|------|------|
| GPS 实时追踪（高德 AMap SDK） | ✅ |
| 配速着色路线（4 色：蓝/绿/橙/红） | ✅ |
| 每公里自动分段 + 语音播报（TTS） | ✅ |
| 自动暂停（8s 无移动检测） | ✅ |
| 卡路里估算 | ✅ |
| 路线回放（动画 + AMap） | ✅ |
| 运动中迷你音乐控制条 | ✅ |
| 运动后快速再开始 | ✅ |

### 📊 数据统计 (StatsFragment)
| 功能 | 状态 |
|------|------|
| 日历热力图 | ✅ |
| MPAndroidChart 折线图 | ✅ |
| 周/月视图切换 | ✅ |
| 5 种运动过滤芯片（All/Run/Ride/Walk/Strength） | ✅ |
| 摘要卡片（距离/时长/次数/热量） | ✅ |
| 趋势箭头（本月 vs 上月 ↑↓%） | ✅ |
| 个人纪录墙（最快配速/最远距离/最大重量） | ✅ |
| 目标进度条（🎯 月目标完成度） | ✅ |
| 运动历史 RecyclerView | ✅ |
| HorizontalScrollView 防溢出 | ✅ v5.2 |

### 👤 个人中心 (ProfileFragment)
| 功能 | 状态 |
|------|------|
| 头像（拍照/相册） | ✅ |
| 昵称 + 体重 + 身高 | ✅ |
| 体重趋势折线图 | ✅ |
| 连续打卡天数 | ✅ |
| 身体围度记录 | ✅ |
| 数据 CSV 导出 | ✅ |
| 训练提醒（AlarmManager） | ✅ |
| 成就徽章系统（9 枚） | ✅ |
| 深色/浅色主题切换 | ✅ |

### 🏘️ 社区 (ShareFragment)
| 功能 | 状态 |
|------|------|
| Firebase Firestore 帖子列表（瀑布流） | ✅ |
| 发布帖子（拍照 + 文字） | ✅ |
| 点赞 + 评论 | ✅ |
| Firebase 匿名登录 | ✅ |
| 离线降级（"社区不可用"提示） | ✅ v5.2 |
| 关注系统 | ❌ |
| 挑战系统 | ❌ |

### ⚙️ 系统
| 功能 | 状态 |
|------|------|
| 引导页（3 页 Onboarding） | ✅ |
| 桌面 Widget（今日数据 + 一键启动） | ✅ |
| 隐私政策页 | ✅ |
| 通知权限运行时请求 | ✅ v5.2 |
| Release 签名 + ProGuard | ✅ |
| Database 迁移 v1→v4 | ✅ |

---

## 三、v5.2 会话成果

本会话完成了 **10 次提交，修复 20+ 个问题**，涵盖 6 个维度：

### 🔴 安全加固（3 项）
- 签名密码从 build.gradle 移到 local.properties
- `android:allowBackup = false`
- ProGuard 规则补全（内部类 keep）

### 🔴 崩溃修复（4 项）
- MusicViewModel `setValue` → `postValue`（后台线程调用崩溃）
- `POST_NOTIFICATIONS` 运行时权限请求（引导页 + 播放前）
- ProfileFragment `requireActivity()` detach 崩溃 → `safePost()`
- WorkoutActivity `aMap` null 后仍调用

### 🟡 资源泄漏（3 项）
- MusicService BroadcastReceiver 未注销
- StrengthActivity ExecutorService 未关闭
- MediaPlayer 异常时未 release

### 🟡 并发安全（3 项）
- MusicGymWidget 主线程访问 Room → 后台线程
- StatsViewModel ArrayList 非线程安全 → 防御性拷贝
- MusicViewModel favSet 多线程竞争 → synchronized

### 🟡 UI 修复（8 项）
- 5 项对齐/边距/间距统一
- 2 项触摸目标尺寸（42dp→48dp）
- 1 项 HorizontalScrollView 防溢出

### 🟡 文本溢出防护（8 项）
- XML：歌名/歌手/作者/运动类型 maxLines+ellipsize
- Java：歌单列表/训练动作 setSingleLine+setEllipsize
- 输入：发帖 maxLength

### 🟡 边界条件（2 项）
- AiPlanGenerator errorStream null NPE
- StatsViewModel 除零保护

---

## 四、已知未解决问题

| # | 问题 | 优先级 | 原因 |
|---|------|--------|------|
| 1 | MediaSession 蓝牙线控不响应 | 🟡 | MediaSessionCompat 在 真机崩溃，已回退，待调试 |
| 2 | 动作演示 GIF 缺失 | 🟡 | 需要从 MuscleWiki 下载 50+ GIF |
| 3 | 旋转屏幕音乐停止 | 🟢 | onDestroyView 释放 MediaPlayer，需架构改动 |
| 4 | 力量训练删除动作后数据丢失 | 🟢 | 无恢复机制 |
| 5 | 横屏适配不完全 | 🟢 | ROADMAP v2.11 |

---

## 五、技术架构

```
┌──────────────────────────────────────┐
│            MainActivity               │
│  ┌────┐ ┌────┐ ┌─────┐ ┌────┐ ┌────┐│
│  │音乐│ │GYM │ │统计  │ │个人│ │社区 ││
│  └────┘ └────┘ └─────┘ └────┘ └────┘│
└──────────────────────────────────────┘
         │          │          │
    ┌────▼──┐  ┌───▼────┐  ┌─▼────────┐
    │MusicVM│  │StatsVM │  │CommunityR │
    │(MVVM) │  │(MVVM)  │  │epository  │
    └───┬───┘  └───┬────┘  └─────┬─────┘
        │          │              │
   ┌────▼──────────▼──────────────▼────┐
   │        AppDatabase (Room)         │
   │  6 Entities · 6 DAOs · v4 schema  │
   └───────────────────────────────────┘
        │          │
   ┌────▼──┐  ┌───▼────────┐
   │Firebase│  │SharedPrefs │
   │Firestore│ │(settings)  │
   └────────┘  └────────────┘
```

### 关键设计决策

- **MVVM 部分覆盖**：MusicFragment + StatsFragment 使用 ViewModel，其余仍在 Fragment 中管理状态
- **单例数据库**：双重检查锁 + volatile，使用 ApplicationContext 防泄漏
- **线程模型**：每个 ViewModel/Activity 持有一个 `newSingleThreadExecutor()`，DAO 操作全部在后台
- **Firebase 降级**：初始化失败 → 静默降级离线模式，社区显示"不可用"
- **API Key 安全**：AMap 通过 manifestPlaceholders，DeepSeek 通过 BuildConfig，均从 local.properties 注入

---

## 六、下一步建议

### 立即可做（低成本高价值）
1. 下载动作 GIF 到 `assets/exercises/`
2. 真机测试 DeepSeek AI 计划生成
3. 真机测试通知权限引导流程

### v5.3 建议
1. MediaSession 兼容性调试（换用 Media3/MediaBrowserService）
2. Spotify SDK 集成
3. 力量训练 GIF 动作演示

---

*MusicGym v5.2 — 2026-06-07*
