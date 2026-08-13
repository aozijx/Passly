# Telemetry 与诊断日志

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

查看最多读取 500 条。明文导出必须先完成 `EXPORT_DIAGNOSTICS` 新鲜认证，文件只写入 `cacheDir`，
通过 FileProvider 只读分享，10 分钟后删除；新导出前会清理旧临时文件。明文导出不属于备份内容。

禁止恢复旧 `core/diagnostics`、`AppLog`、Sanitizer、旧 Runtime/Sink/CrashHandler，也禁止直接
`printStackTrace` 或在其他文件使用 `android.util.Log`。

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
// VaultTransactionRunner 在数据库操作失败后报告
private fun report(error: AppError, operation: String) {
    errorReporter.report(
        error = error,
        context = ErrorReportContext(
            operation = OperationCode(operation),
            category = EventCategory.DATABASE
        )
    )
}

// VaultBackupServiceImpl 在导入/导出失败后报告
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
