# 存储加密

## 两层保护

SQLCipher 使用从已解锁 Vault DEK 得到的数据库口令保护数据库文件、页和索引。对凭据等高敏字段，Repository
还使用会话密钥执行 AES-256-GCM，使数据库打开状态下仍保持字段密文。

## 字段分类

| 类型    | 示例                    | 存储策略            |
|-------|-----------------------|-----------------|
| 列表元数据 | id、条目类型、时间、排序标志       | SQLCipher 内可查询列 |
| 检索标记  | 域名/包名 Blind Index     | 不可逆索引           |
| 敏感负载  | 用户名、密码、TOTP secret、备注 | AES-GCM 加密 blob |

## AES-GCM 约束

- 算法：`AES/GCM/NoPadding`，256-bit key、12-byte 随机 nonce、128-bit tag。
- ciphertext 必须与 nonce 一同保存；若使用 AAD，其组成和版本必须稳定。
- Repository 是 Entity 与明文 Domain model 的转换边界。
- AEAD tag 失败应映射为数据损坏/认证失败，不得返回空对象或删除记录。

加密格式字段以 `CryptoConfig` 和加密 DTO 为事实源，文档不复制完整序列化代码。

