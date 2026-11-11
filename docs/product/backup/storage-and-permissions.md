# Backup 快照、SAF 目录与目标 URI

状态：当前实现（代码核对：2026-11-07）。

## Domain/Data 快照边界

导出时 `BackupArchiveServiceImpl.export(request)` 按用户选项和 Adapter 能力创建
`BackupSnapshotReadOptions(includeIcons, includeAttachments, includeDeleted, includedEntryTypes)`，传给
`BackupSnapshotGateway.read(options)`。App `AppBackupSnapshotGateway.read(options)` 将其映射为
`VaultBackupReadOptions`，传入单资源上限 `BackupBundleValidator.MAX_RESOURCE_BYTES.toLong()`（16 MiB）与累计上限
`BackupBundleValidator.MAX_TOTAL_RESOURCE_BYTES.toLong()`（128 MiB），并调用
`VaultBackupStore.read(options)`。Data `RoomVaultBackupStore.read(options)` 在生成完整快照前按两个上限累积
图标和附件明文资源；若后续文件读取、解密或校验失败，会清零已累积及当前失败资源的缓冲。
App `BackupSnapshotArchiveMapper.toBundle(snapshot, appVersion = { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }, exportedAt = System.currentTimeMillis())`
完成协议映射和 `BackupBundleValidator.validate(bundle, requireResourceData = resources.isNotEmpty())` 校验后才转移缓冲所有权；
映射、版本读取或校验失败时清零 Domain 快照资源。图标 archive ID 生成为 `icon_<entryId>`，附件沿用资源 ID；bundle 的
`resourceData` 持有同一组资源数组。随后由 Archive Service 在最终使用后清零 bundle 资源。
Data 使用 `databaseSession.queryFullVault(onDenied = { throw SessionModeRestricted() }) { ... }` 执行完整 Vault 读取并筛选
`entryQueryDao().getAll()` 的 Entry，再用 `EntryProfileMapper.fromEntity(entity)` 与
`secretFieldStore.readAll(db, entity.entryId, entity.entryType)` 作为
`EntryAssembler.assembleFromDatabase(entity, profile, secret)` 的参数组装完整 Domain `Entry`；按开关读取图标和附件内容，
只返回所选 Entry 两端都存在的 `EntryLink`。App `BackupSnapshotArchiveMapper.toBundle(...)` 使用
`KeyedEntryArchiveMapper.toRecord(entry, attachmentIds)`、`BackupArchiveKeyRegistry.relationTypeKey(type)` 和资源稳定 ID
形成 `BackupBundle`。归档层仍会再次验证资源数和总字节，作为
协议完整性校验；快照内存预算已在 Data 物化期间执行。

导入时 `AppBackupSnapshotGateway.restore(bundle, mode)` 先保留覆盖保护：仅当源 archive 原本为空，或仍有至少一条
兼容后的记录时才允许 `OVERWRITE`。随后通过 `KeyedEntryArchiveMapper.toEntry(record)` 及
`BackupArchiveKeyRegistry.relationType(key)` 转换 archive records/links，以 `bundle.resourceData[record.id]` 提供资源字节，
映射 archive `ImportMode` 到 `VaultBackupRestoreMode`，再调用
`VaultBackupStore.restore(VaultBackupSnapshot(entries, links, resources), mode, onCommitted)`。
Data `RoomVaultBackupStore.restore(snapshot, mode, onCommitted)` 先以 `attachmentGarbageCollector.withMutationLock` 校验
Entry/resource/link 身份和重复项，随后调用
`databaseSession.transactionFullVault(onDenied = { throw SessionModeRestricted() }) { ... }`，按模式清理或跳过已存在 Entry、写入
Entity/秘密字段/附件引用与链接，并通过
`VaultRestoreFileJournal` 暂存/替换图标和附件文件。Room 事务失败时对 journal 逆序 rollback；事务成功后同步执行
`onCommitted`，再 commit journal，覆盖模式最后清理未引用文件，避免提交后清理取消时遗漏派生状态通知。
Data 返回 `VaultBackupRestoreResult(importedEntryCount, existingEntryCount)`，App 转为
`BackupRestoreResult`；archive service 最终清零 bundle 中的资源数组。

