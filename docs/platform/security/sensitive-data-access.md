# 敏感数据读取、显示与剪贴板

状态：部分完成；本文明确当前边界，不把待办写成既有能力。

## 认证语义

“复制敏感数据时验证”位于隐私设置，语义为：

- 直接复制隐藏字段：按全局开关决定是否要求新鲜认证；
- 显示高敏感字段：始终按字段级别验证；
- 字段已在当前详情页会话中验证并显示：随后复制复用这次显示授权，不重复弹窗；
- 字段再次隐藏、离开详情页、应用锁定或敏感状态清理后，显示授权失效。

显示授权只存在于 `DetailUiState.revealedFields`，不进入 SavedState、数据库或 Intent。`DetailViewModel` 以 `DetailRevealStore` 持有对应 `SensitiveValue` 的擦除责任：隐藏单字段、复制后清除整组、条目访问关闭、会话撤权及 ViewModel 清理时调用 `remove()` 或 `clear()`；替换同一字段时先擦除旧值。UI 状态只是展示投影，Store 管理实际敏感值的清理生命周期。

## 当前读取边界

secret 数据位于 `entry_secret_fields`：敏感字段（`PASSWORD`、银行卡号、CVV、支付 PIN 等）按
`entryId + fieldKey` 每字段一个独立 AES-GCM 密文行，低敏结构聚合为 `STRUCT_BUNDLE` 行。
普通详情读取通过 `EntryQueryRepository.getById()` 只解密 `STRUCT_BUNDLE`，字段级值在领域模型中为
`null`。显示字段必须走 `SensitiveFieldRepository.revealMany()` 按需读取对应密文行，并且只能由
完成 reveal 认证的流程调用。

字段级密文行的 AAD 绑定 `entryId + fieldKey`，因此解密 `PASSWORD` 不会加载同条目其他字段的密文；
`readField`/`reveal` 只解密请求的字段，不存在"解一个字段带出整条 secret"的捆绑暴露。完整凭据
快照不通过 `SensitiveFieldRepository` 暴露。Backup 经 App `AppBackupSnapshotGateway.read(options)` 请求
Domain `VaultBackupStore.read(options)`；Data `RoomVaultBackupStore.read(options)` 以
`databaseSession.queryFullVault(onDenied = { throw SessionModeRestricted() }) { ... }` 拒绝不允许完整读取的会话，并调用
`SecretFieldStore.readAll(db, entryId, entryType)` 组装完整凭据。Revision 使用自己的 permit-gated restore
流程，字段 reveal、复制与 QR 则按字段授权读取，不借用完整快照接口。

字段级高敏键包括 `PASSWORD`、银行卡号、CVV、支付 PIN、身份证号、助记词、恢复码、SSH 私钥/口令、
Passkey 私钥引用和 OTP Secret。它们不会在普通详情读取时进入完整明文聚合；详情页只把本次获授权且当前
UI 类型实际支持的字段放入 `DetailUiState.revealedFields`。新增 Entry 类型不能因为 Domain 已声明字段就假定
编辑、详情、reveal 和复制链路已经完整，必须逐类型验证完整生命周期。

完整表单编辑当前仅支持 Login、OTP、Bank Card、Wi-Fi。编辑加载会先确认完整会话和 Repository 中的真实类型；
对存在的 Login 密码、OTP Secret、银行卡号、CVV、付款 PIN 作为一个字段集合申请一次 `REVEAL` 授权，
缺失、重复、额外或部分返回都会拒绝整个表单，不用空字符串替代未读取的高敏值。成功回填后记录 `VIEW` 活动，
并在编辑期间绑定同一个认证对象；锁定、会话切换或页面关闭会取消待处理加载并清空表单。Wi-Fi 密码在加密 bundle 中，
仅经完整会话读取。其他 EntryType 不提供完整表单编辑。

