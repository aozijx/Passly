# Autofill Sealed Save Inbox

状态：实现中。bootstrap key material 的 Domain/Proto 持久化边界、RSA 密钥生成/私钥封装/校验、完整 Vault 解锁后的幂等 provision、pending record 加密协议和受限耐久文件队列已实现；保存入口接入与解锁导入尚未实现。

本设计允许 Vault 锁定时只进行密封写入，不授予数据库读取/解密能力，也不弹出 Passly 认证。当前代码仍使用
认证式保存，见 [Legacy Service](legacy-service.md)和[Credential Manager](credential-manager.md)。

## 安全属性

- 锁定进程只能取得 Inbox RSA public key；private key 由 Vault DEK 派生 key 包装。
- pending 不依赖 SQLCipher，进程死亡与同设备重启后仍在保留期内可恢复。
- plaintext 不写 preferences、Intent、SavedState、cache 或日志。
- 解锁导入防重放，数据库提交后、文件删除前崩溃不会产生重复 Entry。
- Passly/Android 备份和设备迁移不包含 Inbox。

## Bootstrap key material

新 Vault 生成 RSA-3072/OAEP-SHA256 key pair。existing Vault 在首次成功解锁后幂等 provision。
`bootstrap.proto` 保持原文件和根消息，只追加 field 9：

```proto
AutofillInboxKeyMaterial autofill_inbox_key = 9;

message AutofillInboxKeyMaterial {
  uint32 format_version = 1;
  bytes key_id = 2;
  bytes public_key = 3;
  bytes public_key_sha256 = 4;
  bytes private_key_envelope = 5;
}
```

message 缺失表示尚未 provision；存在但不完整/错误版本是完整性失败，不能当成缺失自动覆盖。public key 必须是
RSA-3072 X.509；key ID 16 bytes，fingerprint 32 bytes。解锁后从 private key 派生 public key，校验 modulus、
exponent 和 fingerprint。

private key envelope v1：

```text
magic[14]             "PASSLYINBOXKEY"
format_version        u8 = 1
key_id[16]
public_key_sha256[32]
nonce[12]
ciphertext_length     u32 big endian
ciphertext + tag[16]  AES-256-GCM(PKCS#8 private key)
AAD = magic || format_version || key_id || public_key_sha256
```

`ciphertext_length` 只计密文，不含紧随其后的 16-byte GCM tag；解码要求总长精确匹配且密文不超过 16 KiB。
RSA 公钥编码最多 2 KiB，模数必须是 3072 位且指数固定为 65537。AES wrapping key 由 Vault DEK 通过
`HMAC-SHA256("passly-autofill-inbox-private-wrap-v1")` 域分离得到。
App 密码/生物识别变更只重包同一 DEK，不旋转 Inbox；真正 DEK rotation 必须在同一协议内重新包装 private key。

## Pending store

目录为 `filesDir/vault/autofill-inbox/<key-id>/{pending,quarantine}`。当前实现使用同目录临时文件、文件 `fsync` 与原子 rename，并在读取/写入时清理超期记录、旧 key 目录与孤立临时文件。文件名是随机 draft ID，不包含包名、域名、
用户名、Entry ID 或时间；有效期依据文件修改时间管理，精确记录时间仍由加密 header 认证。

pending record v1：

```text
magic[11]                 "PASSLYINBOX"
format_version            u8 = 1
key_id[16]
draft_id[16]
created_at_ms             i64 big endian
wrapped_key_length        u16 big endian
wrapped_content_key       RSA-OAEP-SHA256(random AES-256 key)
payload_nonce[12]
ciphertext_length         u32 big endian
payload_ciphertext + tag  AES-256-GCM(serialized draft)
```

AAD 是 encoded `magic || version || key_id || draft_id || created_at_ms` 再拼接从 `bootstrap.pb` 读取的
`public_key_sha256`；fingerprint 不在 record header。payload 使用独立 magic/version，重复 draft ID，并包含
source（固定 wire key：Legacy=1、Credential Manager=2）、captured time、package/domain/page title/username/password。
字符串使用 UTF-8 与 big-endian signed i32 长度，nullable 字段用 -1 表示缺失；拒绝未知 source、非法 UTF-8、尾随字节与超限字段。
各 UTF-8 字段上限分别为 package 512、domain 4096、page title 8192、username 8192、password 32768 bytes，整条 record 上限 64 KiB。
Entry ID 由导入事务生成，事务 marker 保存创建后返回的 ID；不在 pending payload 预造 Entry ID。解密后两个 draft ID 必须一致。

## 限制与清理

- 全 root 最多 8 个 active pending、单包 64 KiB、active 总计 256 KiB、保留 24 小时；
- quarantine 最多 8 个、256 KiB、保留 7 天；
- temp/sidecar/pending/quarantine 整个 root 硬上限 512 KiB；
- 满额时拒绝新保存，不删除仍有效旧记录；
- 当前 key 已知时清理非当前 key-ID 目录；当前 quarantine 按 7 天保留；
- record-specific transient failure 最多重试 3 次并指数退避，永久认证/格式错误立即 quarantine。

近似保存时间暴露在 header/mtime 中，用于锁定状态过期清理；这是明确接受的元数据风险。对 app 私有目录具有任意
写权限的攻击者仍可替换 public key 并重定向之后的密封；private/public/fingerprint 检查只能在解锁时检测，不能在
该边界完全失守后预防。

## 幂等导入

解锁后的 single-flight drainer 按创建顺序逐条解密。SQLCipher 增加
`autofill_import_log(draft_id PRIMARY KEY, imported_at_ms, entry_id)`。同一事务先插入 import marker，再通过唯一
`SaveAutofillCredential` 物化 Entry。提交成功才删 pending；唯一约束冲突表示已导入，可安全删文件。

database/session-wide failure 中止整轮，不增加每条 retry；记录级失败按上述策略处理。private envelope 解不开时
保留并隔离，不删除。

## 备份和生命周期

Manifest 当前 `allowBackup=false`，data extraction rules 也必须排除完整 Inbox root 与 `bootstrap.pb` 的 Inbox
材料。该设计只承诺在容量/保留期内同一安装上的进程死亡和设备重启不丢失，不承诺跨设备或 Passly Backup 恢复。
删除 Vault 时清除 bootstrap material、Inbox root 和 import log。

决策见 [ADR-0025](../../decisions/ADR-0025-sealed-autofill-save-inbox.md)。
