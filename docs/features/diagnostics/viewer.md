# 诊断日志查看与管理

状态：当前实现（代码核对：2026-11-06）。

诊断日志是“设置 → 数据管理”中的独立二级页面。数据管理页配置本地日志等级、查看占用和清理缓存/日志；诊断页查看、筛选、搜索、复制和清除诊断记录。两页不共享 ViewModel 状态。记录和 Trace 合同见[可观测性架构](../../architecture/observability/record-and-trace.md)。

## 浏览与复制

`DiagnosticsViewModel.onPageEntered()` 创建访问 owner、清除旧保存目标状态并调用 `refresh()`。`refresh()` 先检查 `SecureSessionAccessState.hasFullSecureSessionAccess()`，再以 `logStore.readLines(MAX_VIEW_LINES)` 读取最多 500 条记录；发布结果前确认 page owner 与 refresh owner 仍有效。普通 `Exception` 转成无详情 `RefreshFailed`；`CancellationException` 与 fatal `Error` 继续传播。手动刷新失败保留已有行，异常源结束后不会假定自动复活，只能由新的刷新重新读取。

默认显示 INFO、WARN、ERROR，FATAL 映射到 ERROR，DEBUG 默认关闭。`ToggleLevel(level)` 与 `SearchChanged(query)` 只变更当前 ViewModel 的可见投影；搜索对 Trace ID、事件名、错误码、稳定异常类别和异常类型执行不区分大小写包含匹配。

单行复制由 `CopyRow(line)` 从 retained rows 中核对当前行后调用 `copyRows(lines)`；Trace 复制由 `CopyTrace(traceId)` 过滤相同 Trace 并裁到 `MAX_COPY_LINES`（100）。`copyRows` 将所选行的脱敏文本合并，以 owner 与 job 两层校验调用 `SensitiveClipboardWriter.writeSensitiveIf(text) { ... }`。离页取消待完成复制，文本不进入可保存 Compose 状态。保存与分享的授权和临时文件边界见[诊断日志导出](export.md)。

## 清理和记录设置

`ConfirmClear` 调用 `clearLogs()`；它拒绝重复清理、检查完整安全会话并等待已有刷新 job 退出，再调用 `logStore.clear()`。成功才清空 retained/visible rows；普通失败保留内容并发出 `ClearFailed`，取消传播。已确认清理不因离开页面而取消，反馈只发布给发起时的 page owner。

数据管理页单独负责缓存/加密日志占用和清理。日志清理失败以“不可用”占用状态呈现，避免旧容量被误报为清空；缓存清理失败后重新读取真实占用，重新读取失败则显示不可用。日志记录等级默认 INFO 及以上，可关闭、调到 WARN 及以上或开启全部等级；此值只控制常规记录是否进入本地加密日志，脱敏 FATAL 崩溃记录仍保留。降低或关闭等级不删除既有记录，错误 handler 和 Trace 不受该开关控制。写入队列、flush 与加密存储合同详见运行时/存储架构页，本页不重复定义协议。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 页面读取、筛选、复制和清理 | `DiagnosticsViewModel.onPageEntered()`、`refresh()`、`copyRows(lines)`、`clearLogs()` | [`DiagnosticsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt) |
| 诊断 UI 与页面访问生命周期 | `DiagnosticsScreen()`、`DiagnosticsRoute()` | [`DiagnosticsScreen.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/diagnostics/DiagnosticsScreen.kt)、[`DiagnosticsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/data/DiagnosticsRoute.kt) |
| 本地记录等级、容量与数据清理 | `StorageSettingsViewModel`、`DataManagementRoute()` | [`StorageSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/data/StorageSettingsViewModel.kt)、[`DataManagementRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/data/DataManagementRoute.kt) |
| 受策略约束的剪贴板写入 | `SensitiveClipboardWriter.writeSensitiveIf()`、`ClipboardCopyController.writeSensitiveIf()` | [`SensitiveClipboardWriter.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/clipboard/port/SensitiveClipboardWriter.kt)、[`ClipboardCopyController.kt`](../../../app/src/main/java/com/aozijx/passly/app/clipboard/ClipboardCopyController.kt) |
