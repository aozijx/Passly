# Database Implementation

## Passly Database Implementation

Version: v1.0

---

# 1. 概述

本文档描述 Passly 数据库的具体实现方案，包括数据库结构、数据表职责、字段设计、索引策略、Mapper、迁移策略以及数据一致性约束。

数据库采用 SQLCipher 作为底层数据库，Room 作为 ORM 框架，BootstrapStore（DataStore）负责数据库启动前所需的数据存储。

整个数据库由多个职责单一的数据表组成，每张表均围绕一种业务能力进行设计，避免单表承担过多职责。

---

# 2. 数据库整体结构

BootstrapStore（DataStore）

负责数据库初始化阶段的数据。

↓

SQLCipher Database

├── vault_metadata
├── vault_credentials
├── lookup_index
├── vault_snapshots
├── vault_activities
└── vault_attachments

其中：

BootstrapStore 不属于 SQLCipher。

SQLCipher 打开之后才开始访问其余数据表。

---

# 3. BootstrapStore

## 职责

BootstrapStore 保存数据库启动之前必须访问的数据。

例如：

• Key Envelope
• Bootstrap Version
• Migration State

由于 SQLCipher 解锁需要 DEK，而 DEK 又存放于 Envelope，因此 Envelope 必须独立于数据库保存，避免循环依赖。

BootstrapStore 推荐使用 Proto DataStore 实现。

---

# 4. vault_metadata

## 职责

保存所有高频访问的数据。

该表不保存密码等敏感信息，仅保存支持列表展示、排序、搜索和 Autofill 所需的数据。

## 字段

entryId

条目唯一标识（UUIDv7）。

vaultId

所属 Vault，当前默认为 default，预留未来多 Vault 支持。

entryType

条目类型。

例如：

• LOGIN
• CARD
• NOTE
• IDENTITY
• WIFI

title

条目标题。

username

用户名。

为了提升 Autofill 体验，该字段属于 Metadata。

primaryUrl

主网址。

domain

解析后的主域名。

icon

图标信息。

可保存网站图标 URL 或缓存标识。

favorite

收藏状态。

tags

标签集合。

createdAt

创建时间。

updatedAt

最后更新时间。

lastUsedAt

最近使用时间。

usageCount

使用次数。

## 索引

PRIMARY KEY(entryId)

INDEX(createdAt)

INDEX(updatedAt)

INDEX(lastUsedAt)

INDEX(favorite)

这些索引支持：

• 最近创建
• 最近修改
• 最近使用
• 收藏列表

整个列表无需解密 Credential。

---

# 5. vault_credentials

## 职责

保存所有敏感信息。

数据库不理解 Credential 内部字段，仅负责保存加密后的 Blob。

## 字段

entryId

与 Metadata 一一对应。

credentialBlob

AES-256-GCM 加密后的二进制数据。

Blob 内保存：

• Password
• OTP
• Card
• SSH
• Wallet
• Identity
• Wifi
• Hardware
• Notes
• Custom Fields

Credential 使用 Kotlin Serialization 序列化。

数据库无需维护大量 Nullable 字段。

未来新增字段无需修改 Schema。

---

# 6. lookup_index

## 职责

实现 Blind Index 搜索。

数据库仅保存关键词哈希，不保存原始字符串。

## 字段

entryId

所属条目。

field

来源字段。

例如：

TITLE

USERNAME

URL

keywordHash

关键词哈希。

gramLength

N-Gram 长度。

支持未来多个长度。

weight

搜索权重。

例如：

Title

>

Username

>

URL

用于结果排序。

## 唯一约束

(entryId,
field,
keywordHash,
gramLength)

保证同一条记录不会重复建立相同索引。

## 索引

INDEX(keywordHash)

用于快速定位搜索结果。

由于联合唯一索引已经覆盖 entryId、field 等字段，因此无需额外建立：

INDEX(entryId, field)

避免冗余索引增加写入成本。

---

# 7. vault_snapshots

## 职责

保存条目完整历史快照。

Snapshot 保存的是某一时刻完整状态，而非修改记录。

恢复时无需回放 Diff。

## 字段

snapshotId

快照唯一标识。

entryId

所属条目。

version

版本号。

snapshotBlob

完整 VaultEntry 序列化后加密的数据。

createdAt

创建时间。

## 唯一约束

(entryId,
version)

