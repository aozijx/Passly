# 权限与消息

权限请求与消息投递已经拆成两个独立中心。本页只保留入口，禁止在功能代码中重新合并二者：

- [消息中心设计](../architecture/message-center.md)
- [权限中心设计](../architecture/permission-center.md)
- [Telemetry 与诊断日志](../architecture/telemetry.md)

产品消息开关表示用户是否希望收到某类提醒；Android 运行时权限表示系统是否允许状态栏通知。
两者是独立状态。开启产品设置时可以按需请求权限，但权限拒绝不得反向篡改产品设置。
