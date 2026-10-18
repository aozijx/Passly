# 诊断导出格式

状态：当前实现（schema 1，代码核对：2026-10-18）。

一次导出只读取、排序并生成一次 manifest。`JSONL` 是 canonical 事件表示，`TSV` 是其固定的人类可读投影，
`ZIP` 打包两种表示与一个 manifest。查看筛选和搜索不改变导出内容。

## Manifest

独立 JSONL 的第一行是 manifest JSON；独立 TSV 的表头前是一行 `# key=value` manifest。ZIP 只包含一个
`manifest.json`，内部 JSONL/TSV 不重复 manifest。

当前 manifest 字段为：`type=manifest`、`schema_version`、生成时间、记录数、App 名称与版本、设备制造商与
型号、SDK、系统版本、locale 和时区。禁止 Android ID、Build fingerprint、serial、明文包名和已安装应用列表。

## JSONL

事件字段按以下稳定顺序输出：

```text
time, timestamp_ms, elapsed_realtime_ms, level, event, event_id,
context, error, trace, session
```

缺失的可选对象不写 `null`。`context` 至少含 category，并优先排列 result、purpose、trigger、reason_code、
elapsed_ms、timeout_ms、retry_count，其余已注册字段按键排序。异常只包含清洗后的 kind、type、应用栈、cause
类型和外部帧数量。

## TSV

TSV 固定八列：

```text
time	level	event	outcome	details	exception	trace	session
```

空列保留，变化字段编码为确定顺序的 `key=value` bag。Tab、换行、反斜杠、分号和等号按目标层级转义。
未来 JSONL 未知字段会扁平化进入 `details`，不能静默丢失。

## ZIP

ZIP 恰好包含 `manifest.json`、`logs.jsonl`、`logs.tsv`、`README.txt`。ZIP entry 时间固定为 0，避免同一输入
产生无意义的时间差异。`logs.jsonl` 和 `logs.tsv` 不含内嵌 manifest。

## 命名与目标

| 格式 | MIME | SAF 建议名 |
| --- | --- | --- |
| JSONL | `application/x-ndjson` | `passly_logs.jsonl` |
| TSV | `text/tab-separated-values` | `passly_logs.tsv` |
| ZIP | `application/zip` | `passly_logs.zip` |

分享缓存使用 `passly_logs_<time>.<ext>`。当前只有 schema 1，不存在旧格式兼容路由。格式变化必须先定义
schema 演进和解析行为，不能用文件名中的 v1/v2 代替协议字段。

相关实现：[`DiagnosticsExportEncoders`](../../../app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt)。
