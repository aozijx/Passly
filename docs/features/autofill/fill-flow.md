# Autofill 共享填充流程

状态：当前实现（代码核对：2026-10-31）。

## 应用内部输入边界

Passly 自身的认证、备份密码、恢复及凭据编辑输入不作为系统填充目标。
Core 的 `Modifier.excludeFromAutofill()` 在真实可编辑节点声明 `ContentDataType.None`，明确表示不可填充；
不清除文本编辑、焦点或无障碍语义。密码显示切换不改变此策略。公共输入组件和直接使用的 Compose 文本框遵守同一约束。
五个 Activity 宿主在创建时调用同名的 Activity 扩展：以 `NO_EXCLUDE_DESCENDANTS` 排除自身 View 子树，并取消当前宿主的填充/保存会话。
只覆盖 Passly 内部输入，不停用系统 Autofill 服务，不取消其他应用的会话，也不改变对外候选发现和结果交付。
应用密码、备份密码、编辑页实际语义节点及宿主排除标记由 `InternalInputAutofillTest` 验证；编辑页原有 IME 焦点测试继续保留。

Android 12+ 有三套平台入口：Legacy `AutofillService` 在 API 31+ 提供填充、`SaveInfo` 和 `onSaveRequest`；
Credential Manager Provider 在 API 34+ 提供 Password get/create/clear-state。两者共享平台无关的候选发现、
授权、单条秘密解析、草稿校验和 Entry 写入，但结果合同各自独立，不互相转换 `Dataset/FillResponse/SaveCallback`
和 Credential Manager response。平台细节见[Legacy Service](legacy-service.md)与
[Credential Manager Provider](credential-manager.md)，保存现状与目标见[保存流程](save-flow.md)。

第三套入口是用户自行在 Android 无障碍设置中启用的手动回退。系统 Autofill 未显示时，用户点击 Passly 的
浮动按钮，服务才读取当前可编辑节点；它复用候选发现、认证和单条解析，但通过 `ACTION_SET_TEXT` 交付，
不参与 Legacy 保存或 Credential Manager response。Legacy 的 compatibility package 由 Android 框架提供
兼容处理，不等于 Passly 无障碍服务。
选择页先关闭，原窗口重新成为活动窗口后才执行写入；待交付凭据只在进程内暂存至多 10 秒，过期取消 interaction。
无障碍 Service 通过 `AutofillLaunchTarget` 请求不含 token 的显式 Activity Intent；选择页从同进程的单次
会话存储读取请求，具体页面类只由 App 平台适配器引用，Feature 不依赖 Presentation。
服务任务在协程启动前或执行期间取消时，都会清除本次暂存请求和 trace continuation，并继续传播取消；普通启动错误才记录为回退失败。

| 入口与展示 | 触发 | 候选 UI | 交付 | 边界与回退 |
| --- | --- | --- | --- | --- |
| Legacy 悬浮 | 系统 `FillRequest` | 系统下拉菜单 `RemoteViews` | `Dataset` | 用户选“悬浮候选”或键盘不支持 Inline 时使用 |
| Legacy Inline | 系统 `FillRequest` 且键盘提供 Inline 规格 | 键盘候选条 `InlinePresentation` | 认证后 `Dataset` | 受键盘最大数量和规格约束；无请求时回到系统菜单 |
| Legacy BottomSheet | 系统 `FillRequest` | 认证入口打开应用内 `ModalBottomSheet` | 选中后 `Dataset` | 无系统请求则不会出现 |
| 无障碍浮动按钮 | 用户点按钮 | 应用内候选选择器 | 返回原窗口后 `ACTION_SET_TEXT` | 仅针对原窗口和包名，目标 30 秒过期，待交付最多 10 秒 |
| Credential Manager | Android 14+ 凭据请求 | 系统凭据 UI | Provider response | 不受 Legacy 展示设置控制 |

这里的“悬浮候选”指系统 Autofill 下拉菜单；“无障碍浮动按钮”是独立的手动触发入口。Android 是否最终显示
Inline 仍由输入法决定。无障碍入口不尝试根据浏览器包名猜网页来源，网页没有可验证 origin 时不会扩大作用域。

## Legacy 结构解析与字段分类

