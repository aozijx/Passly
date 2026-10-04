# Backup 功能与 Passly 备份协议

状态：正式格式 v1
最后修订：2026-10-05

Backup 是 app 内的业务 feature。UI 与流程编排位于 presentation/feature/backup，格式、协议、加密和文件 I/O 位于 feature/backup/internal，Room 快照适配位于 app/database/backup。备份是可恢复的 Vault 业务数据归档，不是 Room 数据库镜像。

## 1. 范围与版本边界

可恢复备份包含所选条目及其完整业务字段、条目关系、已提交附件和可选自定义图标。默认包含回收站条目和 archive registry 中所有数据库仍可读取的条目类型。

Draft、活动记录、修订历史、可重建索引、设置、会话、DEK、Bootstrap 密钥、认证材料、Room 密文结构、本机绝对路径和未提交附件不进入备份。

Room Schema 与备份协议独立。本次格式替换不升级数据库，Room Schema 仍为 1；已有数据库内容可直接重新导出为新格式。

## 2. 外部格式

| formatId | 方向 | 资源 | 保密性 |
|---|---|---|---|
| passly.encrypted | 导入、导出 | 附件与可选图标 | Argon2id + AES-256-GCM |
| passly.json | 导入、导出 | Base64 内嵌 | 无 |
| passly.text | 仅导出 | 无 | 无 |
| bitwarden.json | 仅导入 | 不支持附件 ZIP | 仅接收明文 JSON |

TXT 是有损的人类可读报告，不可恢复。JSON 和 TXT 都可能包含明文敏感信息，UI 必须显示风险。

原生 JSON 与加密容器内的 document.json 共用同一 canonical 文档：

~~~json
{
  "format": "passly-field-archive",
  "version": 1,
  "exportedAt": 0,
  "entries": [],
  "links": [],
  "resources": []
}
~~~

**passly-field-archive + version 1** 是完整协议身份。开发期旧标识 passly-vault、passly-archive 以及旧 typed payload 没有兼容 Decoder。格式标识已更换，因此新文档仍从版本 1 开始。

## 3. Keyed Entry

Entry 不再按 login、card、identity 等 Kotlin 数据类分叉，而是由稳定类型键与字段数组表达：

~~~json
{
  "id": "entry-id",
  "type": "login",
  "revision": 1,
  "createdAt": 0,
  "updatedAt": 0,
  "fields": [
    { "key": "title", "value": { "kind": "text", "text": "GitHub" } },
    { "key": "password", "value": { "kind": "text", "text": "secret" } },
    { "key": "tags", "value": { "kind": "text_list", "texts": ["work"] } }
  ]
}
~~~

字段使用数组以检测重复 key，导入不依赖字段顺序。revision 必须大于等于 1，时间戳必须单调有效，attachmentIds 必须与该 Entry 的 attachment resources 完全一致。

v1 字段值是封闭形状：

| kind | 唯一 payload |
|---|---|
| text | text |
| text_list | texts |
| boolean | boolean |
| integer | integer |
| long | long |
| custom_fields | customFields |

每个值只能携带与 kind 对应的一个 payload。自定义字段 kind 是 text 或 hidden。OTP 类型、算法和编码使用 totp、hotp、steam、sha1、sha256、sha512、base32、base64 等稳定小写值。

## 4. 稳定键与注册表

BackupArchiveKeyRegistry 是 wire key 与 Domain enum 之间的唯一映射边界：

- Entry type、Field key、Relation type 使用显式小写稳定键；
- Resource kind 使用显式序列名 icon 与 attachment；
- 外部字符串不得交给 Enum.valueOf 或 enumValueOf，不得序列化 enum.name 或 ordinal；
- 导出侧缺少映射是开发错误；导入侧未知键交给导入策略处理；
- archiveEntryTypes 必须覆盖全部数据库可读取的 EntryType，完整导出不得因 UI 隐藏类型而漏数据。

数据库字段的敏感度只信任本地 EntryTypeDefinition。备份文件不能声明、覆盖或降低字段访问等级。

## 5. Link 与 Resource

Link 使用 member_of_account、otp_for、recovery_for、related_to 等稳定关系键。导入会校验两端 Entry、关系方向、类型约束、重复关系和时间戳。

Resource 元数据包含 id、entryId、kind、size、sha256 及可选文件名、MIME、创建时间。每个 Entry 最多一个 icon。资源元数据、内容和附件清单必须相互完整，大小与 SHA-256 必须匹配。

JSON 使用 resourcesBase64 保存资源；加密格式的 ZIP 使用 resources/<resource-id>。路径穿越、未知 ZIP 条目、重复条目、未声明资源、截断和尾随数据一律拒绝。

