# Backup 导入规划、事务与文件恢复

状态：当前实现（代码核对：2026-11-07）。

## 导入规划与事务

### 导入服务调用链

`BackupArchiveServiceImpl.import(request)` 在 `BackupIoDispatcher` 上打开受 256 MiB 限制的输入流，并把它交给
`BackupFormatRegistry`。Registry 只读取并回退 8-byte magic：匹配 `PSLYBKP1` 时选择加密 Adapter，否则由
`JsonFormatDispatcher` 在一次 JSON token 遍历中按根级 `document`/`items` 分派；嵌套同名字段不会影响选择，两个标记同时出现会拒绝。
Passly 的 `resourcesBase64` 和 Bitwarden 的 `folders` 可出现在判别字段之前，均按对应预算逐项消费。

Passly JSON 使用 Jackson Core 保留不超过 16 MiB 的 canonical document，并增量解码每项 Base64 资源；Bitwarden
每次只缓冲一个有大小上限的 JSON record，再映射为 canonical records。加密 Adapter 将分块 GCM 解密流接到
`BackupZipCodec`；每个数据块先通过 tag 验证才交给 ZIP reader。ZIP 中央目录读完后，Importer 继续排空同一解密流，
直至 terminal 统计、terminal tag 和物理 EOF 全部验证成功，然后解析文档并执行
`BackupImportPlanner.plan(bundle, strategy)`。旧单块 v1 payload 不作为兼容路径接受。

解码成功后 Service 才调用 `BackupSnapshotGateway.restore(bundle, mode)`。返回的 restore 计数与
`bundle.compatibilitySummary` 合并为 `BackupImportResult`。无论 restore 成功或失败，Service 清零资源数组并关闭
输入流。每次导入在 `backup_import` trace span 中记录开始和成功耗时；失败由 `AppErrorReporter` 以
`BACKUP` 分类及 `backup_import` operation 上报。日志不包含密码、URI、文档字段或资源内容。取消和 fatal `Error`
继续传播；同步 token 解码本身没有 suspend cancellation checkpoint。

`BackupImportPlanner.plan(bundle, strategy)` 对 `STRICT` 调完整 `BackupBundleValidator.validate`；兼容策略先做结构头
和资源完整性校验，再筛选未知类型/字段、逐 Entry 校验并裁剪无效项及其引用，最后形成兼容摘要。逐条校验只将
`IllegalArgumentException` 作为该 Entry 的结构/业务验证失败并计入跳过摘要；fatal `Error` 原样传播并终止规划，不会被误判为兼容成功。
回归覆盖预期校验异常跳过与 fatal 错误传播。加密容器与
Bitwarden/JSON 的具体校验字段和字节预算见[归档格式](passly-archive-v1.md)。文件读取、canonical document、单资源、
资源总量、ZIP 展开总量和条目数量分别受限；这些预算作用于不同层级，不应相互替代。

Bitwarden 明文 JSON 的 OTP URI 使用 Domain 的 `OtpAuthUriCodec`，保留账户标签中的字面 `+`，只解码一次；
没有 issuer 前缀的标签作为账户名。缺失或非法 HOTP counter、损坏数字、重复关键参数及不支持的生成参数
在格式适配阶段拒绝整个源文件，两种导入策略均不猜测替代配置。裸密钥和 `steam://` 分支沿用源格式默认值，
但都先经 Domain 严格 Base32 校验并规范为无补位大写形式；大小写混用的 Steam 前缀不会进入密钥。
裸密钥保留删除输入空白的规则；非法字符、补位或尾部位、空 Steam 密钥均拒绝导入。
OTP URI 语义见[动态口令 URI 导入与导出](../vault/otp-uri.md)；OTP 归档字段映射由原生导出与该适配器共享。

Bitwarden 银行卡的完整合法年月经 Domain `CardExpiry` 统一为 `MM/YYYY`；一位月份会补零，年份必须为
四位。缺失部分或非法值保留为隐藏自定义字段 `Bitwarden.expMonth` / `Bitwarden.expYear`，不合成损坏日期，
不猜测两位年份，也不覆盖已有自定义字段。此转换适用于两种导入策略，原生归档字符串仍原样保留。

写数据库前完成探测、解密、解压、解析、分类和完整校验，生成 `ImportPlan`：

- `COMPATIBLE`：隔离未知 Entry type、未知字段或语义无效 Entry，并裁剪依赖关系/资源；
- `STRICT`：遇到语义不兼容时在写入前拒绝整个导入。

