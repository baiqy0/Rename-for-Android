# Renamdriod · 批量重命名

<div align="center">

**为影视收藏用户打造的 Android 批量重命名工具**

Kotlin + Jetpack Compose + Material 3 ・ 完全离线 ・ 零权限申请

</div>

## 简介

整理影视资源时，最烦的就是把一堆 `[字幕组]剧名.S01E02.1080p.mp4` 改成统一规范的名字。Renamdriod 通过 Android 的 **SAF（存储访问框架）** 读写文件——不申请任何存储权限，你选哪个文件夹，应用就只能看到哪个文件夹，授权随时可收回。

选好文件 → 搭一条规则链 → 实时预览每个文件的新名字 → 满意再执行，执行错了还能一键撤销。

## 功能特性

| | |
|---|---|
| 📂 **文件导入** | 系统选择器多选文件 / 一键导入整个文件夹；支持手机存储、SD 卡、USB OTG |
| 🎯 **导入范围** | 文件夹导入可选「视频+字幕+图片 / 仅视频字幕 / 含音频 / 所有文件」 |
| 📋 **文件列表** | 类型筛选（视频/字幕/图片/音频/其他）、自然排序（Episode 2 排在 Episode 10 前）、全选/反选 |
| ⛓️ **规则链** | 多条规则按序执行，可拖拽排序、随时启用/禁用 |
| 🔧 **9 种规则** | 查找替换（支持正则）・ 删除文字 ・ 插入文字 ・ 添加序号 ・ 符号替换 ・ 多余空格清理 ・ 去字幕组标签 ・ 影视片段移除 ・ 模板命名 |
| 👀 **实时预览** | 改名前看到每个文件的确切新名字，原名字划线、新名字高亮 |
| 🛡️ **安全检测** | 选区内重名、磁盘上已存在、非法字符、文件名过长——有问题直接阻止执行 |
| ⚡ **批量执行** | 协程后台执行，实时进度条，单个失败不影响其他文件，完成后分级报错 |
| ↩️ **一键撤销** | 保存原文件名与新文件名，重命名完成后随时整批撤销（重启应用后仍有效） |
| 🕘 **操作历史** | 最近 20 次操作记录，可查看详情、可撤销 |
| 🎨 **主题** | 动态取色（跟随壁纸）+ 6 套内置配色 × 深色/浅色/跟随系统 |
| 📍 **最近位置** | 记住最近授权的文件夹，一键重新打开，可逐项删除（释放授权） |

## 截图

![image](https://github.com/baiqy0/Rename-for-Android/edit/main/doc/screenshots/IMG_20261017_011151.jpg)

## 下载

到 [Releases](../../releases) 页面下载最新 APK，传到手机安装即可（需允许"安装未知来源应用"）。

- 最低系统：Android 8.0（API 26）
- 目标系统：Android 16（API 36）
- 包名：`com.renamdroid.batchrename`

## 构建

### 环境要求

- Android Studio（建议最新稳定版）
- JDK 17+
- Android SDK 36

### 命令行

```bash
git clone https://github.com/<你的用户名>/Renamdriod.git
cd Renamdriod
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

直接用 Android Studio 打开本目录，等 Gradle 同步完成后点 ▶ Run 也可以。

## 使用指南

1. **添加文件**：首页点「选择文件」（多选）或「选择文件夹」（整文件夹导入）
2. **搭规则**：进「规则」页添加规则，例如：
   - `查找替换`：把 `\.` 换成 ` `（点号变空格）
   - `添加序号`：`E{01}` 从 1 开始、2 位
   - `去字幕组标签`：去掉 `[XXXX]` 前缀
   - 每条规则可拖动排序、随时开关，预览实时刷新
3. **执行**：预览里每行的 `原名 → 新名` 确认无误，点「执行重命名」
4. **后悔药**：执行完在结果页点「撤销」，或去「历史」页撤销任意一次操作

## 模板命名

模板规则支持以下占位符（配合内置影视文件名解析器，自动从原名提取信息）：

| 占位符 | 含义 | 示例 |
|---|---|---|
| `{title}` | 剧名/片名 | 权力的游戏 |
| `{season}` | 季（数字） | 1 |
| `{episode}` | 集（数字） | 2 |
| `{year}` | 年份 | 2011 |
| `{resolution}` | 分辨率 | 1080p |
| `{codec}` | 编码 | HEVC |
| `{source}` | 来源 | WEB-DL |
| `{audio}` | 音频信息 | DDP5.1 |
| `{ext}` | 扩展名 | mkv |
| `{index}` | 序号 | 1 |

示例：`{title} - S{season}E{episode} - {resolution}` → `权力的游戏 - S01E02 - 1080p`

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3（动态取色、深色模式）
- **架构**：单 Activity + Navigation Compose + ViewModel + 仓库分层
- **异步**：Kotlin Coroutines / Flow
- **文件访问**：SAF（`DocumentsContract` / `DocumentFile`），权限持久化（`takePersistableUriPermission`）
- **持久化**：SharedPreferences（设置与历史，无数据库依赖）
- **最低 SDK 26**，无第三方网络库，**零第三方运行时依赖膨胀**

## 项目结构

```
app/src/main/java/com/renamdroid/batchrename/
├── core/                      # 纯逻辑，无 Android 依赖
│   ├── NaturalSort.kt         # 自然排序比较器（数字按数值比，不按字典序）
│   └── MediaNameParser.kt     # 影视文件名解析（剧名/季/集/年份/分辨率/编码…）
├── data/                      # 数据层
│   ├── model/                 # FileItem / RenameRule / HistoryEntry 等模型
│   ├── FileRepository.kt      # SAF 文件操作（列举/重命名/权限持久化/最近位置）
│   ├── RenameEngine.kt        # 规则链执行 + 合法性检查（纯函数，可单测）
│   ├── SettingsRepository.kt  # 设置持久化
│   └── HistoryRepository.kt   # 操作历史 + 撤销数据
├── ui/                        # Compose 界面层
│   ├── MainViewModel.kt       # 全局状态与业务编排
│   ├── AppNav.kt              # 导航图 + 系统文件选择器
│   ├── components/            # 通用组件（文件名格式化展示）
│   ├── theme/                 # Material 3 主题与配色
│   └── screens/               # 首页/文件列表/规则/规则编辑/历史/设置/关于
├── CrashReporter.kt           # 崩溃日志本地落盘（下次启动可读）
├── MainActivity.kt            # 单 Activity 入口
└── RenameApplication.kt       # Application 入口（依赖装配）
```

## 隐私

- 清单文件里**没有任何权限声明**，文件访问完全走 SAF，由用户在系统选择器里亲自授权
- 无网络权限，完全离线运行
- 不上传、不收集任何文件、文件名或目录结构
- 设置与操作历史只保存在本机 SharedPreferences


## 已知限制
- 仅支持当前目录，不递归子文件夹
- 撤销依赖授权仍然有效；若重命名后手动在系统里撤销了授权，撤销会失败（应用会明确提示）
- 单批执行超大文件列表（数千个）时预览计算需要几秒

## 开源协议

[MIT](LICENSE) © 2026 Renamdriod contributors