## 6. 导出流程

1. 已解锁且通过备份导出认证后，Room Snapshot Reader 读取所选条目、完整字段、关系和资源。
2. KeyedEntryArchiveMapper 将 Domain Entry 映射为 keyed records。
3. Validator 在写文件前校验协议键、必填字段、值形状、引用和资源完整性。
4. Registry 选择格式 Adapter；Adapter 只负责 canonical bundle 与外部格式之间的编码。
5. BackupFileStore 写入 SAF URI，完成后清零可控的密码、编码和资源缓冲。

完整导出默认使用 archive registry。UI 的条目类型筛选只影响本次选择性导出，不修改 Vault 数据。数据库出现未注册类型时，映射必须失败，不能生成看似成功但缺条目的备份。

## 7. 导入策略与结果

导入 Sheet 提供两种策略：

- **COMPATIBLE（默认）**：未知 Entry type 跳过；已知类型的未知字段忽略；值形状错误、重复字段、缺少必填字段或语义无效的 Entry 跳过；相关 Link 和 Resource 随之裁剪。
- **STRICT**：遇到上述任一语义不兼容，写数据库前拒绝整个导入。

两种策略都严格拒绝错误格式或版本、容器认证失败、危险 KDF 参数、重复顶层 ID、资源损坏、大小或哈希不匹配、ZIP 路径问题和超限输入。兼容模式只隔离单个 Entry 的语义不兼容，不降低文件完整性要求。

规划完成后才进入恢复事务。APPEND 跳过数据库中已有 ID；OVERWRITE 在同一 Room 事务内清库并插入。若源文档原本有 Entry，但兼容过滤后没有任何可恢复 Entry，覆盖导入会在清库前终止。

成功通知显示写入、已存在、跳过、忽略字段、裁剪关系和裁剪资源数量。通知与日志只记录计数和稳定原因，不记录标题、字段值、用户名、URL、OTP 或文件名。

## 8. 加密容器 v1

- magic 为 ASCII PSLYBKP1；container version 为 1；整数为 big-endian；
- KDF 为 Argon2id v1.3，当前参数 iterations 3、memory 65536 KiB、parallelism 4；
- 加密为 AES-256-GCM，16-byte salt、12-byte nonce、128-bit tag；
- 从 magic 到 nonce 的完整头部作为 AAD；每次导出生成独立随机 salt 和 nonce；
- 导入在执行 KDF 前限制 iterations 不超过 10、memory 不超过 262144 KiB、parallelism 不超过 8；
- 错误密码与认证失败统一作为密码错误或文件损坏处理。

AES-GCM 明文是包含 document.json 与 resources/<resource-id> 的完整 ZIP，因此文档、凭据、附件和图标处于同一认证边界。

## 9. 限制与原子性

- Entry 最多 100000，Resource 最多 100000；
- 单资源最多 16 MiB，解压资源总量最多 128 MiB；
- document.json 最多 16 MiB，外部输入最多 256 MiB；
- 导入先完成探测、解密、解压、解析、规划和完整校验，再写数据库；
- Room 写入处于单一事务；资源文件使用 Restore File Journal，失败时回滚替换；
- Room 与文件系统无法提供真正的跨介质 ACID，进程在极短替换窗口被系统强杀仍是已知限制。

## 10. Bitwarden JSON

Bitwarden Adapter 直接生成 keyed records。Login、Secure Note、Card、Identity 和 OTP 映射到对应稳定字段。当前拒绝 encrypted/account-restricted JSON、SSH item、FIDO2 或 Passkey credential、附件 ZIP、附件描述和密码历史，避免静默丢失无法表达的数据。

## 11. 演进与测试规则

- document version、container version 和 Room Schema version 彼此独立；
- passly-field-archive v1 已发布键和值语义不可原地改写；破坏性变化使用新格式身份或新文档版本；
- 新增 Domain 类型或字段必须先添加稳定 archive key 和完整性测试；
- golden JSON 验证小写稳定键，不能出现 Kotlin enum 名称；
- compatible 与 strict 使用同一 fixture 测试；资源损坏在两种策略下都必须失败；
- JSON 与加密格式解码后必须产生相同 canonical records；
- wire model 不导入 Room DTO，Adapter 不访问数据库或 Android URI，Restorer 不解析 JSON。

相关决策见 [ADR-0022](../decisions/ADR-0022-keyed-backup-document.md)。ADR-0022 替代 ADR-0016 的 typed backup document 决策；加密容器 v1 的安全边界继续沿用。
