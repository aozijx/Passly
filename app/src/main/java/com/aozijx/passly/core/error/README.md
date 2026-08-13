# Passly 错误处理模块

## 分层边界

`AppError` 是跨 data/domain/ui 的统一错误模型。所有 `AppError` 子类放在
`com.aozijx.passly.core.error.model` 包内，便于统一错误码、严重级别、可恢复性和 errorId。

物理目录按职责拆分：

- `model/`：`AppError`、错误码常量、具体错误类型。
- `result/`：`AppResult`（Success/Failure、map/fold、异常转换）。
- `mapping/`：`Throwable` 到 `AppError` 的安全映射。
- `boundary/`：低层边界异常（CryptoException、DatabaseException）。

`presentation/` 目录已移除。UI 文案映射已迁移到 `app/message/mapping/` 或 Feature 的 presentation 层。

低层框架异常保留在专用包：

- `core.error.boundary.CryptoException`
- `core.error.boundary.DatabaseException`

这些异常不能直接透传到 UI。离开低层边界前必须经过 `AppErrorMapper.fromThrowable()` 映射成
`AppError`。

## 当前机制

- `AppResult.runSuspendCatching`：捕获异常并转为 `AppResult.Failure(AppError)`。
- `AppErrorMapper.fromThrowable()`：把框架异常和低层自定义异常映射为稳定错误码。
- `ErrorMessages.toUiMessage()`：把 `AppError.code` 映射为用户可读文案（位于 `app/message/mapping/`）。
- `AppErrorReporter.report(error, context)`：在 repository/use case 边界统一记录结构化日志（属
  telemetry 层，error 不知道 telemetry 的存在）。

## 依赖方向

```
core/error -X-> telemetry
core/error -X-> AppTelemetry
core/error -X-> message center
telemetry  -X-> UI 文案
message    -X-> Throwable
```

只允许单向协作：异常 → mapper → AppError/AppResult → Reporter（可选）或 UI/NoticeMapper。

## 使用原则

1. DAO、加密、文件、系统 API 可以抛低层异常。
2. repository/use case 边界返回 `AppResult`，不要向 UI 透传任意 `Throwable`。
3. UI 只消费 `AppError` 的 `code`，通过 `ErrorMessages.toUiMessage(code)` 获取文案。
4. 不在每一层重复打日志；只在明确边界调用 `AppErrorReporter.report()`。
5. `AppError` 不保存固定中文 UI 文案，文案由稳定的 error code 映射产生。

## 隐私规则

- 不拼接 `Throwable.message`。
- 不拼接密码、恢复码、OTP secret、密钥 alias、数据库路径、文件路径、URL、域名、包名、entryId。
- 需要诊断时使用 `ErrorReportContext` 的 `operation` 和 `category`，或遥测的 `SafeLogValue`，不要用自由字符串。
- 低层异常可以保留 `cause`，但 mapper 输出的 `AppError` 必须是固定安全错误码。

## 相关文档

- [错误处理架构](../../../docs/architecture/error-handling.md)
- [遥测与诊断日志](../../../docs/architecture/telemetry.md)
- [消息中心](../../../docs/architecture/message-center.md)