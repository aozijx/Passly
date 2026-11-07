# Backup 导出

状态：当前实现（代码核对：2026-11-07）。

## 导出

导出选项默认包含 Domain 的全部 `EntryType`。归档稳定键注册表测试逐项验证每种类型均可往返映射，并断言注册表覆盖全集；新增条目类型若未加入归档映射会使测试失败，Presentation 无需依赖 wire-key 注册表决定页面选项。

### 导出服务调用链

`BackupArchiveServiceImpl.export(request)` 在 `BackupIoDispatcher` 上用 `AppResult.runSuspendCatching` 编排：先以
`request.format` 调用 `BackupFormatRegistry.exporter(formatId)` 选 Adapter，再以 Adapter 的
`requiresPassword` 和 `request.password` 调用 `validatePassword(required, password)`。随后构造
`BackupSnapshotReadOptions`：`includeIcons` 同时受用户选项与 `adapter.supportsIcons` 限制，
`includeAttachments` 同时受用户选项与 `adapter.includesAttachments` 限制；`includeDeleted` 与
`includedEntryTypes` 原样来自 `request.options`。该对象传给 `BackupSnapshotGateway.read(options)`。

拿到 `BackupBundle` 后调用 `adapter.encode(bundle, request.password)`；返回的编码 `ByteArray` 传给
`BackupFileStore.writeBytes(request.targetUri, encoded)`，写入结束或失败都在 `finally` 中清零 `encoded`。
外层 `finally` 调用 `bundle.clearResourceData()` 清零每个资源数据数组。失败结果随后由
`report(error, "backup_export")` 送入 `AppErrorReporter.report(error, ErrorReportContext(operation, category))`；
错误上下文的 category 固定为 `BACKUP`。

`BackupFormatRegistry.exporter(request.format)` 返回对应 Adapter；JSON Adapter 的
`encode(bundle, password)` 调 `JsonBackupExporter.export(bundle)` 后编码 UTF-8。加密 Adapter 将同一
`bundle` 与非空密码传给 `EncryptedBackupExporter.export(bundle, password)`。两种导出均由 Service 负责最终写入，
Adapter 不持有目标 URI。

1. 已解锁并通过备份导出认证后读取 Room 快照。
2. `KeyedEntryArchiveMapper` 生成稳定键记录。
3. Validator 在写文件前检查必填字段、值形状、引用和资源完整性。
4. 格式 Adapter 只负责 canonical bundle 与外部格式互转。
5. `BackupFileStore` 写入目标，并在完成或失败后清零可控密码与编码缓冲。导入/导出取消保留取消语义；
   目标创建成功但 URI 尚未交回流程前若普通失败、取消或 fatal `Error`，都在不可取消区尝试删除该文件。
   若 `BackupArchiveService.export(request)` 返回 `AppResult.Failure` 且 `deleteOnFailure == true`，
   `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)` 在 `NonCancellable` 中调用
   `tryDeleteFailedExport(targetUri)`。清理成功时仍返回 `BackupExecutionResult.Failure(result.error)`；删除返回 `false` 或抛普通异常时，
   返回 `ExportCleanupFailed(result.error, cleanupFailure)`，保留原导出错误并携带清理失败。`BackupViewModel.applyResult(...)`
   仍以原错误更新失败状态，同时发送 `BackupNoticeEffect.ExportCleanupFailed`，映射为 `BACKUP_EXPORT_CLEANUP_FAILED`，提示不完整文件可能残留。
   若归档服务抛出异常，Gateway 的异常分支也尝试删除；`preferBackupCleanupFailure(primaryFailure, cleanupFailure)` 按
   fatal `Error`、取消、普通异常的优先级决定主异常，并把另一异常作为 suppressed。`deleteOnFailure == false` 时保留调用方指定目标，不尝试删除。
   SAF 创建和写入探测只把普通异常映射为 `Result.failure`；取消与 fatal `Error` 原样传播。删除接口把普通 provider 异常映射为 `false`，
   但继续传播取消和 fatal `Error`。页面状态与密码生命周期见[操作生命周期](operation-lifecycle.md)。

完整导出使用 archive registry，而不是 UI 当前可创建/筛选类型。数据库出现未注册类型时必须失败，不能静默漏项。
TXT 是人类可读投影，不可恢复，且不输出 favorite 字段。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 选择格式、验证密码、创建快照、编码并写目标 | `BackupArchiveServiceImpl.export(request)`、`BackupFormatRegistry.exporter(formatId)` | [`BackupArchiveServiceImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt)、[`BackupFormatRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatRegistry.kt) |
| 把导出选项转换为领域快照读取参数 | `BackupSnapshotReadOptions(...)`、`AppBackupSnapshotGateway.read(options)`、`RoomVaultBackupStore.read(options)` | [`BackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/snapshot/BackupSnapshotGateway.kt)、[`AppBackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt)、[`RoomVaultBackupStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt) |
| 稳定键映射及 bundle 校验 | `BackupSnapshotArchiveMapper.toBundle(snapshot, appVersion, exportedAt)`、`KeyedEntryArchiveMapper.toRecord(entry, attachmentIds)`、`BackupBundleValidator.validate(bundle, requireResourceData)` | [`BackupSnapshotArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/BackupSnapshotArchiveMapper.kt)、[`KeyedEntryArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveMapper.kt)、[`BackupBundleValidator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupBundleValidator.kt) |
| 编码格式并写入单文件或 tree 目标 | `BackupExportAdapter.encode(bundle, password)`、`BackupArchiveServiceImpl` 调 `BackupFileStore.writeBytes(request.targetUri, encoded)`（接口形参 `uri`, `data`） | [`BackupFormatAdapter.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatAdapter.kt)、[`BackupFileStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/BackupFileStore.kt) |
| 按 `deleteOnFailure` 处理部分导出文件并清理密码副本 | `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)`、`tryDeleteFailedExport(targetUri)` | [`DefaultBackupOperationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt) |
| 将残留文件结果映射为操作失败与专门通知 | `BackupViewModel.applyResult(ExportCleanupFailed)`、`BackupNoticeMapper.toNoticeCode()` | [`BackupViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt)、[`BackupNoticeMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupNoticeMapper.kt) |
