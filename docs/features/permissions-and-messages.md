# 权限与消息

权限请求与消息投递已经拆成两个独立中心。本页只保留入口，禁止在功能代码中重新合并二者：

- [消息中心设计](../architecture/message-center.md)
- [权限中心设计](../architecture/permission-center.md)
- [日志记录、错误与 Trace](../architecture/observability/record-and-trace.md)

产品消息开关表示用户是否希望收到某类提醒；Android 运行时权限表示系统是否允许状态栏通知。
两者是独立状态。关闭产品设置只写用户偏好；开启状态栏通知前必须验证系统通知权限、应用通知总开关和
通知渠道可用性。权限或系统通知不可用时不得把产品开关写成开启，应请求权限或引导用户进入系统通知设置。

设置中的“权限管理”页是用户查看和管理系统访问能力的统一入口，但不是新的权限真值存储：

- Camera 与 Notifications 展示当前版本适用的 Runtime permission snapshot；页面恢复前台或请求结束后
  重读系统状态，API 33 以下不显示不适用的 Notifications 行；
- 备份文件夹展示 SAF persisted URI grant，点击后进入唯一负责选择、更换、重新授权和清除的备份设置页；
- Network、Vibration、Biometric 与 Package visibility 仅说明 Manifest capability，不提供伪开关。

权限 Route 负责运行时权限 Activity Result、系统设置 Intent、生命周期与一次性反馈；备份 Route 独立
负责 SAF 目录选择。Screen 只接收不可变模型并发送语义动作。功能页仍可在真正需要权限的时刻请求，
但必须复用 `PermissionRequestHost`，不得各自读取 Android permission 或复制永久拒绝判断。
