# ADR-0024：统一结构化 Observability Logger

状态：已实施（2026-10-15）

## 决策

Passly 使用自研 `core/observability` Logger 作为唯一生产日志入口。业务代码通过注入的 `Logger`、根 Trace/子 Span
和 `LogStatements` 类型化目录记录事件；不提供静态 Runtime、兼容桥或第二套 Autofill 日志系统。Legacy Autofill
与 Credential Manager 保留各自的平台结果对象，只共享底层日志契约。

一次操作只使用 `trace_id` 关联所有子事件。失败不再创建、传播或持久化第二套 diagnostic id；`error_code`、
`reason` 和安全异常快照表达失败语义。删除该字段后私有 record codec 直接升级并拒绝旧 schema，不提供迁移读取器。
模块不是 Trace 所有者：Security 使用 `inSpan` 继承当前 App/Autofill 操作，独立调用才自动建立根 Trace。认证目的、
锁定策略原因和触发信号均使用独立受控字段，避免把 Autofill、后台、screen-off 与翻转锁定混为一类。

所有记录先通过字段白名单、异常安全快照和本机 Keystore HMAC，再进入聚合器和 Sink。Debug 构建可输出到 Android
Logcat；Release 不输出 Logcat。`LogcatSink.kt` 是唯一允许直接导入 `android.util.Log` 的生产文件。Perfetto 只接收
受控操作名，不接收业务值。

持久化只写 `noBackupFilesDir/logs/*.log` 的 AES-GCM 容器。导出在 fresh authorization 后按用户选择生成 JSONL、
固定八列 TSV 或包含 JSONL/TSV/manifest/README 的 ZIP。“保存”直接写入用户通过 SAF 选择的目标；“分享”才在
`cacheDir` 创建临时明文并定时删除。JSONL 是 canonical 模型，TSV 只做可读投影。

Throwable message 永不进入记录。core 的 `ExceptionMapper` 将失败归入稳定的受控 `exception_kind`，有界安全快照
额外保留 Passly 栈帧用于定位；私有 codec schema 3 不兼容读取旧记录。相同状态的低等级重复观察按一分钟聚合，真实
状态变化和 WARN/ERROR/FATAL 始终输出。Autofill 仅记录平台可观察的发现、认证、结果交付与会话关闭终态并携带
单调 `elapsed_ms`，不伪造 `field_filled`。

## 原因与后果

单一路径保证 ERROR/WARN 的错误码、原因、异常快照和 Trace 关联一致，也让脱敏规则、文件轮换和聚合策略只维护一次。
代价是新增事件必须登记到 `EventCatalog`/`LogStatements`，且非注入式工具不得自行写日志，必须把失败返回给注入所有者。
生产代码只通过 Logger 记录事件，`LogcatSink.kt` 是唯一直接使用 Android Log 的所有者。
