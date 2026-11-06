# Data 映射边界

`:data` 把 Room、加密 payload 与 Domain 模型隔离，不承载界面或跨 Feature 业务流程。

## 当前路径

Entry 数据分为三类，不能用一条统一的“Blob 解密链”描述：

| 数据 | 当前存储 | 主要映射 |
|---|---|---|
| Profile 元数据 | `entries` 结构化列 | `EntryProfileMapper.fromEntity()` |
| 低敏 Secret 结构 | `entry_secret_fields/STRUCT_BUNDLE` 密文 | `SecretBundleCodec` + `EntrySecretMapper` |
| 高敏单字段 | `entry_secret_fields/<fieldKey>` 独立密文 | `SecretFieldCodec` + `SensitiveFieldSetMapper` |

读取普通详情时：

```text
EntryEntity -> EntryProfileMapper -> EntryProfile
STRUCT_BUNDLE row -> SecretBundleCodec -> EntrySecret (高敏值为 null)
EntryAssembler -> Entry
```

读取已授权单字段时，仅查询对应 `fieldKey` 并经 `SecretFieldCodec` 解密。只有 Backup 快照边界会调用
Data 内部的 `SecretFieldStore.readAll()`，把 bundle 与各高敏字段合并；Feature 不经 Domain Repository
读取完整 secret。

## 读取调用与参数传递

普通详情的 `RoomEntryQueryRepository.getById(entryId)` 先检查 `sessionState.hasFullSecureSessionAccess()`；通过后以
`databaseSession.query { ... }` 获取 session read lease，再用 `entryId.value` 调用
`entryQueryDao().getById(...)`。找到 `EntryEntity` 后，`EntryProfileMapper.fromEntity(metaEntity)` 只映射结构化列；
同一个 `AppDatabase` 接收者、`entryId.value` 和 `metaEntity.entryType` 传给
`secretFieldStore.readBundle(db, entryId, entryType)`。最后
`EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)` 组合 Domain `Entry`。无完整会话时 Repository
返回 `null`，不表示数据库确认该 ID 不存在；调用方只能在已建立访问上下文时把该结果解释为缺失。

授权读取单字段时，`RoomSensitiveFieldRepository.reveal(entryId, key, action, permit)` 只是将 `key` 包成
`setOf(key)` 并委托 `revealMany(entryId, keys, action, permit)`，再用 `singleOrNull()` 返回单个结果。
批量路径先要求完整会话，并以同一组 `entryId`、`keys`、`action` 构造
`AuthorizationScope.SensitiveFields` 交给 `permitVerifier.consume(permit, scope)`；通过后才进入
`databaseSession.query`，逐个用 `key.toStorageKey()` 查询 `getField(entryId.value, storageKey)`，并把
`codec.decrypt(entryId.value, key, entity.valueCipher)` 包成 `RevealedSensitiveField` / `OwnedChars`。
`getPresence(entryId)` 只读取非 `STRUCT_BUNDLE` 的存储键并通过 `sensitiveFieldKeyFromStorageKey()` 映射，
不解密值；无完整会话时返回空键集，因此调用方也须先区分访问受限与“确实没有高敏字段”。

Backup 的完整快照由 `RoomBackupSnapshotReader` 在其已有数据库接收者内调用
`secretFieldStore.readAll(db, metaEntity.entryId, metaEntity.entryType)`。该方法读取同一条目的全部密文行，
以 `SecretBundleCodec.decrypt(cipher, entryId, entryType)` 解码 bundle，再按每行稳定 `fieldKey` 经
`sensitiveFieldKeyFromStorageKey()` 和 `SecretFieldCodec.decrypt(entryId, key, cipher)` 解密字段，最后
`bundle.mergeSensitiveFields(fields)` 形成快照输入。该完整读取能力留在受控备份适配器，不改变
`EntryQueryRepository.getById()` 的普通详情边界。

写入时，Profile 直接映射到 `EntryEntity`；Secret 由 `SensitiveFieldSetMapper` 拆成 bundle 与逐字段值，
再由各自 Codec 加密。Revision 另有独立的 `EntryContentSnapshotCodec` 和
`SensitiveRevisionSnapshotCodec`；其中 `SummaryPayload` 是历史快照格式，不是 `entries` 的当前格式。

## 写入调用与参数传递

