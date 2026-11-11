# 通知权限请求未启动结果审查

## 当前合同

通知权限申请返回 `Busy` 或 `HostUnavailable` 表示系统弹窗没有启动。`NotificationsRoute` 将二者统一发送为 `RuntimePermissionRequestNotStarted(request)`，分别展示忙碌或宿主不可用提示。`NotificationSettingsViewModel` 仅在请求 token 和页面 owner 仍匹配时清理 pending；不会报告系统拒绝，也不会写入开启偏好。再次开启时生成新 request，可重新尝试。

## 验收证据

当前工作树中的 `NotificationSettingsViewModelTest.permission launch failure can be retried without reporting system denial` 覆盖未启动不发送拒绝提示、不写偏好，且再次尝试生成新 request。2026-11-11 执行 `:app:testDebugUnitTest --tests '*NotificationSettingsViewModelTest' --no-parallel --max-workers=2`，18 项通过、0 failures/errors。

该测试覆盖 ViewModel 行为；没有驱动 `NotificationsRoute` 的 `Busy` / `HostUnavailable` 分支，也没有验证 Route 与 PermissionRequestHost arbiter lease 的收尾。设备级 Activity 配置重建与系统授权/拒绝弹窗组合仍在 Task 22。

## 配置重建与页面 owner

当前 Route 在 `onDispose` 时用 `activity?.isChangingConfigurations == true` 决定 `onPageExited(owner, preservePendingPermissionRequest)` 是否保留尚未完成的开通请求。配置重建保留原 request token 和 `enableWhenGranted` 意图；新 Route 进入后绑定新 owner 并重发同一请求。真实离页会清理该 owner 的 pending，旧 token 的授权/拒绝结果不会写入设置或覆盖新请求。grant 由 `completeRuntimePermissionRequest(request)` 按活动 pending owner 处理；deny 由 `RuntimePermissionDenied(request)` 按 token 处理。旧的无 owner `CancelRuntimePermissionRequest` 分支已删除，当前 Route 只调用 owner 校验后的 `onPageExited()`。

测试源码中的 `configuration recreation reissues the same pending request and commits the granted setting` 和 `late dispose from an old page owner cannot cancel the new page request` 覆盖相同 token 重绑、只提交一次和旧 owner dispose 不影响新请求。它们不证明真实 Activity locale recreation、系统弹窗 grant/deny、`AnimatedContent` dispose 顺序或 permission arbiter lease 的设备行为；这些仍由主维护计划 Task 22 验收。正式状态机见[通知设置专题](../../product/settings/notification-settings.md)与[权限中心](../../platform/architecture/permission-center.md)。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 忙碌与宿主不可用结果处理 | `NotificationsRoute()` | [`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
| 忽略过期请求并清理有效的未启动请求 | `onAction(RuntimePermissionRequestNotStarted(request))` | [`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt) |
| 未启动后新 request 重试回归 | `NotificationSettingsViewModelTest` | [`NotificationSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt) |
| 配置重建重绑 request 与页面 owner | `NotificationsRoute.onDispose`、`onPageExited()`、`onPageEntered()` | [`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt)、[`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt)、[配置重建回归](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt) |
