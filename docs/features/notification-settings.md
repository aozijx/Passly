# 通知设置

状态：当前实现（代码核对：2026-11-06）。

通知设置将用户偏好与 Android 投递能力分开处理：产品通知总开关、可选消息和主题开关由 Repository 持久化；运行时 `POST_NOTIFICATIONS` 权限、应用通知总开关和通知渠道可用性由平台读取。保存“开启”前必须重新确认平台可用，系统权限不足时不能把产品偏好乐观写成开启。

## 开启系统通知的请求链

`NotificationSettingsViewModel.onAction(SetSystemNotificationsEnabled(true))` 调用 `requestSystemNotificationsEnabled()`。若已有 `PermissionRequestState.Pending`，只更新 `enableWhenGranted = true` 并复用同一请求；否则调用 `readSystemNotificationState()`，根据系统状态分支：运行时权限缺失时生成 `NotificationPermissionRequest()`，记录当前 `pageEffects.currentOwner` 并发送 `RequestRuntimeNotificationPermission(request)`；平台可用时调用 `persistSystemNotificationsEnabled(true)`；系统总开关或渠道不可用时发送拒绝提示和打开系统设置 effect。

`NotificationsRoute` 收到 permission effect 后先调用 `viewModel.canLaunchRuntimePermissionRequest(request)`；它要求请求对象身份仍是当前 pending、开启意图仍有效且 owner 仍活跃。通过后调用 `permissionHost.request(POST_NOTIFICATIONS)`。Activity Result 按 `Granted/Denied` 映射为 `RuntimePermissionGranted(request)` 或 `RuntimePermissionDenied(request)`，请求 token 随结果原样回传。

`completeRuntimePermissionRequest(request)` 再检查对象身份与 page owner，随后清除 pending 并重新调用 `readSystemNotificationState()`。只有 `enableWhenGranted` 仍为 true 且 `system.isPlatformAvailable` 才调用 `persistSystemNotificationsEnabled(true)`；否则不写开启偏好并反馈系统不可用。拒绝回调同样核对 token；用户已撤回开启意图时不显示拒绝通知。关闭开关会先把 pending 请求的 `enableWhenGranted` 设为 false，再调用 `persistSystemNotificationsEnabled(false)`，因此迟到授权不能重新开启偏好。

页面离开时 `onPageExited(owner, preservePendingPermissionRequest)` 只清理原 owner 的请求；配置重建可将仍有效请求的 owner 置空，新 Route 经 `onPageEntered()` 绑定新 owner 并重发同一个 token。请求结果若来自旧 owner 或旧 token，会在 ViewModel 边界被忽略。请求标识和意图只在 ViewModel 内存中，不进入 SavedState 或 Proto。

## 消息偏好写入

可选消息与主题开关经 `NotificationSettingsViewModel.onAction(SetOptionalMessagesEnabled/SetTopicEnabled)` 调用 `saveSetting(write)`，把 Repository setter 作为闭包交给 `writePreferenceWithFeedback`。写入错误发 `SaveFailed(UiErrorMessage)`；它不会转换成权限拒绝，也不会导航系统设置。设置页展示值仍由 Domain `MessageSettings` Flow 驱动，主题列表直接投影 `MessageTopic`，不另造同值枚举。

`NotificationsRoute` 在 STARTED 生命周期内消费权限请求、保存失败与系统设置导航 effect；Toast/notice 文案在消费时用当前资源解析。页面恢复前台时派发 `RefreshSystemNotificationState`，以最新系统状态重算可用性。

对应行为测试见 [`NotificationSettingsViewModelTest`](../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt)，覆盖 pending 请求复用、关开竞争、旧 owner/token、配置重建、平台重检及 Repository 写入失败。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 通知偏好与请求状态机 | `NotificationSettingsViewModel.onAction()`、`requestSystemNotificationsEnabled()`、`completeRuntimePermissionRequest(request)` | [`NotificationSettingsViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt) |
| Activity Result 与 STARTED effect 消费 | `NotificationsRoute()` | [`NotificationsRoute.kt`](../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/general/NotificationsRoute.kt) |
| Android 权限请求租约 | `rememberPermissionRequestHost()`、`PermissionRequestHost.request(permission)` | [`PermissionRequestHost.kt`](../../app/src/main/java/com/aozijx/passly/app/platform/permission/PermissionRequestHost.kt) |
