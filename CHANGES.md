# 修改文档 —— 控制中心快捷开关与中文本地化

本文档记录在 OpenWOL 上所做的全部改动。

| | |
|---|---|
| 上游仓库 | <https://gitlab.com/julien.cardoux/openwol> |
| 本 fork 仓库 | <https://github.com/lanxize/openwol> |
| 基线 | `master` @ `08cbf7e`（v1.0.3 / versionCode 4） |
| 本文档对应状态 | v1.1.0 / versionCode 5 |
| 提交状态 | **已提交** `ac90712`，并已推送至 GitHub（`master` 分支 + `v1.0.0`~`v1.0.3` 四个 tag） |
| 许可 | 上游自述为 AGPLv3（F-Droid 标注 `AGPL-3.0-or-later`）；本 fork 同样以 AGPL-3.0 发布 |

> 之所以单独建这个文件而不是改写上游的 `README.md`，是为了保留原有说明完整无缺。
> 如果要把这些功能提回上游，`README.md` 的 Features / Permissions / How to Use 三节也需要同步补充，见文末「后续建议」。

---

## 目录

- [一、功能新增](#一功能新增)
  - [1. 控制中心快捷开关](#1-控制中心快捷开关qs-tile)
  - [2. 默认设备](#2-默认设备)
  - [3. 磁贴震动反馈](#3-磁贴震动反馈)
  - [4. 首次启动通知权限引导](#4-首次启动通知权限引导)
  - [5. 中文本地化](#5-中文本地化)
- [二、缺陷修复](#二缺陷修复)
  - [6. 修改 MAC 会切断自动化关联](#6-修改-mac-会切断自动化关联)
  - [7. 悬空的设备引用](#7-悬空的设备引用)
  - [8. 磁贴提示在应用未运行时被系统丢弃](#8-磁贴提示在应用未运行时被系统丢弃)
  - [9. 磁贴震动被系统丢弃](#9-磁贴震动被系统丢弃)
- [三、构建配置调整](#三构建配置调整)
- [四、踩到的三个 Android 平台限制](#四踩到的三个-android-平台限制)
- [五、改动文件清单](#五改动文件清单)
- [六、构建与验证](#六构建与验证)
- [七、已知限制](#七已知限制)
- [八、后续建议](#八后续建议)

---

## 一、功能新增

### 1. 控制中心快捷开关（QS Tile）

不打开 App，在控制中心点一下就能唤醒默认设备。

**交互设计**

| 场景 | 行为 |
|---|---|
| 点击（有默认设备 + 在 WiFi/VPN 上） | 发送魔法包，Toast「开机指令已发送」，单次短震 |
| 点击（一个设备都没有） | Toast「当前无可用设备」，双脉冲震动，**不发包** |
| 点击（不在 WiFi/VPN 上） | Toast「网络状态错误」，双脉冲震动，**不发包** |
| 长按 | 打开 App 主页 |
| 标题 | `打开"设备名"`，跟随默认设备变化 |

判定顺序是「无设备」优先于「网络错误」——没配设备是更该先解决的问题。

**实现要点**

- `WolTileService : TileService`，清单里声明 `android.service.quicksettings.action.QS_TILE`
  与 `BIND_QUICK_SETTINGS_TILE` 权限
- 磁贴是**瞬时按钮**而非开关：Wake-on-LAN 是单向 UDP，没有回执，做不出真实的"开机状态"，
  所以显式不声明 `META_DATA_TOGGLEABLE_TILE`，避免给出误导性的开关外观
- `onStartListening()` 每次面板展开时刷新标题，因此 App 里换了默认设备后磁贴会自动跟上
- 长按通过 `ACTION_QS_TILE_PREFERENCES` 实现：给 `MainActivity` 加一个该 action 的
  intent-filter，系统在长按时就会拉起它
- 图标复用 App 已有的闪电字形，新建 24dp 的 `ic_tile_wol.xml`（白色单色，系统按主题染色）

### 2. 默认设备

磁贴需要知道"唤醒哪一个"，所以引入"默认设备"概念。

**规则（单选、不可取消）**

- 只要有设备，就**必有且仅有一个**默认设备
- 添加的第一个设备自动成为默认设备
- 删除默认设备时，自动把列表里剩下的第一个顶上
- 编辑设备（改名、改 IP、改 MAC）**不会**改变默认归属
- 设备列表每张卡片左侧一个单选圈，点击即切换

因为"不能取消选中"是硬规则，控件用了 `RadioButton` 而不是 `CheckBox`——后者的语义是
"再点一下可以取消"，与规则矛盾。

数据层面在 `Device` 上加了一个 `isDefault: Boolean = false` 字段。带默认值所以旧数据能
无痛反序列化；又因为 kotlinx.serialization 默认跳过等于默认值的字段，这个标记对非默认设备
不占任何存储空间。

### 3. 磁贴震动反馈

磁贴的反馈有两条**互相独立**的通道，任何一条失效都还有另一条：

| 通道 | 权限要求 | 生效条件 |
|---|---|---|
| 震动 | `VIBRATE`（安装即授予，用户无法关闭） | 始终生效 |
| 文字提示 | `POST_NOTIFICATIONS` | 授权后生效 |

震动模式刻意做成可区分的：

- **成功** → 单脉冲（`0, 50`）——"发出去了"
- **失败** → 双脉冲（`0, 45, 90, 45`）——"没发出去"，具体原因看提示

### 4. 首次启动通知权限引导

`POST_NOTIFICATIONS` 是磁贴文字反馈的**硬性前提**（原因见第四节），但用户没有理由自己知道
这件事。所以在首次启动时用一个说明弹窗讲清缘由，再走系统权限框，且**只问一次**。

- 仅在 `API ≥ 33`、权限未授予、且从未问过时触发
- 无论用户选「允许」还是「暂不」，都写入 `notification_permission_asked` 标记，不再打扰
- 顺带让自动化功能的通知（含那条常驻的服务通知）也能正常显示——它们此前同样需要这个权限

### 5. 中文本地化

采用标准 Android 国际化做法，而不是把中文硬写进代码：

| 文件 | 内容 |
|---|---|
| `res/values/strings.xml` | 96 条英文（默认，供非中文用户） |
| `res/values-zh-rCN/strings.xml` | 96 条中文，键一一对应 |

原先散落在 5 个 Kotlin 文件里的 84 条界面文案，加上本次新增功能的 12 条，现在共 96 条统一走
资源：Compose 用 `stringResource()`，Service 与工具函数用 `getString()`。跟随系统语言，
中文设备上全中文，英文设备上全英文。

App 名称 `OpenWOL` 保留未翻译（品牌名按惯例不本地化）。

---

## 二、缺陷修复

### 6. 修改 MAC 会切断自动化关联

**根因**：`Automation.deviceIds` 存的是 **MAC 字符串**，等于把 MAC 当外键用，而
`WOLViewModel.updateDevice()` 只更新设备本身，不同步引用。改完 MAC 后，规则的
`deviceIds` 还指向旧 MAC，匹配不到任何设备——**规则留在列表里但永远唤不醒任何东西，
且没有任何提示**。

**修法**：MAC 变化时调用 `replaceMacReferences()`，把所有引用旧 MAC 的自动化改写为新 MAC。

### 7. 悬空的设备引用

`removeDevice()` 原先只在"某条规则引用的设备**全部**被删光"时才删除该规则。混选规则
（同时关联 A、B）里删掉 A 后，A 的 MAC 会作为死引用残留下来——一旦同一个 MAC 被重新
添加为设备，这条规则会**诡异地自动复活**。

**修法**：删除设备时从每条规则中摘掉该设备；摘空的规则一并删除。

### 8. 磁贴提示在应用未运行时被系统丢弃

**现象**：App 不在运行时点磁贴，魔法包发出去了，但没有任何提示。

**根因**：Android 13 起，应用的通知权限一旦关闭，系统会在 `NotificationService` 层直接
丢弃它的 Toast，**除非该应用当时有前台可见界面**。而磁贴最典型的使用场景恰恰是"App 没开"。

```
E NotificationService: Suppressing toast from package com.juliencardoux.openwol by user request.
```

**为什么最初测试没发现**：我验证磁贴时总是把 App 界面开在控制中心后面，那时它有可见界面，
Toast 不受限制。只有真正"App 没运行"时才会暴露。

**修法**：首次启动引导申请 `POST_NOTIFICATIONS`（见 1.4），并在磁贴里补上震动通道兜底。

三轮 A/B/A 对照验证（每轮都杀掉进程再点磁贴）：

| 轮次 | `POST_NOTIFICATIONS` | `Suppressing toast` | 结果 |
|---|---|---|---|
| A1 | 已授权 | 0 次 | ✅ 弹出 |
| B | 已撤销 | 2 次 | ❌ 被压制 |
| A2 | 再次授权 | 0 次 | ✅ 弹出 |

### 9. 磁贴震动被系统丢弃

第一版震动用默认方式实现，代码跑到了，但系统日志显示：

```
| effect | ignored_background | usage: TOUCH | com.juliencardoux.openwol |
```

**震动也被后台限制丢了**，和 Toast 是同一类问题。规则见第四节，改用
`USAGE_PHYSICAL_EMULATION` 后变为 `finished`。

---

## 三、构建配置调整

| 改动 | 原因 |
|---|---|
| `app/build.gradle.kts` 增加 `buildToolsVersion = "35.0.0"` | AGP 8.6 默认要 build-tools 34.0.0，而本机 SDK 只装了 35.0.0；不显式 pin 会直接构建失败 |
| `.gitlab-ci.yml` 的 `ANDROID_BUILD_TOOLS` 由 `34.0.0` 改为 `35.0.0` | 与上一条保持同步，否则 CI 会找不到 35.0.0。顺带让 build-tools 与 `compileSdk 35` 一致 |
| `versionCode` 4 → 5，`versionName` `1.0.3` → `1.1.0` | 新增功能，按语义化版本走次版本号；`versionCode` 每次发版无条件 +1 |

> `versionName` 会被 CI 用 `grep 'versionName' app/build.gradle.kts` 提取，用于生成 tag
> `v1.1.0` 和产物路径，所以这两个数字必须一起改。

---

## 四、踩到的三个 Android 平台限制

这一节是本次改动中**最不显而易见**的部分，全部来自实测而非推测。

### 限制 1：通知权限关闭时，后台应用无法弹 Toast

Android 13（API 33）起，`NotificationManagerService` 会丢弃通知权限被关闭的应用的 Toast，
除非该应用当时有前台可见界面。磁贴由 SystemUI 绑定一个后台服务来响应点击，正好命中这条限制。

**推论**：任何依赖 Toast 做磁贴反馈的实现，都必须持有 `POST_NOTIFICATIONS`。

### 限制 2：未列入白名单的震动会被后台丢弃

AOSP 的 `VibrationSettings.shouldIgnoreVibration()`：

```java
public Status shouldIgnoreVibration(@NonNull CallerInfo callerInfo) {
    final int usage = callerInfo.attrs.getUsage();
    synchronized (mLock) {
        if (!mUidObserver.isUidForeground(callerInfo.uid)
                && !BACKGROUND_PROCESS_USAGE_ALLOWLIST.contains(usage)) {
            return Status.IGNORED_BACKGROUND;
        }
        ...
```

白名单内容：

```java
BACKGROUND_PROCESS_USAGE_ALLOWLIST = {
    USAGE_RINGTONE, USAGE_ALARM, USAGE_NOTIFICATION,
    USAGE_COMMUNICATION_REQUEST, USAGE_HARDWARE_FEEDBACK,
    USAGE_PHYSICAL_EMULATION };
```

**默认的 `USAGE_TOUCH` 不在名单里**，而 `USAGE_PHYSICAL_EMULATION`（模拟物理按键）在——
语义上也恰好贴合控制中心磁贴这个"虚拟按钮"。

API 等级（查 SDK 的 `api-versions.xml` 确认）：

| 能力 | API |
|---|---|
| `VibrationAttributes` 类 + `USAGE_PHYSICAL_EMULATION` | 30 |
| `Vibrator.vibrate(VibrationEffect, VibrationAttributes)` | 33 |

所以代码做了版本分支，API 33 以下回落到默认用法。

### 限制 3：磁贴的 `onClick` 只在面板打开时投递

面板关闭时，SystemUI 收到点击只记录一条日志就丢掉了：

```
CustomTileExt: handleClick: custom(com.juliencardoux.openwol/.WolTileService), pendingBind: false
```

**这解释了为什么自动化测试磁贴必须先 `cmd statusbar expand-settings` 再 `click-tile`**，
也说明这类测试不能想当然地绕过 UI 状态。

---

## 五、改动文件清单

**新增（3）**

| 文件 | 作用 |
|---|---|
| `app/src/main/java/.../WolTileService.kt` | 磁贴服务：发包、三种提示、震动、标题刷新 |
| `app/src/main/res/drawable/ic_tile_wol.xml` | 24dp 白色单色磁贴图标 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 中文文案 |

（本文档 `CHANGES.md` 自身也是新增文件，不计入上表的代码改动。）

**修改（12）**

| 文件 | 改动摘要 |
|---|---|
| `Models.kt` | `Device` 增加 `isDefault` 字段 |
| `WOLViewModel.kt` | 默认设备唯一性不变式、自动补位、旧数据迁移、MAC 引用同步、悬空引用清理 |
| `WolUtils.kt` | 新增 `loadDefaultDevice()`（磁贴独立读取）与 `vibrate()` |
| `Screens.kt` | 设备卡片增加单选圈、空列表引导文案、文案资源化 |
| `Dialogs.kt` | 新增通知权限说明弹窗、文案资源化 |
| `MainActivity.kt` | 接通 `onSetDefault`、首次启动权限引导、文案资源化 |
| `AutomationService.kt` | 通知文案资源化 |
| `Constants.kt` | 新增 `PREFS_NOTIFICATION_PERMISSION_ASKED` |
| `AndroidManifest.xml` | 声明磁贴服务 + `QS_TILE_PREFERENCES` |
| `res/values/strings.xml` | 从 1 条扩展到 96 条英文文案 |
| `app/build.gradle.kts` | pin `buildToolsVersion`、版本号 |
| `.gitlab-ci.yml` | build-tools 版本同步 |

累计 **+521 / −115**。

---

## 六、构建与验证

### 构建

本机环境有几个必须绕开的限制（沙箱只允许写工作区、系统只有 JDK 8）：

```powershell
$env:JAVA_HOME         = '<工作区>\.tools\jdk-17'         # AGP 8.x 需要 JDK 17
$env:GRADLE_USER_HOME  = '<工作区>\.tools\gradle-home'    # 默认的 ~/.gradle 在工作区外，会被拒
$env:ANDROID_USER_HOME = '<工作区>\.tools\android-home'   # AGP 的调试密钥也需落在工作区内
$env:ANDROID_HOME      = "$env:LOCALAPPDATA\Android\Sdk"

Set-Location <仓库>
.\gradlew.bat assembleDebug --console=plain --no-watch-fs
```

`assembleRelease` 同样通过（含 `lintVital` + R8 + 资源压缩），确认不会挂掉 CI。

### 真机验证结果

设备：Xiaomi 2509FPN0BC / HyperOS 4.0 / Android 17 (API 37) / arm64-v8a

| 验证项 | 结果 |
|---|---|
| 首个设备自动成为默认 | ✅ 落盘 `isDefault:true` |
| 新增设备不抢默认 | ✅ 原默认保持 |
| 切换默认（双向） | ✅ |
| 点单选圈不误触发发包 | ✅ 无「Packet Sent」浮层 |
| 删除默认设备自动补位 | ✅ 剩余第一个被提升 |
| 改 MAC 后自动化关联跟随 | ✅ `deviceIds` 同步更新 |
| 磁贴标题 | ✅ `content-desc="打开"PC""` |
| 无设备提示 | ✅「当前无可用设备」 |
| 网络错误提示 | ✅「网络状态错误」 |
| 发送成功提示（**进程已死**） | ✅ `Suppressing toast = 0` |
| 长按进主页 | ✅ 前台窗口变为 `MainActivity` |
| 首次启动权限引导 | ✅ 说明弹窗 → 系统权限框 → 授权 |
| 不再重复询问 | ✅ 无障碍树中查不到该弹窗 |
| 成功震动 | ✅ `finished`，单脉冲 `[0, 50]` |
| 失败震动 | ✅ `finished`，双脉冲 `[0, 45, 90, 45]` |

### 魔法包实测

用独立编写的 UDP 监听器（`wol_sniffer.py`，位于工作区 `wol-sniffer/`）在局域网内抓包，
确认应用发出的是**完全规范**的魔法包：

```
包 #1: 头部 FF FF FF FF FF FF | 目标 MAC D4:93:90:1A:47:C6 | 16 次重复一致 | 102 字节
```

监听器的用法与防火墙排查见该目录下的 `README.md`。

> ⚠️ 注意：本机自己发给自己走的是回环，防火墙不设限，所以监听器的 `--send-test` 自检
> **永远能通过**，它只能证明脚本本身正常，不能证明外部流量进得来。两者要分开看。

### 实机结果（最终验证）

**已实测成功唤醒真实设备**：把 PC 关机后，在控制中心点一下磁贴，机器正常开机。

这是整条链路的终极验证——磁贴点击 → 生成魔法包 → Wi-Fi 广播 → 目标网卡识别 → 主板上电。
前面所有技术项验证的都只是链路中的某一环，只有真正开机才能证明它们全部串通。

---

## 七、已知限制

1. **小米新版控制中心不显示磁贴图标下方的文字。** 所以 `打开"设备名"` 这个标题在 HyperOS 上
   肉眼看不到（已通过无障碍树确认它确实被设置了）。换成经典样式通知栏或 AOSP 设备即可见。
   标题过长时系统会自动截断。

2. **Wake-on-LAN 没有回执。** 「开机指令已发送」只代表包已交给网络栈，不代表设备真的开机了。
   这句话在协议层面依然成立（UDP 无确认）——本次虽已实测唤醒成功，但应用本身**无法知道**结果。
   磁贴因此做成瞬时按钮而非开关，做成开关会是欺骗性的。这是上游既有语义，本次未改动。

3. **`sendWakeOnLan()` 会吞掉所有异常。** 发送失败时 UI 与磁贴仍提示成功。若要更严谨的反馈，
   需要让它返回结果，属于独立改动。

4. **磁贴窗口小部件/多开关等未实现。** 一个 App 可以声明多个 TileService 组件，从而提供
   "每个设备一个开关"。本次只做了"默认设备"单开关，符合需求范围。

---

## 八、后续建议

若要提回上游，建议补上：

1. **`README.md` 同步**：Features 一节加入控制中心快捷开关与中文；Permissions 表格补充
   `POST_NOTIFICATIONS` 并说明用途；How to Use 加入"如何把磁贴拖进控制中心"与
   "首次启动会请求通知权限，原因是什么"。
2. **`fastlane/metadata/android/`**：目前只有 `en-US`，可补 `zh-CN` 的商店元数据。
3. **`sendWakeOnLan()` 返回结果**：让 UI 与磁贴能区分"发出去了"和"发送失败"。
4. **把 MAC 当外键这件事**：本次只是在改 MAC 时同步引用，治标。更彻底的做法是给 `Device`
   加一个稳定的 UUID 主键，自动化引用 UUID 而非 MAC，但那需要一次数据迁移。
5. **自动化前台服务通知**同样依赖 `POST_NOTIFICATIONS`，现在首次启动的引导顺带覆盖了它。

---

## 九、作为独立 fork 发布的调整

本节记录把它作为一个**独立的 GitHub fork**（而非上游的补丁）发布时，额外做的品牌与合规调整。

| 改动 | 内容 | 原因 |
|---|---|---|
| `applicationId` | `com.juliencardoux.openwol` → **`io.github.lanxize.openwol`** | 沿用上游包名会与官方版本冲突（签名不同，更新时必须先卸载重装），而且占用了原作者的反向域名命名空间 |
| `LICENCE` → `LICENSE` | 重命名（Git 识别为 rename，内容一字未改） | GitHub 与多数工具链的默认拼写；已确认仓库内没有任何地方按文件名引用它 |
| `README.md` 顶部 | 加入中英双语的 fork 声明 | 明确这是社区修改版、与原作者无隶属关系，避免被误认为官方发布 |
| `README.md` 许可证段 | 补充版权归属（原作者 + 本 fork） | 上游仓库里**没有任何版权行**，AGPL §5(b) 要求保留或补充归属声明 |
| 12 个被修改的文件 | 文件头加 `Modified by lanxize in 2026-10. See CHANGES.md for details.` | AGPL §5(a)「prominent notices stating that you changed the files and the date」 |

**刻意没改的地方**：`namespace` 仍是 `com.juliencardoux.openwol`。它只决定生成 R 类的包名，
对用户完全不可见；改它要移动整个源码目录并重写所有 `package` 声明，收益不足以抵消改动风险。

### 换包名带来的两个副作用

1. **它会成为手机上一个全新的 App**，与旧包名的应用并存，二者数据不共享。旧应用里已配置的
   设备不会自动迁移过来。
2. **控制中心里已添加的磁贴会失效**——磁贴绑定的是组件名
   `com.juliencardoux.openwol/.WolTileService`，而新包对应的组件是
   `io.github.lanxize.openwol/com.juliencardoux.openwol.WolTileService`，需要在控制中心里重新添加。
