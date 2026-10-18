# 可观测性架构

状态：当前实现（代码核对：2026-10-18）。

Passly 只有一套结构化日志管线。业务代码提交类型化 `LogStatement`，底层统一完成 Trace、字段校验、
异常归类、隐私清洗、采样/聚合和 Sink 分发；业务层不得拼 JSON、直接写文件或调用 `android.util.Log`。

## 分层

| 层 | 职责 |
| --- | --- |
| `core/observability` | Logger API、事件与字段目录、Trace、隐私策略、聚合和 Sink 契约 |
| `app/observability` | Android 装配、Logcat、Perfetto、崩溃捕获、导出编码和目标分发 |
| `data/observability` | 已脱敏记录 codec、加密日志文件、轮换和分页读取 |
| `feature/settings/diagnostics` | 查看与导出的应用层合同，不依赖 Android UI |
| Settings presentation | 查看、筛选、搜索、复制、清除、保存与分享交互 |

唯一数据链为：

```text
Logger 调用点
  -> DefaultLogger
  -> PrivacyPolicy
  -> EventAggregator
  -> Logcat / local alert / crash capture / encrypted store
```

所有 Sink 只接受不可变的 `SanitizedLogRecord`。隐私边界以前的 `Throwable`、待 HMAC 标识符和业务对象
不能进入 codec、Logcat 或导出编码器。

## 主题文档

- [记录、错误与 Trace](record-and-trace.md)
- [运行时、Logcat 与加密存储](runtime-and-storage.md)
- [JSONL、TSV 与 ZIP 导出协议](export-formats.md)
- [诊断查看与导出功能](../../features/diagnostics/README.md)
- [诊断安全与隐私](../../security/diagnostics.md)
- [错误码目录](../../ERROR_CODES.md)（由 Gradle 生成，禁止手工修改）

架构决策见 [ADR-0024](../../decisions/ADR-0024-structured-observability-logger.md)。
