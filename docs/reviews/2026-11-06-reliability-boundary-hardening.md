# 2026-11-06 运行可靠性边界修复记录

## 范围

本记录归档维护 TODO Task 60–63、65–69 的完成证据。当前行为合同仍以各功能主题页为准；记录中的测试结果均为对应实施时结果，本次文档整理没有重跑测试。

## 诊断日志与导出（Task 60）

- `EncryptedLogRecordStore.readPageLocked()` 到达分页边界时调用 `nextFileCursor()`；旧实现通过 `runCatching { manager.readHeader(...) }.getOrNull()` 吞 fatal。现在仅把可恢复 `Exception` 视为缺失 cursor，`Error` 继续传播。`EncryptedLogFileManager.countRecords()` 同样区分普通损坏/IO 异常与 fatal。
- emergency writer 在失败清理中删除未完成 staging file、擦除 plaintext buffer、销毁 key entry 并移除线程登记；清理 fatal 不覆盖较早 fatal。队列重试只消费 `RejectedExecutionException`，同步 join 只消费并恢复 `InterruptedException`。
- `ObservabilityExportService.export()` 读取记录与 `exportToDocument()` SAF 写入把普通异常保留为 `Result.failure`；取消继续传播，fatal 在清理必要临时文件/目标文档后传播。`AndroidDiagnosticsExportStorageGateway.share()` 的临时文件清理与延迟删除调度按相同错误优先级处理；成功分享后的 URI 不会因取消/fatal 删除。
- crash diagnostics 次级失败以 suppressed 附到原始崩溃错误并继续委托原 handler；shutdown 仍完成 handler 恢复和 store close，再传播 fatal。

历史验证：`:data`、`:app` observability 定向测试覆盖跨文件 unwrap、损坏记录、emergency cleanup、导出读取/分享/SAF 写入与删除中的普通异常、取消和 fatal；KtLint、`verifyArchitecture`、`git diff --check` 通过。

## 持久化键与 Autofill 归一化（Task 61–62）

- `SharedPreferencesPermissionRequestHistory.wasRequested(permission)` / `markRequested(permission)` 使用 `RuntimePermission.requestHistoryKey`，保留 `CAMERA`、`POST_NOTIFICATIONS` 的旧存储键。`PermissionRequestHost` 仍以 `requestedBefore && !canAskAgain` 判定永久拒绝。
- 持久化 enum 审查把 OTP `SecretPayload` 的 `OtpType`、`OtpHashAlgorithm`、`OtpSecretEncoding` 改成稳定字符串字段，经 `StorageEnumMappings` 穷尽映射，原 JSON 字符串值不变；诊断 schema 1 导出通过显式 `exportValue` 写出日志枚举值。只用于当前 draft/UI 或开放诊断文本的 `.name` 保持原用途。
- Domain `AutofillScope` 是 package/application ID 与 web host 的规范化 owner。`AutofillGrantContext.normalized()`、交互创建/比较、Data 查询及保存关联共用 trim、`Locale.ROOT` lowercase 和无效值拒绝规则。Host 精确匹配，忽略 scheme/port/path，保留 `www` 差异；DNS 尾点、IDN、bracketed IPv6 和 zone ID 按显式合同处理。
- 候选查询对 DAO 子串预筛结果做完整精确 canonical host 匹配，避免近似结果截断精确命中；网站域名保存只写 DOMAINS/PRIMARY_URL，不叠加浏览器 package association。`EntryVisualCategoryClassifier`、OTP Base32、Wi-Fi、银行卡、标签、Favicon URL 和 UI 搜索继续使用各自语义，不抽取通用字符串规范化桥。

历史验证：权限旧键读写与 enum 映射测试、Autofill Domain/Discover/Credential Manager/selection/Data exact-host/save mapping 测试，以及 KtLint、架构检查通过。Autofill 全量精确过滤可能增加本地候选行读取数量，实施时未做性能测量。

## 认证失败、恢复 journal 与附件复用（Task 63、65）

- `DefaultAuthenticationManager.authenticate()` 将凭据消费放在最外层，覆盖 trace、slot 忙拒绝、会话复用、状态切换、executor 与失败恢复；`recoverAuthenticationFailure()` 恢复 session 后只将普通 `Exception` 映射为 `SESSION_TRANSITION_FAILED`，取消在 `NonCancellable` 恢复后传播，fatal `Error` 恢复后传播。`withSlot()` 与 Host lease 各自由 `finally` 释放。`consumeRecoveryCodeOrRollback()` 保留 fatal 删除/回滚优先级和 suppressed 关系。
- `RestoreFileJournal.replace()` 在暂存旧文件后记录 `(target, temp, previous)`，失败补偿不完整时保留 change；`rollback()` 逐项确认 delete/rename，只移除已恢复项。`RoomBackupSnapshotRestorer` 仅在 Room transaction 失败时 rollback；成功提交后的 journal/未引用资源清理处于独立阶段，普通清理失败不伪装成数据库回滚。
- 附件复用先解密现有密文，再校验 keyed content ID、归档明文长度和字节；损坏密文或 ID 不符时从已验证归档内容重新加密并经 journal 替换。明文/密文临时数组在清理路径擦除。
- 限制：journal 没有持久化启动恢复记录；进程在跨介质提交窗口强制退出时，不承诺自动恢复。失败 `.previous` 由后续成功的 `OVERWRITE` 资源扫描回收。

