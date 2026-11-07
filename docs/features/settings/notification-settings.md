# 通知设置

状态：当前实现（代码核对：2026-11-07）。

通知设置将用户偏好与 Android 投递能力分开处理：产品通知总开关、可选消息和主题开关由 Repository 持久化；运行时 `POST_NOTIFICATIONS` 权限、应用通知总开关和通知渠道可用性由平台读取。保存“开启”前必须重新确认平台可用，系统权限不足时不能把产品偏好乐观写成开启。

## 开启系统通知的请求链

`NotificationSettingsViewModel.onAction(SetSystemNotificationsEnabled(true))` 调用 `requestSystemNotificationsEnabled()`。若已有 `PermissionRequestState.Pending`，只更新 `enableWhenGranted = true` 并复用同一请求；否则调用 `readSystemNotificationState()`，根据系统状态分支：运行时权限缺失时生成 `NotificationPermissionRequest()`，记录当前 `pageEffects.currentOwner` 并发送 `RequestRuntimeNotificationPermission(request)`；平台可用时调用 `persistSystemNotificationsEnabled(true)`；系统总开关或渠道不可用时发送拒绝提示和打开系统设置 effect。

`NotificationsRoute` 收到 permission effect 后先调用 `viewModel.canLaunchRuntimePermissionRequest(request)`；它要求请求对象身份仍是当前 pending、开启意图仍有效且 owner 仍活跃。通过后调用 `permissionHost.request(POST_NOTIFICATIONS)`。Activity Result 按 `Granted/Denied` 映射为 `RuntimePermissionGranted(request)` 或 `RuntimePermissionDenied(request)`，请求 token 随结果原样回传。

另有未启动分支：`permissionHost.request(...)` 返回 `Busy` 或 `HostUnavailable` 时，Route 当前把它们也映射为 `RuntimePermissionDenied(request)`。这不是系统弹窗的拒绝回执；ViewModel 会按 token 清除 pending、重读平台状态，并在 `enableWhenGranted` 仍为 true 时发出拒绝提示。忙碌/宿主不可用需要自己的失败、保留意图或重试合同，不能把未启动当成用户拒绝；跟进见[维护 TODO Task 22](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md#task-22)。

授权回调进入 `completeRuntimePermissionRequest(request)`，检查 request 对象身份和 pending owner 仍 active，再清除 pending 并重新调用 `readSystemNotificationState()`。只有 `enableWhenGranted` 仍为 true 且 `system.isPlatformAvailable` 才调用 `persistSystemNotificationsEnabled(true)`；否则不写开启偏好并反馈系统不可用。拒绝回调在 `onAction(RuntimePermissionDenied(request))` 分支只检查 request 对象身份，清除 pending，并在 `enableWhenGranted` 为 true 时发出拒绝提示；它没有单独检查 owner。正常离页时 `onPageExited(owner, preservePendingPermissionRequest=false)` 已清除该 owner 的 pending，因此旧 token 会失效。关闭开关会先把 pending 请求的 `enableWhenGranted` 设为 false，再调用 `persistSystemNotificationsEnabled(false)`，因此迟到授权不能重新开启偏好，迟到拒绝也不会显示拒绝通知。

页面离开时 `onPageExited(owner, preservePendingPermissionRequest)` 只清理原 owner 的请求；配置重建可将仍有效请求的 owner 置空，新 Route 经 `onPageEntered()` 绑定新 owner 并重发同一个 token。真正离页清除 pending 后，旧 token 的 grant/deny 都会被忽略；配置重建保留同一 token，因此结果按新的 pending owner 处理。请求标识和意图只在 ViewModel 内存中，不进入 SavedState 或 Proto。拒绝分支与授权分支的 owner 校验位置不同；Activity/系统弹窗组合仍以 Task 22 设备验收为准。

## 消息偏好写入

可选消息与主题开关经 `NotificationSettingsViewModel.onAction(SetOptionalMessagesEnabled/SetTopicEnabled)` 调用 `saveSetting(write)`，把 Repository setter 作为闭包交给 `writePreferenceWithFeedback`。写入错误发 `SaveFailed(UiErrorMessage)`；它不会转换成权限拒绝，也不会导航系统设置。设置页展示值仍由 Domain `MessageSettings` Flow 驱动，主题列表直接投影 `MessageTopic`，不另造同值枚举。

`NotificationsRoute` 在 STARTED 生命周期内消费权限请求、保存失败与系统设置导航 effect；Toast/notice 文案在消费时用当前资源解析。页面恢复前台时派发 `RefreshSystemNotificationState`，以最新系统状态重算可用性。

对应行为测试见 [`NotificationSettingsViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt)，覆盖 pending 请求复用、关开竞争、旧 owner/token、配置重建、平台重检及 Repository 写入失败。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 通知偏好与请求状态机 | `NotificationSettingsViewModel.onAction()`、`requestSystemNotificationsEnabled()`、`completeRuntimePermissionRequest(request)` | [`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt) |
| Activity Result 与 STARTED effect 消费 | `NotificationsRoute()` | [`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
| Android 权限请求租约 | `rememberPermissionRequestHost()`、`PermissionRequestHost.request(permission)` | [`PermissionRequestHost.kt`](../../../app/src/main/java/com/aozijx/passly/app/platform/permission/PermissionRequestHost.kt) |
| 绑定页面 owner、重建后重发请求及离页清理 | `onPageEntered()`、`onPageExited(owner, preservePendingPermissionRequest)` | [`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt)、[`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
| 在系统弹窗前验证请求身份和 owner，并重新确认平台状态后写偏好 | `canLaunchRuntimePermissionRequest(request)`、`completeRuntimePermissionRequest(request)`、`persistSystemNotificationsEnabled(enabled)` | [`NotificationSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt) |
| 将运行时申请启动结果折算为权限结果 Action | `permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)` -> `RuntimePermissionGranted(request)` / `RuntimePermissionDenied(request)` | [`NotificationsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
