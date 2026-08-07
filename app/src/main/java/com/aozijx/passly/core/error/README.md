# Passly 错误处理模块

## 分层边界

`AppError` 是跨 data/domain/ui 的统一错误模型，所有 `AppError` 子类放在
`com.aozijx.passly.core.error` 包内，便于统一错误码、严重级别、可恢复性和 trace。

物理目录按职责拆分：

- `model/`：`AppError`、错误码、具体错误类型。
- `result/`：`AppResult` 与失败日志扩展。
- `mapping/`：`Throwable` 到 `AppError` 的安全映射。
- `presentation/`：UI 文案和展示级别映射。
- `boundary/`：低层边界异常。

`model/`、`result/`、`mapping/` 下的 public API 仍使用 `com.aozijx.passly.core.error`
包，避免 AppError 子类分散和调用方无意义 import 迁移。

低层框架异常保留在专用包：

- `core.error.boundary.CryptoException`
- `core.error.boundary.DatabaseException`

这些异常不能直接透传到 UI。离开低层边界前必须经过 `AppErrorMapper.fromThrowable()` 映射成
`AppError`。

## 当前机制

- `AppResult.runCatching/runSuspendCatching`：捕获异常并转为 `AppResult.Failure(AppError)`。
- `AppErrorMapper.fromThrowable()`：把框架异常和低层自定义异常映射为稳定错误码。
- `ErrorMessages.toUiMessage()`：把 `AppError.code` 映射为用户可读文案。
- `ErrorDisplay.displayLevel()`：根据 `severity + recoverable` 给出推荐展示方式。
- `onFailureLog()/logFailureWithContext()`：在 repository/use case 边界 opt-in 记录结构化失败日志。

## 使用原则

1. DAO、加密、文件、系统 API 可以抛低层异常。
2. repository/use case 边界返回 `AppResult`，不要向 UI 透传任意 `Throwable`。
3. UI 只消费 `AppError` 的 `code/message/displayLevel` 或 `toUiMessage()`。
4. 不在每一层重复打日志；只在明确边界调用 `onFailureLog()` 或 `logFailureWithContext()`。

## 隐私规则

`AppError.message` 默认会进入 UI 状态或提示文案，必须视为用户可见文本：

- 不拼接 `Throwable.message`。
- 不拼接密码、恢复码、OTP secret、密钥 alias、数据库路径、文件路径、URL、域名、包名、entryId。
- 需要诊断时使用 `ErrorTrace.operation`、`ErrorTraceValue` 或遥测的 `SafeLogValue`，不要用自由字符串。
- 低层异常可以保留 `cause`，但 mapper 输出的 `AppError.message` 必须是固定安全文案。
