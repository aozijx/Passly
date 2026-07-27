# ADR-0002: 采用信封加密

- 状态：Accepted
- 日期：未记录

## 背景

Vault 需要支持生物识别、应用密码和恢复码等多种认证方式，并允许替换认证凭据而不重加密全部业务数据。

## 决策

生成随机 Vault DEK。每种认证方式产生包装能力，并保存独立 Envelope；Envelope 只保存密文、nonce、salt、KDF
标识和版本，不保存明文 DEK。

## 后果

新增或更换认证方式只需新增/替换 Envelope。Bootstrap 数据成为关键恢复资产，必须严格版本化并与数据库分离保存。

## 备选方案

未采用“用户密码直接加密全部数据”和“数据库直接绑定单个 Keystore key”，因为凭据变更与恢复成本过高。

## 关联

[密钥管理](../security/key-management.md)
