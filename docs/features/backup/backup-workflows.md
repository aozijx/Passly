# Backup 导入与导出流程

状态：当前实现（代码核对：2026-11-06）。

Backup 归档 Vault 业务数据，不是 Room 数据库镜像。UI 与流程位于 `feature/backup/presentation`；Presentation 和执行器
共享的格式选择、导入选项、操作请求/结果及 `BackupOperationGateway` 位于 `feature/backup/contract`。稳定 wire format ID
只在内部映射，协议、加密和归档文件 I/O 位于 `feature/backup/internal`；稳定键映射位于 `archive/mapper`，Room 快照及
恢复文件事务位于 `app/database/backup`。能力侧仅声明 `BackupSnapshotGateway` 合同；App 适配器的 Reader/Restorer 读取
Data 实现，页面和普通 Backup 流程不导入 Data。

| 格式 | 方向 | 资源 | 保密性 |
| --- | --- | --- | --- |
| `passly.encrypted` | 导入、导出 | 附件与可选图标 | Argon2id + AES-256-GCM |
| `passly.json` | 导入、导出 | Base64 内嵌 | 明文 |
| `passly.text` | 仅导出 | 不支持 | 明文、有损 |
| `bitwarden.json` | 仅导入 | 不支持附件 ZIP | 仅接收明文 JSON |

完整备份可包含所选条目及业务字段、关系、已提交附件和可选图标。Draft、活动记录、修订历史、可重建索引、
设置、会话、DEK、Bootstrap、认证材料、Room 密文结构、本机路径和未提交附件不进入备份。
协议细节见[Passly Archive v1](passly-archive-v1.md)，验证边界见[验证要求](verification.md)与
[ADR-0022](../../decisions/ADR-0022-keyed-backup-document.md)。文档、加密容器和 Room Schema 各自独立版本；
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

导出选项默认包含 Domain 的全部 `EntryType`。归档稳定键注册表测试逐项验证每种类型均可往返映射，并断言注册表覆盖全集；新增条目类型若未加入归档映射会使测试失败，Presentation 无需依赖 wire-key 注册表决定页面选项。

## 导出

### 导出服务调用链

`BackupArchiveServiceImpl.export(request)` 在 `BackupIoDispatcher` 上用 `AppResult.runSuspendCatching` 编排：先以
`request.format` 调用 `BackupFormatRegistry.exporter(formatId)` 选 Adapter，再以 Adapter 的
`requiresPassword` 和 `request.password` 调用 `validatePassword(required, password)`。随后构造
`BackupSnapshotReadOptions`：`includeIcons` 同时受用户选项与 `adapter.supportsIcons` 限制，
`includeAttachments` 同时受用户选项与 `adapter.includesAttachments` 限制；`includeDeleted` 与
`includedEntryTypes` 原样来自 `request.options`。该对象传给 `BackupSnapshotGateway.read(options)`。

拿到 `BackupBundle` 后调用 `adapter.encode(bundle, request.password)`；返回的编码 `ByteArray` 传给
`BackupFileStore.writeBytes(request.targetUri, encoded)`，写入结束或失败都在 `finally` 中清零 `encoded`。
外层 `finally` 调用 `bundle.clearResourceData()` 清零每个资源数据数组。失败结果随后由
`report(error, "backup_export")` 送入 `AppErrorReporter.report(error, ErrorReportContext(operation, category))`；
错误上下文的 category 固定为 `BACKUP`。

`BackupFormatRegistry.exporter(request.format)` 返回对应 Adapter；JSON Adapter 的
`encode(bundle, password)` 调 `JsonBackupExporter.export(bundle)` 后编码 UTF-8。加密 Adapter 将同一
`bundle` 与非空密码传给 `EncryptedBackupExporter.export(bundle, password)`。两种导出均由 Service 负责最终写入，
Adapter 不持有目标 URI。

1. 已解锁并通过备份导出认证后读取 Room 快照。
2. `KeyedEntryArchiveMapper` 生成稳定键记录。
3. Validator 在写文件前检查必填字段、值形状、引用和资源完整性。
4. 格式 Adapter 只负责 canonical bundle 与外部格式互转。
5. `BackupFileStore` 写入目标，并在完成或失败后清零可控密码与编码缓冲。导入/导出取消保留取消语义；
   目标创建成功但 URI 尚未交回流程前若普通失败、取消或 fatal `Error`，都在不可取消区尝试删除该文件。
   导出目标的部分文件也在不可取消清理区删除。若 archive service 抛出异常，Gateway 将删除失败返回值与主异常按
   fatal `Error`、取消、普通异常的顺序合并，被替代的主异常保留为 suppressed；但若 archive service 已返回
   `AppResult.Failure`，Gateway 当前仅重新抛出清理阶段的 fatal `Error`/取消，普通清理失败（包括 delete 返回 `false`）
   会被丢弃，并继续返回原操作失败，因此不保证部分文件已删除，也没有单独提示残留文件。此返回失败分支待补齐合同，见
   [备份扫描待办 Task 5](../../superpowers/plans/2026-11-06-passly-backup-review-followups.md)。SAF 创建和写入探测只把普通异常映射为 `Result.failure`；取消与 fatal `Error`
   原样传播。删除接口把普通 provider 异常映射为 `false`，但继续传播取消和 fatal `Error`。

