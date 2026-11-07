# Passly Backup 文档扫描待办

**2026-11-07 复核/排序：** Tasks 1/3/5 retained；Task 1 fatal 边界与 Task 3 service 编排测试已实现并验证；Task 5 清理失败反馈已实现并补测试，但本次运行被无关认证测试源码编译错误阻断。Task 4 为敏感 String 驻留评估，不升级为外泄结论。下一步落实 Task 2 前置/累计预算；已有 ZIP 单项/累计 cap 和 bundle 限制，不描述为完全无上限；缺口是物化前拒绝及峰值副本预算。

**状态：** Task 1/3 已实现并有针对性验证；Task 5 已实现，测试执行受无关源码编译错误阻断；Task 2/4 待实现与评估。条目来自对归档调用链的静态核对；本文件记录问题和验收进展。

## Task 1：兼容导入不能把 fatal Error 当成无效条目

- [x] 检查 `BackupImportPlanner.plan(bundle, strategy)` 兼容分支逐 Entry 校验处原有的 `runCatching { BackupBundleValidator.validate(...) }`。Kotlin `runCatching` 捕获 `Throwable`，会把 fatal `Error` 误记为普通候选验证失败并继续导入。
- [x] 收窄候选校验失败分类：只把 `IllegalArgumentException`（validator 的结构/业务 `require` 失败）转换为“跳过该条目”，fatal `Error` 原样逸出。
- [x] 增加兼容导入测试，证明 `IllegalArgumentException` 会跳过候选、fatal `Error` 不会变成成功的兼容导入结果；保留未知字段/类型及结构无效条目的行为。
- [x] 更新导入工作流文档和兼容导入验证记录，说明何种错误会跳过单个 Entry、何种错误终止整个导入。

**实现与验证（2026-11-07）：** 逐 Entry 分类提取为 `BackupImportPlanner.isCandidateValid()`，仅捕获 validator 用 `require` 发出的 `IllegalArgumentException`；其余异常和 fatal `Error` 均继续传播。新增分类测试证明 fatal `AssertionError` 保持同一实例逸出，并覆盖结构无效条目跳过；原有未知字段、未知类型、严格策略与重复 Link 测试保留。`BackupImportPlannerTest` 定向运行通过。正式流程与验证映射见[导入工作流](../../features/backup/import.md)和[备份验证](../../features/backup/verification.md)。

**验收：** 验证异常按兼容合同计数；fatal `Error` 从规划器传播且不产生部分恢复成功；相关导入测试和质量门禁通过。

## Task 2：为备份导入定义累计内存预算

- [ ] 核对原生 JSON、Bitwarden JSON、加密容器各阶段的输入、文本、JSON tree、ZIP、解压资源及重序列化副本，并确定统一累计预算与格式级上限。
- [ ] 证明超限在完整资源物化前被拒绝；边界测试覆盖单资源、累计资源、畸形 UTF-8/Base64 和刚好达到上限的有效文件。
- [ ] 更新备份导入格式文档，记录预算、验证顺序与峰值副本；保留现有 wire key 与格式兼容合同。

**当前证据（2026-11-06）：** `BackupFileStore.readBytesSafely()` 限制单次输入为 256 MiB，但 Passly JSON/Bitwarden JSON 会构建完整文本或 JSON tree；加密导入会持有原容器、解密 ZIP 字节及提取的 JSON/资源。不同阶段副本和累计内存尚无统一上限证明。

## Task 3：覆盖 BackupArchiveService 编排的参数与清理合同

- [x] 新增针对 `BackupArchiveServiceImpl.export(request)` 的专用单测：验证 `request.format/password/options/targetUri` 分别流向 Registry、`validatePassword`、`BackupSnapshotReadOptions`、Adapter 和 `BackupFileStore.writeBytes`；特别断言 `includeIcons` 与 `includeAttachments` 同时受请求选项和 Adapter 能力限制，其他筛选项按请求传递。
- [x] 新增针对 `BackupArchiveServiceImpl.import(request)` 的专用单测：验证 `sourceUri` 传给 `readBytesSafely`，`format + payload` 传给 probe 分派，`password/strategy` 传给 decode，`bundle + mode` 传给 `restore`，并验证 `BackupImportResult` 合并 restore 计数与兼容摘要。
- [x] 通过可观察 ByteArray/资源引用断言成功、decode/restore 普通失败和取消路径的 payload、encoded bytes、bundle resource data 清理；确认失败才调用 `AppErrorReporter.report`，report operation 分别为 `backup_export` 与 `backup_import`，且取消不会伪装成普通失败。
- [x] 保留组件测试职责：Codec 证明 wire/container，Planner 证明策略筛选，Gateway 证明授权/目标清理；Service 测试只验证真实编排与跨组件参数合同，不复制底层编解码用例。
- [x] 更新 [`备份验证要求`](../../features/backup/verification.md) 的测试映射，并验证新增测试能在不依赖 Android 文件 provider、Room 或真实 KDF 慢路径的情况下稳定运行。