Wi-Fi 密码的详情显示、复制和二维码导出当前采用不同授权路径。`WifiEntryDefinition` 将 `PASSWORD` 定义为
`SECRET`，并保存在加密 bundle；详情字段 reveal 从已解锁的 Entry 读取该值，不申请字段级 `REVEAL` permit。
复制因该字段没有独立敏感存储键，走全局 `COPY_SECRET` 授权；二维码导出走全局 `REVEAL_SECRET` 授权，
并在授权范围内读取 bundle。这里记录的是当前实现，不预设这些路径是否应统一；是否要求 Wi-Fi 内联显示进行新鲜认证
仍待产品/安全策略裁定，见[安全扫描遗留问题 Task 1](../../project/work/plans/2026-11-06-passly-security-scan-followups.md#task-1)。

编辑表单清空表示失去 Store/UI state 对对象的引用，不等于 JVM 字符串内存的确定擦除。`LoadEditableEntryUseCase.invoke()`
把每个 `SensitiveValue` 先转换为临时 `CharArray`，再通过 `String(chars)` 放入 Entry/表单；`finally` 只覆盖临时数组，
不可变 String 的副本由运行时回收。锁定和页面关闭会尽快释放这些可控引用，但不声称覆盖所有 JVM 副本；降低敏感值在
字符串模型中的驻留范围是开放的内存所有权评估，见[维护 TODO Task 42](../../project/work/plans/2026-10-30-passly-maintainability-todo.md)。

## 敏感输入生命周期

密码、恢复码和其他用于认证或验证的输入只能由当前操作的状态所有者持有。输入回调产生的 `String` 无法可靠擦除；应尽量
缩短它在 UI 与状态中的引用时间，并用可清零的 `SensitiveValue`/`OwnedChars` 保存由应用拥有的副本。派生的 `CharArray` 必须
在 `finally` 中清零；替换、提交、取消、页面退出和拥有者销毁时，也必须清零旧的可变缓冲并从状态中移除引用。

认证/验证进入终态后，UI 状态只保留结果、错误类型或重试提示，不得继续携带刚提交的凭据。此规则适用于成功、拒绝、普通
异常和取消；如果重试需要凭据，要求用户重新输入。结果提示的延迟消失只控制反馈显示时间，不能延迟敏感值清理。Activity
暂停、对话框取消或宿主失效时，待处理的认证输入也必须被取消并释放。

仍处于活动编辑中的设置草稿可为便于修正而暂存，但不得混入终态认证结果；草稿在提交后应立即从编辑状态移除，并在关闭、
取消、页面离开或 ViewModel/Composable 所有者销毁时清理。禁止把凭据写入 `SavedStateHandle`、持久化 UI 状态、日志或诊断事件。
这些清理只能缩短可控引用和可变缓冲的寿命，不代表 JVM 中不可变 `String` 或运行时副本可被确定擦除。

当前落实位置：设置页恢复码验证终态不再持有输入，见
[`RecoveryCodeSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt)；
解锁页认证在所有终态清理密码/恢复码，见
[`UnlockViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/unlock/presentation/UnlockViewModel.kt)；
Activity 暂停时取消认证弹窗请求，见
[`AuthenticationHost.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/host/AuthenticationHost.kt)。
新密码设置在提交后清空编辑态输入，见
[`UnlockViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/unlock/presentation/UnlockViewModel.kt) 与
[`RecoveryModeViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/recovery/presentation/RecoveryModeViewModel.kt)。

## 剪贴板

Passly 复制敏感内容时使用敏感剪贴板标记。应用进入后台或详情页关闭时，只清除仍由 Passly 写入的剪贴板；
前台最长保留 60 秒后自动清除。所有权检查避免误删用户随后从其他应用复制的内容。
新剪贴板发布与过期任务替换共用串行边界；过期回调校验所有权并清除时也持有该边界，避免旧计时器清掉新内容或新内容失去过期任务。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 声明字段读取授权端口 | `SensitiveFieldRepository` | [`SensitiveFieldRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/SensitiveFieldRepository.kt) |
| 按字段读取或揭示加密内容 | `RoomSensitiveFieldRepository.revealMany()`、`SecretFieldStore.readField()` | [`RoomSensitiveFieldRepository.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/RoomSensitiveFieldRepository.kt)、[`SecretFieldStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt) |
| 详情页按字段访问控制 reveal | `RevealEntryFieldsUseCase.reveal()` | [`RevealEntryFieldsUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/RevealEntryFieldsUseCase.kt) |
| 复制字段时选择字段级或全局授权 | `CopyEntryFieldUseCase.invoke()` | [`CopyEntryFieldUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/CopyEntryFieldUseCase.kt) |
| 导出 Wi-Fi 与 OTP 二维码 | `ExportEntryQrUseCase.invoke()` | [`ExportEntryQrUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/ExportEntryQrUseCase.kt) |
| 为完整表单读取并授权现存高敏字段 | `LoadEditableEntryUseCase.invoke()`、`Entry.withRevealedField()`、`SensitiveValue.toTransientString()` | [`LoadEditableEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/LoadEditableEntryUseCase.kt) |
| 驱动详情页字段揭示状态 | `DetailViewModel`、`DetailRevealStore` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRevealStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRevealStore.kt) |