更新入口为 `RoomEntryCommandRepository.updateEntry(id, expectedVersion, changes)`，它把类型化 ID/version
解包成字符串/整数传给 `UpdateEntryExecutor.execute(id.value, expectedVersion.value, changes)`。
Executor 调用 `databaseTransactions.write("entry_update") { ... }`；该 runner 检查完整会话访问并以
`AppDatabaseSession.transaction` 提供事务接收者，然后把 `db`、`id`、`expectedVersion`、`changes` 和
`clock.now()` 传给 `EntryUpdateTransaction.update(...)`。事务内先读 `EntryEntity` 并比较预期版本，只用
`secretFieldStore.readBundle(db, id, entity.entryType)` 取低敏结构，再读存储键名判断 Preserve/Clear/Replace
语义；替换时 `updateExisting(db, id, newSecret, entity.entryType)` 只改有显式更新的字段行并重写 bundle。
随后按实际敏感键重算 capability flags，以
`entryCommandDao().optimisticUpdate(entryId, expectedVersion, title, username, primaryUrl, domains, applicationIds, iconName, iconCustomReference, favorite, tags, iconColor, expiresAt, capabilityFlags, otpType, updatedAt)` 写入 Profile 与版本；这些值分别来自 `newProfile`、`flags`、`newSecret.otp?.config?.type` 和 `now`。影响行数不为 1 即抛 `Conflict`。同一事务再调用
`revisionWriter.snapshotChanges(db = db, entryId = id, entryVersion = expectedVersion + 1, summary = newProfile, secret = newSecret, entryType = entity.entryType, now = now)`
和 `activityWriter.recordActivity(db, id, UPDATE, now, ContentRevision(EntryVersion(expectedVersion + 1)))`。

`EntryUpdateExecutor` 仅在事务结果为 `Changed` 时于同一事务内登记附件 GC；提交后
`postCommitCleanup.afterCommit(result) { ... }` 分别执行附件 `drain()` 与旧图标
`cleanReplacedIcon(previousIcon, currentIcon)`。普通清理异常只记录，不改写已提交的 `AppResult.Success`；取消与
fatal `Error` 继续传播。若提交后取消，调用方应重查 Repository/Room 真相，不把取消解释成事务回滚。

创建路径由 `RoomEntryCommandRepository.createEntry(entry)` 转给 `CreateEntryExecutor.execute(entry)`，并使用
`databaseTransactions.write("entry_create")`；Autofill 与 Inbox 分别选择 `executeForAutofill(entry)` 的
`writeForAutofill`，以及 `executeForInbox(entry)` 的同一访问门并启用 `idempotentByEntryId = true`。
事务用 `Entry` 的 ID、Profile 和 type 构造 `EntryEntity`，以 `EntryCapabilities.from(entry.secret)` 生成初始能力，
再顺序调用 `insertStrict(entity)`、`secretFieldStore.replaceAll(db, entryId, entry.secret, entry.type)`、
`revisionWriter.snapshotChanges(..., entryVersion = 1)`、同事务附件 GC 登记和
`activityWriter.recordActivity(..., CREATE, ..., ContentRevision(EntryVersion.INITIAL))`。提交后才由
`postCommitCleanup.afterCommit(result) { attachmentGarbageCollector.drain() }` 排空资源队列。

