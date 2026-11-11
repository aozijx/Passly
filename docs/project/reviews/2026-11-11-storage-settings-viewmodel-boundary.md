# Storage Settings ViewModel 边界审查

审查日期：2026-11-11。

## 结论

保留 `StorageSettingsViewModel` 作为数据管理页面的状态和任务 owner；本次不提取 UseCase、应用服务或转发 Coordinator。它虽调用缓存 Store、诊断日志 Store 和设置 Repository，但每个用户动作只执行一个能力：清缓存、清日志或保存记录等级。页面同时展示两种容量属于组合展示，不构成跨 Store 的业务事务；容量格式化和失败状态也是页面投影。

## 边界证据

| 职责 | 当前 owner 与证据 |
| --- | --- |
| 页面状态、进入/退出及刷新任务 | `StorageSettingsViewModel.onPageEntered()`、`onPageExited()`、`refreshStorageUsage()`、`refreshCacheSize()`、`refreshLogSize()`；分别取消两个刷新 Job，并用 page owner 防止离页结果或反馈写回。 |
| 缓存清理及容量呈现 | `clearCache()`、`readCacheSize()` 调用 `AppCacheStore`；清理失败后读取实际容量，失败时呈现 `Unavailable`。Store 接口与 Android 实现负责文件系统操作及 IO。 |
| 诊断日志清理及容量呈现 | `clearLogs()`、`readLogSize()` 先检查完整安全会话访问，再调用 `DiagnosticsLogStore`；清理失败显示 `Unavailable`，避免保留旧容量。Store/Adapter 负责日志存储操作及 IO。 |
| 记录等级读取与保存 | `RecoverableRead` 消费 `DiagnosticsLogSettingsRepository` 的设置流并支持显式 retry；保存失败只发布页面反馈。它是单一偏好读写，没有额外业务编排。 |
| Android 生命周期和系统反馈 | `DataManagementRoute()` 通过 `DisposableEffect` 配对页面进入/退出，生命周期内收集 effect 并显示本地化 Toast；不承载清理规则或状态转换。 |

上述职责由 `StorageSettingsViewModelTest` 中 13 个测试源码覆盖，包括设置重试、写入失败、页面重入、容量读取、清理成功/失败、受限会话与取消传播。本轮核对测试源码，**未运行测试**；该记录不代表运行时或设备验收。

## 核对位置

- [数据管理和诊断日志专题](../../product/diagnostics/viewer.md)
- [StorageSettingsViewModel.kt](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/data/StorageSettingsViewModel.kt)
- [DataManagementRoute.kt](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/data/DataManagementRoute.kt)
- [StorageSettingsViewModelTest.kt](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/data/StorageSettingsViewModelTest.kt)
- [ViewModel owner 审查准则](../../platform/architecture/viewmodel-ownership.md)
