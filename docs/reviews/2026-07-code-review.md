# 2026-07 代码审查

范围：文档重构期间核对数据库、信封、备份、设置、权限、消息与认证实现。原始发现保留作为历史证据；修复状态以
2026-07-18 复核表为准。

## 2026-07-18 复核

| 原问题 | 状态 | 当前实现 |
|---|---|---|
| 认证失败双消息 | 已解决 | `AuthFeedbackPresenter` 按 correlation ID 发布一次；用户取消静默 |
| 底层直接 Toast | 已解决 | 旧 Gateway、Authenticator、Launcher 与 ViewModel error event 已删除 |
| 日志旁路与脆弱过滤 | 已解决 | 所有调用点进入 `Logger`，隐私策略在 Sink 前执行 |
| Crash 路径锁与 Keystore 风险 | 已解决 | 有界 writer；预加载 emergency key；崩溃写入不取普通锁、不访问 Keystore |
| 恢复码重新生成提前失效 | 已解决 | 候选草稿确认后才提交 Envelope；进程恢复显示 DraftExpired |
| DatabaseSession 并发关闭 | 已解决 | lease gate 拒绝新操作并排干活跃事务；API 36 SQLCipher 仪器回归通过，资源关闭运行于 IO dispatcher |
| 备份 v1 KDF 自描述 | 已解决 | `PSLYBKP1` 头包含 KDF/cipher 参数并作为 AAD |

## 高优先级

### 已解决：DatabaseSession 并发关闭

`RuntimeSessionManager` 通过 `SessionLeaseGate` 在同一互斥区拒绝新 lease 并计数活跃操作；封存等待事务排干，超时则记录剩余 lease 并取消观察任务，再关闭资源。
资源关闭和打开失败后的句柄回收都在配置的 IO dispatcher 执行，避免 Room 的同步关闭阻塞锁定调用方线程。
Runtime 单测覆盖 dispatcher 与 lease 排干，API 36 SQLCipher 仪器测试用暂停的 Room 事务验证封存等待、拒绝新读取、提交后关闭并可重新打开。

清理时删除了只被仪器测试调用的 `AppDatabaseSession.closeDatabase()` 和其下游 `RuntimeSessionManager.closeResource()` 转发；
封存本身是唯一关闭所有者，测试不再在 `seal()` 后重复关库。

### 已解决：旧实验备份不是自描述 KDF 格式

头部只写 magic、version、salt、nonce 和密文长度，解码始终调用当前 `deriveKeyArgon2id` 默认参数。未来调整
Argon2id 参数后，旧备份可能无法恢复；头部也未作为明确 AAD 认证。

处理结果：旧实验格式整体废弃。正式格式重新从 container v1 开始，使用新 magic
`PSLYBKP1`，写入 KDF/cipher ID、Argon2 参数和所有长度，并把完整头作为 AAD。旧
`PASSLYBK` 文件明确不兼容，不保留实验 reader。

证据：`EncryptedBackupContainerCodec.kt`；当前协议文档已迁移到 `docs/features/backup/`。

### 已解决：认证消息只有一个展示所有者

旧 `DefaultVerificationGateway` 已移除；解锁失败由 `UnlockViewModel` 保留为类型化 UI 状态，认证执行路径不再同时发布全局错误消息。

## 中优先级

### P2：Room 仍保留失效 KeyEnvelope 实现

当前 Envelope 真相源是 Proto `VaultBootstrapStore`，但 `AppDatabase` 仍注册 `KeyEnvelopeEntity/Dao`
。仓库中没有业务调用；Entity 缺少当前 Proto Envelope 的 `iv`，且 `type` 为 `String`，DAO 的
`getByType/deleteByType` 却接收 `Int`。这既扩大 Schema，也容易让后续代码误用两个真相源。

建议在全新安装基线下从 Room v1 移除 Entity、DAO、DatabaseSchema 常量与 AppDatabase accessor，并重新导出
Schema；Bootstrap Proto 保持唯一来源。

证据：`AppDatabase.kt:27-52`、`KeyEnvelopeEntity.kt:15-41`、`KeyEnvelopeDao.kt:24-25,50-51`。

### P2：暗色模式仍以布尔字段持久化

界面需要“跟随系统 / 浅色 / 深色”三态，但 Proto 的 `dark_mode` 是 `optional bool`。presence
可以勉强表达第三态，却让 Repository/UI 容易错误地折叠为二态，也不利于后续扩展。

建议新增 `ThemeMode` enum 字段并 reserve 旧字段号，Mapper 显式处理未识别值。

证据：`app_settings.proto:18-20`。

### 已解决：认证 Toast 旁路和消息去重误抑制

认证执行器不直接调用 Toast；当前消息 Host 负责展示，`eventId` 只对同一业务事件去重。此批次删除曾按 `NoticeCode` 时间窗抑制不同事件的逻辑，
并令容量 64 的应用内队列在满载时明确返回失败而不丢旧消息；全失败的派发释放 claim，允许同一事件重试。

### 已解决：密钥擦除注释可能高估保证

`key.encoded.fill(0)` 通常覆盖 `SecretKeySpec` 返回的编码副本，不保证覆盖对象内部字节；同时 ZIP/加密
API 可能产生不可控副本。

处理结果：容器 KDF 返回可控 `ByteArray`，所有异常路径都会覆盖；密码 UTF-8 backing
buffer、归档和资源缓冲也执行 best-effort 清零。协议文档明确说明 JCE/JVM 内部副本无法
保证物理擦除。

证据：`EncryptedBackupContainerCodec.kt`、`KeyDerivation.kt`。

## 已解决的低优先级项

- Schema 不再包含 `vault_historys`；当前历史实体使用 `entry_activities` 与 `entry_revisions` 表名。
- 滑动动作、Autofill 展示模式、Vault 排序等固定集合已使用 Proto enum；`card_style` 不再是持久化 Proto 字段。
- Manifest 未声明 `QUERY_ALL_PACKAGES`，应用查询范围由 launcher intent `<queries>` 限定。

### 已解决：Android GitHub Actions CI 门禁

新增 `.github/workflows/android-ci.yml`，在 push 和 pull request 上运行架构校验、App Kotlin 编译、各 JVM/Android 模块单元测试、Lint、Debug APK 与 Android 测试 APK 构建。CodeQL workflow 保留独立手动构建，以供 CodeQL 提取分析数据库。

## 已核对的正向结果

- Domain 未发现 Data/Feature/Android import，Security 未发现 Data 实现 import，Feature 未发现
  Entity/DAO/具体 Repository import。
- AppSettings 与 Bootstrap Serializer 已将 Proto 解析失败转换为 `CorruptionException`。
- 备份读取会读满固定字段、拒绝尾随数据，并限制 ZIP 路径、重复条目和大小。
- 权限目录覆盖 Manifest 当前声明的相机、通知、网络、前台服务、振动、生物识别和应用查询权限。
