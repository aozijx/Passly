如果这是一个长期维护的开源项目（例如 Passly），我不会只写一份《数据库设计》。我会把 docs/database/ 拆成多个文档，每份职责单一，随着项目演进也容易维护。

建议结构如下：

docs/
└── database/
├── README.md
├── architecture.md
├── schema.md
├── encryption.md
├── payload.md
├── search.md
├── history.md
├── attachment.md
├── envelope.md
├── migration.md
└── decisions/
├── ADR-0001-hot-cold-data.md
├── ADR-0002-blind-index.md
├── ADR-0003-uuidv7.md
└── ADR-0004-metadata-credential.md
README.md

数据库目录首页。

负责说明：

为什么存在这些文档
数据库总体结构
阅读顺序

例如：

Database Documentation

Passly 使用 SQLCipher + Room + AES-GCM 实现 Zero Knowledge 数据存储。

建议阅读顺序：

1. Architecture
2. Schema
3. Encryption
4. Payload
5. Search
6. History
7. Envelope
   architecture.md

整个数据库设计思想。

这里不写字段。

而是写：

为什么采用 Metadata + Credential

为什么采用 Hot / Cold Data

为什么 Search 独立

为什么 History 独立

为什么 Attachment 独立

并画图：

UI
│
Repository
│
VaultEntry
│
Metadata      Credential
│               │
AES-GCM      AES-GCM
│               │
Room Tables

这是最重要的一份。

schema.md

只写数据库。

例如：

vault_metadata
Column	Type	Description
entryId	TEXT	Primary Key
vaultId	TEXT	Vault identifier
entryType	INTEGER	Entry type
metadataBlob	BLOB	Encrypted MetadataPayload
updatedAt	INTEGER	Update timestamp
deletedAt	INTEGER?	Soft delete timestamp

下面再画 ER 图：

vault_metadata
│
├──── vault_credentials
├──── vault_history
├──── vault_activity
├──── lookup_index
└──── vault_attachment

这一份专门给数据库开发者。

encryption.md

只讨论加密。

例如：

Android Keystore

↓

Master Key

↓

DEK

↓

SQLCipher

+

AES-GCM

↓

MetadataBlob
CredentialBlob
HistoryBlob
AttachmentBlob

再说明：

AAD
HKDF
SearchKey
Cipher Hash

全部放这里。

payload.md

专门描述 Payload。

例如：

MetadataPayload

CredentialPayload

AttachmentPayload

HistoryPayload

每个 Payload 的字段都写清楚。

未来新增：

Passkey

SSH

Identity

这里只改这一份。

数据库不用改。

search.md

只讨论 Blind Index。

包括：

Normalize

↓

Tokenize

↓

HMAC(SearchKey)

↓

LookupIndex

以及：

为什么不用 SQLite FTS

为什么不用 LIKE

为什么采用 Blind Index
history.md

只讨论版本。

例如：

Snapshot

Restore

Delete History

Retention

说明为什么不用 Diff。

attachment.md

说明：

数据库

+

文件系统

关系。

例如：

VaultAttachmentEntity

↓

encryptedBlob

↓

AttachmentPayload

↓

encryptedPath

↓

files/vault/attachments/
envelope.md

说明：

MasterKey

↓

DEK

↓

Envelope

以及：

Biometric

Password

Recovery

Passkey

关系。

migration.md

每一个数据库版本：

例如：

v1

↓

v2

↓

v3

记录：

为什么改。

Migration 做了什么。

以后查 Bug 非常方便。

decisions（ADR）

这是我非常推荐的。

不要把：

为什么这么设计

写在正文里。

而是写 ADR。

例如：

ADR-0001

为什么 Metadata 和 Credential 分离

里面记录：

背景
备选方案
最终决策
为什么没选其他方案

以后你回头看，会知道当初为什么这么设计，而不是只知道“现在长这样”。

我认为最好的文档结构
docs/
└── database/
├── README.md              # 导航
├── architecture.md        # 总体架构（最重要）
├── schema.md              # 表结构
├── encryption.md          # 加密体系
├── payload.md             # Payload 设计
├── search.md              # Blind Index
├── history.md             # 历史版本
├── attachment.md          # 附件设计
├── envelope.md            # 多信封架构
├── migration.md           # 数据库演进
└── decisions/             # ADR 决策记录

这种组织方式比一份几百行的数据库设计文档更容易阅读、维护和扩展，也更符合长期项目和开源项目的文档实践。