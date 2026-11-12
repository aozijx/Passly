# 错误标识与反馈标准化

状态：进行中

## 目标

以 `config/observability/error-catalog.json` 作为稳定错误标识和原因标识的唯一来源；明确业务 `AppError`、诊断 `ErrorCode`/`ReasonCode` 与展示 `NoticeCode` 的边界。错误码标识失败类型，原因码标识具体原因，UI 只接收本地化资源键或受控 Notice。

## 原始审计基线

以下发现记录的是本计划创建时的状态，不再代表当前工作树：

- 日志架构文档已要求错误目录生成 `core:common` 应用错误码常量，但多数 `AppError` 仍从手写 `core/error/model/ErrorCodes.kt` 取值；只有 `BackupFailed` 使用生成常量。
- `LogStatements.ApplicationError.failure()` 将 `AppError.code` 同时写成 `error_code` 和 `reason_code`，造成原因信息重复且丢失；`BackupFailed.reasonCode` 未进入此上报路径。
- `AppError.code` 当前是裸 `String`；展示层有按错误子类选资源的局部分支，部分可以归回稳定错误码映射。
- 从 Throwable 映射的失败提示必须传播取消与 fatal `Error`；当前错误显示扩展会把 fatal `Error` 转成普通提示。

## 当前工作树状态（静态核查）

- `AppError.code` 已改为 `AppErrorCode`；当前各 `AppError` 子类使用生成的 `CatalogErrorCodes`，旧手写 `ErrorCodes.kt` 已删除，并已通过 App Kotlin 编译和架构/错误目录校验。
- `LogStatements.ApplicationError.failure()` 已分别取得 error code 与 failure reason；显式 `reasonCode` 优先，否则从目录读取 `default_reason`。仍需验证具体错误覆盖和最终日志字段。
- `Throwable.toUiErrorMessage()` 现在先传播 `CancellationException`，再仅将普通 `Exception` 映射为 UI 错误；非 `Exception` 的 fatal `Error` 会继续抛出。资源映射与调用方 fallback 仍需按验收项核对。
- 错误目录保持 schema 1；`group_path` 是必需归属元数据，生成器按现有层级构造目录常量。生成器校验、架构门禁和生产编译已通过；日志字段和 UI 映射的逐项行为核对仍未完成。

## 尚待完成的核验

1. 核验目录 `default_reason`、显式具体原因（如 `BackupFailed.reasonCode`）和最终 Reporter 字段确实独立且已登记；沿调用方检查重复记录、分类错误和通用兜底。
2. 核对仍可到达的 UI 错误与 Notice 映射、中英文资源以及各调用方 fallback；确认取消和 fatal `Error` 不会变成普通提示。
3. 完成错误处理、观测与对应产品文档的逐项调用链核对。保留稳定日志 wire 字符串，不恢复扁平业务码别名。

## 验收证据

- `AppError` 生产代码不再引用手写错误码常量，且其 code 均来自生成的类型化目录。
- Reporter 输出的 error/reason 使用独立且已登记的稳定标识（具体运行时字段覆盖仍待核验）。
- 中英文资源映射覆盖仍可到达的 UI 错误；未知码使用显式调用方 fallback，不展示原始标识。
- 取消与 fatal `Error` 不会变成普通 AppError 或失败提示。
- 每个目录节点有明确分层归属，生产/测试 Kotlin 调用不再引用扁平旧路径；生成的 Error/Reason 文档与 Kotlin 嵌套树一致。
- 错误目录校验、架构检查和受影响编译通过。

## 本轮进度（2026-11-12）

- 目录保持 schema 1；`group_path` 是必需归属元数据，不因开发期目录结构变化升级 schema。生成的 `ErrorCodes`、`Reasons`、`CatalogErrorCodes` 和 Markdown 使用嵌套归属，并校验叶节点/父级命名空间冲突。
- 已迁移 Kotlin 生产与测试引用，没有保留扁平别名；现有目录 wire 字符串与基线逐项比较，变化数为 0。
- 当前工作树运行 `:app:compileDebugKotlin verifyArchitecture --no-parallel --max-workers=2` 成功；其中 `verifyErrorCodes`、`verifyModuleBoundaries`、`verifyArchitecture` 与 App Kotlin 编译均通过。测试未运行。
- 运行时 Reporter 字段覆盖、UI 错误映射及调用方 fallback 尚未逐项核验；这些是剩余事项，不是编译阻断。