完整导出使用 archive registry，而不是 UI 当前可创建/筛选类型。数据库出现未注册类型时必须失败，不能静默漏项。
TXT 是人类可读投影，不可恢复，且不输出 favorite 字段。

## 工作流状态

界面使用封闭状态：`Idle`、`FormatPicker`、`ConfigureExport`、`AwaitingExportTarget`、`ConfigureImport`、
`Executing`、`Failed`。ViewModel workflow 中的密码以 `SensitiveValue` 持有；关闭或替换 workflow、拒绝状态更新、
操作结束及 ViewModel 清理时会擦除其可控字符缓冲。密码不通过 SavedState 或 Navigation 参数持久化。不过当前 Compose 输入/投影仍会产生不可变 `String` 副本，清零 `SensitiveValue` 不等于擦除所有副本，具体边界和后续评估见下文。

密码输入调用链是 `OutlinedTextField(value = state.password)` → `BackupUiAction.UpdatePassword(value: String)` →
`BackupViewModel.onAction()` → `OwnedChars.fromString(action.password)`，后者将 UI 字符串复制到可控 `CharArray`，再由
`replacePassword()` 写入 workflow；`BackupSheetUiMapper.toSheetUiState()` 又调用 `SensitiveValue.copyToString()`，该函数将字符数组转换成新 String 并在 `finally` 擦除临时数组。故表单 UI 的值和 Action 暂时以 String 表示。操作时 `BackupViewModel.execute(operation, password)` 在 coroutine `finally` 擦除 workflow 的 `SensitiveValue`；`DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)` / `performImport(request)` 再复制成传给 `BackupExportRequest` / `BackupImportRequest` 的 `CharArray`，并在各自 `finally` 擦除副本。`BackupArchiveServiceImpl.validatePassword(required, password)` 仅在 `required == true` 时拒绝 null/empty，不执行强度校验；算法和文件格式 Adapter 接收同一密码数组。当前确认的是进程内 String 驻留边界，没有证据表明密码进入持久状态、日志或外部目标；评估项见[备份扫描待办 Task 4](../../superpowers/plans/2026-11-06-passly-backup-review-followups.md)。

提交导入或导出后，操作由 `BackupViewModel` 的任务持有，不随 Backup 页面离开而取消。页面离开会关闭尚未提交的配置并使旧页面通知失效；执行中的状态保留，重进时不接受第二个并行 Backup 操作，完成结果更新该操作状态。Activity/ViewModel 销毁会取消任务：操作 `finally` 擦除 UI 持有的密码，归档流程传播取消并补偿已创建的部分导出文件；导入事务按 Room/文件 journal 的回滚合同结束。SAF 文档选择回执绑定发起它的页面 owner 和一次性请求 ID，旧回执不能在重进后启动新的导入或导出。页面仍有效但宿主暂停时，选择器命令会等到 `STARTED` 后启动；页面离开会丢弃旧回执。导出选择命令入队失败时恢复 `ConfigureExport`，可重新提交；普通提示队列满时允许丢弃。

## 导入规划与事务

### 导入服务调用链

`BackupArchiveServiceImpl.import(request)` 在 `BackupIoDispatcher` 上先调用
`BackupFileStore.readBytesSafely(request.sourceUri)` 得到完整输入 `payload`，然后调用
`BackupFormatRegistry.importer(request.format, payload)`。若请求指定格式，Registry 仍要求该 Adapter 的
`probe(payload) > 0`；未指定时计算所有 Adapter 的 probe 分数，只接受唯一最高分候选。选择后以
`adapter.requiresPassword` 和 `request.password` 调用 `validatePassword(required, password)`，再把原始
`payload`、密码和 `request.strategy` 原样传入 `adapter.decode(payload, password, strategy)`。

Passly JSON Adapter 调 `JsonBackupImporter.import(bytes, strategy)`：严格 UTF-8 解码为 String，反序列化
`JsonBackupPackage`，逐项 Base64 解码资源，组成 `BackupBundle`，再调用
`BackupImportPlanner.plan(bundle, strategy).bundle`。加密 Adapter 调
`EncryptedBackupImporter.import(container, password, strategy)`：先解密为 ZIP 字节，调用
`BackupArchiveCodec.readZip(bytes)` 提取 `document.json` 和资源，反序列化 `BackupDocument` 并组成 bundle，
最后同样调用 `BackupImportPlanner.plan(bundle, strategy).bundle`。Bitwarden Adapter 则先严格解码 UTF-8，
解析 JSON tree、转换为 canonical `BackupDocument` 和记录，再由 `BackupBundleValidator.validate(bundle,
requireResourceData = false)` 检查。

