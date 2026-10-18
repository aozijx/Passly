# Passly Archive v1

状态：当前实现（代码核对：2026-10-18）。

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

- magic：ASCII `PSLYBKP1`；container version 1；整数 big-endian；
- Argon2id v1.3：iterations 3、memory 65536 KiB、parallelism 4；
- AES-256-GCM：16-byte salt、12-byte nonce、128-bit tag；
- 从 magic 到 nonce 的完整头部作为 AAD；每次导出生成新 salt 和 nonce；
- KDF 前限制 iterations ≤ 10、memory ≤ 262144 KiB、parallelism ≤ 8；
- 错误密码与认证失败统一表现为密码错误或文件损坏。

AES-GCM 明文是包含 `document.json` 与 resources 的完整 ZIP，因此文档和资源属于同一认证边界。

## 输入上限

- Entry 与 Resource 分别最多 100000；
- 单资源 16 MiB，解压资源总计 128 MiB；
- `document.json` 16 MiB，外部输入 256 MiB。

Bitwarden 导入当前支持 Login、Secure Note、Card、Identity 和 OTP；拒绝加密/账号受限 JSON、SSH、FIDO2/
Passkey、附件 ZIP、附件描述和密码历史，避免静默丢失无法表达的数据。
