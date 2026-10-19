# 日志记录、错误与 Trace

状态：当前实现（代码核对：2026-10-19）。

## Logger 合同

业务层通过注入的 `Logger` 调用 `logger.log(LogStatements.<领域>.<事件>(...))`。当前事件目录覆盖
Application、Database、Security、Autofill、Backup 和 Vault。类型化语句绑定等级、事件、错误码、原因码和允许字段，
调用点不重复导入协议常量。

- `DEBUG`、`INFO` 用于观察和状态转换。
- 已知降级但没有异常对象时使用 `degraded`。
- `warn`、`error`、`fatal` 强制传入 `ErrorCode + ReasonCode + Throwable`。
- Sink 失败不能改变业务控制流；业务异常与协程取消仍按原实例传播。

错误码和原因码的唯一来源是 `config/observability/error-catalog.json`。Gradle 生成 core 类型、App 用户提示
resolver 和 `docs/ERROR_CODES.md`。错误码按 Application、Database、Security、Autofill、Backup 分组，但 wire
值不依赖 Kotlin 枚举名。只有 `USER_ACTIONABLE` 错误能映射本地化提示；UI 不得回退显示原始错误码。

持久化 codec 中的等级、类别和异常类别均使用显式 `wireId`，禁止写入 `enum.ordinal`。已有 ID 在
schema 1 内保持不变，新增值只能分配未使用 ID，枚举重排不得改变日志协议。

## Vault 写操作

条目创建由 `CreateEntryUseCase` 持有 `vault_entry_create` Span，无论来自编辑器还是 Autofill 保存，均输出
`vault.entry.create_started`，并以 `vault.entry.create_completed` 或 `vault.entry.create_failed` 结束。允许字段仅为
`entry_type`、`trigger` 和 `elapsed_ms`；`trigger` 区分 `USER` 与 `AUTOFILL`。不得记录条目 ID、标题、账号、
密码、URL、SSID、应用包名或草稿内容。

受控 `AppResult.Failure` 保持原返回值并按错误严重级别输出；未包装异常记录原始 `Throwable` 以便底层生成
脱敏应用栈后再原样抛出；协程取消不记为业务失败。ViewModel、Autofill 适配层和 Repository 不得重复记录
同一次创建失败。

## Trace 所有权

Trace 属于一次用户操作或系统回调，不属于模块。入口使用 `withRootTrace` 创建随机 `trace_id`；内部操作使用
`inSpan` 继承 Trace 并创建新的 `span_id/parent_span_id`。Security 在 Autofill 操作中只是子 Span，独立调用时
才创建自己的根 Trace，因此不存在 Security 对 Autofill 模块的依赖。

`TraceContextElement` 在协程与线程上下文间传播 Trace。跨 Service、PendingIntent、Activity 回调只传递
process-local 的 opaque `TraceToken`：默认单次消费、最长 10 分钟、最多 128 个，并绑定
`process_session_id`。进程重启、会话不匹配、过期或重复消费都不能恢复旧上下文，更不能恢复授权。

认证事件记录受控 `purpose`。锁定使用 `SessionLockRequest(reason, trigger)`，分别表达策略原因和实际触发信号。
例如后台超时、屏幕关闭、翻转、Autofill 完成和 App 退出不能压缩成同一个“锁定”原因。

## 异常语义

`ExceptionMapper` 依据错误码上下文把异常映射为稳定类别：`DATABASE_IO`、`CRYPTO`、
`AUTHENTICATION`、`PLATFORM`、`LIFECYCLE`、`IO` 或 `UNKNOWN`。它不读取 `message`、
`localizedMessage` 或 suppressed exception。

安全快照最多保留 24 个 Passly 应用栈帧、3 层 cause 的简单类型、外部栈帧计数，以及稳定
`exception_kind` 与受限异常类型。应用栈帧格式为 `相对类名#方法(源码文件:行号)`；源码文件名只接受
受限的 `.kt`/`.java` 标识符，不接受路径。Debug 通过 Passly 包前缀识别应用帧；Release 的 R8 短类名帧
通过已保留的 `SourceFile + LineNumberTable` 生成可读源码类名和行号，仍不输出第三方完整包名。方法名和
异常类型在 Release 中可能被混淆，因此错误语义必须由 `error_code + reason_code + exception_kind` 承担，
精确还原仍使用与 App 版本匹配的 R8 mapping。失败通过同一操作的 `trace_id` 关联，不生成第二套
diagnostic/correlation ID。

## 聚合和采样

`EventAggregator` 只接收已脱敏记录。默认维护 256 个 LRU bucket，窗口一分钟；聚合键不包含 Trace、Span 或
原始标识符。只有 `from_state == to_state` 的重复 DEBUG/INFO 观察可以抑制，窗口首条仍输出。
真实状态转换以及 WARN/ERROR/FATAL 永不采样。聚合快照带递归保护，Logger 自身指标不会再次进入聚合器。

相关实现：

- [`Logger`](../../../core/src/main/kotlin/com/aozijx/passly/core/observability/Logger.kt)
- [`LogRecord`](../../../core/src/main/kotlin/com/aozijx/passly/core/observability/LogRecord.kt)
- [`LogStatements`](../../../core/src/main/kotlin/com/aozijx/passly/core/observability/LogStatements.kt)
- [`PrivacyPolicy`](../../../core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/PrivacyPolicy.kt)
