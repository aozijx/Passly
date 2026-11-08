# Backup Data Port 迁移审查记录

**日期：** 2026-11-07

**范围：** 将 Backup 的 Room 快照读取、恢复与资源文件事务从 App 实现迁移到 Domain port / Data 实现，并核对导出缓冲所有权。

**状态：** Data port 迁移与端到端流式 I/O 重构均已完成。Archive Service 的跨边界参数路由、流关闭及缓冲清理由[验证矩阵](../features/backup/verification.md#现有测试入口与覆盖范围)和对应测试覆盖；本记录保留 Data 端口迁移证据，不再引用已关闭的流式待办。

## 当前调用合同

导出由 `BackupArchiveServiceImpl.export(request)` 按请求和格式能力构造 `BackupSnapshotReadOptions`，调用
`BackupSnapshotGateway.read(options)`。App `AppBackupSnapshotGateway.read(options)` 将筛选项和
`BackupBundleValidator.MAX_RESOURCE_BYTES.toLong()`（16 MiB）、`MAX_TOTAL_RESOURCE_BYTES.toLong()`（128 MiB）传入
`VaultBackupReadOptions`，然后调用 `VaultBackupStore.read(options)`。

Data `RoomVaultBackupStore.read(options)` 通过
`databaseSession.queryFullVault(onDenied = { throw SessionModeRestricted() })` 获取完整 Vault 查询边界；按
`includeDeleted` / `includedEntryTypes` 筛 Entry，经 `EntryProfileMapper.fromEntity(entity)` 与
`EntryAssembler.assembleFromDatabase(entity, EntryProfileMapper.fromEntity(entity), SecretFieldStore.readAll(db, entryId, entryType))`
组装 Domain Entry。图标由 `readIcon(entryId, path, root, maxBytes)`
检查 WebP、目录边界和单项大小；附件由 `attachmentRefQueryDao().getByEntryIds(entryIds)`、
`attachmentResourceDao().getById(resourceId)`、`attachmentGarbageCollector.resourceFile(resourceId)` 读取并解密，
清零密文后验证明文大小与 content ID。`VaultBackupResourceAccumulator.add(resource)` 实施单项与累计预算；失败时
`fail(failure)` 擦除已累积明文并原样重抛。

App `BackupSnapshotArchiveMapper.toBundle(snapshot, appVersion, exportedAt)` 生成稳定 archive keys 并调用
`BackupBundleValidator.validate(...)`。只有映射和验证成功才把资源数组所有权移交给 bundle；失败时清零 Domain snapshot
缓冲。Archive Service 在操作收尾时清零 bundle 资源。

导入由 `AppBackupSnapshotGateway.restore(bundle, mode)` 保护非空来源被兼容过滤成空列表时的 `OVERWRITE`，验证 bundle，
将 records/links/resources 映射到 Domain snapshot，再把 `ImportMode` 映射成 `VaultBackupRestoreMode` 并调用
`VaultBackupStore.restore(snapshot, mode)`。Data `RoomVaultBackupStore.restore(snapshot, mode)` 在 GC mutation lock 下
执行 `databaseSession.transactionFullVault(onDenied = { throw SessionModeRestricted() })`；Room 事务失败时回滚
`VaultRestoreFileJournal`，提交后才清理旧资源。普通 cleanup Logger 异常由
`reportBestEffortBackupCleanupFailure { ... }` 吞掉，避免改变已提交的结果；取消与 fatal `Error` 仍传播。

## 变更与边界

- Domain 只声明持久化中立的 `VaultBackupStore` 和 snapshot/read/restore 模型。
- App 保留 archive 格式、兼容策略、稳定键和 `BackupSnapshotGateway` adapter；archive 转换独立由
  `BackupSnapshotArchiveMapper` 承担。
- Data 独占 Room、`SecretFieldStore`、附件 GC、资源加解密和 Room/文件 journal。
- 旧 App `RoomBackupSnapshotReader`、`RoomBackupSnapshotRestorer`、`RoomBackupFaviconResource` 与 App `RestoreFileJournal`
  已删除。`RoomVaultBackupStore.kt` 的重复 failure-priority helpers 也已移除，只保留
  `VaultRestoreFileJournal.kt` 中的 `preferVaultRestoreFailure(...)`。
- Room、archive 与 container 版本未因迁移变化。

## 验收记录

原实施计划记录：Domain 测试与 compile 通过；Data unit tests 与 AndroidTest source compile 通过；
`RoomVaultBackupStoreTest` 在 Pixel_8_Pro AVD 上 6/6 通过；`VaultBackupResourceAccumulatorTest`、
`BestEffortBackupCleanupLoggingTest`、`BackupSnapshotArchiveMapperTest` 和 `AppBackupSnapshotGatewayTest` 覆盖预算、
缓冲擦除、提交后清理日志和映射合同。计划另记录 `:buildSrc:test`、`verifyArchitecture`、
`:data:testDebugUnitTest`、App Backup 定向测试、`:app:compileDebugKotlin`、App AndroidTest source compile 与
`git diff --check` 通过。本记录引用的是实施计划中的执行结果，本次归档未重跑这些命令。

App 没有 Backup 专属的设备测试。`BackupArchiveServiceImplTest` 已验证 `export(request)` 按 `format`、`password`、
`options` 与 `targetUri` 路由，按 Adapter capability 裁剪 `BackupSnapshotReadOptions`，并在编码、写入失败或取消时清理
bundle/resource 缓冲；`import(request)` 按 `sourceUri`、`format`、`password`、`strategy`、`mode` 路由，恢复失败或取消时清理
payload 与 bundle 资源。上述已由验证矩阵记录，不作为开放 TODO。兼容导入将普通无效条目异常限制为
`IllegalArgumentException`、传播 fatal `Error`，以及指定 URI 导出失败后的目标文件补偿合同，分别由验证矩阵与[导出清理说明](../features/backup/export.md)记录。当前 FileStore、格式 adapter 和 archive service 以受限流传递内容；流式协议与累计读取预算见[格式说明](../features/backup/passly-archive-v1.md)。

## 代码位置

| 职责 | 生产符号 | 文件 |
| --- | --- | --- |
| App Domain gateway | `AppBackupSnapshotGateway.read(options)`、`restore(bundle, mode)` | [`AppBackupSnapshotGateway.kt`](../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt) |
| App archive mapper | `BackupSnapshotArchiveMapper.toBundle(snapshot, appVersion, exportedAt)` | [`BackupSnapshotArchiveMapper.kt`](../../app/src/main/java/com/aozijx/passly/app/database/backup/BackupSnapshotArchiveMapper.kt) |
| Data Room snapshot | `RoomVaultBackupStore.read(options)`、`restore(snapshot, mode)` | [`RoomVaultBackupStore.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt) |
| Data resource ownership | `VaultBackupResourceAccumulator.add(resource)`、`fail(failure)` | [`VaultBackupResourceAccumulator.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/backup/VaultBackupResourceAccumulator.kt) |
| Post-commit best-effort reporting | `reportBestEffortBackupCleanupFailure(report)` | [`BackupCleanupFailureReporting.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/backup/BackupCleanupFailureReporting.kt) |
