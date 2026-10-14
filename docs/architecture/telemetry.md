# Logger v1 与诊断日志

> 正式 Logger v1 的目标设计与迁移边界见
> [Passly Logger v1 与可观测性系统设计](../superpowers/specs/2026-10-14-observability-logger-v1-design.md)。
> Logger v1 正在按模块迁移；当前 `.elog1` 文件格式不代表正式 Logger API 版本，业务代码不得继续扩展
> per-event `correlationId` 模型。

## Logger v1 契约

`core/observability` 已定义唯一业务 API：事件、错误码、原因码、操作码和字段名均为验证后的值类型；字段只允许
计数、时长、比率、布尔、枚举、受控代码和待 HMAC 的标识符。`Logger.warn/error` 的签名强制要求
`ErrorCode + ReasonCode + Throwable`，已知但没有异常对象的降级使用 `degraded`。

业务调用不能构造 `SanitizedLogRecord` 或直接调用 Sink。后续隐私层必须把 pending 标识符替换为 HMAC、把
Throwable 转换为不含 message/path 的安全快照后，才能将不可变记录交给 `LogSink`。Trace API 已固定在 Logger
契约中，具体协程传播和 Android/加密 Sink 在后续迁移任务接入。

## 分层

- `core/telemetry`：纯事件、安全字段、等级、类别、Emitter 与 Policy 契约。
- `app/diagnostics`：运行时装配、崩溃处理和认证后的临时明文导出。
- `data/diagnostics`：加密文件存储与二进制 codec。
- `AndroidLogSink`：唯一允许调用 `android.util.Log` 的位置。

业务事件名使用受控 lower-dot 标识符。持久化字段只允许计数、时长、有限比率、布尔、枚举名、错误码和操作码。
自由文本 message、Throwable message、密码、Token、邮箱、URL 与文件路径不得进入事件。无法注入的 framework
App/Android 框架回调可以使用 `AppTelemetry` 桥；其 legacy message 会被丢弃。Data、Security 和普通对象
必须注入 `TelemetryReporter`，共享模块不得依赖应用级全局桥。

## 加密文件 v1

文件位于 `noBackupFilesDir/diagnostics_v1`，扩展名为 `.elog1`。每个文件生成独立 256-bit 数据密钥；
数据密钥由 Android Keystore 中的 v1 wrapping key 以 AES-256-GCM 包装。

- Header AAD：magic、format version、16-byte fileId、createdAt。
- Record AAD：magic、format version、fileId、严格递增 sequence、level。
- wrapping nonce 与每条 record nonce 都独立随机生成且必须为 12 bytes；GCM tag 为 128 bits。
- 解码严格校验长度、UTF-8、枚举范围、sequence、外层/载荷 level 一致性和尾随字节。
- 读取使用 `fileId + byte offset + sequence` 游标，不把整个文件载入内存。
- writer 是容量 256 的单线程队列；文件按日期或 1 MiB 轮换，最多 3 个文件、3 MiB、3 天。

文件日志默认关闭。设置写入独立 Proto 的绝对截止时间，当前窗口为 24 小时；debug 构建也不得绕过开关。
Android sink 有独立开关。

## 崩溃和导出

运行时预生成 emergency 数据密钥。崩溃时先 flush 普通队列 300 ms；失败后 emergency writer 最多等待
200 ms，且不调用 Keystore、不等待普通 writer lock。队列溢出只为 ERROR/FATAL 尝试一次独立 fallback。

查看最多读取 500 条。明文导出仅包含本地当天最多 5000 条日志，并在存储层以最多 500 条一页跨文件读取；
导出必须先完成 `EXPORT_DIAGNOSTICS` 新鲜认证，文件只写入 `cacheDir`，
通过 FileProvider 只读分享，10 分钟后删除；新导出前会清理旧临时文件。明文导出不属于备份内容。

禁止恢复旧 `core/diagnostics`、`AppLog`、Sanitizer、旧 Runtime/Sink/CrashHandler，也禁止直接
`printStackTrace` 或在其他文件使用 `android.util.Log`。

## Android 系统日志边界

当前 legacy 实现及正式 Logger v1 都只允许单一 Android Sink 调用 `android.util.Log`。Logcat 用于 adb、
Android Studio 和系统
时间线中的即时诊断，不替代加密持久化日志。事件必须先完成字段校验、异常快照和脱敏，再按 Android 原生
DEBUG/INFO/WARN/ERROR 优先级输出；业务层不得直接打印异常或敏感字段。

Logger v1 将使用有限稳定 Tag 和有界单行摘要，并与加密记录共享 `event_id/trace_id/diag_id`。根据 Android 官方
关于 Log 信息泄露的建议，Release 默认完全关闭 Logcat Sink；显式诊断窗口只启用内部加密日志，不提升 Logcat。
Debug Sink 在格式化前使用 `Log.isLoggable`，且永远不把原始 Throwable 交给 Android Log API。

Logger v1 还会把受控 Span 名称和匿名计数接入 `android.os.Trace`，用于 Perfetto 性能分析；并在下次启动时从
`ApplicationExitInfo` 读取稳定的退出 reason/status/importance/内存数值。系统提供的 description、进程名和原始
ANR trace 不进入标准日志或导出。

## 错误接入规范

Telemetry 可以观察 Error，但 Error 不知道 Telemetry 的存在。依赖方向：

```
core/error -X-> core/telemetry
core/error -X-> AppTelemetry
```

### AppErrorReporter

```kotlin
fun interface AppErrorReporter {
    fun report(error: AppError, context: ErrorReportContext)
}

data class ErrorReportContext(
    val operation: OperationCode,
    val category: EventCategory,
)
```

Reporter 属于 telemetry 层，不属 error 层。在 repository / use case 边界统一调用一次，避免重复记录。

### 白名单字段

实现只能写入以下字段，禁止自由文本：

| 允许                    | 禁止                   |
|-----------------------|----------------------|
| error_code            | error.message        |
| error_layer           | Throwable.message    |
| severity              | entryId              |
| recoverable           | 文件路径                 |
| operation             | URL / 域名             |
| correlation_id        | 包名                   |
| throwable_type（白名单校验） | 用户名、备份 URI、密码、密钥、OTP |

### 记录策略

| 错误类型      | 日志策略             |
|-----------|------------------|
| 输入校验失败    | 通常不记录            |
| 条目不存在     | 通常不记录            |
| 乐观锁冲突     | WARN，可计数         |
| 用户取消认证    | 不记录              |
| 密码错误      | 匿名计数，不能记录密码或次数明细 |
| 数据库初始化失败  | ERROR            |
| 加密 Tag 失败 | ERROR            |
| 备份格式错误    | WARN             |
| 未知异常      | ERROR            |

### 接入示例

```kotlin
// DatabaseTransactionRunner 在数据库操作失败后报告
private fun report(error: AppError, operation: String) {
    errorReporter.report(
        error = error,
        context = ErrorReportContext(
            operation = OperationCode(operation),
            category = EventCategory.DATABASE
        )
    )
}

// BackupArchiveServiceImpl 在导入/导出失败后报告
private fun report(error: AppError, operation: String) {
    errorReporter.report(
        error = error,
        context = ErrorReportContext(
            operation = OperationCode(operation),
            category = EventCategory.BACKUP
        )
    )
}
```
