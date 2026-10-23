# 诊断查看与导出

状态：当前实现（代码核对：2026-10-18）。

诊断日志是“设置 → 数据管理”下的独立二级页面。数据管理页负责缓存/日志占用统计和清理入口；诊断页负责
查看、过滤、搜索、复制、保存和分享。两页不共享 ViewModel 状态。记录和 Trace 合同见
[可观测性架构](../../architecture/observability/record-and-trace.md)。

## 页面行为

进入诊断页立即刷新最多 500 条记录。默认显示 INFO、WARN、ERROR，FATAL 归入 ERROR，DEBUG 默认关闭。
筛选只影响当前页面；搜索对 trace ID、事件名、错误码、稳定异常类别和异常类型执行不区分大小写的包含匹配。

每行使用等宽文本，WARN 有有限强调，ERROR/FATAL 使用错误色，同时保留等级文字。长按只提供复制该行的
已脱敏 TSV，或复制同一 Trace（最多 100 行）。复制内容使用 Android 敏感剪贴板标记，不进入可保存 Compose
状态。清除成功后同时清空 retained 和 visible 状态。诊断页不统计加密日志占用，该数值在数据管理页的
“清除日志”行右侧显示。

## 保存与分享

Top app bar 提供保存、分享和清除。保存与分享分别打开相同风格的格式 sheet，可选择 JSONL、TSV 或 ZIP，
并显示每种格式的用途。选择器取消时清除 pending 格式。

两种动作共用 `DiagnosticsExportRequest(action, format, documentUri)`：

- `EXPORT_LOCAL` 必须携带系统 `CreateDocument` 返回的 `content://` URI；认证成功后直接写入，不创建分享缓存。
- `SHARE` 禁止携带 URI；认证成功后在 `cacheDir` 创建临时明文文件，通过 FileProvider 只读分享。

两种动作都要求 `EXPORT_DIAGNOSTICS` 的新鲜授权。保存取消、拒绝或写入失败时尝试删除系统选择器预创建的
未完成文档。分享失败立即删除临时文件；成功后 10 分钟删除，并在下一次分享前清理陈旧文件。

诊断保存不复用备份目录。Android 的 `ACTION_CREATE_DOCUMENT` 由用户选择单个目标文档；它与备份使用的
目录 tree grant 是不同授权合同。

相关实现：

- [`DiagnosticsScreen`](../../../app/src/main/java/com/aozijx/passly/presentation/feature/settings/ui/diagnostics/DiagnosticsScreen.kt)
- [`DiagnosticsViewModel`](../../../app/src/main/java/com/aozijx/passly/presentation/feature/settings/main/diagnostics/DiagnosticsViewModel.kt)
- [`ObservabilityExportService`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt)

平台依据：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。
