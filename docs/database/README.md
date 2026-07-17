# Passly 数据库架构设计报告（v2.0）

## 一、设计目标

Passly 是一款面向 Android 的离线密码管理器，采用 **Zero Knowledge**、**Offline First**、**Envelope Encryption** 的安全架构。

数据库设计目标如下：

1. 全部敏感数据默认加密存储。
2. 支持 Metadata 与 Credential 分离，减少敏感数据暴露。
3. 搜索无需解密数据库。
4. 自动填充按需读取凭据。
5. Schema 长期稳定，新增业务字段尽量无需 Migration。
6. 支持未来多 Vault、同步、共享、企业策略等扩展。

---

# 二、整体架构

采用 **Hot / Cold Data Separation（冷热数据分离）**。

```
                 VaultEntry (Domain)
                        │
        ┌───────────────┴───────────────┐
        │                               │
MetadataMapper                 CredentialMapper
        │                               │
        ▼                               ▼
 MetadataPayload               CredentialPayload
        │                               │
 AES-256-GCM                   AES-256-GCM
        │                               │
        ▼                               ▼
 vault_metadata             vault_credentials
```

其中：

Metadata 属于 **Hot Data**

Credential 属于 **Cold Data**

整个 Session：

Metadata 会统一解密。

Credential 永远按需解密。

---

# 三、数据库结构

最终数据库包含以下业务表：

```
vault_metadata
vault_credentials
lookup_index
vault_history
vault_activity
vault_attachments
key_envelopes
vault_metadata_info
```

各表职责如下。

---

# 四、vault_metadata

## 作用

保存所有用于展示、搜索结果、自动填充候选的元数据。

应用解锁后：

统一读取

↓

统一解密

↓

缓存整个 Session

整个 Session 不再重复解密 Metadata。

---

## 字段

```
entryId
vaultId
entryType
entryVersion
metadataBlob
updatedAt
deletedAt
```

其中：

metadataBlob

保存：

```
title
icon
category
favorite
tags
associatedDomains
associatedPackages
subtitle
displayColor
displayOptions
...
```

未来新增 Metadata 字段：

仅修改 MetadataPayload。

数据库 Schema 无需变化。

---

# 五、vault_credentials

## 作用

保存所有真正敏感的数据。

只有以下场景访问：

• 查看详情

• 编辑

• Autofill

• Copy Password

• Copy Username

• 查看 TOTP

• Export

默认情况下不会批量读取。

---

## 字段

```
entryId
credentialBlob
```

CredentialPayload 包括：

```
username
password
email
notes
totpSecret
passkey
sshKey
identity
card
payment
seedPhrase
customFields
...
```

所有字段统一：

AES-256-GCM

加密。

---

# 六、Metadata 与 Credential 关系

两张表：

一对一关系。

```
VaultMetadata
        │
 entryId
        │
VaultCredential
```

删除 Metadata：

CASCADE

自动删除：

Credential

History

Activity

Attachment

LookupIndex

保持数据库一致性。

---

# 七、Session 生命周期

应用启动：

```
Unlock

↓

读取 vault_metadata

↓

解密 metadataBlob

↓

MetadataCache
```

之后：

密码列表

搜索结果

最近使用

分类

标签

收藏

Autofill Candidate

全部直接访问 MetadataCache。

Credential 不缓存。

---

# 八、密码列表

流程：

```
MetadataRepository

↓

MetadataCache

↓

RecyclerView
```

不会读取：

Credential。

因此：

密码不会进入内存。

---

# 九、搜索

Passly 使用：

Blind Index

而不是：

SQLite FTS。

Search Builder：

```
Metadata

+

Credential

↓

Normalize

↓

HMAC(SearchKey)

↓

keywordHash

↓

lookup_index
```

数据库保存：

```
entryId
field
keywordHash
weight
```

搜索：

```
Keyword

↓

Blind Index

↓

EntryId

↓

Metadata

↓

UI
```

整个过程：

无需读取 Credential。

---

# 十、自动填充

自动填充分两阶段。

## 第一阶段