保证每个版本仅存在一份快照。

Snapshot 不参与正常查询，仅在历史恢复时访问。

---

# 8. vault_activities

## 职责

记录用户操作。

用于：

• Activity Timeline
• 审计日志
• 最近操作

## 字段

activityId

活动唯一标识。

entryId

所属条目。

activityType

操作类型。

例如：

CREATE

UPDATE

DELETE

RESTORE

AUTOFILL

COPY_USERNAME

COPY_PASSWORD

source

操作来源。

Autofill 时可记录调用应用包名。

createdAt

操作时间。

## 索引

INDEX(entryId, createdAt)

支持查看某个条目的活动历史。

INDEX(createdAt)

支持查看全局时间线。

Activity 不参与数据恢复，仅负责记录事件。

---

# 9. vault_attachments

## 职责

保存附件元数据。

数据库仅保存：

attachmentId

entryId

metadata

encryptedPath

createdAt

真正附件文件保存于：

files/{uuid}.bin

数据库仅负责维护引用关系。

未来可扩展：

• 图片
• PDF
• Key File
• Backup

---

# 10. Mapper

数据库 Entity 与 Domain Model 保持解耦。

Mapper 负责完成转换。

MetadataMapper

VaultMetadata

⇄

VaultMetadataEntity

CredentialMapper

VaultCredential

⇄

VaultCredentialEntity

SnapshotMapper

VaultSnapshot

⇄

VaultSnapshotEntity

ActivityMapper

VaultActivity

⇄

VaultActivityEntity

EntryMapper

VaultEntry

⇄

Metadata + Credential

Repository 负责聚合多个 Mapper。

---

# 11. Repository

Repository 是 Domain 与 Database 的边界。

读取：

MetadataEntity

+

CredentialEntity

↓

Mapper

↓

VaultEntry

写入：

VaultEntry

↓

Mapper

↓

MetadataEntity

CredentialEntity

Repository 保证冷热数据的一致性。

---

# 12. 数据一致性

数据库更新遵循事务。

创建条目：

Metadata

+

Credential

+

Lookup Index

+

Snapshot

+

Activity

必须全部成功。

修改条目：

更新 Metadata

↓

更新 Credential

↓

重建 Lookup Index

↓

新增 Snapshot

↓

记录 Activity

整个过程运行于同一数据库事务中。

删除条目：

删除 Metadata

↓

删除 Credential

↓

删除 Lookup Index

↓

删除 Snapshot

↓

删除 Attachment

↓

记录 Activity（可选）

所有关联数据保持一致。

---

# 13. 数据迁移

数据库版本通过 Room Migration 管理。

迁移原则：

禁止直接删除用户数据。

新增字段：

优先提供默认值。

删除字段：

优先保留兼容版本。

Credential Blob 内新增字段：

依赖 Kotlin Serialization 默认值完成兼容。

由于大量数据存储于 Blob，未来 Schema 变化次数将明显减少。

---

# 14. 性能策略

列表：

仅查询 vault_metadata。

搜索：

仅查询 lookup_index。

Autofill：

查询 Metadata 获取候选项。

用户确认后：

读取 Credential。

详情：

按需解密 Credential。

历史：

按需读取 Snapshot。

活动：

按需读取 Activity。

绝大多数数据库操作均无需解密敏感数据。

---

# 15. 实现原则总结

1. BootstrapStore 与 SQLCipher 完全解耦，避免密钥循环依赖。

2. Metadata 与 Credential 实现冷热分离，高频数据无需解密。

3. 敏感数据统一采用 AES-256-GCM 加密后存储为 BLOB，数据库不感知其内部结构。

4. Lookup Index 使用 Blind Index，实现零解密搜索，同时兼顾搜索性能与隐私保护。

5. Snapshot 保存完整版本状态，用于历史恢复；Activity 保存用户操作，用于审计，两者职责明确。

6. Repository 负责聚合多个 Entity，Mapper 负责 Domain 与 Database 的转换，保持业务模型与持久化模型解耦。

7. 数据更新通过事务保证一致性，确保 Metadata、Credential、Lookup Index、Snapshot 与 Activity 始终保持同步。

8. 整体数据库设计遵循最小暴露、按需解密、高内聚、低耦合的原则，为未来扩展多 Vault、Passkey、附件及同步等能力提供稳定基础。