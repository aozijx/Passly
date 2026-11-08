# Backup 验证要求

状态：当前实现合同（代码核对：2026-11-08）。

协议或流程变化至少验证：

- JSON 与加密格式解码到相同 canonical records；
- golden JSON 只含显式小写稳定键，不含 Kotlin enum 名/ordinal；
- compatible 与 strict 使用同一输入覆盖不同处置；
- 格式身份、版本、KDF、GCM、大小、摘要、ZIP 路径和重复 ID 错误在两种策略下都失败；
- `OVERWRITE` 在可恢复集合为空时不清库，执行失败整体回滚；
- 资源 journal 能补偿数据库/文件失败，并记录强杀窗口限制；
- 默认目录 grant 丢失、provider 不可用、测试文件清理失败都不能显示“可写”；
- 指定 URI 导出与默认目录导出不共享隐含目标；
- TXT 不可导入且不包含 favorite 字段；
- 新增 Domain 类型/字段前，archive registry 和 round-trip 测试已覆盖。

归档格式 Adapter 不导入 Room DTO，也不执行 Room/DAO 访问；App `AppBackupSnapshotGateway` 只消费 Domain
`VaultBackupStore`；Data `RoomVaultBackupStore` 拥有 Room 与资源持久化，不解析归档 JSON。Room Schema、文档
version 和 container version 的测试必须分开。

## 现有测试入口与覆盖范围

按调用边界选择测试，避免只因测试名含“Backup”就把它当成端到端证据：

| 行为 | 当前测试入口 | 主要证明 |
| --- | --- | --- |
| 归档字段映射 | [`KeyedEntryArchiveRoundTripTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveRoundTripTest.kt) | `KeyedEntryArchiveMapper.toRecord(entry, attachmentIds)` / `toEntry(record)` 的银行卡、OTP 和稳定键往返；对应断言包括 `bankCardRoundTripPreservesBusinessFieldsAndStableWireKeys`、`otpRoundTripPreservesEveryTypeAlgorithmAndEncoding`。 |
| 加密 Header 与 record 流 | [`BackupContainerHeaderTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupContainerHeaderTest.kt)、[`ChunkedBackupStreamsTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/ChunkedBackupStreamsTest.kt) | 数字容器版本 1 与动态 KDF 子版本、跨块往返、记录 tag 篡改和 terminal 后尾随字节拒绝；测试密钥派生为确定性替身，不验证 Argon2 JNI。 |
| JSON/ZIP 与加密编排 | [`BackupCodecTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt)、[`JsonBackupStreamTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/format/json/JsonBackupStreamTest.kt) | ZIP 和加密流组合往返、文件/字段不泄露、JSON 流 round-trip、root decoy 忽略、Passly/Bitwarden 自动分派与歧义根拒绝。 |
| 严格/兼容规划 | [`BackupImportPlannerTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlannerTest.kt) | 未知字段在兼容策略中忽略、未知/结构无效 Entry 在兼容模式跳过而严格模式拒绝、兼容验证只将 `IllegalArgumentException` 计作条目无效并传播 fatal `Error`、重复顶层 Link ID 两种策略都拒绝。 |
| Service 编排与日志 | [`BackupArchiveServiceImplTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImplTest.kt) | 导出选项按请求与 Adapter 能力裁剪、导入格式与 payload/密码/策略路由、恢复模式与结果摘要合并、密码必填校验；覆盖成功/普通失败/取消后的字节清理、错误报告以及成功操作的 start/completed 事件。 |
| wire 值完整性 | [`BackupArchiveKeyRegistryTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveKeyRegistryTest.kt)、[`BackupCodecTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt) | Registry 覆盖领域类型与稳定键；`fieldArchiveV1_wireFieldNamesAreStableAndIndependentFromDatabasePayloads` 覆盖文档版本、字段值编码和稳定 wire keys。 |
| 上层操作与部分文件清理 | [`DefaultBackupOperationGatewayTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGatewayTest.kt)、`BackupCreatedDocumentCleanupTest`、`BackupWriteTestCleanupTest` | 目标 URI 的授权范围、取消传播、部分导出文件删除和清理错误优先级；Service 返回导出失败后清理返回 false/抛普通异常时保留主错误并发出目标可能残留的提示；这不直接证明 archive service 内部的 Adapter 参数传递。 |
| Room 快照与资源写入 | [`RoomVaultBackupStoreTest`](../../../data/src/androidTest/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStoreTest.kt)、[`VaultRestoreFileJournalTest`](../../../data/src/test/java/com/aozijx/passly/data/repository/backup/VaultRestoreFileJournalTest.kt)、[`RestoredAttachmentFileTest`](../../../data/src/test/java/com/aozijx/passly/data/repository/backup/RestoredAttachmentFileTest.kt) | Domain 快照筛选与资源读写、追加/覆盖、同一 Room 会话事务和资源文件变更补偿；迁移计划记录 Room instrumentation 在 Pixel_8_Pro AVD 上 6/6 通过。 |
| 导出资源累计预算与缓冲所有权 | [`VaultBackupResourceAccumulatorTest`](../../../data/src/test/java/com/aozijx/passly/data/repository/backup/VaultBackupResourceAccumulatorTest.kt)、[`BackupSnapshotArchiveMapperTest`](../../../app/src/test/java/com/aozijx/passly/app/database/backup/BackupSnapshotArchiveMapperTest.kt) | Data 逐资源及累计限额拒绝、拒绝/异常路径擦除缓冲及完整快照所有权移交；App 映射或 bundle 校验失败时擦除 Domain snapshot 内容。 |
| 提交后最佳努力日志 | [`BestEffortBackupCleanupLoggingTest`](../../../data/src/test/java/com/aozijx/passly/data/repository/backup/BestEffortBackupCleanupLoggingTest.kt) | Room 已提交后，普通 Logger 异常不改变恢复成功结果；取消和 fatal `Error` 仍传播。 |
| App Gateway Domain/archive 边界 | [`AppBackupSnapshotGatewayTest`](../../../app/src/test/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGatewayTest.kt)、[`BackupSnapshotArchiveMapperTest`](../../../app/src/test/java/com/aozijx/passly/app/database/backup/BackupSnapshotArchiveMapperTest.kt) | 稳定图标键、archive→Domain 资源 ID/字节所有权、兼容过滤后覆盖保护，以及映射失败时清除快照资源缓冲。 |

