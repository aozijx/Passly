# ADR-0024：统一结构化 Observability Logger

状态：已实施（2026-10-15）

## 决策

Passly 使用自研 `core/observability` Logger 作为唯一生产日志入口。业务代码通过注入的 `Logger`、根 Trace/子 Span
和 `LogStatements` 类型化目录记录事件；不提供静态 Runtime、兼容桥或第二套 Autofill 日志系统。Legacy Autofill
与 Credential Manager 保留各自的平台结果对象，只共享底层日志契约。

所有记录先通过字段白名单、异常安全快照和本机 Keystore HMAC，再进入聚合器和 Sink。Debug 构建可输出到 Android
Logcat；Release 不输出 Logcat。`LogcatSink.kt` 是唯一允许直接导入 `android.util.Log` 的生产文件。Perfetto 只接收
受控操作名，不接收业务值。

持久化只写 `noBackupFilesDir/logs/*.log` 的 AES-GCM 容器。旧 Telemetry API、`.elog1` writer/codec、运行时和 DI 已删除；
现有旧文件不扫描、不迁移、不合并，也不由新代码删除。导出在 fresh authorization 后创建 UTF-8 `.txt`，首行为设备/App
manifest，事件行按固定列顺序只写值并保留空列，避免重复键名；临时明文只位于 `cacheDir` 并定时删除。

## 原因与后果

单一路径保证 ERROR/WARN 的错误码、原因、异常快照和 Trace 关联一致，也让脱敏规则、文件轮换和聚合策略只维护一次。
代价是新增事件必须登记到 `EventCatalog`/`LogStatements`，且非注入式工具不得自行写日志，必须把失败返回给注入所有者。
架构测试持续禁止旧 Telemetry 符号、额外 Android Log 调用和旧存储路径重新进入生产源码。
