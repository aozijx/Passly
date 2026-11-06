# Passly Backup 文档扫描待办

**状态：** 待实现与验证。条目来自 2026-11-06 对归档调用链的静态核对；本文件记录问题和验收，不代表已修改运行实现。

## Task 1：兼容导入不能把 fatal Error 当成无效条目

- [ ] 检查 `BackupImportPlanner.plan(bundle, strategy)` 兼容分支逐 Entry 校验处的 `runCatching { BackupBundleValidator.validate(...) }`。Kotlin `runCatching` 捕获 `Throwable`，当前 `isSuccess` 将 `OutOfMemoryError`、`LinkageError` 等 fatal `Error` 解释为普通候选校验失败，随后可能把该 Entry 计入 `skippedEntryCount` 并继续导入。
- [ ] 收窄候选校验失败分类：只把预期的结构/业务验证异常转换为“跳过该条目”，让 fatal `Error` 原样逸出；不要掩盖恢复资源清理或主错误传播合同。
- [ ] 增加兼容导入测试，证明候选验证抛出的普通校验异常可跳过该条目，而 fatal `Error` 不会变成成功的兼容导入结果；同时保留未知字段/类型的计数行为。
- [ ] 更新导入工作流文档和兼容导入验证记录，说明何种错误会跳过单个 Entry、何种错误终止整个导入。

**当前证据（2026-11-06）：** `BackupImportPlanner.kt` 的兼容策略对每个候选构造无附件的 `BackupBundle` 并调用 `BackupBundleValidator.validate(..., requireResourceData = false)`；其结果由 `runCatching { ... }.isSuccess` 判定。Kotlin `runCatching` 捕获所有 `Throwable`，因此 fatal `Error` 会被当作 `valid = false`，计入跳过分支。相邻的 `JsonBackupImporter` 与 `BackupArchiveCodec.readZip()` 也使用 `catch(Throwable)`，但只在解码失败时擦除已分配的资源缓冲后立即 `throw error`；这类窄清理逻辑保留原 fatal 传播，不是本 Task 的同一问题。此条是静态确认，尚未运行测试或修改实现。

**验收：** 验证异常按兼容合同计数；fatal `Error` 从规划器传播且不产生部分恢复成功；相关导入测试和质量门禁通过。

## Task 2：为备份导入定义累计内存预算

- [ ] 核对原生 JSON、Bitwarden JSON、加密容器各阶段的输入、文本、JSON tree、ZIP、解压资源及重序列化副本，并确定统一累计预算与格式级上限。
- [ ] 证明超限在完整资源物化前被拒绝；边界测试覆盖单资源、累计资源、畸形 UTF-8/Base64 和刚好达到上限的有效文件。
- [ ] 更新备份导入格式文档，记录预算、验证顺序与峰值副本；保留现有 wire key 与格式兼容合同。

**当前证据（2026-11-06）：** `BackupFileStore.readBytesSafely()` 限制单次输入为 256 MiB，但 Passly JSON/Bitwarden JSON 会构建完整文本或 JSON tree；加密导入会持有原容器、解密 ZIP 字节及提取的 JSON/资源。不同阶段副本和累计内存尚无统一上限证明。

## Task 3：覆盖 BackupArchiveService 编排的参数与清理合同

- [ ] 新增针对 `BackupArchiveServiceImpl.export(request)` 的专用单测：验证 `request.format/password/options/targetUri` 分别流向 Registry、`validatePassword`、`BackupSnapshotReadOptions`、Adapter 和 `BackupFileStore.writeBytes`；特别断言 `includeIcons` 与 `includeAttachments` 同时受请求选项和 Adapter 能力限制，其他筛选项按请求传递。
- [ ] 新增针对 `BackupArchiveServiceImpl.import(request)` 的专用单测：验证 `sourceUri` 传给 `readBytesSafely`，`format + payload` 传给 probe 分派，`password/strategy` 传给 decode，`bundle + mode` 传给 `restore`，并验证 `BackupImportResult` 合并 restore 计数与兼容摘要。
- [ ] 通过可观察 ByteArray/资源引用断言成功、decode/restore 普通失败和取消路径的 payload、encoded bytes、bundle resource data 清理；确认失败才调用 `AppErrorReporter.report`，report operation 分别为 `backup_export` 与 `backup_import`，且取消不会伪装成普通失败。
- [ ] 保留组件测试职责：Codec 证明 wire/container，Planner 证明策略筛选，Gateway 证明授权/目标清理；Service 测试只验证真实编排与跨组件参数合同，不复制底层编解码用例。
- [ ] 更新 [`备份验证要求`](../../features/backup/verification.md) 的测试映射，并验证新增测试能在不依赖 Android 文件 provider、Room 或真实 KDF 慢路径的情况下稳定运行。

