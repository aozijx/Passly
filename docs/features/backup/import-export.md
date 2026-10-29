# Backup 导入与导出

状态：当前实现（代码核对：2026-10-29）。

Backup 归档 Vault 业务数据，不是 Room 数据库镜像。UI 与流程位于 feature/backup/presentation，协议、
加密和归档文件 I/O 位于 `feature/backup/internal`；稳定键映射位于 `archive/mapper`，Room 快照及恢复文件事务位于
`app/database/backup`。能力侧仅声明 `BackupSnapshotGateway` 合同；App 适配器的 Reader/Restorer 读取
Data 实现，页面和普通 Backup 流程不导入 Data。

| 格式 | 方向 | 资源 | 保密性 |
| --- | --- | --- | --- |
| `passly.encrypted` | 导入、导出 | 附件与可选图标 | Argon2id + AES-256-GCM |
| `passly.json` | 导入、导出 | Base64 内嵌 | 明文 |
| `passly.text` | 仅导出 | 不支持 | 明文、有损 |
| `bitwarden.json` | 仅导入 | 不支持附件 ZIP | 仅接收明文 JSON |

完整备份可包含所选条目及业务字段、关系、已提交附件和可选图标。Draft、活动记录、修订历史、可重建索引、
设置、会话、DEK、Bootstrap、认证材料、Room 密文结构、本机路径和未提交附件不进入备份。
协议细节见[Passly Archive v1](archive-format.md)，验证边界见[验证要求](verification.md)与
[ADR-0022](../../decisions/ADR-0022-keyed-backup-document.md)。文档、加密容器和 Room Schema 各自独立版本；
当前协议不兼容旧 `passly-vault`/`passly-archive` 文档。

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
授权回收失败可能暂时留下多余的旧系统授权，但不会破坏当前设置与系统授权的一致性。

另存到指定位置使用单文件 `CreateDocument` 合同，与默认目录 tree grant 分开。两者都由系统选择器授予目标访问，
不需要完整存储权限。UI 将平台 URI 转成字符串交给流程编排；SAF 存储实现负责解析 URI 并执行平台操作。

## 导出

1. 已解锁并通过备份导出认证后读取 Room 快照。
2. `KeyedEntryArchiveMapper` 生成稳定键记录。
3. Validator 在写文件前检查必填字段、值形状、引用和资源完整性。
4. 格式 Adapter 只负责 canonical bundle 与外部格式互转。
5. `BackupFileStore` 写入目标，并在完成或失败后清零可控密码与编码缓冲。导入/导出取消保留取消语义；
   导出已创建目标时，在不可取消清理区尝试删除部分文件，清理失败附在原取消异常后再传播。

完整导出使用 archive registry，而不是 UI 当前可创建/筛选类型。数据库出现未注册类型时必须失败，不能静默漏项。
TXT 是人类可读投影，不可恢复，且不输出 favorite 字段。

## 工作流状态

界面使用封闭状态：`Idle`、`FormatPicker`、`ConfigureExport`、`AwaitingExportTarget`、`ConfigureImport`、
`Executing`、`Failed`。导出配置、导入 URI/策略和密码只存在于各自分支；关闭、取消、完成和 ViewModel 清理时
清零受控密码缓冲。不得用多个 nullable/Boolean 拼出相互冲突的流程。

## 导入规划与事务

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
可选参数的默认值见[归档格式](archive-format.md)，验证通过的 OTP 不会再因恢复时使用另一套默认值而失败。

两种策略都严格拒绝错误身份/版本、认证失败、危险 KDF 参数、重复顶层 ID、损坏资源、路径穿越、未知 ZIP
条目和超限输入。`APPEND` 跳过已有 ID；`OVERWRITE` 在单个 Room 事务中清库并写入。如果源文件有 Entry，
但兼容过滤后没有可恢复 Entry，必须在清库前失败。

资源恢复使用文件 journal 补偿 Room 与文件系统之间的失败。两种介质不能提供真正跨介质 ACID，进程在极短
替换窗口被系统强杀仍是已知限制。

通知与日志只记录数量和稳定原因，不记录标题、用户名、URL、字段值、OTP 或文件名。

平台依据：[Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)。