`AutofillStructureParser` 只负责从 Android `AssistStructure` 抽取可编辑字段的静态元数据和值，不判断
username/password/OTP 语义。`LegacyPlatformAdapter` 将结果映射为平台无关的 `AutofillRequest`，随后仅由
`HeuristicMatchStrategy` 的规则链分类字段，避免解析层和策略层各维护一套关键词规则。

结构遍历在 Autofill 后台协程中执行，并使用迭代式先序遍历；单次请求最多访问 10,000 个节点、最多下探 64 层，
避免复杂页面或异常 WebView 造成回调线程卡顿或递归栈溢出。限制不会改变限制范围内的窗口和节点顺序。

提交按钮不作为 Legacy 保存协议的输入。`SaveInfo` 只声明已分类的密码字段、可选用户名字段，并使用
`FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE` 判断表单离场，因此不恢复无消费方的 `submitId`。

保存草稿不会再以 `roleMap` 或节点的偶然顺序覆盖候选：每种角色优先唯一的聚焦非空字段，否则只接受唯一非空字段。
多个未聚焦密码候选会拒绝保存；多个未聚焦用户名候选会留空，避免把不同表单的用户名和密码拼成一条凭据。

## 发现与授权

`DiscoverAutofillCandidates` 原子读取当前设置、规范化目标作用域、创建/恢复 interaction，并只查询
`AutofillCandidateSummary` 元数据。候选阶段不读取 Entry 聚合、密码或 OTP secret。

Legacy 和手动无障碍入口可用 `HeuristicMatchStrategy` 识别非标准表单；Credential Manager 使用自己的严格 option/request 解析。
平台 adapter 不直接依赖 Entry repository 或认证实现。

需要解锁或设置要求每次验证时，`AuthorizeAutofillInteraction` 以 `AuthenticationPurpose.AUTOFILL` 授权同一个
interaction。并发授权合并为一次认证；成功后重新发现或最终选择不再次认证。interaction 绑定来源、规范化作用域和
实际展示过的 Entry IDs，不是按包名授予的时间窗口。

## 解析秘密

`ResolveAutofillSelection` 依次验证：入口仍启用、interaction 存在且已授权、最终调用方作用域相同、Entry ID
确实展示过、Credential Manager allowed user 仍通过。之后 `AutofillCredentialResolver` 只读取选中条目的密码。
请求需要 OTP 时，才沿 `OTP_FOR` 关系读取独立加密 secret 并生成验证码。
Base32 secret 的字符、补位和尾部位由 Domain 的 `OtpSecretCodec` 统一校验；`OtpGenerator` 遇到损坏密钥时
返回 `InvalidSecret`，不生成或填充 OTP 码。

## 结果与关闭顺序

Legacy Activity 的输入解析和领域请求映射分别由 `AutofillFillRequestParser`、`AutofillFillRequestMapping` 负责；
Activity 只编排平台结果，ViewModel 只编排授权、发现和选择。Legacy 结果用 `DiscoveryResponse`、
`SelectedResponse`、`SelectedDataset`、`Cancelled` 明确区分，不使用 nullable receipt 猜测是否需要关闭临时会话。
Credential Manager 同样用 `Immediate` 与 `CommittedSelection` 区分普通平台响应和已选择凭据。

`LegacyAutofillResultTarget` 是 Legacy 启动协议的一部分：解锁发现返回 `RESPONSE`，单候选和底部候选选择返回
`DATASET`。PendingIntent 工厂在创建入口时写入显式稳定字符串，解析器拒绝缺失或未知值；Compose 底部列表直接传递
类型化 `EntryId`，不再经过字符串往返转换。该目标只决定 Android 平台结果形态，不进入 domain 候选数据结构。

Compose 路由将工作流状态映射为显式的 `Hidden` 或 `Candidates` 显示状态；终态提交属于独立副作用，不再通过
nullable 候选列表或组合函数提前返回表达“当前不显示 UI”。

平台 Activity 在 `AutofillResultDelivery.commitResult` 的提交回调内连续执行 `setResult` 和 `finish`；仅调用
`setResult` 只是保存待返回值，不表示 Android 已收到结果。提交开始即关闭本轮 lease 的借用窗口，平台 callback
成功返回后才异步调用 `CompleteAutofillSelection` 并释放 lease。首次终态转换记录使用活动；重复的交付完成仍幂等。

