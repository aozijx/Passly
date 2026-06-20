# core/error 模块说明

- Status: Draft
- Last Updated: 2026-05-16

## 1. 模块职责

`core/error` 负责定义应用统一错误语义与结果封装，目标是：

- 将异常转换为可追踪、可分层识别的业务错误（`AppError`）
- 通过统一结果模型（`AppResult<T>`）在 Domain/UI 传递成功或失败
- 为 UI 层文案映射与日志追踪提供稳定字段（错误码、追踪信息、层级）

## 2. 关键数据结构

### 2.1 `ErrorLayer`

- 定义错误来源层级：`DATA` / `DOMAIN` / `UI`

### 2.2 `ErrorTrace`

- 追踪字段：`traceId`、`originLayer`、`operation`、`timestampMs`、`extras`
- 作用：跨层传递时保留上下文，便于定位一次操作中的错误路径

### 2.3 `AppError`

当前已实现子类型：

- `AuthFailed`
- `DatabaseLocked`
- `DatabaseInitFailed`
- `BackupFailed`
- `Unexpected`

公共字段：

- `code`: 稳定错误码
- `message`: 默认消息
- `layer`: 错误层级
- `recoverable`: 是否可恢复
- `trace`: 追踪信息
- `cause`: 原始异常

### 2.4 `AppResult<T>`

- `Success<T>(data)`
- `Failure(error: AppError)`

辅助函数：

- `runCatching {}`：同步包装
- `runSuspendCatching(operation, layer) {}`：协程包装（会透传 `CancellationException`）

## 3. 分层边界（当前目标）

- Data 层：抛出或构造 `AppError`（不直接向 UI 暴露 Throwable）
- Domain 层：统一返回 `AppResult<T>`
- UI 层：统一通过 `toUiMessage(...)` 做文案映射，不直接读取 `Throwable.message`

## 4. 关键调用链（示例）

### 4.1 备份链路

1. `data/repository/backup/BackupRepositoryImpl.kt`
    - 捕获底层异常并映射为 `AppError.BackupFailed`
2. `domain/usecase/backup/BackupUseCases.kt`
    - 使用 `AppResult.runSuspendCatching(...)` 返回 `AppResult`
3. `features/backup/BackupCoordinator.kt`
    - 消费 `AppResult`，失败分支走 `error.toUiMessage(...)`

### 4.2 通用 UI 映射

- `features/common/AppErrorUiMapper.kt`
    - `AppError.toUiMessage(...)`
    - `Throwable.toUiMessage(...)`（兜底统一转 `AppError`）

## 5. 已完成改造

- `AppError` 增加 `code/layer/recoverable/trace`
- 引入 `ErrorTrace`，支持 `traceId` 与 `operation` 追踪
- `AppResult.runSuspendCatching(...)` 已支持挂起函数包装
- Backup 主链路已改为：Data 产出 `AppError`，Domain 返回 `AppResult`
- UI 层 `features/**` 已完成 `error.message`/`Throwable.message` 展示点收口到 `toUiMessage(...)`

## 6. 待修改项

### P0（优先处理）

1. 统一 `AppError.layer` 与 `ErrorTrace.originLayer` 语义
    - 现状：`AppError.Unexpected` 等类型内部 `layer` 固定为 `DATA`，而 `fromThrowable(layer=...)` 可能传入
      `DOMAIN/UI`，存在语义不一致风险
    - 建议：允许子类接收 `layer`，或将 `layer` 统一由 `trace.originLayer` 推导

2. 完成 Data 层全链路收口
    - 现状：部分 Data 实现仍直接返回 `AppResult`（例如认证链路）
    - 目标：Data 层仅产出 `AppError`，`AppResult` 仅在 Domain 层构建

### P1（中期优化）

1. 错误码治理
    - 将 `code` 统一为可检索规范（例如 `AUTH_FAILED`、`DB_LOCKED`、`BACKUP_IMPORT_FAILED`）
    - 建立错误码到埋点/日志字段的映射表

2. UI 文案资源化
    - 当前 `toUiMessage(...)` 仍包含硬编码中文
    - 建议迁移到 `R.string`，支持统一多语言与产品文案迭代

3. 操作名 `operation` 标准化
    - 当前是自由字符串（如 `domain.backup.import`）
    - 建议定义常量或枚举，避免拼写不一致导致追踪分散

### P2（质量保障）

1. 补齐错误处理测试矩阵
    - `AppError.fromThrowable(...)`
    - `runSuspendCatching(...)` 的取消语义
    - UI mapper 的默认文案与降级行为

2. 文档联动
    - 在 `docs/architecture/ARCHITECTURE_DECISIONS.md` 增加错误处理分层 ADR 条目
    - 在 `docs/operations/CHANGE_PLAYBOOK.md` 增加改造操作清单

## 7. 验证清单

- 编译检查：`fullDebug` / `vaultDebug`
- 静态检查：UI 层不再直接显示 `Throwable.message`
- 回归检查：备份、认证、数据库初始化三条关键路径错误提示一致
- 追踪检查：失败场景能拿到 `traceId + operation + code`

## 8. 相关文件

- `app/src/main/java/com/aozijx/passly/core/error/AppError.kt`
- `app/src/main/java/com/aozijx/passly/core/error/AppResult.kt`
- `app/src/main/java/com/aozijx/passly/features/common/AppErrorUiMapper.kt`
- `app/src/main/java/com/aozijx/passly/data/repository/backup/BackupRepositoryImpl.kt`
- `app/src/main/java/com/aozijx/passly/domain/usecase/backup/BackupUseCases.kt`
- `app/src/main/java/com/aozijx/passly/features/backup/BackupCoordinator.kt`