解码成功后 Service 将 `bundle` 与 `request.mode` 传给 `BackupSnapshotGateway.restore(bundle, mode)`。
返回的 restore 计数与 `bundle.compatibilitySummary` 计数合并为 `BackupImportResult`。无论 restore 成功或失败，
`bundle.clearResourceData()` 清零资源字节；最外层 `finally` 清零输入 `payload`。失败结果经
`report(error, "backup_import")` 上报。JSON 与加密解码器将未识别的 `Exception` 映射为 `BackupFailed`；Service
仍通过 suspend 文件读取、快照恢复和 `AppResult.runSuspendCatching` 处理操作级失败与取消。同步 Adapter 解码函数本身
没有 suspend cancellation checkpoint，不能把它描述成可中途响应协程取消。

`BackupImportPlanner.plan(bundle, strategy)` 对 `STRICT` 调完整 `BackupBundleValidator.validate`；兼容策略先做结构头
和资源完整性校验，再筛选未知类型/字段、逐 Entry 校验并裁剪无效项及其引用，最后形成兼容摘要。逐条校验当前使用
`runCatching`，会捕获 `Throwable`，fatal `Error` 可能被误判为无效 Entry；待修复项见[备份扫描待办 Task 1](../../superpowers/plans/2026-11-06-passly-backup-review-followups.md)。加密容器与
Bitwarden/JSON 的具体校验字段见[归档格式](passly-archive-v1.md)。导入目前先将原始文件读入内存；JSON 文本、解密 ZIP、
解压资源等副本的预算与峰值内存仍由[备份导入预算 TODO](../../superpowers/plans/2026-11-06-passly-backup-review-followups.md)跟踪，
单文件读取上限不等同于全链路累计内存上限。

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

资源恢复使用文件 journal 补偿 Room 与文件系统之间的失败。替换时先登记目标、临时文件与旧文件路径；写入
或替换失败会立即补偿，未成功的补偿会保留在 journal 中供本次恢复流程重试。Room 事务失败时仍以原事务异常
为主，普通文件回滚异常作为 suppressed 信息附加；回滚中的 fatal `Error` 优先传播并保留事务异常。Room 提交后才清理旧文件；提交后的清理失败会记录并保留残留文件，
不会再回滚已提交的数据库。后续成功的 `OVERWRITE` 会清理未被恢复条目引用的残留资源文件。进程重启后不能仅凭
`.previous` 文件名安全判断事务是否提交，因此不会无条件删除这类文件；要保证跨进程恢复需要持久化事务记录。
两种介质不能提供真正跨介质 ACID，进程在极短替换窗口被系统强杀仍是已知限制。

恢复附件时，即使目标路径已存在，也会先解密并验证 keyed content ID，再与本次已校验的归档明文逐字节比较。
密文损坏或内容不匹配时，恢复器用归档明文重新加密并通过同一 journal 替换；Room 回滚时可恢复被替换前的文件。

通知与日志只记录数量和稳定原因，不记录标题、用户名、URL、字段值、OTP 或文件名。

平台依据：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 编排备份格式、快照、授权与文件读写 | `BackupArchiveServiceImpl` | [`BackupArchiveServiceImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt) |
| 管理密码输入投影、受控缓冲与一次性请求 | `BackupUiAction.UpdatePassword(value)`、`OwnedChars.fromString(value)`、`BackupUiState.toSheetUiState(configuredDirectoryLabel)`、`BackupViewModel.execute(operation, password)` | [`BackupViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt)、[`BackupSheetUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupSheetUiMapper.kt)、[`BackupRestoreSheetUiState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/presentation/ui/model/BackupRestoreSheetUiState.kt)、[`OwnedChars.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/sensitive/OwnedChars.kt) |
| 将一次性密码副本交给 Service、按 `deleteOnFailure` 补偿目标并在返回/失败/取消后擦除 | `DefaultBackupOperationGateway.performExport(request, targetUri, deleteOnFailure)`、`tryDeleteFailedExport(targetUri)`、`performImport(request)` | [`DefaultBackupOperationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt) |
| 生成稳定键归档条目并规划导入 | `KeyedEntryArchiveMapper`、`BackupImportPlanner` | [`KeyedEntryArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveMapper.kt)、[`BackupImportPlanner.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlanner.kt) |
| 读取与恢复 Room 快照 | `RoomBackupSnapshotReader`、`RoomBackupSnapshotRestorer` | [`RoomBackupSnapshotReader.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotReader.kt)、[`RoomBackupSnapshotRestorer.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotRestorer.kt) |
| 将备份字节写入目标并清理缓冲 | `BackupFileStore` | [`BackupFileStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/BackupFileStore.kt) |
