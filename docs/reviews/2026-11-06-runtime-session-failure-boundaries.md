# Runtime Session 解锁与状态事件失败边界交付记录

审查日期：2026-11-06。

状态：已完成。本文保留 Runtime session fatal/cancellation 清理与遥测边界的历史验收证据；当前调用合同以[运行时流程](../architecture/runtime-flows.md)为准。

## 解锁、资源清理与错误优先级

`RuntimeSessionManager.unlock()` 先检查 `leaseGate.closeFailure()`，再尝试 `leaseGate.resumeIfOpen()`；需要重新打开时，在 `withContext(ioDispatcher)` 中调用 `keySource.copyKey()`，随后将 key 传入 `leaseGate.open(key)`，并在 `finally` 将 key buffer 归零。key source 的普通 `Exception` 返回 `KeyUnavailable(cause)`，资源打开普通异常返回 `OpenFailed(cause)`；`CancellationException` 传播。JVM fatal `Error` 经 dispatcher 区域清理 key 后，从 `unlock()` 以原对象重抛，不转成页面可重试的业务结果。

`SessionLeaseGate.open(key)` 调用 `resource.open(key)` 后记录 handle、发布 handle、切换到 `UNLOCKED` 并发送 OPEN 事件。打开期间失败时，`discardOpened(opened, published, cause)` 在 `NonCancellable` 区撤销已发布 handle、关闭新资源并恢复 `SEALED`。若原始错误是 fatal，清理错误作为 suppressed 附加；若原始错误是普通异常而关闭抛 fatal，fatal 清理错误成为主异常并保留普通异常为 suppressed。清理完成后同一 Runtime session 可重试解锁。

此前 key source/open 边界把所有非取消 `Throwable` 包装成 `KeyUnavailable` 或 `OpenFailed`，会将 `AssertionError`、`OutOfMemoryError` 等严重错误伪装成普通解锁失败。另一条清理路径曾把 cleanup `Throwable` 一概加到原始 cause 的 suppressed，导致“普通 open 错误 + fatal close 错误”仍返回普通 `OpenFailed`。Task 54 增加了两种主/清理错误优先级、fatal 对象身份、key wipe、SEALED 恢复、资源关闭和后续重试的回归覆盖。

## 状态事件与业务结果隔离

`SessionLeaseGate.emitTransition(startedAt, fromState, toState, reason, lockType, timeoutMs, activeLeaseCount, error)` 构造 owner 为 `SESSION_RUNTIME` 的 `SessionTransitionEvent` 并交给 `SessionEventSink.emit(event)`。普通 sink `Exception` 是 best-effort 丢失，不回滚 `OPEN/RESUME/SOFT_LOCK/SEAL` 的已提交状态或资源结果；取消传播，fatal `Error` 未被捕获。尤其 OPEN 已发布 handle 和 `UNLOCKED` 后，普通遥测失败不得执行 `discardOpened()` 并错误关闭资源。

`DRAIN_TIMEOUT` 上的遥测取消或 fatal 会先被保存为 transition failure；SessionLeaseGate 仍完成 active lease 取消、资源 close 和 `SEALED` 恢复，之后再传播保存的失败。`CLOSE_FAILED`/SEAL 等后续事件也不能覆盖资源 close 的原始错误。生产 adapter 为 `AppDatabaseSession` 注入 `SessionEventSink(eventLogger::record)`；`DatabaseSessionEventLogger.record()` 将事件交给结构化 Logger，`DefaultLogger` 在同步 sink 边界隔离普通异常并保留取消/fatal 传播。

Task 55 的 Runtime 替身覆盖 OPEN、RESUME、SOFT_LOCK、SEAL、CLOSE_FAILED 和 DRAIN_TIMEOUT 阶段的普通异常、取消与 `AssertionError`，验证事件可丢失时状态和资源仍一致、取消/fatal 只在必要清理后传播。Runtime Session 最终记录 27 tests，Data logger adapter 定向测试 5 项，Core `DefaultLogger` 定向测试 3 项，均无失败；`:runtime:session:test verifyArchitecture --max-workers=32` 与 Data/Core 定向测试、`git diff --check` 通过。Task 54 的独立阶段报告为 21 tests、0 failures/errors；此处 27 项为纳入 Task 55 扩展后的总数。无设备级仪器验证，此边界由 JVM Runtime/Data/Core 测试覆盖。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 取 key、映射可恢复结果并确保缓冲清理 | `RuntimeSessionManager.unlock()` | [`RuntimeSessionManager.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManager.kt) |
| 开资源、发布状态、失败回滚及 close 错误优先级 | `SessionLeaseGate.open()`、`discardOpened()`、`closeAfterFailedOpen()` | [`SessionLeaseGate.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt) |
| 构造并派发状态迁移事件 | `emitTransition(...)` | [`SessionLeaseGate.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt) |
| 将 Data session 事件交给结构化 Logger | `DatabaseSessionEventLogger.record()` | [`DatabaseSessionEventLogger`](../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) |
| 隔离普通日志 sink 异常 | `DefaultLogger` | [`DefaultLogger.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/observability/internal/DefaultLogger.kt) |