Credential Manager Route 只在 Activity 处于 `STARTED` 时消费进程内缓冲的单次结果事件；结果 Intent 不保存在 UI
StateFlow 或 SavedState。Credential 与 Legacy Activity 均用 `ActivityResultClaim` 约束实例内重复提交。平台 callback
失败会释放 claim：Credential Manager 取消 interaction 并返回 `RESULT_CANCELED`，Legacy Autofill 取消 ViewModel
工作流并返回取消结果，`onDestroy()` 不会因残留的防重标志跳过清理。callback 已成功后，日志/trace 失败不会触发
第二次平台提交或把成功结果覆盖为取消；结果已交给 Android 后继续完成 interaction 与 lease 收尾。

最后一个已提交结果完成后，仍未向 Android 提交结果的列表、认证和选择 interaction 全部过期，再立即回收临时会话。
关闭期间的新请求等待该会话完成锁定后再创建 interaction，不能落入“旧数据库仍开着但已禁止借用”的中间态。
多个已经提交的 Legacy/Credential 结果分别完成，不互相误标为过期。

最后一个 Autofill borrower 释放时，lease 保留临时会话所有权直到 `SessionLockController.lock()` 返回；锁请求在
不可取消收尾中执行。失败会立即反馈给当前调用，并将 pending obligation 交给 lease 的进程级 retry owner；重试
采用 1 秒起步、最多 60 秒的指数退避，等待期间不持有 lease mutex。新交互的 readiness gate 在 pending lock
未完成时拒绝继续，Autofill 不会借用尚未锁定的临时会话。因此取消发生在等待 session mutex 之前或期间都不能
静默丢失锁定责任。Accessibility Activity 的 `onDestroy()`
只发起 ViewModel 清理，不在主线程 `runBlocking` 等待数据库封存；清理进入 `NonCancellable + Dispatchers.IO`
后可跨 ViewModel 清除继续完成。

这保证 Android 收到结果以前不会关库，同时填充完成后再次点击输入框会从全新锁定状态开始。Android 没有确认字段
实际写入的回调时，日志不能伪造 `field_filled`。

## 临时会话

`AutofillAuthenticated` 只允许候选、单条秘密解析、使用记录和已确认凭据保存，不挂载普通 Vault。多个已授权
interaction 可以借用同一个临时数据库会话，但不共享候选或选择权限。任一凭据成功交付会结束这一轮临时授权；
遗留 borrower 不得把数据库继续保持为解锁状态。

每个 interaction 有 60 秒 process-local 超时。进程死亡后 extras 不能恢复授权；系统必须重新发起请求。主 App
完成普通解锁后会话提升为 `Authenticated`，迟到的 Autofill 释放不得锁掉它。

平台依据：[Autofill 与 IME Inline 集成](https://developer.android.com/identity/autofill/ime-autofill)、
[AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)、
[AccessibilityNodeInfo](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 发现可填充凭据候选 | `DiscoverAutofillCandidates` | [`DiscoverAutofillCandidates.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt) |
| 解析用户选择并准备填充结果 | `ResolveAutofillSelection` | [`ResolveAutofillSelection.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt) |
| 向平台提交 Autofill 结果 | `AutofillResultDelivery.commitResult()` | [`AutofillResultDelivery.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
| 维护交互授权、候选与结束状态 | `AutofillInteractionStore.create()`、`beginAuthorization()`、`recordCandidates()`、`finish()` | [`AutofillInteractionStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt) |
| 保留、重试临时 Autofill session 锁责任 | `AutofillUnlockLease.release()`、`awaitReadyForInteraction()` | [`AutofillUnlockLease.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillUnlockLease.kt) |
| 异步结束无障碍选择页工作 | `AccessibilityFillActivity.onDestroy()`、`AccessibilityFillViewModel.cancel()` | [`AccessibilityFillActivity.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillActivity.kt)、[`AccessibilityFillViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillViewModel.kt) |
| 无障碍填充事件与服务清理 | `ManualAccessibilityFillService.onAccessibilityEvent()`、`onDestroy()` | [`ManualAccessibilityFillService.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/accessibility/ManualAccessibilityFillService.kt) |
