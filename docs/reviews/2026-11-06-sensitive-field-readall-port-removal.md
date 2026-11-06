# 2026-11-06 敏感字段完整读取端口收窄记录

## 结论

已从 `SensitiveFieldRepository` 移除无生产消费者的 `readAll(entryId)` 及 `RoomSensitiveFieldRepository` 对应实现，并删除 App 测试替身中的死 override。完整敏感数据不再通过通用 Domain Repository 暴露；Backup 使用的 Data 内部 `SecretFieldStore.readAll(db, ...)` 仍保留在专用快照流程中。

## 调用链与参数

当前 `RoomSensitiveFieldRepository` 实现 `getPresence(entryId)`、`reveal(entryId, key, action, permit)`、`revealMany(entryId, keys, action, permit)` 和 `readBundle(entryId)`。

- `reveal()` 将单个 `key` 转成 `setOf(key)` 并传入 `revealMany(entryId, keys, action, permit)`；`revealMany()` 先要求 `sessionState.hasFullSecureSessionAccess()`，再通过 `permitVerifier.consume(permit, AuthorizationScope.SensitiveFields(entryId, fieldKeys = keys, action))` 校验条目、字段集合和动作。成功后只查询请求的 `key.toStorageKey()`，并以 `codec.decrypt(entryId.value, key, entity.valueCipher)` 解密该字段。
- `readBundle(entryId)` 只在完整安全会话开放时进入 `databaseSession.query { ... }`，查询 `entryQueryDao().getById(entryId.value)?.entryType`，然后调用 `fieldStore.readBundle(this, entryId.value, entryType)`，仅解密结构 bundle。
- 被移除的 Repository `readAll(entryId)` 只检查完整安全会话，随后读取 entry type 并将全部内容交给 `SecretFieldStore.readAll()`；它没有 `AuthorizationPermit` 参数，也没有生产调用者。相比 permit-gated 的字段读取，它会一次性解密 bundle 和所有字段行，暴露了不必要的宽权限能力。
- Backup 保留独立调用：`RoomBackupSnapshotReader.readBundle(includeIcons, includeAttachments, includeDeleted, includedEntryTypes)` 在 `databaseSession.query` 中筛选条目，随后对每个 `metaEntity` 调用 `secretFieldStore.readAll(this, metaEntity.entryId, metaEntity.entryType)`，并把结果传给 `EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)`。该批量读取属于 Backup 快照与操作授权边界，不是通用字段访问 API。

## 调查范围

生产源码全仓 `readAll(` 调用闭包仅剩 Data 内部 `SecretFieldStore.readAll(db, entryId, entryType)` 与上述 Backup 调用。此前 `SensitiveFieldRepository.readAll(entryId)` 的其余匹配只在测试 fake 的无用 override 中。已核对 Hilt binding，以及编辑、reveal、复制、二维码和 revision restore 调用；这些流程继续使用各自的字段级授权或 revision permit 合同，无需新增桥接层。

同步后的正式文档为 [敏感数据读取、显示与剪贴板](../security/sensitive-data-access.md)、[数据库](../data/database.md) 和 [数据映射](../data/mapping.md)：普通详情读取只解密低敏 bundle；字段 reveal 按 permit 读取；完整凭据快照仅由 Backup 内部快照读取。

## 历史验证记录

实施时记录的验证结果：`:app:testDebugUnitTest` 1,019 tests、`:data:testDebugUnitTest` 123 tests 均通过，`verifyArchitecture` 与 `git diff --check` 通过。以上为实施时结果，本归档轮次没有重跑测试。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 字段存在性、授权揭示与低敏 bundle 读取 | `RoomSensitiveFieldRepository.getPresence()`、`reveal()`、`revealMany()`、`readBundle()` | [`RoomSensitiveFieldRepository.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/RoomSensitiveFieldRepository.kt) |
| Backup 专用完整秘密读取 | `SecretFieldStore.readAll()` | [`SecretFieldStore.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt) |
| 按所选条目组装备份快照 | `RoomBackupSnapshotReader.readBundle()` | [`RoomBackupSnapshotReader.kt`](../../app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotReader.kt) |
