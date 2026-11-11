# 通知权限请求未启动结果审查

## 当前合同

通知权限申请返回 `Busy` 或 `HostUnavailable` 表示系统弹窗没有启动。`NotificationsRoute` 将二者统一发送为 `RuntimePermissionRequestNotStarted(request)`，分别展示忙碌或宿主不可用提示。`NotificationSettingsViewModel` 仅在请求 token 和页面 owner 仍匹配时清理 pending；不会报告系统拒绝，也不会写入开启偏好。再次开启时生成新 request，可重新尝试。

## 验收证据

当前工作树中的 `NotificationSettingsViewModelTest.permission launch failure can be retried without reporting system denial` 覆盖未启动不发送拒绝提示、不写偏好，且再次尝试生成新 request。2026-11-11 执行 `:app:testDebugUnitTest --tests '*NotificationSettingsViewModelTest' --no-parallel --max-workers=2`，18 项通过、0 failures/errors。

该测试覆盖 ViewModel 行为；没有驱动 `NotificationsRoute` 的 `Busy` / `HostUnavailable` 分支，也没有验证 Route 与 PermissionRequestHost arbiter lease 的收尾。设备级 Activity 配置重建与系统授权/拒绝弹窗组合仍在 Task 22。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 忙碌与宿主不可用结果处理 | `NotificationsRoute()` | [`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
| 忽略过期请求并清理有效的未启动请求 | `onAction(RuntimePermissionRequestNotStarted(request))` | [`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt) |
| 未启动后新 request 重试回归 | `NotificationSettingsViewModelTest` | [`NotificationSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt) |
