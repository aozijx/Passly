# 运行时流程

## 解锁与锁定

```mermaid
sequenceDiagram
    actor User as 用户
    participant UI as Verification UI
    participant Auth as AuthenticationManager
    participant DEK as DekManager
    participant Boot as BootstrapStore
    participant DB as DatabaseSession
    User->>UI: 生物识别 / 应用密码 / 恢复码
    UI->>Auth: 验证凭据
    Auth->>Boot: 读取对应 Envelope 与 Verification Tag
    Auth->>DEK: 解封并验证 DEK
    Auth->>DB: 允许新的数据库 lease
    DEK-->>UI: Authenticated
    Note over DEK,DB: 锁定时先关闭数据库，再擦除 DEK 与派生会话密钥
```

`BootstrapStore` 位于数据库之外，使应用在 SQLCipher 尚未打开时也能取得信封。当前实现由 Proto DataStore
提供，DEK 解锁后 `DatabaseProvider` 才能创建 Room 实例。

Activity 只通过 `AuthenticationHost` 提供 UI 能力；认证请求本身不携带 Activity、Launcher、Cipher 或
PendingIntent。锁定由 `VaultSessionController` 拒绝新 lease、等待活跃操作、关闭 Room，再擦除会话密钥与 DEK。

## 一次性消息

```mermaid
flowchart LR
    Producers["Publisher"] --> Dispatcher["AppNoticeDispatcher"]
    Dispatcher --> InApp["In-app notice stream"]
    Dispatcher --> System["SystemNotificationGateway"]
    Prefs["Atomic AppMessageSettings snapshot"] --> Dispatcher
    Permission["Notification permission / channel"] --> Dispatcher
```

同一业务事件使用稳定 eventId；可信注册表决定等级、主题、投递目标和去重窗口。通知偏好控制产品行为，
系统权限决定状态栏通知是否可投递，两者不能混为一个布尔值。详细规则见[消息中心](message-center.md)。

## 备份

导出先生成一致性快照与附件，再写入 ZIP 负载，最后用备份密码派生的密钥进行 AES-GCM
加密。导入必须先完整验证容器、解密和校验路径，再在事务中执行覆盖或合并；失败不得留下部分数据。

详细格式见[备份格式](../data/backup-format.md)。
