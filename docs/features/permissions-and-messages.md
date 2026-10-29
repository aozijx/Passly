# 权限与消息

权限请求与消息投递已经拆成两个独立中心。本页只保留入口，禁止在功能代码中重新合并二者：

- [消息中心设计](../architecture/message-center.md)
- [权限中心设计](../architecture/permission-center.md)
- [日志记录、错误与 Trace](../architecture/observability/record-and-trace.md)

产品消息开关表示用户是否希望收到某类提醒；Android 运行时权限表示系统是否允许状态栏通知。
两者是独立状态。关闭产品设置只写用户偏好；开启状态栏通知前必须验证系统通知权限、应用通知总开关和
通知渠道可用性。权限或系统通知不可用时不得把产品开关写成开启，应请求权限或引导用户进入系统通知设置。

通知设置页为每次运行时权限请求创建独立 `NotificationPermissionRequest`，请求结果必须匹配当前等待标识。
重复开启复用等待中的请求；等待时关闭会撤销开启意图，迟到授权不再保存开启，迟到拒绝也不提示权限错误。
同一请求完成前再次开启可恢复该请求的开启意图。Route 消费请求前检查意图，已撤销或过期的排队事件不启动系统弹窗。
离开页面时撤销等待请求，重新进入后可创建新请求；旧授权或拒绝不能消费新请求。请求标识只存于当前内存，不进入保存状态。

通知总开关、可选消息开关和主题开关通过 `writePreferenceWithFeedback` 处理存储失败，页面仍由 Repository Flow 驱动。
权限授予后仍需重新检查平台可用性，再尝试保存偏好；存储失败只发布 `SaveFailed(UiErrorMessage)`，不冒充权限拒绝或打开系统设置。
Route 在 STARTED 生命周期消费导航及反馈，保存错误按当前 `LocalResources` 解析；取消传播，销毁后的迟到写入失败不补发提示。
通知设置页将 Domain `MessageTopic` 的设置按固定顺序投影为展示列表，Content 直接发送
`NotificationSettingsAction`；不另建同值 topic 枚举和转发事件。

设置中的“权限管理”页是用户查看和管理系统访问能力的统一入口，但不是新的权限真值存储：

- Camera 与 Notifications 展示当前版本适用的 Runtime permission snapshot；页面恢复前台或请求结束后
  重读系统状态，API 33 以下不显示不适用的 Notifications 行；
- 备份文件夹展示 SAF persisted URI grant，点击后进入唯一负责选择、更换、重新授权和清除的备份设置页；
- Network、Vibration、Biometric 与 Package visibility 仅说明 Manifest capability，不提供伪开关。

权限 Route 负责运行时权限 Activity Result、系统设置 Intent、生命周期与一次性反馈；备份 Route 独立
负责 SAF 目录选择。Screen 只接收不可变模型并发送语义动作。功能页仍可在真正需要权限的时刻请求，
但必须复用 `PermissionRequestHost`，不得各自读取 Android permission 或复制永久拒绝判断。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 运行时权限申请宿主 | `PermissionRequestHost.access()` | [`PermissionRequestHost.kt`](../../app/src/main/java/com/aozijx/passly/app/platform/permission/PermissionRequestHost.kt) |
| 权限设置路由与平台结果处理 | `PermissionsRoute()` | [`PermissionsRoute.kt`](../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/core/PermissionsRoute.kt) |
| 全局消息投递与路由 | `DefaultAppNoticeDispatcher.dispatch()`、`DefaultNoticeRouter.route()` | [`DefaultAppNoticeDispatcher.kt`](../../app/src/main/java/com/aozijx/passly/app/message/runtime/DefaultAppNoticeDispatcher.kt)、[`DefaultNoticeRouter.kt`](../../app/src/main/java/com/aozijx/passly/app/message/runtime/DefaultNoticeRouter.kt) |
