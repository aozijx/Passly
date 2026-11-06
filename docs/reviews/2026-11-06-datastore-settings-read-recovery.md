# Proto DataStore 设置读取恢复交付记录

审查日期：2026-11-06。

状态：已完成。本文保留 DataStore 设置冷流失败处理的调查范围与验收证据；当前读取和重试合同以[设置存储](../data/settings-storage.md)及[状态管理](../development/architecture-and-state.md)为准。

## 范围与修复前证据

审查覆盖 `ProtoAppPreferencesStore`、`ProtoSecurityPreferencesStore` 的持久化 Flow、页面 ViewModel 的 `stateIn` 投影、设置 Route、设备锁以及诊断和消息设置的进程级消费者。发现设置冷流异常会终止 `stateIn` 或常驻 `collect`：首次读取时页面可能一直留在无内容的 Loading；先发值后失败时页面会继续展示旧快照。重新进入页面不能保证已终止的上游重新订阅。

设备锁存在单独的生命周期风险：`DeviceLockController` 常驻订阅失败后不会再接收设置变化；`DeviceLockHandler.handle()` 在 `SCREEN_OFF` 与 `FLIP` 路径都等待 `securitySettings.filterNotNull().first()`。屏幕关闭广播在 `onReceive()` 调用 `goAsync()` 后 dispatch，若首次设置读取异常使 StateFlow 永久保持 null，锁任务的等待不可达，`PendingResult.finish()` 也无法在该任务的 `finally` 执行。健康读取尚未发首值时的等待是既有 readiness 合同，因此修复要区分源异常与正常等待，不能直接取消等待屏障。

诊断记录策略在缺少设置首值时保持拒绝；消息设置快照会留在 Loading。通知首值到达前是否排队属于独立的 Task 23 语义，不在本项改动范围。

## 读取与重试合同

Proto repository Flow 继续作为持久化真相源。`SettingsDataStoreRead.tagCorruptSettingsData()` 仅把 DataStore `CorruptionException` 映射为 `CorruptSettingsDataException`；Serializer 仍传播损坏，Store 不安装会以默认值替换文件的 corruption handler。DataStore `data` 读取可抛 `IOException`；损坏文件默认抛 `CorruptionException`，替换文件只会在配置 corruption handler 时发生。[DataStore 数据层说明](https://developer.android.com/topic/libraries/architecture/datastore)；[DataStore API](https://developer.android.com/reference/androidx/datastore/core/DataStore)。

`RecoverableRead<T>(source, scope, retryDelayMillis, sharedRetrySignal)` 持有单个 cold source 的收集与恢复：每次订阅记录本地/共享 retry version，先发布 `Loading`，再收集 `source` 并逐值发布 `Ready(value)`。source 的 `catch` 只捕获普通 `Exception`，`CancellationException` 和 fatal `Error` 原样传播；`retryWhen` 将 `IOException` 映射为 `Failed(IO)` 并按指数退避自动重订阅（最长 60 秒），将损坏和其他普通异常映射为对应 `Failed` 并等待显式 retry。`RecoverableRead.retry()` 增加本地版本并调用可选的 `SettingsReadRetrySignal.request()`，触发失败冷源重新收集。StateFlow 使用 `WhileSubscribed(5_000L)`，初值为 `Loading`；source 正常完成时不合成新的 Ready/Failed 状态，StateFlow 保留最后一次发出的值。

页面 ViewModel 通过 `RecoverableRead.state` 把 `Loading/Ready/Failed` 映射为页面状态；Route 在失败分支显示共享 `SettingsReadFailureContent(onRetry)`。例如 `SecurityRoute` 将点击回调映射为 `SecuritySettingsAction.RetryRead`，ViewModel 再调用 `settingsRead.retry()`。异步写入开始前再次要求状态为 `Ready`，失败或未加载期间不允许基于伪默认值编辑或保存。

`SettingsReadRetrySignal` 是应用级共享重试版本。消息快照与诊断记录策略等长生命周期读者在显式重试时重新订阅；设备锁读者在前台恢复时重试，`SCREEN_OFF` 读取失败仍走安全锁定并确保异步广播完成。短期 `.first()` 操作继续把读取异常交给操作调用方处理，不转成默认偏好。

## 验证与限制

- 设置状态投影覆盖 App Theme/App Shell、Vault Display、Security、Privacy、Appearance、Interaction、Autofill、Notification、Permissions、Data Management 与 Storage；额外验证 Device Lock、previous-exit diagnostics 与消息设置快照的失败及恢复路径。
- `RecoverableReadTest` 覆盖 I/O 退避、损坏数据显式重试、共享重试和取消传播；`InterfaceSettingsViewModelTest` 覆盖源异常重订阅、Repository 真相、写入反馈和页面 owner 失效。
- 记录的最终命令为 `:data:testDebugUnitTest :app:testDebugUnitTest :app:ktlintCheck :data:ktlintCheck verifyArchitecture`，使用 `--no-parallel --max-workers=2` 串行执行；报告 App 1,140 项、Data 149 项全部通过，`git diff --check` 通过。最初架构门禁发现共享 Presentation 测试越层，已将其移入 `app.settings` 后重跑门禁。
- 本项没有设备仪器测试或设备复现；屏幕关闭的异常可达路径来自源码与测试合同核对，不能视作 OEM 实机验收。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 管理设置冷流读取、失败分类和重新订阅 | `RecoverableRead<T>.updates`、`retry()` | [`RecoverableRead.kt`](../../app/src/main/java/com/aozijx/passly/app/settings/RecoverableRead.kt) |
| 传播页面 retry 到进程级读取者 | `SettingsReadRetrySignal.request()` | [`SettingsReadRetrySignal.kt`](../../app/src/main/java/com/aozijx/passly/app/settings/SettingsReadRetrySignal.kt) |
| 将数据层损坏异常标记为领域读取错误 | `Flow<Preferences>.tagCorruptSettingsData()` | [`SettingsDataStoreRead.kt`](../../data/src/main/java/com/aozijx/passly/data/local/datastore/SettingsDataStoreRead.kt) |
| 设置页统一显示失败与重试入口 | `SettingsReadFailureContent()` | [`SettingsReadFailureContent.kt`](../../app/src/main/java/com/aozijx/passly/presentation/shared/state/SettingsReadFailureContent.kt) |
