# ADR-0018：Metadata / Lookup / Credential 三层数据架构

状态：Accepted
日期：2026-07

---

## 背景

最初所有数据都存放在：

encryptedBlob

打开密码列表时：

Repository

↓

解密所有 Blob

↓

获取标题

↓

显示列表

虽然安全，但存在两个问题：

第一：

首页滚动会频繁解密。

第二：

搜索需要遍历全部记录。

性能较差。

---

## 决策

数据库拆分为三层。

第一层：

vault_metadata

第二层：

lookup_index

第三层：

vault_credentials

三者职责完全不同。

---

## vault_metadata

负责：

首页展示。

字段包括：

- entryId
- title
- icon
- category
- favorite
- updatedAt

Metadata 为热数据。

列表滚动只读取 Metadata。

不会加载 Credential。

---

## vault_credentials

负责：

真正敏感数据。

包括：

- username
- password
- notes
- Passkey
- TOTP
- SSH
- Payment
- CustomField

全部存放：

credentialBlob

点击详情后：

Repository

↓

解密 credentialBlob

↓

显示详情

首页不会提前解密。

---

## lookup_index

负责：

所有搜索。

包括：

- 标题
- 用户名
- 邮箱
- Domain
- Package

以及：

Autofill

统一使用 Blind Index。

查询结果：

entryId

↓

Metadata

↓

列表

↓

Credential

整个过程不会扫描所有 Blob。

---

## 数据流

新增：

VaultEntry

↓

MetadataMapper

↓

CredentialMapper

↓

LookupMapper

↓

Database

查询：

Lookup

↓

Metadata

↓

Credential

各层互不依赖。

---

## 优点

首页滚动速度提升。

搜索无需解密全部数据。

详情按需解密。

Autofill 与普通搜索复用同一套索引。

新增字段无需影响 Metadata。

冷热数据完全分离。

---

## 最终决策

数据库采用三层架构：

Metadata

Lookup

Credential

Metadata 负责展示。

Lookup 负责检索。

Credential 负责敏感数据。

Repository 根据业务按需加载对应数据，避免一次性解密全部密码数据。