# 错误处理架构

## 三层分离

错误处理拆分为三个独立层，只允许单向协作：

```mermaid
flowchart TD
    EX["Throwable（DAO / 文件 / 密码学）"] -->|"mapper"| AE["AppError / AppResult"]
    AE -->|"suspend report(error, context)"| REP["AppErrorReporter（observability）"]
    FEATURE["Feature result / effect"] -->|"explicit NoticeCode"| MC["消息中心"]
```

- **core/error**：只表达失败和控制流（`AppError`、`AppResult`）。不记录日志、不发布消息、不依赖 Android。
- **core/observability**：可观察 Error，但 Error 不知道 Logger 的存在。通过 `AppErrorReporter`
  契约在边界统一记录。
- **app/message**：承载 `AppNotice`、稳定 `NoticeCode`、注册策略和投递；Feature 根据具体操作结果选择通知，避免把所有 `AppError` 全局映射成消息。

## 禁止反向依赖

Feature 自有的行内错误与全局 Notice 分开处理：`presentation/shared/error/UiErrorMessage.kt` 的
`toUiErrorMessage` 只将稳定错误码映射为 `UiErrorMessage` 资源键，未知错误使用调用方的资源键回退。
数据库重置的失败说明、回收站错误状态、交互设置保存失败效果和应用壳层重试失败效果只保存资源键，
不携带已解析文案、Throwable 或原始异常文本。
Compose 内容用 `stringResource`，Toast 在消费时用当前 `LocalResources` 解析资源；中英文文案在
`values/strings_errors.xml` 与 `values-en/strings_errors.xml`，语言切换不需重建 ViewModel 的错误状态。
取消异常必须继续传播，不能变成失败提示。此映射不记录日志、不发布 Notice，也不改变 Core 的错误码合同。

```
core/error -X-> observability
core/error -X-> Logger
core/error -X-> message center
observability -X-> UI 文案
message    -X-> Throwable
```

## 目录结构

```
core/common/.../core/error/
  model/
    AppError.kt          -- 纯错误模型（code、layer、severity、recoverable、errorId）
    ErrorCodes.kt        -- 集中错误码常量
    DatabaseErrors.kt    -- 数据库错误子类
    CryptoErrors.kt      -- 密码学错误子类
    BackupErrors.kt      -- 备份错误子类
    FileErrors.kt        -- 文件错误子类
    DomainErrors.kt      -- 领域层错误子类
  result/
    AppResult.kt         -- Success/Failure、map/fold、异常转换
  mapping/
    AppErrorMapper.kt    -- Throwable → AppError 安全映射
  boundary/
    CryptoException.kt   -- 密码学边界异常
    DatabaseException.kt -- 数据库边界异常

core/observability/
  LogStatements.kt
  privacy/
  reporting/
    AppErrorReporter.kt   -- suspend Reporter 契约（属 observability，不属 error）
    ErrorReportContext.kt -- 上报上下文（operation + category）

app/observability/
  ObservabilityAppErrorReporter.kt -- Reporter 实现
  ObservabilityRuntimeController.kt -- 生命周期、崩溃与本地存储所有者

app/message/
  model/                   -- AppNotice、NoticeCode 与类型化参数
  contract/                -- NoticeCode 注册策略
  runtime/                 -- 分发、路由和去重
```

## 数据流

### 异常 → AppError

```
DAO / 文件 / 密码学抛出异常
  ↓ (在 Repository 边界被 AppResult.runSuspendCatching 捕获)
AppErrorMapper.fromThrowable() 映射为 AppError
  ↓
AppResult.Failure(AppError)
```

低层可以抛异常（`CryptoException`、`DatabaseException`、`IOException`），但 UI 不收到任意 `Throwable`
。Repository 边界统一返回 `AppResult`。

### AppError → Reporter

```kotlin
fun interface AppErrorReporter {
    suspend fun report(error: AppError, context: ErrorReportContext)
}

data class ErrorReportContext(
    val operation: OperationCode,
    val category: LogCategory,
)
```

在 repository / use case 边界统一调用一次，避免重复记录。实现只能写入白名单字段：

| 允许                    | 禁止                   |
|-----------------------|----------------------|
| error_code            | error.message        |
| error_layer           | Throwable.message    |
| severity              | entryId              |
| recoverable           | 文件路径                 |
| category              | URL / 域名             |
| throwable_type（白名单校验） | 包名                   |
|                         | 用户名、备份 URI、密码、密钥、OTP |

### AppError → UI；Feature result → Notice

```
页面根据 AppError 映射内联资源键
  ↓
UI feedback
```

`AppError` 不直接发布 `AppNotice`。内联显示（Snackbar、输入框错误）由页面根据错误码决定；需要全局通知时，Feature 根据其类型化操作结果明确选择 `NoticeCode`，再交给消息中心分发。Backup 等有独立操作语义的通知映射留在对应 Feature。

## 记录策略

不是所有失败都应该写日志：

| 错误类型      | 日志               | 用户消息          |
|-----------|------------------|---------------|
| 输入校验失败    | 通常不记录            | 内联显示          |
| 条目不存在     | 通常不记录            | Snackbar / 刷新 |
| 乐观锁冲突     | WARN，可计数         | 提示刷新          |
| 用户取消认证    | 不记录              | 不显示           |
| 密码错误      | 匿名计数，不能记录密码或次数明细 | 输入框错误         |
| 数据库初始化失败  | ERROR            | 强制消息 / 恢复入口   |
| 加密 Tag 失败 | ERROR            | 数据损坏提示        |
| 备份格式错误    | WARN             | 导入失败提示        |
| 未知异常      | ERROR            | 通用失败提示        |

## AppError 模型

```kotlin
sealed class AppError(
    open val code: String,          // 稳定错误码，如 "DATABASE_LOCKED"
    open val layer: ErrorLayer,     // DATA / DOMAIN / UI
    open val severity: ErrorSeverity, // WARNING / ERROR
    open val recoverable: Boolean,  // 是否可恢复
    open val errorId: String,       // 关联 ID，用于诊断
)
```

- `code` 是稳定标识符，UI 文案通过受控 mapper 映射。
- 不保存 `message`、`cause`、日志字段、Android 类型。
- 当前仍扩展 `Exception` 以兼容 `throw`/`catch` 语义。异常仅由底层转换为不含 message/path 的安全快照。
- `errorId` 不进入 Trace、诊断值或导出，只用于 Reporter 的进程内有界重复抑制。
- 长期目标：Repository 边界统一返回 `AppResult`，不再 `throw AppError` 子类，届时可移除 `Exception` 继承。

## 相关文档

- [日志记录、错误与 Trace](observability/record-and-trace.md)
- [消息中心](message-center.md)
- [包边界](package-boundaries.md)
- [安全总览](../security/overview.md)
