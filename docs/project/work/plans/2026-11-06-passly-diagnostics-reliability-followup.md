# Passly 诊断日志写入与 flush 可靠性跟进

**范围：** 诊断日志写入失败后的清理、失败状态传播和 flush 可观察语义。本文跟踪可靠性问题，不是安全扫描报告或漏洞清单。

**当前优先序：** 先处理 Task 1 的 writer/flush 失败，再处理 Task 2 的导出结果；Task 3 的流清理可独立处理。


<a id="task-1"></a>

## Task 1：让 fatal 写入失败反映到 flush 结果

- [ ] 检查 `EncryptedLogRecordStore.WriteTask.run()` / `writeBatch(batch)` 的 fatal `Error` 边界。保持 Error 在敏感缓冲及文件回滚清理后原样逸出，同时记录本批失败，不能因 `finally` 仍运行 flush marker 就把未落盘批次报告为成功。
- [ ] 定义普通 flush、生命周期 flush 和 crash flush 对 fatal worker 失败的可观察合同；不能吞 fatal Error，也不能让 flush marker 越过失败批次而返回 success。
- [ ] 为在 `beforeWrite`、编码/加密和文件 append 路径注入 fatal Error 的场景添加测试：确认明文与密文缓冲清理、部分文件回滚、worker uncaught handler 收到原 Error、drop/failure 计数与 flush 返回值不会伪报成功；并验证后续 writer replacement 的语义。

**源码复核（HEAD `6064da2f4`，2026-11-12；未运行测试）：** 当前 `WriteTask.run()` 只捕获 `Exception`，捕获后为整个 batch 增加 `writeFailureCount`；其 `finally` 无论 `writeBatch(batch)` 是否异常都会执行 `boundary?.run()`。`flush()` 将 marker 排在等待队列边界，并只通过 failure counter 与 `observedWriteFailureCount` 比较返回状态。`writeBatch` 的 `finally` 会清零 encoded/plain 和 encrypted frame 缓冲，append rollback 捕获 Throwable 后截断文件并重抛；fatal Error 仍绕过 `WriteTask.run()` 的 Exception catch，未增加该 counter，已取出的 boundary 仍 count down，因此 flush 仍可能成功。现有测试覆盖 emergency writer 的 fatal cleanup 和普通 writer Exception 失败，没有覆盖 normal `WriteTask` fatal path。该缺口经当前源码复核仍存在，尚未以新用例验证。当前行为和崩溃 flush 路径已记入[运行时与加密存储](../../../platform/architecture/observability/runtime-and-storage.md)；修复后按新合同更新该页。

**验收：** fatal Error 清理后仍原样传播；失败批次对 flush 可观察且不伪报成功；普通失败和正常 flush 语义保持清楚；相关测试覆盖清理、回滚及 writer 后续状态。

<a id="task-2"></a>

## Task 2：让诊断导出处理 flush 失败

- [ ] 在 `ObservabilityExportService.export(permit, request)` 中明确处理 `store.flush(FLUSH_TIMEOUT_MS)` 的 `false`：不得继续以普通成功语义导出可能不完整的快照；保留取消传播和 fatal `Error` 语义，并明确本地 SAF 预建目标的删除责任仍由 `ExportDiagnosticsUseCase`/`discard(request)` 承担。
- [ ] 决定 flush 失败对分享和本地保存的用户可见结果与重试路径；如果产品选择允许部分导出，须显式标记不完整并包含可核对的失败原因，不能静默当成完整导出。
- [ ] 为 flush 超时、普通写入失败和成功分别覆盖 SHARE/EXPORT_LOCAL 的结果、文件/URI 清理与授权 permit 单次消费；确保空记录也不能掩盖 flush 失败。

**源码与测试定义复核（HEAD `6064da2f4`，2026-11-12；未运行测试）：** `EncryptedLogRecordStore.flush(timeoutMs)` 在 writer shutdown、队列无法入队、marker/紧急线程超时、中断或观测到新增普通写入失败时返回 `false`。当前 `ObservabilityExportService.export()` 传入 `FLUSH_TIMEOUT_MS = 300`，没有检查返回值而继续 `readRecords()`；读取按 500 条分页、最多 5,000 条。`ObservabilityExportServiceTest.PageStore.flush()` 仍固定返回 `true`；`coroutine cancellation during record flush stops before export` 覆盖取消，不覆盖 `false` 结果。flush 失败传播仍未实现或覆盖，本次只复核源码和测试定义，未运行测试。当前超时、分页与忽略失败结果的行为已写入[诊断日志导出](../../../product/diagnostics/export.md)；用户可见失败语义确定并实现后再更新为最终合同。

<a id="task-3"></a>

## Task 3：在取得诊断文档输出流后立即建立关闭责任

- [ ] 由取得 OutputStream 的 owner 覆盖取得流后的全部阶段，包括 ensureActive、manifest 创建和编码；取消、普通异常、fatal 均关闭已取得流，保留目标删除补偿和异常主次序。
- [ ] 覆盖 openDocument 返回时取消、manifest 创建失败、编码/关闭失败及成功；断言流关闭次数、目标补偿、授权单次消费及异常传播。协调 encoder 现有 use 合同，避免重复关闭掩盖根因。

**源码与测试定义复核（HEAD `6064da2f4`，2026-11-12；未运行测试）：** `exportToDocument()` 取得 `OutputStream` 后先执行 `ensureActive()`，再调用 `writeExport()`；`writeExport()` 先创建 manifest，再进入 encoder。encoder 的 `use` 只在调用开始后承担关闭责任。因此，打开后取消或 manifest 创建失败时，服务会删除 URI，但没有关闭已取得的流。现有 `ObservabilityExportServiceTest` 覆盖进入编码后的写入失败/取消与目标删除，没有覆盖打开流后、encoder 启动前的取消或 manifest 失败；测试流也未记录关闭次数。本次复核源码和测试定义，未运行测试。涉及入口为 [`ObservabilityExportService.exportToDocument()` / `writeExport()`](../../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) 与 [`DiagnosticsExportEncoders`](../../../../app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt)。
