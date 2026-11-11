# Backup 工作流总览

状态：当前实现（代码核对：2026-11-08）。

Backup 归档 Vault 业务数据，不是 Room 数据库镜像。UI 与流程位于 `feature/backup/presentation`；Presentation 和执行器共享的格式选择、导入选项、操作请求/结果及 `BackupOperationGateway` 位于 `feature/backup/contract`。稳定 wire format ID 只在内部映射，协议、加密和归档文件 I/O 位于 `feature/backup/internal`；稳定键映射位于 `archive/mapper`。Domain 声明持久化中立的 `VaultBackupStore`；App `AppBackupSnapshotGateway` 把 archive DTO 与 Domain snapshot 相互映射，并在覆盖事务提交后通过 OTP runtime invalidation contract 清理活动租约的派生状态；Data `RoomVaultBackupStore` 独占 Room、秘密字段、附件 GC 和图标/附件文件事务。旧 App `RoomBackupSnapshotReader` / `RoomBackupSnapshotRestorer` 与 App 文件 journal/resource helper 已删除；页面和 archive 格式适配器不直接导入 Data 实现。

| 格式 | 方向 | 资源 | 保密性 |
| --- | --- | --- | --- |
| `passly.encrypted` | 导入、导出 | 附件与可选图标 | Argon2id + AES-256-GCM |
| `passly.json` | 导入、导出 | Base64 内嵌 | 明文 |
| `passly.text` | 仅导出 | 不支持 | 明文、有损 |
| `bitwarden.json` | 仅导入 | 不支持附件 ZIP | 仅接收明文 JSON |

完整备份不包含 Draft、活动记录、修订历史、可重建索引、设置、会话、DEK、Bootstrap、认证材料、Room 密文结构、本机路径或未提交附件。文档、加密容器、Room Schema 各自独立版本；当前协议不兼容旧 `passly-vault` / `passly-archive` 文档。

按流程阅读：

- [快照、SAF 目录授权与目标 URI](storage-and-permissions.md)
- [导出流程](export.md)
- [工作流状态、密码和页面生命周期](operation-lifecycle.md)
- [导入规划、事务与文件恢复](import.md)
- [Passly Archive v1 协议](passly-archive-v1.md)
- [验证要求与测试映射](verification.md)

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 页面操作状态与命令 | `BackupViewModel.onAction(action)` | [`BackupViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt) |
| 导入/导出流程编排 | `BackupArchiveServiceImpl.export(request)`、`import(request)` | [`BackupArchiveServiceImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt) |
| 格式选择与自动导入识别 | `BackupFormatRegistry.exporter(formatId)`、`selectImporter(requestedFormat, source)` | [`BackupFormatRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatRegistry.kt) |
| Archive 与 Domain 快照转换 | `AppBackupSnapshotGateway.read(options)`、`restore(bundle, mode)` | [`AppBackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt) |
| Room 快照及资源持久化 | `RoomVaultBackupStore.read(options)`、`restore(snapshot, mode, onCommitted)` | [`RoomVaultBackupStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt) |
| 持久化中立的 Domain 合同 | `VaultBackupStore.read(options)`、`restore(snapshot, mode, onCommitted)` | [`VaultBackupStore.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/backup/port/VaultBackupStore.kt) |
