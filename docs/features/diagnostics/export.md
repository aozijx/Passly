# 诊断日志导出

状态：当前实现（代码核对：2026-11-06）。

诊断页面的保存与分享都要求 `EXPORT_DIAGNOSTICS` 新鲜授权，但交付目标不同：保存写入用户用 SAF `CreateDocument` 选定的 URI；分享创建 cache 临时明文文件并经 FileProvider 只读共享。两种动作不复用备份目录授权。

## 格式选择与授权

Top app bar 的保存/分享先由 `DiagnosticsViewModel.onAction(RequestExport(action))` 记录待处理 `ExportAction` 并打开格式 sheet。`SelectFormat(format)` 进入 `selectFormat(format)`：分享直接创建 `DiagnosticsExportRequest(SHARE, format)` 并交给 `authenticateAndExport(request, pageOwner)`；本地保存先生成页面 owner 绑定的一次性 request ID，再发送 `SelectSaveDocument(format, format.mimeType, fileName, requestId)`。

`DiagnosticsRoute` 依格式选择 JSONL、TSV 或 ZIP 对应的 `CreateDocument` launcher。回执以 URI、request ID 和 page owner 映射回 `DiagnosticsAction.SaveToDocument`；取消映射为 `SaveDocumentCancelled`。`completeLocalDocument(pageOwner, requestId, uri)` 只接受当前访问与当前 request ID，有 URI 时才构造 `DiagnosticsExportRequest(EXPORT_LOCAL, format, uri)`。离页、过期 ID 或取消不启动导出，并释放 pending 格式。

`authenticateAndExport(request, pageOwner)` 调 `ExportDiagnosticsUseCase.invoke(request)`。Use Case 先要求完整 secure session，再通过 `AuthorizationGate.authorize(AuthorizationScope.Global(EXPORT_DIAGNOSTICS))` 获取一次性 permit 并调用 `DiagnosticsExportGateway.export(permit, request)`。会话受限、拒绝、取消或 export 失败时都调用 `gateway.discard(request)`，用于删除本地 SAF 已预建目标；取消返回 `Cancelled`，fatal `Error` 不折成普通失败。

## 文件写入与清理

`ObservabilityExportService.export(permit, request)` 切到 `Dispatchers.IO`，先用 `permitVerifier.consume(permit, EXPORT_SCOPE)` 消费一次性授权；校验失败返回 `Result.failure(SecurityException)`。通过后调用 `store.flush(FLUSH_TIMEOUT_MS)`（当前 300 ms），随后通过 `readRecords()` 从 `store.readPage(cursor, minOf(PAGE_SIZE, MAX_EXPORT_RECORDS - records.size))` 分页读取，单页最多 500 条、总计最多 5,000 条，再按 `eventId` 去重并按 `timestampMs`、`elapsedRealtimeMs`、`eventId.value` 排序。

**当前 flush 返回值缺口：** `store.flush(300)` 的 Boolean 结果目前被忽略；即使 flush 超时或报告写入失败，导出仍会读取已有记录并继续生成文件，可能把不完整日志作为成功导出交付。该行为已登记在[诊断日志可靠性 TODO](../../superpowers/plans/2026-11-06-passly-diagnostics-reliability-followup.md)，尚未修复。fatal worker `Error` 目前也可能因 flush marker 完成而没有被 failure counter 观察到，详见同一 TODO Task 1。

`SHARE` 创建格式 artifact 临时文件、编码并通过 FileProvider 分享；成功后尝试安排 10 分钟删除，调度失败时保留文件并依下一次分享时的过期清理回收。未成功分享时，在清理边界删除临时明文文件。`EXPORT_LOCAL` 要求 document URI，打开 provider 输出流并写编码内容；失败或取消时尝试删除系统预建 URI。

两种格式都在读取、编码和写入边界检查协程取消。同步编码或日志读取不可中断时，在步骤返回后检查取消并执行补偿。普通读取/写入/分享异常以 `Result.failure` 返回；fatal `Error` 经清理后传播。清理 fatal Error 优先传播并保留原错误为 suppressed；分享已成功后，调度删除失败不撤销已交付文件。

平台合同：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。日志结构与 TSV/JSONL/ZIP 字段见[可观测性导出格式](../../architecture/observability/export-formats.md)。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 格式/目标 request 与新鲜认证 | `DiagnosticsViewModel.selectFormat(format)`、`completeLocalDocument(pageOwner, requestId, uri)`、`authenticateAndExport(request, pageOwner)` | [`DiagnosticsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt) |
| `EXPORT_DIAGNOSTICS` gate 与失败目标补偿 | `ExportDiagnosticsUseCase.invoke(request)`、`discard(request)` | [`ExportDiagnosticsUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/diagnostics/ExportDiagnosticsUseCase.kt) |
| 记录 flush、编码、SAF 写入与分享 | `ObservabilityExportService.export(permit, request)` | [`ObservabilityExportService.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
| 一次性文档选择器与 owner 回执 | `DiagnosticsRoute()` | [`DiagnosticsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/data/DiagnosticsRoute.kt) |