完整备份可包含所选条目及业务字段、关系、已提交附件和可选图标。Draft、活动记录、修订历史、可重建索引、
设置、会话、DEK、Bootstrap、认证材料、Room 密文结构、本机路径和未提交附件不进入备份。
协议细节见[Passly Archive v1](passly-archive-v1.md)，验证边界见[验证要求](verification.md)与
[ADR-0022](../../project/decisions/ADR-0022-keyed-backup-document.md)。文档、加密容器和 Room Schema 各自独立版本；
当前协议不兼容旧 `passly-vault`/`passly-archive` 文档。

恢复的图标以本机 `favicon_restored_<id>.webp` 名称落盘；图片处理器重建时的资源发现也兼容此前使用的 `restored_<id>.webp`。
这些本机文件名不进入归档格式。崩溃恢复候选在删除前仍按数据库中的当前条目和修订引用检查，避免回收有效资源。


## SAF 目录和指定目标

默认备份目录使用 Storage Access Framework tree URI 与 persisted write grant，不请求传统存储权限或
`MANAGE_EXTERNAL_STORAGE`。`BackupDirectoryGrantInspector` 是设置页和导出链路共享的授权检查器，区分未配置、
grant 有效、grant 丢失和 provider/tree 不可用。

“权限管理”只显示状态并导航到 Backup；选择、更换、重新授权和清除目录只在 Backup 页面完成。清除设置不会
删除已有备份。用户主动执行“测试写入权限”时，在实际 Passly 子目录创建单字节临时文件并清理；清理失败同样
视为检查失败。tree URI 不是普通文件，不能用 `openFileDescriptor` 覆盖或截断它。

更换目录时先取得新授权并保存新 tree URI，再尽力释放旧授权；清除目录时先清除设置，再回收旧授权。设置变更成功后，
旧授权释放失败不能撤销或保留对旧 URI 的设置，否则设置会指向已经失效的目录；更换时若设置写入失败，先读取当前设置，
仅在确认设置不指向新 URI 后才尽力释放刚取得的新授权。读取设置失败时保留授权，避免释放已被持久化引用的 URI。
清除操作若在设置已经清空后被取消或报错，也会在不可取消区重读设置，并仅当 URI 已不再被引用时释放授权；随后仍传播原错误或取消。
授权回收失败可能暂时留下多余的旧系统授权，但不会破坏当前设置与系统授权的一致性。
选择、更换和清除使用同一互斥区串行执行。每次操作在取得互斥权后读取当前已保存 URI；清除不使用 UI 事件携带的旧快照，
因此排队等待期间完成的选择会成为随后清除实际移除并释放的 URI。

另存到指定位置使用单文件 `CreateDocument` 合同，与默认目录 tree grant 分开。两者都由系统选择器授予目标访问，
不需要完整存储权限。SAF Route 收到平台 `Uri` 后立即转为不透明 URI 字符串交给流程编排；授权由系统按应用与 URI 授予，
不依赖同一个 `Uri` 对象实例。Backup 不为单文件导入/导出持久化 URI 授权，也不承诺设备重启后继续访问；操作在当前授权有效时直接使用该 URI。
SAF 存储实现负责解析字符串并执行平台操作。默认目录 tree grant 仍由目录授权流程单独持久化和回收。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| App archive↔Domain 快照映射与读写入口 | `AppBackupSnapshotGateway.read(options)`、`restore(bundle, mode)`、`BackupSnapshotArchiveMapper.toBundle(snapshot, appVersion, exportedAt)` | [`AppBackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt)、[`BackupSnapshotArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/BackupSnapshotArchiveMapper.kt) |
| Domain 持久化中立的快照合同 | `VaultBackupStore.read(options)`、`restore(snapshot, mode, onCommitted)` | [`VaultBackupStore.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/backup/port/VaultBackupStore.kt) |
| Room、秘密字段、附件资源与链接快照 | `RoomVaultBackupStore.read(options)` | [`RoomVaultBackupStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt) |
| SAF 持久 grant 与 URI 可达状态检查 | `BackupDirectoryGrantInspector.inspect(treeUri)`、`AndroidBackupDirectoryAccess.inspect(treeUri)` | [`BackupDirectoryGrantInspector.kt`](../../../app/src/main/java/com/aozijx/passly/app/backup/BackupDirectoryGrantInspector.kt)、[`AndroidBackupDirectoryAccess.kt`](../../../app/src/main/java/com/aozijx/passly/app/backup/AndroidBackupDirectoryAccess.kt) |
| 目录选择、更换、清除时的授权提交和补偿 | `BackupDirectorySettingsInteractor.select(treeUri)`、`clear()` | [`BackupDirectorySettingsInteractor.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/backup/BackupDirectorySettingsInteractor.kt) |
