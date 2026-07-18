# 2026-07 代码审查

范围：文档重构期间核对数据库、信封、备份、设置、权限、消息与认证实现。原始发现保留作为历史证据；修复状态以
2026-07-18 复核表为准。

## 2026-07-18 复核

| 原问题 | 状态 | 当前实现 |
|---|---|---|
| 认证失败双消息 | 已解决 | `AuthFeedbackPresenter` 按 correlation ID 发布一次；用户取消静默 |
| 底层直接 Toast | 已解决 | 旧 Gateway、Authenticator、Launcher 与 ViewModel error event 已删除 |
| 旧日志旁路与脆弱过滤 | 已解决 | 所有调用点进入 `AppLog`，Sanitizer 在 Sink 前执行 |
| Crash 路径锁与 Keystore 风险 | 已解决 | 有界 writer；预加载 emergency key；崩溃写入不取普通锁、不访问 Keystore |
| 恢复码重新生成提前失效 | 已解决 | 候选草稿确认后才提交 Envelope；进程恢复显示 DraftExpired |
| DatabaseSession 并发关闭 | 待设备复核 | 会话控制已接入 lease gate，事务故障注入仍需设备测试 |
| 备份 v1 KDF 自描述 | 待后续 | 不属于本轮认证/诊断范围 |

## 高优先级

### P1：DatabaseSession 可能在数据库仍被使用时关闭

`withDatabase()` 只在取得/创建实例时持有 mutex，随后在锁外执行调用方 block；`closeAndAwait()` 可同时取得
mutex 并调用 `RoomDatabase.close()`。长事务或查询与锁定并发时，可能在操作中关闭数据库。超时分支还会在
mutex 外直接把引用置空；`RoomDatabase.close()` 是阻塞调用，协程超时不保证能中止它，因此日志可能声称“强制关闭”但实际资源仍在运行。

建议使用显式 session lease/read-write gate：新操作在 Locking
后被拒绝，锁定等待活跃操作归零，再关闭实例并擦除密钥。不要把引用置空等同于资源关闭。

证据：`DatabaseSession.kt:51-89`。

### P1：备份 v1 不是自描述 KDF 格式

头部只写 magic、version、salt、nonce 和密文长度，解码始终调用当前 `deriveKeyArgon2id` 默认参数。未来调整
Argon2id 参数后，旧备份可能无法恢复；头部也未作为明确 AAD 认证。

建议新增 v2：写入 KDF algorithm id、内存、迭代、并行度、salt/nonce 长度，并以规范化 header 作为 AES-GCM
AAD；保留 v1 reader 测试向量。

证据：`BackupArchiveCodec.kt:24-78`、`BackupManager.kt`。

### P1：消息生产者同时“发布错误”和“返回错误”

`DefaultVerificationGateway` 在失败时发布 `AppMessageCenter`，随后仍把同一个 `AppResult.Failure`
交给回调。任何调用方只要也按失败显示消息，就会恢复双 Toast 问题。当前 Verification
的生物识别路径暂时没有再次发布，但契约本身没有保证单一所有权。

建议二选一：Gateway 只返回结果，由页面级 effect 发布；或 Gateway 返回不含展示职责的结果并明确由它独占发布。更推荐前者，使
Domain/认证编排不依赖全局 UI 消息中心。

证据：`DefaultVerificationGateway.kt:34-59,93-110`。

## 中优先级

### P2：Room 仍保留失效 KeyEnvelope 实现

当前 Envelope 真相源是 Proto `BootstrapStore`，但 `AppDatabase` 仍注册 `KeyEnvelopeEntity/Dao`
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

### P2：集中消息仍存在旁路 Toast 和脆弱去重

`BiometricAuthenticator` 对非取消错误直接调用 Android Toast，绕过设置、host 和测试边界。
`AppMessageCenter` 的去重只记住最后一个 key，交替事件可以绕过；合法的相同事件也可能被吞。
`MutableSharedFlow.tryEmit` 在没有收集者或缓冲溢出时不会提供可靠交付语义。

建议删除底层直接 Toast；使用带稳定 event id/来源的 UI effect 或有界队列。去重只针对同一业务事件
id，不基于文本猜测。

证据：`BiometricAuthenticator.kt:40-48`、`AppMessageCenter.kt:19-38`。

### P2：密钥擦除注释可能高估保证

`key.encoded.fill(0)` 通常覆盖 `SecretKeySpec` 返回的编码副本，不保证覆盖对象内部字节；同时 ZIP/加密
API 可能产生不可控副本。

建议由密钥派生函数返回可控 `ByteArray`，在构造短生命周期 `SecretKeySpec` 后覆盖源数组，并把文档/注释表述为
best-effort，而非可靠清零。

证据：`BackupArchiveCodec.kt:46-48,77-79`。

## 低优先级与维护项

- `vault_historys` 是公开 Schema 中的拼写错误。全新安装基线下应在冻结 v1 前改为 `vault_history` 或
  `vault_snapshots`，否则后续只能通过迁移修正。
- Proto 中 `card_style`、滑动动作、Autofill 模式、排序等固定集合仍使用 string；逐步改 enum，避免运行时无效值。
- 仓库只有 CodeQL workflow，缺少文档声明的编译、单测、Lint 和 APK GitHub Actions 门禁；应新增独立 CI
  workflow。
- `QUERY_ALL_PACKAGES` 属于高敏 Manifest 权限。应确认应用商店政策必要性，能用 `<queries>` 精确声明时优先收窄。

## 已核对的正向结果

- Domain 未发现 Data/Feature/Android import，Security 未发现 Data 实现 import，Feature 未发现
  Entity/DAO/具体 Repository import。
- AppSettings 与 Bootstrap Serializer 已将 Proto 解析失败转换为 `CorruptionException`。
- 备份读取会读满固定字段、拒绝尾随数据，并限制 ZIP 路径、重复条目和大小。
- 权限目录覆盖 Manifest 当前声明的相机、通知、网络、前台服务、振动、生物识别和应用查询权限。
