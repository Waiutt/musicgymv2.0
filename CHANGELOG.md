# MusicGym 更新日志

---

## v2.1 — 产品化重构 (2026-06-02)

### 🎵 音乐模块全面升级

| 改进 | 说明 |
|------|------|
| **音频焦点管理** | 来电/通知时自动暂停，挂断后自动恢复播放。`AudioManager` + `AudioFocusRequest`（Android 8+ 兼容回退） |
| **通知栏播放控制** | 锁屏/通知栏可直接上一首/暂停/下一首，无需打开 App。`MediaStyle` 通知 + `BroadcastReceiver` |
| **三种播放模式** | 🔁 顺序播放 → 🔂 单曲循环 → 🔀 随机播放。模式状态标签实时显示 |
| **歌单实时搜索** | 输入关键词按歌曲名/歌手名实时过滤。`TextWatcher` 驱动 |
| **空状态引导** | 无音乐文件时显示操作指南，权限被拒时提示去设置开启 |
| **后台 Service** | `MusicService` 升级为 `LocalBinder` 模式，Fragment 绑定通信 |

### 🏋️ 力量训练优化

| 改进 | 说明 |
|------|------|
| **动作卡片视觉升级** | Emoji 32sp 大字 + Bold 字体 + 居中布局，去除渐变占位色块 |
| **上次训练数据参考** | 每个动作卡片显示历史最近一组数据（如 "上次 80kg×8"），无需退出查看 |
| **组间休息可配置** | 60s / 90s / 120s / 180s 四档选择，点击即时切换，选中档橙色高亮 |

### 📊 统计页增强

| 改进 | 说明 |
|------|------|
| **日历热力图可点击** | 点击任意日期弹出当天运动详情对话框：有氧（类型+距离+卡路里）+ 力量（动作列表） |
| **MVVM 架构（Stats）** | 新增 `StatsViewModel` + `StatsRepository`，Fragment 只负责 UI 渲染。`LiveData` 驱动数据更新 |

### 🗺️ GPS 追踪

| 改进 | 说明 |
|------|------|
| **自动暂停检测** | 8 秒无移动自动暂停计时和轨迹记录，恢复移动后自动继续。Toast 提示状态变化 |
| **权限拒绝提示** | GPS 权限被拒时显示 Toast "需要位置权限才能记录运动轨迹"，不再静默失败 |

### 🏘 社区模块 — Firebase 重构

| 改进 | 说明 |
|------|------|
| **真实社交后端** | 从 Room mock 假数据 → **Firebase Firestore** 云端同步 |
| **用户系统** | `Firebase Auth` 匿名登录，零摩擦进入社区。`UserManager` 管理昵称（SharedPreferences 缓存） |
| **发帖** | 发布到 Firestore `posts` 集合，支持图片 URI |
| **点赞** | `FieldValue.arrayUnion/arrayRemove` 原子操作切换点赞状态 |
| **评论** | 弹出输入框 → Firestore `comments` 数组追加 |
| **瀑布流** | `CommunityAdapter` + `Glide` 图片加载 + 互动统计（❤ N 💬 N） |

### ⚙️ 系统功能

| 改进 | 说明 |
|------|------|
| **设置页** | 导出全部数据 CSV / 清除所有数据（确认弹窗）/ 隐私政策 / 关于 / 版本号 |
| **隐私政策** | 完整合规文本，Google Play 上架必需 |
| **数据导出** | `DataExporter.java` 统一导出有氧+力量+体重记录到 `Downloads/MusicGym_data.csv` |
| **API Key 安全** | 高德 API Key 从 `AndroidManifest.xml` 移至 `local.properties`（Git 忽略），通过 `manifestPlaceholders` 注入 |
| **GitHub Actions CI** | 推送自动编译流水线（`.github/workflows/android-ci.yml`） |

### 🏗️ 架构升级

| 改进 | 说明 |
|------|------|
| **MVVM 架构** | Stats 页引入 `ViewModel` + `LiveData` + `Repository` 三层架构 |
| **颜色系统统一** | 新增 `ColorTokens.java`（Java 常量）+ `colors.xml` 扩展至 27 色值。43 处硬编码 `Color.parseColor` 全部替换 |
| **样式集中化** | 新建 `styles.xml` 定义 8 组可复用样式（TopBar/CloseButton/ToolbarTitle/ProfileButton/FilterChip 等） |
| **工具类合并** | `UiUtils.java` 统一 `dp()` 方法和 `WRAP`/`MATCH` 常量（消除 4 处重复） |
| **适配器合并** | `StrengthHistoryAdapter` 合并到 `WorkoutHistoryAdapter`（消除 54 行重复代码） |
| **测试框架** | Robolectric + 数据模型单元测试（`ModelsTest.java`，6 项） |

### 📁 文件变更统计

| 类别 | 数量 |
|------|------|
| 新建文件 | 15 |
| 重构文件 | 18 |
| 布局修改 | 10 |
| 删除文件 | 2 |

### 🔜 待 Firebase 配置

社区模块需要完成以下步骤才能运行：
1. [console.firebase.google.com](https://console.firebase.google.com) 创建项目
2. 添加 Android 应用 → 包名 `com.example.musicgym`
3. 下载 `google-services.json` 替换 `app/google-services.json`
4. 启用 **Anonymous Authentication**
5. 创建 **Cloud Firestore** 数据库

---

## v2.0 — 功能完整版 (2026-05)

- 5 Tab 主框架（音乐/GYM/统计/我的/分享）
- GPS 高德地图实时追踪
- 力量训练：7大肌群 × 17子分组 × 50+ 动作库
- 训练记录：组数/重量/次数 + PR 追踪 + 组间休息
- MPAndroidChart 折线图 + 日历热力图
- 音乐播放器：双路径扫描 + 提拉式面板
- 个人中心：围度记录 / CSV 导出 / 训练提醒
- Room 数据库 v4（6 实体 + 3 次增量迁移）

---

*2026 | MusicGym 独立开发者作品*
