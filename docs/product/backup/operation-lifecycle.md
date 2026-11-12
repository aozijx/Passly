# Backup 操作状态、密码与页面生命周期

状态：当前实现（代码核对：2026-11-07）。

## 工作流状态

界面使用封闭状态：`Idle`、`FormatPicker`、`ConfigureExport`、`AwaitingExportTarget`、`ConfigureImport`、
`Executing`、`Failed`。ViewModel workflow 中的密码以 `SensitiveValue` 持有；关闭或替换 workflow、拒绝状态更新、
操作结束及 ViewModel 清理时会擦除其可控字符缓冲。密码不通过 SavedState 或 Navigation 参数持久化。不过当前 Compose 输入/投影仍会产生不可变 `String` 副本，清零 `SensitiveValue` 不等于擦除所有副本，具体边界和后续评估见下文。

密码输入调用链是 `OutlinedTextField(value = state.password)` → `BackupUiAction.UpdatePassword(value: String)` →
`BackupViewModel.onAction()` → `OwnedChars.fromString(action.password)`，后者将 UI 字符串复制到可控 `CharArray`，再由
`replacePassword()` 写入 workflow；`BackupSheetUiMapper.toSheetUiState()` 又调用 `SensitiveValue.copyToString()`，该函数将字符数组转换成新 String 并在 `finally` 擦除临时数组。Compose TextField value/onValueChange 与展示模型使用 String，改为 `TextFieldValue` 仍包含 String；现有 UI API 无法可靠覆盖这些不可变副本，继续包装或复制只会增加一条同样不可擦除的路径，因此该重构保留此边界。操作时 `BackupViewModel.execute(operation, password)` 在 coroutine `finally` 擦除 workflow 的 `SensitiveValue`；`DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)` / `performImport(request)` 再复制成传给 `BackupExportRequest` / `BackupImportRequest` 的 `CharArray`，并在各自 `finally` 擦除副本。`BackupArchiveServiceImpl.validatePassword(required, password)` 仅在 `required == true` 时拒绝 null/empty，不执行强度校验；算法和文件格式 Adapter 接收同一密码数组。`BackupViewModel.onPageExited()` 在非执行态关闭表单并擦除 workflow 缓冲，`onCleared()` 擦除当前 workflow 缓冲；提交中的请求由 coroutine 和 Gateway 分层擦除。源码未发现该表单接入 `SavedStateHandle`/`rememberSaveable`、Navigation 参数、日志、持久化或外部 Intent。结论是将可控 `CharArray` 尽早擦除并避免新增 String 副本；不能声称能覆写 Compose/JVM String。

提交导入或导出后，操作由 `BackupViewModel` 的任务持有，不随 Backup 页面离开而取消。页面离开会关闭尚未提交的配置并使旧页面通知失效；执行中的状态保留，重进时不接受第二个并行 Backup 操作，完成结果更新该操作状态。Activity/ViewModel 销毁会取消任务：操作 `finally` 擦除 UI 持有的密码，归档流程传播取消并补偿已创建的部分导出文件；导入事务按 Room/文件 journal 的回滚合同结束。SAF 文档选择回执绑定发起它的页面 owner 和一次性请求 ID，旧回执不能在重进后启动新的导入或导出。页面仍有效但宿主暂停时，选择器命令会等到 `STARTED` 后启动；页面离开会丢弃旧回执。导出选择命令入队失败时恢复 `ConfigureExport`，可重新提交；普通提示队列满时允许丢弃。

若归档服务返回导出失败且补偿删除也失败，`DefaultBackupOperationGateway` 保留原导出 `AppError` 作为操作失败状态，并返回带有清理异常的 `ExportCleanupFailed` 结果；页面发送专门提示，告知不完整文件可能仍在备份位置。删除返回 `false` 与普通删除异常都按此处理。取消和 fatal `Error` 仍传播；`deleteOnFailure == false` 时不会尝试删除调用方指定目标。

归档服务在同一 trace 中记录 `backup.operation.phase`：导出快照就绪、开始及完成写入，导入格式选择、解码完成、开始恢复和恢复返回。快照与解码阶段只附带条目数和资源数；日志不包含密码、URI 或业务内容。阶段和完成日志的普通写入异常不会改变操作结果，取消和 fatal `Error` 仍传播。最后一个阶段可用于缩小失败范围，恢复返回阶段表示持久化调用已成功返回。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 操作状态、密码 owner、一次性目标选择与执行 | `BackupViewModel.onAction(action)`、`execute(operation, password, block)`、`closeWorkflow()` | [`BackupViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt) |
| UI 字符串复制至受控字符缓冲 | `OwnedChars.fromString(value)`、`BackupUiState.toSheetUiState(configuredDirectoryLabel)`、`SensitiveValue.copyToString()` | [`OwnedChars.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/sensitive/OwnedChars.kt)、[`BackupSheetUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupSheetUiMapper.kt) |
| 将受控密码副本传给导出/导入请求并在结束后擦除 | `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)`、`performImport(request)` | [`DefaultBackupOperationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt) |
| 导入/导出任务的页面回执与前台启动 | `BackupOperationRoute(...)` | [`BackupOperationRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupOperationRoute.kt) |