搜索候选。

```
Package

↓

lookup_index

↓

EntryId

↓

Metadata

↓

Candidate
```

如果需要显示：

username

则：

```
EntryId

↓

CredentialRepository

↓

解密对应 Credential

↓

读取 username

↓

Candidate
```

仅解密命中的几条记录。

不会批量解密数据库。

---

## 第二阶段

用户点击：

```
CredentialRepository

↓

CredentialPayload

↓

username

password

totp

↓

FillResponse
```

真正填充时：

才读取完整 Credential。

---

# 十一、附件

附件采用独立表。

数据库：

保存：

```
attachmentId

entryId

encryptedBlob

cipherHash
```

业务字段：

```
filename

mimeType

encryptedPath

thumbnail

dimension

size

...
```

统一进入：

AttachmentPayload

AES-GCM

加密。

附件文件：

```
files/vault/attachments/
```

保存：

加密文件。

数据库仅保存：

Metadata。

---

# 十二、History

History 保存：

完整快照。

而不是：

字段 Diff。

HistoryPayload：

包含：

```
MetadataPayload

+

CredentialPayload
```

恢复：

直接恢复整个 Entry。

无需 Diff。

---

# 十三、Activity

Activity：

记录用户行为。

包括：

```
VIEW

COPY_USERNAME

COPY_PASSWORD

AUTOFILL

CREATE

UPDATE

DELETE

RESTORE

EXPORT

IMPORT
```

Activity：

不属于业务聚合。

直接使用：

VaultActivityEntity。

无需单独 Domain Model。

---

# 十四、Envelope

Passly 使用：

Multi Envelope。

每个 Envelope：

负责解开：

同一个 DEK。

例如：

```
Biometric

Device Credential

App Password

Recovery Code

Passkey（未来）

Hardware Key（未来）
```

数据库：

```
key_envelopes
```

新增认证方式：

无需重新加密数据库。

仅新增 Envelope。

---

# 十五、加密设计

数据库采用双层保护。

```
Android Keystore

↓

Master Key

↓

DEK

↓

SQLCipher
```

同时：

```
DEK

↓

SessionKey

↓

AES-256-GCM

↓

MetadataBlob

CredentialBlob

AttachmentBlob

HistoryBlob
```

AAD：

```
tableName

entryId

columnName
```

防止：

Cut-and-Paste Attack。

---

# 十六、Schema 设计原则

数据库仅保存：

```
ID

Version

Blob

Timestamp

关联关系
```

所有业务字段：

全部进入 Payload。

因此：

新增：

Passkey

SSH

Card

Crypto

Identity

Attachment Metadata

Custom Field

均无需数据库 Migration。

---

# 十七、设计优势

## 安全

所有敏感数据统一 AES-GCM 加密。

数据库无法直接读取任何业务内容。

---

## 性能

Metadata 解锁后统一缓存。

密码列表、搜索、自动填充无需批量解密 Credential。

---

## 可维护性

Metadata 与 Credential 完全解耦。

新增业务字段无需修改数据库 Schema。

---

## 可扩展性

天然支持：

* 多 Vault
* Sync
* Shared Vault
* Enterprise
* Hardware Key
* Passkey
* Organization

无需推翻现有数据库设计。

---

# 十八、最终架构总结

Passly 数据库采用 **Metadata / Credential 双层模型 + Blind Index + Payload Blob** 的设计。

* **Metadata**：热数据（Hot Data），应用解锁后统一解密并缓存，负责列表展示、搜索结果和自动填充候选。
* **Credential**：冷数据（Cold Data），仅在用户查看详情、编辑、自动填充或复制凭据时按需解密。
* **Search**：独立 Blind Index 层，仅负责定位 Entry，不接触明文业务数据。
* **History**：保存完整快照，支持版本恢复。
* **Attachment**：附件与数据库分离，业务元数据统一加密。
* **Envelope**：采用多信封架构，实现认证方式与数据库加密解耦。

该设计兼顾了安全性、性能、可维护性和长期扩展能力，可作为 Passly 数据层的长期架构标准。