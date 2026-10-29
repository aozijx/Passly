# ADR-0025：锁定状态使用 Sealed Inbox 保存 Autofill 凭据

- 状态：Accepted（实现中；bootstrap key material 的 Domain/Proto 持久化及密钥编解码已落地）
- 日期：2026-10-18

## 背景

当前 Legacy Autofill 与 Credential Manager create 在 Vault 锁定时都需要启动认证，取得临时数据库会话后才能
加密并写入 Entry。这把“用户允许 Android 保存”与“读取/解锁 Vault”绑定，也使 Legacy 需要进程内明文草稿和
专用 Activity。产品要求锁定状态能够耐久接受保存，但不能因此获得 Vault 读取或解密能力。

Android `SaveInfo.FLAG_DELAY_SAVE` 只延迟同一 task 的 Autofill save context，服务不能在未来任意解锁时主动消费；
它不能提供 Passly 所需的同安装重启耐久队列。SQLCipher 也不能作为 pending store，因为锁定时打开数据库就重新引入
DEK 和读权限。

## 决策

每个 Vault 使用一对 RSA-3072/OAEP-SHA256 Inbox key。public key 在锁定状态可读，只用于包装随机 AES-256
content key；PKCS#8 private key 由 Vault DEK 派生的 AES-256-GCM key 包装，并作为一个原子 nested message
追加到现有 `bootstrap.pb`。

锁定保存把完整草稿写入 `filesDir/vault/autofill-inbox/<key-id>/pending` 的独立认证 envelope。该目录不依赖
SQLCipher，并排除 Android backup/transfer 和 Passly Backup。写入成功只表示 `Queued`；解锁后 single-flight
drainer 验证 key/fingerprint/AAD、解密并在同一 SQLCipher 事务中写入 `autofill_import_log` 唯一 marker 与 Entry。
事务提交后才删 pending，以保证崩溃幂等。

容量、保留期、quarantine、retry、stale key-ID 清理、精确格式和残余 public-key substitution 风险由稳定主题文档
[Sealed Inbox](../features/autofill/sealed-inbox.md)定义。

Legacy 和 Credential Manager 保留各自平台 response。它们只共享平台无关 `AcceptAutofillSave` 与 Entry
materialization；不合并 Android 结果类型。该实现落地后，ADR-0023 中“Legacy locked save 使用认证 continuation”
的部分将由本 ADR 替代，typed interaction、fill authorization 和 result-delivery ordering 仍然有效。

## 后果

- 锁定保存不弹认证，也不获得 Vault 读权限。
- 在限制和保留期内，进程死亡/设备重启不会丢失已接受 save。
- 引入新的 key lifecycle、binary envelope、文件存储、quarantine 和 import-log 表，需要安全测试与清理协议。
- App 私有目录完全失守时，public key 可被替换；解锁时校验只能检测，不能阻止替换后密封的数据被重定向。
- 尚未 provision 的旧 Vault 必须明确拒绝 locked save，不能虚报成功或临时回退到认证。

## 未采用方案

- `FLAG_DELAY_SAVE`：没有未来主动消费 API，语义是多屏 workflow，而非应用持久 pending queue。
- 长期 Android Keystore 对称写入 key：能加密通常也能解密，不形成应用层 write-only 能力。
- SQLCipher pending 表：锁定写入需要打开 Vault 数据库，破坏权限边界。
- 只存在内存的一次性 key：进程死亡会丢失已接受 save。
