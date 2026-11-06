# Backup 验证要求

状态：当前实现合同（代码核对：2026-11-06）。

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

Wire model 不导入 Room DTO；Adapter 不访问数据库或 Android URI；Restorer 不解析 JSON。Room Schema、文档
version 和 container version 的测试必须分开。

## 现有测试入口与覆盖范围

按调用边界选择测试，避免只因测试名含“Backup”就把它当成端到端证据：

| 行为 | 当前测试入口 | 主要证明 |
| --- | --- | --- |
| 归档字段映射 | [`KeyedEntryArchiveRoundTripTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveRoundTripTest.kt) | `KeyedEntryArchiveMapper.toArchiveRecord()` / `toDomainEntry()` 的银行卡、OTP 和稳定键往返；对应断言包括 `serializedBankCardUsesStableKeyedFieldsAndRoundTrips`、`otpRoundTripPreservesEveryTypeAlgorithmAndEncoding`。 |
| Canonical 文档和容器 | [`BackupCodecTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt) | JSON、ZIP、Argon2id/AES-GCM 容器的往返、认证头、篡改、错误密码、危险 KDF 参数和输入上限拒绝。 |
| 严格/兼容规划 | [`BackupImportPlannerTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlannerTest.kt) | 未知字段在兼容策略中忽略、未知 Entry type 在兼容模式跳过而严格模式拒绝、重复顶层 Link ID 两种策略都拒绝。 |
| wire 值完整性 | [`BackupArchiveKeyRegistryTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveKeyRegistryTest.kt)、[`KeyedBackupWireModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/KeyedBackupWireModelTest.kt) | Registry 覆盖领域类型与稳定键、模型不依赖数据库 payload 类型。 |
| 上层操作与部分文件清理 | [`DefaultBackupOperationGatewayTest`](../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGatewayTest.kt)、`BackupCreatedDocumentCleanupTest`、`BackupWriteTestCleanupTest` | 目标 URI 的授权范围、取消传播、部分导出文件删除和清理错误优先级；这不直接证明 archive service 内部的 Adapter 参数传递。 |
| 恢复文件 journal | [`RestoreFileJournalTest`](../../../app/src/test/java/com/aozijx/passly/app/database/backup/RestoreFileJournalTest.kt)、`RoomBackupFaviconResourceTest` | Room 恢复时附件/图标文件变更的 journal 补偿和资源引用保护。 |

`BackupArchiveServiceImpl.export(request)` 和 `import(request)` 当前没有对应的专用测试类。现有 Codec、Planner、Gateway
测试分别覆盖相邻组件，但未直接断言 service 如何把 `BackupExportRequest.options` 变成
`BackupSnapshotReadOptions`、将 `targetUri/sourceUri` 传入 FileStore、把 `mode/strategy/password` 传给恢复或 Adapter，
以及在成功、普通失败与取消后清零 `payload`、编码结果和 bundle 资源。此验证缺口排入[备份扫描待办 Task 3](../../superpowers/plans/2026-11-06-passly-backup-review-followups.md)。

严格策略和兼容策略共享同一 canonical 校验合同，但不能仅凭 `BackupImportPlannerTest` 推断每个格式 Adapter 的
异常映射、密码传递和资源清理都已覆盖。修改 Adapter 时还需分别核对 `PasslyJsonFormatAdapter.decode()`、
`PasslyEncryptedFormatAdapter.decode()`、`BitwardenJsonImportAdapter.decode()` 的格式测试；修改文件事务时核对
Restore journal 测试，而非只运行纯归档 codec 测试。
