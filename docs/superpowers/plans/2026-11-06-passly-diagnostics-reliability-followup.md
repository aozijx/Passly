# Passly 诊断日志写入与 flush 可靠性跟进

**范围：** 诊断日志写入失败后的清理、失败状态传播和 flush 可观察语义。本文跟踪可靠性问题，不是安全扫描报告或漏洞清单。

## Task 1：让 fatal 写入失败反映到 flush 结果

- [ ] 检查 `EncryptedLogRecordStore.WriteTask.run()` / `writeBatch(batch)` 的 fatal `Error` 边界。保持 Error 在敏感缓冲及文件回滚清理后原样逸出，同时记录本批失败，不能因 `finally` 仍运行 flush marker 就把未落盘批次报告为成功。
- [ ] 定义普通 flush、生命周期 flush 和 crash flush 对 fatal worker 失败的可观察合同；不能吞 fatal Error，也不能让 flush marker 越过失败批次而返回 success。
- [ ] 为在 `beforeWrite`、编码/加密和文件 append 路径注入 fatal Error 的场景添加测试：确认明文与密文缓冲清理、部分文件回滚、worker uncaught handler 收到原 Error、drop/failure 计数与 flush 返回值不会伪报成功；并验证后续 writer replacement 的语义。
- [ ] 更新[运行时与加密存储](../../architecture/observability/runtime-and-storage.md)及崩溃 flush 记录，区分普通 Exception 失败、fatal Error 传播与边界 marker 完成；不把 fatal 错误包装成 Result/重试状态。

**当前证据（2026-11-06）：** `WriteTask.run()` 只捕获 `Exception`，捕获后为整个 batch 增加 `writeFailureCount`；其 `finally` 无论 `writeBatch(batch)` 是否异常都会执行 `boundary?.run()`。`flush()` 将 marker 排在等待队列边界，并只通过 failure counter 与 `observedWriteFailureCount` 比较返回状态。`writeBatch` 的 `finally` 会清零 encoded/plain 和 encrypted frame 缓冲，append rollback 捕获 Throwable 后截断文件并重抛；但 fatal Error 绕过 `WriteTask.run` 的 Exception catch，故未增加该 counter，已取出的 boundary 仍 count down，flush 可能成功。现有测试验证 emergency writer 的 fatal cleanup 和普通 writer Exception 失败，不验证 normal `WriteTask` fatal path。此为源码确认的可靠性问题候选，尚未以新用例运行复现。

**验收：** fatal Error 清理后仍原样传播；失败批次对 flush 可观察且不伪报成功；普通失败和正常 flush 语义保持清楚；相关测试覆盖清理、回滚及 writer 后续状态。

## Task 2：让诊断导出处理 flush 失败

- [ ] 在 `ObservabilityExportService.export(permit, request)` 中明确处理 `store.flush(FLUSH_TIMEOUT_MS)` 的 `false`：不得继续以普通成功语义导出可能不完整的快照；保留取消传播和 fatal `Error` 语义，并明确本地 SAF 预建目标的删除责任仍由 `ExportDiagnosticsUseCase`/`discard(request)` 承担。
- [ ] 决定 flush 失败对分享和本地保存的用户可见结果与重试路径；如果产品选择允许部分导出，须显式标记不完整并包含可核对的失败原因，不能静默当成完整导出。
- [ ] 为 flush 超时、普通写入失败和成功分别覆盖 SHARE/EXPORT_LOCAL 的结果、文件/URI 清理与授权 permit 单次消费；确保空记录也不能掩盖 flush 失败。
- [ ] 同步更新[诊断日志导出](../../features/diagnostics/export.md)，记录 flush 超时、分页上限和明确的失败合同。

**当前证据（2026-11-06）：** `EncryptedLogRecordStore.flush(timeoutMs)` 在 writer shutdown、队列无法入队、marker/紧急线程超时、中断或观测到新增普通写入失败时返回 `false`。`ObservabilityExportService.export()` 传入 `FLUSH_TIMEOUT_MS = 300`，但丢弃返回值并继续 `readRecords()`；读取按 500 条分页、最多 5,000 条。现有 `ObservabilityExportServiceTest` 搜索结果未见针对 flush=false 的断言；需要在后续实现前核实完整测试覆盖。源码确认当前调用没有把失败反馈给导出调用方，属于待修逻辑，不是已验证的设备复现。
