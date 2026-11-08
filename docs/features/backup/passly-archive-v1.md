# Passly Archive v1

状态：当前实现（代码核对：2026-11-08）。

## 文档身份

原生 JSON 与加密容器内 `document.json` 使用同一 canonical 文档：

```json
{
  "format": "passly-field-archive",
  "version": 1,
  "exportedAt": 0,
  "entries": [],
  "links": [],
  "resources": []
}
```

`passly-field-archive + version 1` 是完整身份。旧 `passly-vault`、`passly-archive` 和 typed payload 没有兼容
decoder；新身份从版本 1 开始，而不是沿用旧格式版本。

## Keyed Entry

Entry 由稳定 type key 和字段数组表达。数组允许检测重复 key，导入不依赖字段顺序。revision 至少为 1；
attachment IDs 必须和该 Entry 的 attachment resource 完全一致。

v1 字段值是封闭形状：`text`、`text_list`、`boolean`、`integer`、`long`、`custom_fields`，且只能携带对应
payload。OTP 类型、算法和编码使用明确的小写 wire 值。

OTP 字段由 `KeyedEntryArchiveMapper.readOtpConfig` 统一读取；完整校验与恢复使用同一读取器，
参数规则由 Domain `OtpConfigValidation` 提供。未知类型、算法、编码及不可解码密钥均无效。
TOTP/HOTP 支持 6–8 位，TOTP 周期为正 Int，不另设 300 秒上限；HOTP 必须提供非负 Long counter。
Steam 固定 SHA1、5 位和 30 秒，不接受冲突配置。

缺省 type 为 TOTP，algorithm 为 SHA1，encoding 为 Base32；缺省 digits 在 Steam 为 5，其他类型为 6。
缺省定时周期为 30 秒，HOTP 不推测缺失的 counter。缺省与非法字段分开处理，非法显式值不得回退。
恢复会校验密钥能被所选编码解码，但保留其合法字符串表示（包括小写与补位），也不修剪 issuer 和账户名。
原生归档不是手动输入，不对损坏字段做规范化修补。

`BackupArchiveKeyRegistry` 是 Entry type、Field key、Relation type、Resource kind 与 Domain 类型间的唯一
映射。外部格式禁止使用 Kotlin `enum.name`、ordinal、`Enum.valueOf()` 或 `enumValueOf()`。字段敏感度只信任
本地 `EntryTypeDefinition`，备份文件不能降低访问等级。

## Link 和 Resource

Link 使用 `member_of_account`、`otp_for`、`recovery_for`、`related_to` 等稳定键。导入校验端点、方向、类型、
重复关系和时间戳。

Resource 元数据包含 ID、entry ID、kind、size、SHA-256 及可选文件名/MIME/创建时间。每个 Entry 最多一个 icon。
JSON 使用 Base64；加密 ZIP 使用 `resources/<resource-id>`。路径穿越、重复/未知条目、未声明资源、截断、尾随
数据、大小或摘要不匹配都拒绝。

## 加密容器

- Header 使用 ASCII magic `PSLYBKP1`、container version `1`、80-byte canonical header 和 big-endian 整数。头字段依次为 KDF/cipher id、Argon2 参数、salt/prefix 长度、chunk size 与 tag 长度，之后是 16-byte salt 和 4-byte nonce prefix。
- Argon2id 参数由 `Argon2idParameters` 保存并原样编码；Argon2 v1.3 的参数版本为 `0x13`，当前导出默认 iterations 3、memory 65536 KiB、parallelism 4、key 256 bit。容器版本与 KDF 子版本是不同字段，不互相代替。
- 导入在派生密钥前拒绝不安全参数：iterations `1..10`、memory `8192..262144 KiB`、parallelism `1..8`、key 256 bit。该范围限制不可信 header 请求的资源上限，但最低支持设备上的耗时、内存与并发预算尚待实测，见[安全跟进 Task 3](../../superpowers/plans/2026-11-06-passly-security-scan-followups.md#task-3)。认证系统的设备 KDF 设置不参与文件格式。
- 每条记录头为 `type:u8 | sequence:u64be | plaintextLength:u32be`。Data type 为 1，明文最多 1 MiB；terminal type 为 2，明文固定 16 bytes，包含 data record 数与总明文字节数。每条记录使用独立 AES-256-GCM 操作和 16-byte tag。
- Nonce 是 4-byte 每文件随机 prefix 加 8-byte big-endian 连续序号；完整原始 Header 与 record header 拼接为 AAD。Data sequence 从 0 开始；terminal 使用下一个序号。即使 ZIP reader 已读完中央目录，也必须继续排空容器，验证 terminal 统计、terminal tag 和物理 EOF，之后才能规划或恢复。
- 每个 chunk 的 tag 通过后才向 ZIP reader 释放明文；错误密码、损坏记录、缺失 terminal、错误统计和尾随字节均失败。导出和导入均为流接口，外部文件上限为 256 MiB。
- Header/record layout 的职责分别属于 `BackupContainerHeader` 与 `BackupRecordCrypto`；输入/输出分别由 `ChunkedBackupInputStream` / `ChunkedBackupOutputStream` 负责；ZIP entry 由 `BackupZipCodec` 处理。旧单块 v1 payload 不再导入，magic 与数字版本仍保持 v1。

## 输入上限

- Entry 与 Resource 分别最多 100000；
- 单资源 16 MiB，解压资源总计 128 MiB；
- `document.json` 16 MiB，外部输入 256 MiB。

Bitwarden 导入当前支持 Login、Secure Note、Card、Identity 和 OTP；拒绝加密/账号受限 JSON、SSH、FIDO2/
Passkey、附件 ZIP、附件描述和密码历史，避免静默丢失无法表达的数据。

## 代码位置

导入调用顺序与请求参数传递见[备份导入规划与事务](import.md#导入服务调用链)。Service 将有界输入流交给
Registry；加密 magic 直接选择加密 Adapter，JSON 由 `JsonFormatDispatcher` 按根级字段单遍选择 Passly 或 Bitwarden。
三种格式最后都产出 canonical `BackupBundle`，再经 `BackupImportPlanner.plan(bundle, strategy)`。

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 注册归档稳定类型键 | `BackupArchiveKeyRegistry` | [`BackupArchiveKeyRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveKeyRegistry.kt) |
| 在稳定键与领域 Entry 之间映射 | `KeyedEntryArchiveMapper` | [`KeyedEntryArchiveMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveMapper.kt) |
| ZIP document/resource entry 校验 | `BackupZipCodec`、`KeyedBackupBundleValidator.validate()` | [`BackupZipCodec.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupZipCodec.kt)、[`KeyedBackupBundleValidator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/KeyedBackupBundleValidator.kt) |
| 固定协议字段与安全范围 | `BackupContainerV1Spec` | [`BackupContainerV1Spec.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupContainerV1Spec.kt) |
| Header 与 record 编码/认证 | `BackupContainerHeader`、`BackupRecordCrypto` | [`BackupContainerHeader.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupContainerHeader.kt)、[`BackupRecordCrypto.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupRecordCrypto.kt) |
| 记录加密流与解密流 | `ChunkedBackupOutputStream`、`ChunkedBackupInputStream` | [`ChunkedBackupOutputStream.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/ChunkedBackupOutputStream.kt)、[`ChunkedBackupInputStream.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/ChunkedBackupInputStream.kt) |