`BackupArchiveServiceImplTest` 直接断言 `BackupExportRequest.options` 到 `BackupSnapshotReadOptions` 的能力裁剪、
URI/格式/密码/策略/导入模式的流向、结果摘要合并，以及 service 持有的 bundle 资源字节在成功、普通失败与取消后的清理。
它不代替 Codec、Planner、Gateway、Room 或 Android provider 的组件测试。

严格策略和兼容策略共享同一 canonical 校验合同，但不能仅凭 `BackupImportPlannerTest` 推断每个格式 Adapter 的
异常映射、密码传递和资源清理都已覆盖。修改 Adapter 时还需分别核对 `PasslyJsonFormatAdapter.decode()`、
`PasslyEncryptedFormatAdapter.decode()`、`BitwardenJsonImportAdapter.decode()` 的格式测试；修改文件事务时核对
Restore journal 测试，而非只运行纯归档 codec 测试。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Strict/compatible 条目校验与处置规划 | `BackupImportPlanner.plan(bundle, strategy)` | [`BackupImportPlanner.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlanner.kt) |
| 归档导入导出编排、资源与错误收尾 | `BackupArchiveServiceImpl.export(request)`、`import(request)` | [`BackupArchiveServiceImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt) |
| 格式 Adapter 注册与导入格式选择 | `BackupFormatRegistry.exporter(formatId)`、`selectImporter(requestedFormat, source)` | [`BackupFormatRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatRegistry.kt) |
| URI 文件访问与操作层补偿 | `BackupFileStore.openInput(uri)`、`openOutput(uri)`、`DefaultBackupOperationGateway.performExport(...)` | [`BackupFileStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/BackupFileStore.kt)、[`DefaultBackupOperationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt) |
| Room 快照、恢复事务和资源合同 | `RoomVaultBackupStore.read(options)`、`restore(snapshot, mode)` | [`RoomVaultBackupStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt) |