**当前证据（2026-11-06）：** `app/src/test` 有 `BackupCodecTest`、`BackupImportPlannerTest`、`DefaultBackupOperationGatewayTest` 和 Room 文件 journal 测试，但没有 `BackupArchiveServiceImpl` 对应测试文件。Service 本身负责格式选择、选项能力裁剪、参数传递、资源/输入清零和错误报告；相邻单测分别覆盖这些组件，却没有端到端断言上述拼接合同。此项为测试覆盖缺口，不是已确认运行时故障。

## Task 4：评估备份口令在 Compose String 投影中的驻留范围

- [ ] 评估 `OutlinedTextField` 与 `BackupRestoreSheetUiState.password: String` 的输入/回显接口能否在不破坏 Compose 编辑、选择和 IME 行为的前提下缩短口令 String 生命周期；任何改动必须沿 TextField → Action → `OwnedChars` → workflow → UI projection 全链路评估，不能只替换 ViewModel 内单个类型或增加擦除不了 String 的薄包装。
- [ ] 保留当前已验证的字符数组责任：`BackupViewModel.execute()` finally 擦除 workflow `SensitiveValue`；`DefaultBackupOperationGateway.performExport()` / `performImport()` finally 擦除传给 `BackupArchiveService` 的副本；替换/关闭 workflow 时旧/拒绝的 owned buffer 必须擦除。任何调整都要验证普通失败、取消、ViewModel 清理和输入重放后的数组生命周期。
- [ ] 核对 Compose 与 Navigation 未把备份口令写入 SavedState、持久化、日志、异常消息或外部 Intent；若 `String` 仍不可避免，记录明确内存边界，避免声称可擦除所有口令副本。无需为了理论擦除引入平行表单状态模型。

**当前证据（2026-11-06）：** 两个 `OutlinedTextField` 都以 `state.password: String` 为 value，并将 `onValueChange(value)` 发为 `BackupUiAction.UpdatePassword(value)`。`BackupViewModel.onAction()` 调 `OwnedChars.fromString(action.password)`；`BackupSheetUiMapper` 的私有 `SensitiveValue.copyToString()` 将临时 `CharArray` 转成新 String 并在 `finally` 擦除临时数组。`BackupViewModel.execute()` 擦除 workflow 持有的原 `SensitiveValue`；Gateway 在 `performExport` / `performImport` 中将其复制成 service 请求 `CharArray` 并于 finally 擦除。Compose projection 中的 String 本身不能可靠覆写；当前源码路径未见 SavedState、Navigation、日志或持久化传递。属于可控内存驻留范围评估，不是已证实的外泄或普通应用可利用漏洞。

## Task 5：不要静默丢弃导出结果失败后的目标清理错误

- [ ] 明确 `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)` 在 `BackupArchiveService.export(...)` 返回 `AppResult.Failure` 且 `deleteOnFailure == true` 时的清理结果合同。Gateway 应保留主导出失败，同时让普通清理失败或 `deleteDocument(targetUri) == false` 可被页面 owner 感知；不得声称不完整文件已删除，也不得将该情况当作普通可重试失败而不说明可能残留的目标文件。
- [ ] 为 Archive Service 返回 `AppResult.Failure` 的路径覆盖删除成功、删除返回 false、删除抛普通异常、取消与 fatal `Error`；分别断言 Gateway 结果、清理/主错误优先级和 suppressed 关系、页面提示与密码副本擦除。区分 Service 返回 Failure 与抛异常的路径，后者当前由 Gateway catch 分支处理，不能用它替代本任务覆盖。
- [ ] 更新备份操作结果合同与工作流文档；保持 `deleteOnFailure == false` 的指定目标语义不变，并保留取消传播和 fatal `Error` 传播。

**当前证据（2026-11-06）：** `BackupArchiveServiceImpl.export(request)` 在 `AppResult.runSuspendCatching` 中执行格式选择、授权后的快照/编码/写入，普通异常以 `AppResult.Failure` 返回并调用 `report(error, "backup_export")`。`DefaultBackupOperationGateway.performExport()` 收到该结果后，在 `deleteOnFailure` 为真时调用 `tryDeleteFailedExport(targetUri)`；该 helper 在 `NonCancellable` 中删除，删除返回 false 会构造 `IOException`，抛出的异常也作为 `Throwable` 返回。当前 `AppResult.Failure` 分支只重抛清理阶段的 `CancellationException`/fatal `Error`，普通清理异常被忽略，随后只返回 `BackupExecutionResult.Failure(result.error)`。相邻 catch 分支会以 `preferBackupCleanupFailure(error, cleanupFailure)` 合并普通异常，因此两种失败形态行为不同。`DefaultBackupOperationGatewayTest` 已覆盖取消/异常抛出时的删除与 fatal 清理优先级，但没有覆盖 Service 返回 `AppResult.Failure` 后普通删除失败或 `false`；这会让可能仍存在的部分文件没有独立的 UI 提示。当前仅记录待实现/验证项，未修改生产逻辑。
