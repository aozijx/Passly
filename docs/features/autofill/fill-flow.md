# Autofill 共享填充流程

状态：当前实现（代码核对：2026-10-20）。

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

Legacy 可用 `HeuristicMatchStrategy` 识别非标准表单；Credential Manager 使用自己的严格 option/request 解析。
平台 adapter 不直接依赖 Entry repository 或认证实现。

需要解锁或设置要求每次验证时，`AuthorizeAutofillInteraction` 以 `AuthenticationPurpose.AUTOFILL` 授权同一个
interaction。并发授权合并为一次认证；成功后重新发现或最终选择不再次认证。interaction 绑定来源、规范化作用域和
实际展示过的 Entry IDs，不是按包名授予的时间窗口。

## 解析秘密

`ResolveAutofillSelection` 依次验证：入口仍启用、interaction 存在且已授权、最终调用方作用域相同、Entry ID
确实展示过、Credential Manager allowed user 仍通过。之后 `AutofillCredentialResolver` 只读取选中条目的密码。
请求需要 OTP 时，才沿 `OTP_FOR` 关系读取独立加密 secret 并生成验证码。

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

平台 Activity 必须在 `AutofillResultDelivery.commitResult` 的提交回调内连续执行 `setResult` 和 `finish`；仅调用
`setResult` 只是保存待返回值，不表示 Android 已收到结果。提交开始即关闭本轮 lease 的借用窗口，`finish` 返回后才在
不可取消收尾中调用 `CompleteAutofillSelection`。首次终态转换记录使用活动并释放 lease；重复回调幂等。

最后一个已提交结果完成后，仍未向 Android 提交结果的列表、认证和选择 interaction 全部过期，再立即回收临时会话。
关闭期间的新请求等待该会话完成锁定后再创建 interaction，不能落入“旧数据库仍开着但已禁止借用”的中间态。
多个已经提交的 Legacy/Credential 结果分别完成，不互相误标为过期。

这保证 Android 收到结果以前不会关库，同时填充完成后再次点击输入框会从全新锁定状态开始。Android 没有确认字段
实际写入的回调时，日志不能伪造 `field_filled`。

## 临时会话

`AutofillAuthenticated` 只允许候选、单条秘密解析、使用记录和已确认凭据保存，不挂载普通 Vault。多个已授权
interaction 可以借用同一个临时数据库会话，但不共享候选或选择权限。任一凭据成功交付会结束这一轮临时授权；
遗留 borrower 不得把数据库继续保持为解锁状态。

每个 interaction 有 60 秒 process-local 超时。进程死亡后 extras 不能恢复授权；系统必须重新发起请求。主 App
完成普通解锁后会话提升为 `Authenticated`，迟到的 Autofill 释放不得锁掉它。

相关实现：

- [`DiscoverAutofillCandidates`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt)
- [`ResolveAutofillSelection`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt)
- [`AutofillResultDelivery`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt)
- [`AutofillInteractionStore`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt)
