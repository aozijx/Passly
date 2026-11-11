# 诊断日志

状态：当前实现（代码核对：2026-11-06）。

诊断日志由数据管理设置控制本地收集等级，由独立诊断页面查看和导出。本文是主题入口：日志浏览与清理见[查看与管理](viewer.md)，用户授权后的本地保存和分享见[导出](export.md)。记录结构、Trace 与加密落盘见[可观测性架构](../../platform/architecture/observability/record-and-trace.md)、[运行时与存储](../../platform/architecture/observability/runtime-and-storage.md)和[导出格式](../../platform/architecture/observability/export-formats.md)。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 诊断页面浏览与导出编排 | `DiagnosticsViewModel`、`DiagnosticsRoute()` | [`DiagnosticsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt)、[`DiagnosticsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/data/DiagnosticsRoute.kt) |
| 导出授权与文件操作 | `ExportDiagnosticsUseCase.invoke(request)`、`ObservabilityExportService.export(permit, request)` | [`ExportDiagnosticsUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/diagnostics/ExportDiagnosticsUseCase.kt)、[`ObservabilityExportService.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