历史验证：认证定向 security unit tests、journal/附件 JVM 故障注入、`:app:ktlintCheck`、`verifyArchitecture` 与 `git diff --check` 通过；真实 Keystore/设备及 Room/磁盘崩溃场景未验证。

## Flow、Favicon 与 SAF 错误分类（Task 66–68）

- `TrashViewModel.observeTrash()` 的 `Flow.catch` 仅映射普通 `Exception`，并先核对 collector active；`CancellationException` 和 fatal `Error` 原样传播。普通查询失败进入 `Failed`，`RetryLoad` 重新订阅冷 Flow。
- `DefaultFaviconImageProcessor` 只将普通 `Exception` 转为 `FaviconProcessingException`；转换失败先清理 `.webp` 输出，`FaviconImageStore.stage()` 读取失败清理 `.input` 后传播主错误。清理次错按 fatal/cancellation/普通异常优先级合并。
- `BackupExportStorageSupport.createNamedExportTarget()`、`deleteDocument()`、`checkDirectoryWritable()` 仅把可恢复异常映射为 `Result.failure` / `false`。SAF 创建目标、探测文件和导出目标清理保持主错与 cleanup 错的优先级；取消/fatal 下已登记文档进入 `NonCancellable` 清理。

历史验证：Trash ViewModel 普通失败/重试、独立取消和 fatal 用例通过；Favicon JVM 及 AndroidTest 编译覆盖错误映射和 staging cleanup；Backup SAF create/write/delete/cleanup JVM 用例、`:app:ktlintCheck`、`verifyArchitecture` 通过。设备上的 AndroidTest 和真实 DocumentsProvider/ContentResolver 集成未运行。

## Diagnostics 页面 owner（Task 69）

`DiagnosticsViewModel.refresh()` 调用 `logStore.readLines(MAX_VIEW_LINES)`；普通异常现在结束 Loading、设置 `isRefreshFailed` 并提供重试，手动刷新失败保留旧行；成功、会话受限和清理成功清除失败态，取消/fatal 传播。`clearLogs()` 单飞执行；页面退出关闭旧确认框但不取消已确认的存储清理，完成后只有原页面 owner 能发布状态或 effect，避免旧页面结果写入重进页面。

历史验证：Diagnostics ViewModel 15 项、Reducer 5 项定向测试，`:app:ktlintCheck`、`verifyArchitecture`、独立 review re-check Ready。

## 代码位置

| 领域 | 关键函数 | 源码 |
| --- | --- | --- |
| 加密日志分页/计数 | `readPageLocked()`、`nextFileCursor()`、`countRecords()` | [`EncryptedLogRecordStore.kt`](../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt)、[`EncryptedLogFileManager.kt`](../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogFileManager.kt) |
| 日志导出与分享 | `export()`、`exportToDocument()`、`share()` | [`ObservabilityExportService.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
| 权限持久历史 | `wasRequested()`、`markRequested()`、`requestHistoryKey` | [`SharedPreferencesPermissionRequestHistory.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/permission/internal/SharedPreferencesPermissionRequestHistory.kt)、[`RuntimePermission.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/permission/model/RuntimePermission.kt) |
| Autofill scope | `AutofillScope`、`AutofillGrantContext.normalized()` | [`AutofillScope.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/AutofillScope.kt) |
| 认证恢复 | `authenticate()`、`recoverAuthenticationFailure()`、`withSlot()` | [`DefaultAuthenticationManager.kt`](../../security/src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt) |
| 备份 restore 文件事务 | `replace()`、`rollback()`、`commit()`、`restore()` | [`RestoreFileJournal.kt`](../../app/src/main/java/com/aozijx/passly/app/database/backup/RestoreFileJournal.kt)、[`RoomBackupSnapshotRestorer.kt`](../../app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotRestorer.kt) |
| Trash / Favicon / SAF | `observeTrash()`、`process()`、`createNamedExportTarget()` | [`TrashViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/trash/TrashViewModel.kt)、[`DefaultFaviconImageProcessor.kt`](../../app/src/main/java/com/aozijx/passly/app/entry/favicon/DefaultFaviconImageProcessor.kt)、[`BackupExportStorageSupport.kt`](../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/platform/BackupExportStorageSupport.kt) |
| Diagnostics 页面所有权 | `refresh()`、`clearLogs()`、`onPageExited()` | [`DiagnosticsViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt) |