原生归档的 OTP 在规划阶段经恢复使用的同一读取器与 Domain 配置规则校验；不再放过 9/10 位、未知算法/
编码、损坏密钥或冲突的 Steam 配置。兼容模式隔离这些条目，严格模式拒绝；合法的长周期 TOTP 可以备份恢复。
可选参数的默认值见[归档格式](passly-archive-v1.md)，验证通过的 OTP 不会再因恢复时使用另一套默认值而失败。

两种策略都严格拒绝错误身份/版本、认证失败、危险 KDF 参数、重复顶层 ID、损坏资源、路径穿越、未知 ZIP
条目和超限输入。`APPEND` 跳过已有 ID；`OVERWRITE` 在单个 Room 事务中清库并写入。如果源文件有 Entry，
但兼容过滤后没有可恢复 Entry，必须在清库前失败。

资源恢复使用 Data 的文件 journal 补偿 Room 与文件系统之间的失败。替换时先登记目标、临时文件与旧文件路径；写入
或替换失败会立即补偿，未成功的补偿会保留在 journal 中供本次恢复流程重试。Room 事务失败时仍以原事务异常
为主，普通文件回滚异常作为 suppressed 信息附加；回滚中的 fatal `Error` 优先传播并保留事务异常。Room 提交后才清理旧文件；提交后的清理失败会记录并保留残留文件，
不会再回滚已提交的数据库。清理失败的日志使用 best-effort 报告：普通 logger 异常不会覆盖已提交结果，取消与 fatal `Error` 仍传播。
对应行为由 `BestEffortBackupCleanupLoggingTest` 覆盖。后续成功的 `OVERWRITE` 会清理未被恢复条目引用的残留资源文件。进程重启后不能仅凭
`.previous` 文件名安全判断事务是否提交，因此不会无条件删除这类文件；要保证跨进程恢复需要持久化事务记录。
两种介质不能提供真正跨介质 ACID，进程在极短替换窗口被系统强杀仍是已知限制。

恢复附件时，即使目标路径已存在，也会先解密并验证 keyed content ID，再与本次已校验的归档明文逐字节比较。
密文损坏或内容不匹配时，恢复器用归档明文重新加密并通过同一 journal 替换；Room 回滚时可恢复被替换前的文件。

通知与日志只记录数量和稳定原因，不记录标题、用户名、URL、字段值、OTP 或文件名。

平台依据：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 打开源 URI、识别格式、委托恢复 | `BackupFileStore.openInput(uri)`、`BackupFormatRegistry.importer(format, input)`、`BackupSnapshotGateway.restore(bundle, mode)` | [`BackupArchiveServiceImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt)、[`BackupFileStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/BackupFileStore.kt)、[`BackupFormatRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatRegistry.kt) |
| 单遍识别 JSON 根格式 | `JsonFormatDispatcher.decode(input, password, strategy)` | [`JsonFormatDispatcher.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/json/JsonFormatDispatcher.kt) |
| 各格式解码为 canonical bundle | `BackupImportAdapter.decode(input, password, strategy)` | [`BackupFormatAdapter.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatAdapter.kt)、[`JsonBackupImporter.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/json/JsonBackupImporter.kt)、[`EncryptedBackupImporter.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/EncryptedBackupImporter.kt)、[`BitwardenJsonImportAdapter.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/bitwarden/BitwardenJsonImportAdapter.kt) |
| 兼容筛选或严格验证并形成 canonical bundle | `BackupImportPlanner.plan(bundle, strategy)`、`KeyedEntryArchiveMapper.toEntry(record)` | [`BackupImportPlanner.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlanner.kt)、[`KeyedEntryArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveMapper.kt) |
| Archive bundle 映射为 Domain snapshot 并执行覆盖保护 | `AppBackupSnapshotGateway.restore(bundle, mode)` 中的 records/links/resources 映射 | [`AppBackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt) |
| 在完整 Vault 事务中写 Entry、关系与资源，并于提交后清理派生状态 | `RoomVaultBackupStore.restore(snapshot, mode, onCommitted)`、`VaultRestoreFileJournal.replace(target, content)`、`rollback()`、`commit()` | [`RoomVaultBackupStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/RoomVaultBackupStore.kt)、[`VaultRestoreFileJournal.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/backup/VaultRestoreFileJournal.kt) |
