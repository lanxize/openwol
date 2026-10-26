# 修改文档 —— 控制中心快捷开关与中文本地化

本 fork 相对上游 OpenWOL 的全部改动。

| | |
|---|---|
| 上游仓库 | <https://gitlab.com/julien.cardoux/openwol> |
| 本 fork 仓库 | <https://github.com/lanxize/openwol> |
| 基线 | `master` @ `08cbf7e`（v1.0.3 / versionCode 4） |
| 本 fork 版本 | v1.1.0 / versionCode 5（已推送至 `master`） |
| 许可 | 上游自述 AGPLv3（F-Droid 标注 `AGPL-3.0-or-later`）；本 fork 同样以 AGPL-3.0 发布 |

---

## 目录

- [一、功能新增](#一功能新增)
  - [1. 控制中心快捷开关](#1-控制中心快捷开关qs-tile)
  - [2. 默认设备](#2-默认设备)
  - [3. 磁贴的双通道反馈](#3-磁贴的双通道反馈)
  - [4. 首次启动通知权限引导](#4-首次启动通知权限引导)
  - [5. 中文本地化](#5-中文本地化)
- [二、缺陷修复](#二缺陷修复)
- [三、构建配置调整](#三构建配置调整)
- [四、三个 Android 平台限制](#四三个-android-平台限制)
- [五、验证](#五验证)
- [六、已知限制](#六已知限制)
- [七、作为独立 fork 发布的调整](#七作为独立-fork-发布的调整)
- [八、若要提回上游](#八若要提回上游)

---

## 一、功能新增

### 1. 控制中心快捷开关（QS Tile）

不打开 App，在控制中心点一下即可唤醒默认设备。

| 场景 | 行为 |
|---|---|
| 有默认设备，且在 WiFi/VPN 上 | 发送魔法包，提示「开机指令已发送」，单次短震 |
| 一个设备都没有 | 提示「当前无可用设备」，双脉冲震动，**不发包** |
| 不在 WiFi/VPN 上 | 提示「网络状态错误」，双脉冲震动，**不发包** |
| 长按 | 打开 App 主页 |
| 标题 | `打开"设备名"`，跟随默认设备变化 |

「无设备」优先于「网络错误」——没配设备是更该先解决的问题。

实现要点：

- `WolTileService : TileService`，清单声明 `QS_TILE` action 与 `BIND_QUICK_SETTINGS_TILE` 权限
- 做成**瞬时按钮**而非开关：Wake-on-LAN 是单向 UDP、没有回执，做不出真实的"开机状态"，
  所以显式不声明 `META_DATA_TOGGLEABLE_TILE`，避免给出误导性的开关外观
- `onStartListening()` 每次面板展开时刷新标题，App 里换了默认设备磁贴会自动跟上
- 长按通过 `ACTION_QS_TILE_PREFERENCES` 实现：给 `MainActivity` 加该 action 的 intent-filter
- 图标复用 App 已有的闪电字形，新建 24dp 白色单色 `ic_tile_wol.xml`，系统按主题染色

### 2. 默认设备

磁贴需要知道"唤醒哪一个"，因此引入"默认设备"。

规则（单选、不可取消）：

- 只要有设备，就**必有且仅有一个**默认设备
- 添加的第一个设备自动成为默认
- 删除默认设备时，把列表里剩下的第一个顶上
- 编辑设备（改名 / IP / MAC）**不会**改变默认归属
- 卡片左侧一个单选圈，点击即切换

因为"不能取消选中"是硬规则，控件用 `RadioButton` 而非 `CheckBox`——后者的语义是"再点一下
取消"，与规则矛盾。

数据层在 `Device` 上加了 `isDefault: Boolean = false`。带默认值，旧数据可无痛反序列化；
kotlinx.serialization 默认跳过等于默认值的字段，所以这个标记对非默认设备不占存储空间。

### 3. 磁贴的双通道反馈

两条**互相独立**的通道，任何一条失效都还有另一条：

| 通道 | 权限 | 生效条件 |
|---|---|---|
| 震动 | `VIBRATE`（安装即授予，用户无法关闭） | 始终生效 |
| 文字 | `POST_NOTIFICATIONS` | 授权后生效 |

震动模式刻意做成可区分的：**成功**单脉冲（`0, 50`），**失败**双脉冲（`0, 45, 90, 45`）。

这样即便用户拒绝通知权限，也不会再出现"点了完全没反应"的假死感。两条通道各自为什么会被
系统限制，见第四节。

### 4. 首次启动通知权限引导

`POST_NOTIFICATIONS` 是磁贴文字反馈的**硬性前提**（原因见第四节），但用户没有理由自己知道
这件事。所以首次启动用一个说明弹窗讲清缘由，再走系统权限框，且**只问一次**：

- 仅在 `API ≥ 33`、未授权、且从未问过时触发
- 无论选「允许」还是「暂不」，都写入 `notification_permission_asked` 标记，不再打扰
- 顺带让自动化功能的通知（含那条常驻服务通知）也能显示——它们此前同样需要这个权限

### 5. 中文本地化

采用标准 Android 国际化做法，而不是把中文硬写进代码：

- `res/values/strings.xml` —— 96 条英文（默认，供非中文用户）
- `res/values-zh-rCN/strings.xml` —— 96 条中文，键一一对应

原先散落在 5 个 Kotlin 文件里的 84 条界面文案，加上新增功能的 12 条，共 96 条统一走资源：
Compose 用 `stringResource()`，Service 与工具函数用 `getString()`。跟随系统语言，中文设备上
全中文，英文设备上全英文。

App 名称 `OpenWOL` 保留未翻译（品牌名按惯例不本地化）。

---

## 二、缺陷修复

### 改 MAC 会切断自动化关联

`Automation.deviceIds` 存的是 **MAC 字符串**，等于把 MAC 当外键；而 `updateDevice()` 只更新
设备本身，不同步引用。改完 MAC 后规则的 `deviceIds` 仍指向旧 MAC，匹配不到任何设备——
**规则留在列表里但永远唤不醒任何东西，且没有任何提示**。

修法：MAC 变化时，把所有引用旧 MAC 的自动化改写为新 MAC。

### 悬空的设备引用

`removeDevice()` 原先只在"某条规则引用的设备**全部**被删光"时才删除该规则。混选规则（同时
关联 A、B）里删掉 A 后，A 的 MAC 会作为死引用残留下来——一旦同一个 MAC 被重新添加为设备，
这条规则会**诡异地自动复活**。

修法：删设备时从每条规则里摘掉它，摘空的规则一并删除。

### 磁贴的提示与震动在 App 未运行时被系统丢弃

**现象**：App 不在运行时点磁贴，魔法包发出去了，但既没有提示也没有震动。

**根因**：两条平台限制各管一路（详见第四节）——通知权限关闭时系统丢弃后台应用的 Toast；
默认的 `USAGE_TOUCH` 不在后台震动白名单里。

**为什么最初测试没发现**：我验证磁贴时总把 App 界面开在控制中心后面，那时它有可见界面，
两条限制都不触发。只有真正"App 没运行"时才会暴露。

修法：首次启动引导申请权限（见 1.4）+ 震动改用白名单内的用途（见第四节）。

---

## 三、构建配置调整

| 改动 | 原因 |
|---|---|
| 增加 `buildToolsVersion = "35.0.0"` | AGP 8.6 默认要 34.0.0，而本机 SDK 只装了 35.0.0，不 pin 会直接构建失败 |
| `.gitlab-ci.yml` 的 `ANDROID_BUILD_TOOLS` 同步为 `35.0.0` | 否则 CI 找不到；顺带让 build-tools 与 `compileSdk 35` 一致 |
| `versionCode` 4 → 5，`versionName` `1.0.3` → `1.1.0` | 新增功能走次版本号；`versionCode` 每次发版无条件 +1 |

> CI 用 `grep 'versionName' app/build.gradle.kts` 提取版本号来生成 tag `v1.1.0` 与产物路径，
> 所以这两个数字必须一起改。

---

## 四、三个 Android 平台限制

本节是本次改动中**最不显而易见**的部分，全部来自实测和 AOSP 源码，而非推测。

### 限制 1：通知权限关闭时，后台应用无法弹 Toast

Android 13（API 33）起，`NotificationManagerService` 会丢弃通知权限被关闭的应用的 Toast，
除非该应用当时有前台可见界面。磁贴由 SystemUI 绑定一个后台服务来响应点击，正好命中这条。

```
E NotificationService: Suppressing toast from package ... by user request.
```

**推论**：任何依赖 Toast 做磁贴反馈的实现，都必须持有 `POST_NOTIFICATIONS`。

三轮 A/B/A 对照（每轮都杀掉进程再点磁贴）：

| 轮次 | `POST_NOTIFICATIONS` | `Suppressing toast` | 结果 |
|---|---|---|---|
| A1 | 已授权 | 0 次 | ✅ 弹出 |
| B | 已撤销 | 2 次 | ❌ 被压制 |
| A2 | 再次授权 | 0 次 | ✅ 弹出 |

### 限制 2：未列入白名单的震动会被后台丢弃

AOSP `VibrationSettings.shouldIgnoreVibration()`：

```java
if (!mUidObserver.isUidForeground(callerInfo.uid)
        && !BACKGROUND_PROCESS_USAGE_ALLOWLIST.contains(usage)) {
    return Status.IGNORED_BACKGROUND;
}
```

白名单内容：

```java
BACKGROUND_PROCESS_USAGE_ALLOWLIST = {
    USAGE_RINGTONE, USAGE_ALARM, USAGE_NOTIFICATION,
    USAGE_COMMUNICATION_REQUEST, USAGE_HARDWARE_FEEDBACK,
    USAGE_PHYSICAL_EMULATION };
```

**默认的 `USAGE_TOUCH` 不在名单里**，而 `USAGE_PHYSICAL_EMULATION`（模拟物理按键）在——
语义上也恰好贴合控制中心磁贴这个"虚拟按钮"。改动前系统日志是 `ignored_background`，改用
该用途后变为 `finished`。

API 等级（查 SDK 的 `api-versions.xml` 确认）：`VibrationAttributes` 类与
`USAGE_PHYSICAL_EMULATION` 是 **30**，`vibrate(VibrationEffect, VibrationAttributes)` 重载
是 **33**，所以代码做了版本分支。

### 限制 3：磁贴的 `onClick` 只在面板打开时投递

面板关闭时，SystemUI 收到点击只记录一条日志就丢掉：

```
CustomTileExt: handleClick: ..., pendingBind: false
```

这也解释了为什么自动化测试磁贴必须先 `cmd statusbar expand-settings` 再 `click-tile`。

---

## 五、验证

设备：Xiaomi 2509FPN0BC / HyperOS 4.0 / Android 17 (API 37) / arm64-v8a。
`assembleDebug` 与 `assembleRelease`（含 `lintVital` + R8 + 资源压缩）均通过。

| 验证项 | 结果 |
|---|---|
| 首个设备自动成为默认 | ✅ 落盘 `isDefault:true` |
| 新增设备不抢默认 / 双向切换默认 | ✅ |
| 删默认设备自动补位 | ✅ 剩余第一个被提升 |
| 点单选圈不误触发发包 | ✅ 无「Packet Sent」浮层 |
| 改 MAC 后自动化关联跟随 | ✅ `deviceIds` 同步更新 |
| 磁贴标题 | ✅ `content-desc="打开"PC""` |
| 三种提示 + 长按进主页 | ✅ |
| 提示在**进程已死**时仍显示 | ✅ `Suppressing toast = 0` |
| 成功 / 失败震动 | ✅ 单脉冲 `[0,50]` / 双脉冲 `[0,45,90,45]` |
| 首次启动权限引导 | ✅ 弹窗 → 系统框 → 授权；重启不再询问 |

**魔法包内容**（在局域网内抓包确认）：

```
头部 FF FF FF FF FF FF | 目标 MAC D4:93:90:1A:47:C6 | 16 次重复一致 | 102 字节
```

**最终验证**：把 PC 关机后在控制中心点一下磁贴，**机器正常开机**。这是整条链路的终极证明——
磁贴点击 → 生成魔法包 → Wi-Fi 广播 → 目标网卡识别 → 主板上电。前面所有技术项验证的都只是
链路中的某一环。

---

## 六、已知限制

1. **小米新版控制中心不显示磁贴图标下方的文字。** `打开"设备名"` 这个标题在 HyperOS 上肉眼
   看不到（已通过无障碍树确认它确实被设置了）。换成经典样式通知栏或 AOSP 设备即可见；标题
   过长时系统会自动截断。

2. **Wake-on-LAN 没有回执。** 「开机指令已发送」只代表包已交给网络栈，不代表设备真的开机了
   ——这句话在协议层面依然成立（UDP 无确认），本次虽已实测唤醒成功，但应用本身**无法知道**
   结果。磁贴因此做成瞬时按钮，做成开关会是欺骗性的。这是上游既有语义，本次未改动。

3. **`sendWakeOnLan()` 会吞掉所有异常。** 发送失败时 UI 与磁贴仍提示成功。要更严谨的反馈需要
   让它返回结果，属于独立改动。

4. **多磁贴未实现。** 一个 App 可以声明多个 TileService 组件，从而做到"每个设备一个开关"；
   本次只做了"默认设备"单开关。

---

## 七、作为独立 fork 发布的调整

| 改动 | 内容 | 原因 |
|---|---|---|
| `applicationId` | `com.juliencardoux.openwol` → **`io.github.lanxize.openwol`** | 沿用上游包名会与官方版冲突（签名不同，更新必须先卸载重装），且占用了原作者的反向域名命名空间 |
| `LICENCE` → `LICENSE` | 重命名，内容一字未改 | GitHub 与多数工具链的默认拼写；已确认仓库内无任何地方按文件名引用 |
| `README.md` 顶部 | 中英双语 fork 声明 | 明确这是社区修改版、与原作者无隶属关系，避免被误认为官方发布 |
| `README.md` 许可证段 | 补充版权归属 | 上游仓库**没有任何版权行**，AGPL §5(b) 要求保留或补充归属声明 |
| 12 个被改文件 | 文件头加 `Modified by lanxize in 2026-10. See CHANGES.md for details.` | AGPL §5(a)「prominent notices stating that you changed the files and the date」 |

**刻意没改 `namespace`**（仍是 `com.juliencardoux.openwol`）：它只决定生成 R 类的包名，对用户
完全不可见；改它需要移动整个源码目录并重写所有 `package` 声明，收益不足以抵消改动风险。

**换包名带来的两个副作用**：

1. 它是手机上一个全新的 App，与旧包名并存，二者数据不共享
2. 控制中心里已添加的磁贴会失效（组件名变了），需要在控制中心里重新添加

---

## 八、若要提回上游

1. **`README.md` 同步**：Features 一节加入快捷开关与中文；Permissions 表格补
   `POST_NOTIFICATIONS` 并说明用途；How to Use 加入"如何把磁贴拖进控制中心"。
2. **`fastlane/metadata/android/`**：目前只有 `en-US`，可补 `zh-CN` 商店元数据。
3. **`sendWakeOnLan()` 返回结果**：让 UI 与磁贴能区分"发出去了"和"发送失败"。
4. **把 MAC 当外键这件事**：本次只在改 MAC 时同步引用，治标。更彻底的做法是给 `Device` 加一个
   稳定的 UUID 主键、自动化引用 UUID 而非 MAC，但那需要一次数据迁移。