这些 Repository 在检查会话权限后再进入 `AppDatabaseSession.query/transaction`；权限检查与排队 lease 获取之间的
线性化边界由[维护 TODO Task 37](../superpowers/plans/2026-10-30-passly-maintainability-todo.md#task-37为认证撤权与数据库操作定义同一线性化边界)跟踪，
不能仅凭本文的顺序描述推断并发锁定时所有入口已原子化。

`SecretPayload` 保持扁平的版本 1 JSON 结构。`EntrySecretMapper` 是根边界：编码时核对凭据种类与预期
`EntryType`；解码时拒绝未知 schema 版本、多个凭据槽位及槽位与条目类型不匹配。凭据槽位由按
`EntryCredentialKind` 索引的 `EntryPayloadAdapterRegistry` 转换；`LOGIN` 等多个条目类型可共享同一凭据
适配器。适配器只负责自己的凭据槽位，根映射仍负责 `notes`、自定义字段和 schema/类型校验。缺失适配器、
不支持的值或适配失败都会显式失败，不会回退到其他凭据映射。

当前适配器覆盖 OTP、Login、Card、Wi-Fi、Identity、SSH 和 Passkey；`NONE` 不需要适配器。适配器拆分
不改变 `SecretPayload` 的字段名、默认值、顺序或版本，也不改变 Room schema、加密/AAD 或备份格式。
新增凭据槽位时，需同步验证注册覆盖、所有字段双向往返和根边界的条目类型校验。

银行卡发卡行、支行、产品名称、币种、客服电话及账单地址位于 `CardCredential` / `CardCredentialPayload`，
经 Secret bundle 加密保存，不新增 Profile 明文列。持卡人的真相源为 `cardHolder`；字段读取与编辑不借用用户名。
扩展字段保持 Payload 版本 1，读取、详情、修订快照和备份使用同一组凭据属性。

修订的 `iconCustomReference` 是可查询的资源引用，与快照中的图标路径同步写入并建立索引；当前条目路径也建索引。
资源回收无需解密修订，必须在写事务内同时确认两种引用均不存在。历史裁剪与条目永久删除先收集路径，
提交后通过可重试队列回收；附件 `PENDING_GC` 状态在同一事务持久化。完整 Vault 会话解锁期间，collector 以 64 条为上限稳定分页并完整排空；文件/数据库暂态失败按 250 ms 起步、最多 30 s 的指数退避重试。封存后停止数据库访问，下次授权解锁继续处理 durable 队列。
附件 drain 和图标队列 drain 分阶段执行，某一阶段的普通异常不会阻止另一阶段登记/重试资源。普通 post-commit
清理异常记录结构化 `ResourceCleanupFailed`（未分类的存储阶段异常使用 `STORAGE_FAILURE`；明确的文件删除失败
保留 `FILE_DELETE_FAILED`），但保留数据库事务原结果；`CancellationException` 与 fatal `Error`
继续传播。取消可能发生在事务提交之后，此时事务不会回滚，调用方应重新读取 Repository/Room 真相后决定下一步，
不把取消本身解释为写入失败。图标文件扫描覆盖普通 `favicon_*.webp` 与备份恢复的 `favicon_restored_*.webp`，
也兼容旧 `restored_*.webp` 名称；删除前仍由事务查询当前及历史引用。数据库失败不能退化为直接删除文件。
Room schema 仍为 1，不迁移旧开发数据库，也不在结构不匹配时静默删库。

## 已存储条目的能力标记

普通查询返回的 Secret bundle 中，高敏值为 null，表示未揭示；不能据此推断密码或 OTP 不存在。
`EntryCapabilities.from` 用于创建时的完整凭据。已存储条目通过 `fromStoredSecret` 组合结构信息和实际高敏键：
`PASSWORD` / `SSH_PASSPHRASE` 行提供密码能力，`OTP_SECRET` 行提供 OTP 能力，其余高敏键不冒充这些能力。
Wi-Fi 密码保存在加密 bundle 中，没有独立密码行；其密码能力仍由 bundle 内的实际值决定。

更新先检查条目版本，在同一事务内执行高敏字段的 Preserve/Clear/Replace，再读取实际键集并更新能力位图、
条目、修订和活动记录。只读键名，不为能力计算解密高敏内容。保留字段仍保留能力，显式清空则撤销对应能力；
失败或取消由现有事务边界回滚。修订恢复也使用同一 Domain 规则，从恢复的高敏快照键重建标记。

## 目录职责

- `local/database/entity`：Room 表结构与约束。
- `codec/entry`：Secret bundle 和单字段密文编码。
- `codec/revision`：Revision 快照编码。
- `codec/entry/payload`：加密内容解密后的稳定序列化模型。
- `mapper/entry`：Entity、Payload 与 Domain 的纯转换、拆分和组装；根映射验证 schema/类型，凭据适配器映射各自槽位。
- `repository/entry`：在 session/事务边界内编排 DAO、Codec 和 Mapper。

## 约束

- Mapper 不访问 DAO、不开事务、不执行加密，也不依赖 Android UI。
- Codec 不实现业务规则，只维护稳定序列化格式、版本和记录级 AAD。
- Entity 不泄漏到一般 Feature。备份快照适配器是受测试约束的明确例外，但备份文件仍使用自己的 DTO。
- 附件引用映射只表达现存 Entry 与资源的关系；附件编辑暂存需要由具体工作流定义 owner 以及提交、取消和清理边界，再决定持久化方式。
- 需要查询、排序、索引或约束的非敏感 Profile 数据使用结构化列；不能为了省字段把它重新塞回不透明
  Blob。
- 高敏字段保持逐字段密文，不进入 Profile、日志、普通详情 bundle 或查询索引。
- 持久化枚举使用显式稳定键；不使用 ordinal，也不在未知值上静默回退。

备份协议与流程归 `app/feature/backup`。它可以通过受控快照边界读取完整数据，但不属于 `:data` 的
通用映射职责。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 将数据库条目映射为领域 Profile | `EntryProfileMapper.fromEntity()` | [`EntryProfileMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryProfileMapper.kt) |
| 拆分并组装低敏与高敏字段 | `EntrySecret.toSensitiveFieldValues()`、`toSensitiveFieldUpdates()`、`toBundleSecret()`、`mergeSensitiveFields()`（Data 内部） | [`SensitiveFieldSetMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/SensitiveFieldSetMapper.kt) |
| 按凭据种类注册并转换 Secret payload 槽位 | `EntryPayloadAdapterRegistry`、`EntryPayloadAdapter` | [`EntryPayloadAdapterRegistry.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapterRegistry.kt)、[`EntryPayloadAdapter.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapter.kt) |
| 编排秘密字段的持久化读取与写入 | `SecretFieldStore.readAll()`、`readField()` | [`SecretFieldStore.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt) |
