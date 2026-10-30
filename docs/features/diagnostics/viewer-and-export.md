# 诊断查看与导出

状态：当前实现（代码核对：2026-10-30）。

诊断日志是“设置 → 数据管理”下的独立二级页面。数据管理页负责缓存/日志占用统计和清理入口；诊断页负责
查看、过滤、搜索、复制、保存和分享。两页不共享 ViewModel 状态。记录和 Trace 合同见
[可观测性架构](../../architecture/observability/record-and-trace.md)。

## 页面行为

进入诊断页立即刷新最多 500 条记录。默认显示 INFO、WARN、ERROR，FATAL 归入 ERROR，DEBUG 默认关闭。
筛选只影响当前页面；搜索对 trace ID、事件名、错误码、稳定异常类别和异常类型执行不区分大小写的包含匹配。

每行使用等宽文本，WARN 有有限强调，ERROR/FATAL 使用错误色，同时保留等级文字。长按只提供复制该行的
已脱敏 TSV，或复制同一 Trace（最多 100 行）。复制经共享敏感剪贴板端口执行，使用 Android 敏感标记并遵守隐私设置中的保留策略；
文本不进入可保存 Compose 状态。离开诊断页会取消尚未发布的复制。清除成功后同时清空 retained 和 visible 状态；排队写入未能及时排空
或文件删除失败时保留当前内容并显示清除失败提示。数据管理页也会将清除失败显示为不可用的日志占用状态，避免把旧占用误报为清空。
诊断页不统计加密日志占用，该数值在数据管理页的“清除日志”行右侧显示。

“数据管理”的日志 section 在“查看日志”上方以内嵌下拉框配置本地记录等级，默认 INFO 及以上，可选关闭、WARN 及以上
或全部等级。它只控制新记录是否写入本地加密日志文件；诊断查看页的等级筛选仍只影响显示。关闭或降低等级不会
删除已有日志，用户可从诊断页菜单显式清除。错误处理、崩溃原 handler 委托和 Trace 不受该设置控制。

## 保存与分享

Top app bar 提供保存、分享和清除。保存与分享分别打开相同风格的格式 sheet，可选择 JSONL、TSV 或 ZIP，
并显示每种格式的用途。选择器取消时清除 pending 格式。

两种动作共用 `DiagnosticsExportRequest(action, format, documentUri)`：

- `EXPORT_LOCAL` 必须携带系统 `CreateDocument` 返回的 `content://` URI；认证成功后直接写入，不创建分享缓存。
- `SHARE` 禁止携带 URI；认证成功后在 `cacheDir` 创建临时明文文件，通过 FileProvider 只读分享。

两种动作都要求 `EXPORT_DIAGNOSTICS` 的新鲜授权。保存取消、拒绝或写入失败时尝试删除系统选择器预创建的
未完成文档。分享失败立即删除临时文件；成功后尝试安排 10 分钟删除。若系统不接受延迟删除任务，保留分享文件供接收方读取，并由下一次分享的陈旧文件清理兜底；该清理只删除超过 10 分钟的文件。

诊断保存不复用备份目录。Android 的 `ACTION_CREATE_DOCUMENT` 由用户选择单个目标文档；它与备份使用的
目录 tree grant 是不同授权合同。

导出在读取日志、创建输出和开始分享之间检查协程取消。取消会继续向调用方传播，不转换为普通失败结果；分享
尚未启动时删除已创建的临时明文文件，本地文档写入失败或取消时尽力删除系统选择的目标。同步编码期间无法
中断的取消会在编码返回后检查并清理；同步日志读取也会在读取完成后检查取消。清理异常不会替换原始取消或失败。

平台依据：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 诊断查看与操作界面 | `DiagnosticsScreen()` | [`DiagnosticsScreen.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/diagnostics/DiagnosticsScreen.kt) |
| 诊断页面状态与用户动作 | `DiagnosticsViewModel.onAction()`、`DiagnosticsEffect.ClearFailed` | [`DiagnosticsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt)、[`DiagnosticsContract.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsContract.kt) |
| 诊断复制与剪贴板保留策略 | `SensitiveClipboardWriter`、`ClipboardCopyController.writeSensitiveIf()` | [`SensitiveClipboardWriter.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/clipboard/port/SensitiveClipboardWriter.kt)、[`ClipboardCopyController.kt`](../../../app/src/main/java/com/aozijx/passly/app/clipboard/ClipboardCopyController.kt) |
| 诊断文件导出、丢弃与过期文件清理 | `ObservabilityExportService.export()`、`discard()`、`clearStaleExports()` | [`ObservabilityExportService.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
