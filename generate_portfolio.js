const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, PageNumber, PageBreak
} = require("docx");

const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 60, bottom: 60, left: 100, right: 100 };

function cell(text, width, opts = {}) {
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    margins: cellMargins,
    shading: opts.shading ? { fill: opts.shading, type: ShadingType.CLEAR } : undefined,
    children: [new Paragraph({
      children: [new TextRun({ text, ...opts.run })],
      alignment: opts.align || AlignmentType.LEFT,
    })],
  });
}

function heading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    children: [new TextRun(text)],
  });
}

function heading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    children: [new TextRun(text)],
  });
}

function para(text, opts = {}) {
  return new Paragraph({
    children: [new TextRun({ text, ...opts })],
    spacing: opts.spacing ? { after: opts.spacing } : { after: 80 },
    alignment: opts.align || AlignmentType.LEFT,
  });
}

function bullet(text) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    children: [new TextRun(text)],
  });
}

function tableHeaderRow(cols, widths) {
  return new TableRow({
    children: cols.map((c, i) => cell(c, widths[i], { shading: "1E293B", run: { bold: true, color: "FFFFFF", size: 20 } })),
  });
}

function tableRow(cols, widths) {
  return new TableRow({
    children: cols.map((c, i) => cell(c, widths[i], { run: { size: 20 }, shading: i === 0 ? "F1F5F9" : undefined })),
  });
}

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal",
        run: { size: 36, bold: true, font: "Arial", color: "1E293B" },
        paragraph: { spacing: { before: 360, after: 180 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal",
        run: { size: 28, bold: true, font: "Arial", color: "334155" },
        paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 1 } },
    ],
  },
  numbering: {
    config: [
      { reference: "bullets",
        levels: [{ level: 0, format: "bullet", text: "•", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ],
  },
  sections: [
    // ═══ Cover Page ═══
    {
      properties: {
        page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } },
      },
      children: [
        new Paragraph({ spacing: { before: 3600 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "MusicGym", size: 72, bold: true, color: "FC4C02" })],
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 120 },
          children: [new TextRun({ text: "一站式健身记录 Android 应用", size: 32, color: "475569" })],
        }),
        new Paragraph({ spacing: { before: 400 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "项目作品集", size: 40, bold: true, color: "1E293B" })],
        }),
        new Paragraph({ spacing: { before: 600 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "2026.03 — 2026.06", size: 24, color: "64748B" })],
        }),
        new Paragraph({ spacing: { before: 200 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "独立开发者 · Vibe Coding · AI 驱动开发", size: 24, color: "64748B" })],
        }),
        new Paragraph({ spacing: { before: 400 } }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: "GitHub: github.com/Waiutt/musicgymv2.0", size: 20, color: "38BDF8" })],
        }),
      ],
    },

    // ═══ Main Content ═══
    {
      properties: {
        page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            children: [new TextRun({ text: "MusicGym 项目作品集", size: 18, color: "94A3B8" })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: "— ", size: 18, color: "94A3B8" }), new TextRun({ children: [PageNumber.CURRENT], size: 18, color: "94A3B8" }), new TextRun({ text: " —", size: 18, color: "94A3B8" })],
          })],
        }),
      },
      children: [

        // ── 1. 项目概述 ──
        heading1("1. 项目概述"),
        para("MusicGym 是一款一站式健身记录 Android 应用，集 GPS 运动追踪、力量训练、本地音乐播放、社区社交、AI 训练计划于一体。全流程采用 Vibe Coding 模式，使用 Claude Code 进行需求分析、架构设计、代码实现、测试审计和产品迭代。"),
        para("项目已发布至 GitHub，包含完整源码、真机截图、技术文档和产品路线图。"),

        // ── 2. 开发方式 ──
        heading2("1.1 开发方式：Vibe Coding"),
        para("采用 AI 驱动的开发模式，工作流程如下："),
        bullet("自然语言描述需求 → AI 生成方案 → 人工审查关键决策 → AI 执行实现 → 编译验证 → 真机测试 → 多轮审计 → 迭代发布"),
        bullet("单会话最高 35 次 Git 提交，累计 10 轮全维度代码质量审计"),
        bullet("AI 生成代码占比约 70%，人工负责架构决策、安全审查和最终验收"),
        bullet("开发效率：单会话实现 7 项新功能 + 20+ 处 Bug 修复 + 5 轮审计"),

        // ── 3. 技术架构 ──
        new Paragraph({ children: [new PageBreak()] }),
        heading1("2. 技术架构"),
        para("采用 MVVM + Repository 分层架构，ViewModel 驱动 LiveData 响应式 UI。核心技术栈如下："),

        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [2800, 6560],
          rows: [
            tableHeaderRow(["分类", "技术"], [2800, 6560]),
            tableRow(["语言", "Java 11"], [2800, 6560]),
            tableRow(["架构模式", "MVVM (4 ViewModels) + Repository Pattern"], [2800, 6560]),
            tableRow(["数据库", "Room v5 (8 Entities, 7 DAOs, 5 次增量迁移)"], [2800, 6560]),
            tableRow(["地图", "高德 3D 地图 SDK 10.0.600 (GPS追踪/配速着色路线/路线回放)"], [2800, 6560]),
            tableRow(["图表", "MPAndroidChart 3.1.0 (折线图/热力图/趋势图)"], [2800, 6560]),
            tableRow(["社交后端", "Firebase Firestore + Auth (匿名登录/离线降级)"], [2800, 6560]),
            tableRow(["AI", "DeepSeek Chat API (训练计勒生成/BuildConfig安全注入)"], [2800, 6560]),
            tableRow(["音乐", "MediaPlayer + AudioFocus + Gapless无缝播放 + 5段EQ"], [2800, 6560]),
            tableRow(["图片", "Glide 4.16 (diskCache本地缓存)"], [2800, 6560]),
            tableRow(["构建", "Gradle + ProGuard/R8 + Release签名 + 共享线程池"], [2800, 6560]),
            tableRow(["兼容性", "minSdk 26 (Android 8+) 覆盖 95% 设备"], [2800, 6560]),
          ],
        }),

        para(""),
        heading2("2.1 架构图"),
        para("├── MainActivity (5 Tab 底部导航)"),
        para("│   ├── GymFragment (MVVM)   → WorkoutActivity (GPS追踪)"),
        para("│   ├── MusicFragment (MVVM) → MusicService (前台服务)"),
        para("│   ├── StatsFragment (MVVM) → StatsRepository → Room"),
        para("│   ├── ProfileFragment     → 成就/趋势图/导出"),
        para("│   └── ShareFragment      → CommunityRepository → Firebase"),
        para("└── AppDatabase (Room v5 单例) → 8 Entity + 7 DAO"),

        // ── 4. 功能模块 ──
        new Paragraph({ children: [new PageBreak()] }),
        heading1("3. 功能模块"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [1600, 1200, 6560],
          rows: [
            tableHeaderRow(["模块", "完成度", "核心功能"], [1600, 1200, 6560]),
            tableRow(["运动追踪", "92%", "GPS实时追踪/配速着色路线/每公里语音播报/自动暂停/路线回放"], [1600, 1200, 6560]),
            tableRow(["力量训练", "85%", "7肌群x50+动作/ViewPager2滑动训练/%1RM胶囊/组间休息计时/AI四周计划"], [1600, 1200, 6560]),
            tableRow(["音乐播放", "93%", "MP3扫描/3种播放模式/5段EQ/收藏/步频匹配/歌单管理/Gapless无缝播放"], [1600, 1200, 6560]),
            tableRow(["数据统计", "80%", "日历热力图/折线图/过滤芯片/趋势箭头/个人纪录墙/目标进度"], [1600, 1200, 6560]),
            tableRow(["个人中心", "88%", "头像/体重趋势图/身体围度/CSV导出/训练提醒/成就徽章(9枚)"], [1600, 1200, 6560]),
            tableRow(["社区社交", "85%", "帖子瀑布流/发布动态/点赞评论/关注系统/挑战排行/举报"], [1600, 1200, 6560]),
            tableRow(["系统", "75%", "引导页/桌面Widget/通知权限/深色浅色主题/Release签名+ProGuard"], [1600, 1200, 6560]),
          ],
        }),

        // ── 5. 代码规模 ──
        para(""),
        heading1("4. 代码规模"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [3120, 3120, 3120],
          rows: [
            tableHeaderRow(["指标", "数值", "说明"], [3120, 3120, 3120]),
            tableRow(["Java 文件", "54 个", "含 10 Activity + 5 Fragment + 4 ViewModel"], [3120, 3120, 3120]),
            tableRow(["XML 布局", "23 个", "含 3 BottomSheet + 5 自定义样式"], [3120, 3120, 3120]),
            tableRow(["Room Entity", "8 个", "Workout/Strength/Weight/BodyMeasurement/Playlist/..."], [3120, 3120, 3120]),
            tableRow(["Room DAO", "7 个", "每个 Entity 对应独立 DAO接口"], [3120, 3120, 3120]),
            tableRow(["代码总行", "11,300+", "Java ~8,300 + XML ~3,000"], [3120, 3120, 3120]),
            tableRow(["单元测试", "14 个", "覆盖 Entity/边界条件/工具方法"], [3120, 3120, 3120]),
            tableRow(["Git Commits", "73", "语义化提交信息 + Tag版本管理"], [3120, 3120, 3120]),
            tableRow(["APK 体积", "45-83MB", "Release 45MB(ProGuard) / Debug 83MB"], [3120, 3120, 3120]),
          ],
        }),

        // ── 6. 质量保障 ──
        new Paragraph({ children: [new PageBreak()] }),
        heading1("5. 质量保障"),
        para("全流程执行多维度代码审计，确保产品质量："),
        para(""),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [1800, 3600, 3960],
          rows: [
            tableHeaderRow(["轮次", "审计维度", "主要发现与修复"], [1800, 3600, 3960]),
            tableRow(["第1轮", "空安全与异常处理", "aMap null守卫、前台服务通知权限、requireActivity崩溃"], [1800, 3600, 3960]),
            tableRow(["第2轮", "资源管理与生命周期", "BroadcastReceiver未注销、ExecutorService未关闭、MediaPlayer异常泄漏"], [1800, 3600, 3960]),
            tableRow(["第3轮", "并发与线程安全", "Room主线程访问、ArrayList非线程安全、LinkedHashSet竞争"], [1800, 3600, 3960]),
            tableRow(["第4轮", "边界条件与数据校验", "除零保护、parseDouble全局try-catch、发帖空标题拦截"], [1800, 3600, 3960]),
            tableRow(["第5轮", "依赖可靠性", "UserManager单例volatile、ProGuard内部类keep规则、DB迁移链验证"], [1800, 3600, 3960]),
          ],
        }),
        para(""),
        heading2("5.1 真机测试 Bug 修复"),
        bullet("音乐播放按钮变形：面板重构为播放器+mini条+歌单三层分离布局"),
        bullet("地图显示世界地图：预置 moveCamera 到街道级缩放"),
        bullet("社区发帖失败：Firebase异步登录未完成就调用，改为在发布时重新获取userId"),
        bullet("个人页闪退：Fragment重建时旧任务竞争View引用，改为局部final捕获"),

        // ── 7. 综合评估 ──
        para(""),
        heading1("6. 综合质量评估"),
        new Table({
          width: { size: 9360, type: WidthType.DXA },
          columnWidths: [1872, 1872, 1872, 1872, 1872],
          rows: [
            tableHeaderRow(["易用性", "性能", "可靠性", "无障碇", "安全性"], [1872, 1872, 1872, 1872, 1872]),
            tableRow(["88/100", "86/100", "93/100", "83/100", "92/100"], [1872, 1872, 1872, 1872, 1872]),
          ],
        }),
        para("综合评分: 88/100 (B+)", { bold: true, align: AlignmentType.CENTER, spacing: { before: 200 } }),
        para("全部 6 维度均达 80+，其中 4 维度 85+，2 维度 90+", { align: AlignmentType.CENTER }),

        // ── 8. 截图说明 ──
        new Paragraph({ children: [new PageBreak()] }),
        heading1("7. 真机截图"),
        para("以下截图均来自实际设备真机测试："),
        para(""),
        bullet("运动入口页 (GYM) — 4张运动卡片 + 上次训练信息"),
        bullet("GPS运动追踪页 — 实时地图、配速、距离、卡路里仪表盘"),
        bullet("音乐播放器页 — 专辑封面、控制按钮、歌单面板"),
        bullet("力量训练页 — 7肌群标签、3列卡片网格、AI按钮"),
        bullet("数据统计页 — 日历热力图、折线图、摘要卡片、纪录墙"),
        bullet("社区页 — 瀑布流帖子列表、FAB发布按钮"),
        bullet("个人中心页 — 头像、累计统计、体重趋势图、功能按钮"),
        para(""),
        para("→ 完整截图见 GitHub: screenshots/ 目录 (7张真机截图)", { italics: true }),

        // ── 9. 项目链接 ──
        new Paragraph({ children: [new PageBreak()] }),
        heading1("8. 项目链接"),
        para(""),
        bullet("源码: https://github.com/Waiutt/musicgymv2.0"),
        bullet("产品路线图: ROADMAP.md"),
        bullet("技术总结: SUMMARY_v5.2.md"),
        bullet("真机截图: screenshots/ (7张)"),
        bullet("构建说明: README.md"),

      ],
    },
  ],
});

Packer.toBuffer(doc).then(buf => {
  const out = "C:/Users/ASUS/Desktop/MusicGym_Portfolio_v2.docx";
  fs.writeFileSync(out, buf);
  console.log("Done: " + out);
});