**实现与验证（2026-11-07）：** 新增 `BackupArchiveServiceImplTest` 8 项，不使用 Android provider、Room、真实 KDF 或 UI 慢路径。覆盖导出请求/能力裁剪、导入格式 probe 与参数路由、恢复摘要合并、必填密码、编码/读取/资源字节在成功/普通失败/取消后的清零，以及失败报告和取消传播；`:app:testDebugUnitTest --tests "com.aozijx.passly.feature.backup.internal.archive.BackupArchiveServiceImplTest" --no-build-cache --no-parallel --max-workers=2` 通过。测试职责映射见[备份验证](../../features/backup/verification.md)。

## Task 4：评估备份口令在 Compose String 投影中的驻留范围

- [ ] 评估 `OutlinedTextField` 与 `BackupRestoreSheetUiState.password: String` 的输入/回显接口能否在不破坏 Compose 编辑、选择和 IME 行为的前提下缩短口令 String 生命周期；任何改动必须沿 TextField → Action → `OwnedChars` → workflow → UI projection 全链路评估，不能只替换 ViewModel 内单个类型或增加擦除不了 String 的薄包装。
- [ ] 保留当前已验证的字符数组责任：`BackupViewModel.execute()` finally 擦除 workflow `SensitiveValue`；`DefaultBackupOperationGateway.performExport()` / `performImport()` finally 擦除传给 `BackupArchiveService` 的副本；替换/关闭 workflow 时旧/拒绝的 owned buffer 必须擦除。任何调整都要验证普通失败、取消、ViewModel 清理和输入重放后的数组生命周期。
- [ ] 核对 Compose 与 Navigation 未把备份口令写入 SavedState、持久化、日志、异常消息或外部 Intent；若 `String` 仍不可避免，记录明确内存边界，避免声称可擦除所有口令副本。无需为了理论擦除引入平行表单状态模型。

**当前证据（2026-11-06）：** 两个 `OutlinedTextField` 都以 `state.password: String` 为 value，并将 `onValueChange(value)` 发为 `BackupUiAction.UpdatePassword(value)`。`BackupViewModel.onAction()` 调 `OwnedChars.fromString(action.password)`；`BackupSheetUiMapper` 的私有 `SensitiveValue.copyToString()` 将临时 `CharArray` 转成新 String 并在 `finally` 擦除临时数组。`BackupViewModel.execute()` 擦除 workflow 持有的原 `SensitiveValue`；Gateway 在 `performExport` / `performImport` 中将其复制成 service 请求 `CharArray` 并于 finally 擦除。Compose projection 中的 String 本身不能可靠覆写；当前源码路径未见 SavedState、Navigation、日志或持久化传递。属于可控内存驻留范围评估，不是已证实的外泄或普通应用可利用漏洞。

## Task 5：不要静默丢弃导出结果失败后的目标清理错误

- [x] 明确 `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)` 在 `BackupArchiveService.export(...)` 返回 `AppResult.Failure` 且 `deleteOnFailure == true` 时的清理结果合同。Gateway 保留主导出错误，并通过专门结果和页面提示暴露普通清理失败或删除返回 false，说明目标可能残留。
- [x] 为 Archive Service 返回 `AppResult.Failure` 的路径覆盖删除成功、删除返回 false、删除抛普通异常、取消与 fatal `Error`；另验证删除禁用时不删目标，及密码副本最终擦除。异常抛出路径仍与返回 Failure 路径分别覆盖。
- [x] 更新备份操作结果合同与工作流文档；保持 `deleteOnFailure == false` 的指定目标语义不变，并保留取消传播和 fatal `Error` 传播。

**实现与验证（2026-11-07）：** `AppResult.Failure` 分支现返回 `ExportCleanupFailed(originalError, cleanupFailure)`，只在补偿删除失败时触发；ViewModel 仍用原导出错误更新失败状态，并发出独立残留提示。已添加删除返回 false、普通异常、删除成功、`deleteOnFailure == false`、取消和 fatal `Error` 的 Gateway 测试，以及残留提示的通知码映射测试；运行定向任务时 `:app:compileDebugUnitTestKotlin` 被无关的 `UnlockViewModelTest.kt:39` SAM 编译错误阻断，故本任务测试尚未执行。Service 抛异常路径继续维持原有 cleanup 主次与 suppressed 规则。对应工作流合同见[操作生命周期](../../features/backup/operation-lifecycle.md)。
