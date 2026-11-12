# 数据库重试与设置读取 owner 审查

**范围：** 复核 Task 84 中的 `DatabaseSessionRetryCoordinator` 与 `RecoverableRead` 候选，检查创建者、调用方、状态 owner、相关测试和正式合同。结论只关闭这两个候选，不代表 AppShell 或 Task 84 的 ViewModel 全量审计已完成。

## 数据库会话重试

`VaultSessionController` 是安全会话与数据库故障的状态 owner，同时实现 Domain `DatabaseSessionRecovery` 端口。它在 `retry()` 中持有和锁定、封存、数据库重置相同的 mutex，再创建一次性 `DatabaseSessionRetryCoordinator`。Coordinator 不保存副本状态；它封装已有故障准入、调用 `DatabaseSessionLifecycle.unlock()`、更新最新故障，以及成功后按“清故障 → 标记 Runtime 解锁 → 发布当前认证状态”的顺序收尾。Shell 只通过端口发起重试，观察 Security 发布的故障状态，并拥有页面级重复请求去重、取消和反馈。

该职责与 `SecureDatabaseResetCoordinator` 不重复：它在同一会话 mutex 下串行执行重置，并拥有操作期间的授权撤销、敏感 key 清理和认证状态恢复；不能为了复用“数据库重新打开”而合并其事务与安全清理合同。Retry coordinator 当前由单一 Security owner 创建，并有独立的顺序和结果测试；`VaultSessionControllerLifecycleTest` 另覆盖 reset reopen 失败后锁定并可重试。保留两者，见[认证专题](../../platform/security/authentication.md#安全会话可观测性)。

## RecoverableRead

`RecoverableRead<T>` 是 App 中一个 cold Repository Flow 的状态和重订阅 owner。订阅期间发布 `Loading / Ready / Failed`；只有普通 `Exception` 被分类，取消与 fatal `Error` 原样传播。损坏及意外异常等待显式 retry，I/O 异常以有界退避重试；每次 retry 都重新订阅 cold source。`stateIn(WhileSubscribed(5s))` 将收集绑定到消费者生命周期窗口。`retry()` 同时递增本地请求并通知可选的进程级 `SettingsReadRetrySignal`，因此同一设置来源的页面可唤醒常驻读取 owner。

页面只消费其 `state` 并调用 `retry()`；它们不复制冷源异常捕获、失败分类或重订阅策略。需要页面级写入门控、迟到写入校验和 Retry 的职责仍由页面 ViewModel 持有。该 owner 已在[状态管理规范](../../engineering/development/architecture-and-state.md#持久设置读取失败与恢复)与[设置存储专题](../../platform/data/settings-storage.md)说明；`RecoverableReadTest` 覆盖 IO 退避、损坏数据显式重试、共享 retry 不丢失及取消传播，`AppShellViewModelTest` 覆盖 Shell 内设置重试和数据库重试的独立请求 owner。

## 证据边界

本轮为源码/调用者/测试审查，没有新增或运行测试。现有 `RecoverableReadTest` 未覆盖 `UNEXPECTED` 分类及 fatal `Error`；源 Flow 的完成语义仍以实现为准，文档不得把正常完成说成自动重新订阅。Task 84 中 ViewModel 全量调用闭包、其他 coordinator 候选和 Route/宿主行为仍保持开放。

## 代码位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 持有 Security 会话故障并串行执行数据库恢复 | `VaultSessionController.retry()` | [`VaultSessionController.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt) |
| 决定故障准入、恢复结果和认证发布顺序 | `DatabaseSessionRetryCoordinator.retry()` | [`DatabaseSessionRetryCoordinator.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/DatabaseSessionRetryCoordinator.kt) |
| 验证 coordinator 结果和发布顺序 | `DatabaseSessionRetryCoordinatorTest` | [`DatabaseSessionRetryCoordinatorTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/session/DatabaseSessionRetryCoordinatorTest.kt) |
| 保持 Shell 重试为页面级请求 owner | `AppShellViewModel.retryDatabaseSession()` | [`AppShellViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt)、[`AppShellViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModelTest.kt) |
| 管理一个设置冷源的失败状态与重新订阅 | `RecoverableRead<T>` | [`RecoverableRead.kt`](../../../app/src/main/java/com/aozijx/passly/app/settings/RecoverableRead.kt)、[`RecoverableReadTest.kt`](../../../app/src/test/java/com/aozijx/passly/app/settings/RecoverableReadTest.kt) |
