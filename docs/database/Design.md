```txt
# Database Design

## Passly Database Design

Version: v1.0

---

# 1. 设计目标（Design Goals）

Passly 是一款面向 Android 平台的离线密码管理器，数据库设计遵循 Zero Knowledge 与 Offline First 原则，在保证安全性的同时兼顾性能、可维护性及未来扩展能力。

数据库设计并非简单地将业务对象映射为数据表，而是围绕"最小暴露、冷热分离、按需解密"三个核心思想展开，使数据库成为整个安全架构的重要组成部分。

数据库设计遵循以下目标：

• Zero Knowledge
任何敏感信息仅在用户设备本地解密，数据库永远不会保存明文密码、TOTP 密钥、银行卡信息等数据。

• Offline First
所有功能均可离线完成，不依赖网络服务。

• High Performance
首页列表、搜索、排序、Autofill 等高频操作无需解密整个数据库。

• Minimal Metadata Exposure
仅保留必要元数据，其余信息全部存储于加密 Blob 中。

• Scalable
支持未来扩展多 Vault、Passkey、附件、云同步、冲突解决等功能，而无需推翻数据库结构。

---

# 2. 核心设计思想（Design Philosophy）

## 2.1 Metadata / Credential 热冷分离

数据库最大的设计原则是冷热分离（Hot / Cold Separation）。

传统密码管理器通常将所有字段保存在同一张表中，每次打开列表或进行搜索都需要解密完整记录，这会造成大量不必要的解密操作。

Passly 将条目拆分为两个逻辑部分：

Metadata

负责保存高频访问的数据，例如：

• 标题
• 用户名
• 网站地址
• 域名
• 图标
• 收藏状态
• 标签
• 创建时间
• 更新时间
• 最近使用时间
• 使用次数

这些数据主要用于：

• 首页列表
• 搜索
• 排序
• Autofill 候选展示

Credential

负责保存真正敏感的数据，例如：

• Password
• OTP
• SSH Key
• Bank Card
• Identity
• Wallet
• Notes
• Custom Fields

Credential 永远不会参与列表展示，仅在进入详情页、编辑页面、导出等低频操作时才进行解密。

冷热分离使数据库能够在保证安全性的同时，大幅减少解密次数。

---

## 2.2 Repository 聚合模型

数据库并不存在一个 VaultEntry 表。

真正的业务对象 VaultEntry 由 Repository 聚合得到。

数据库：

Metadata Entity

+

Credential Entity

↓

Mapper

↓

VaultEntry（Domain）

Repository 负责协调多个 Entity 的读取与写入，而 Domain 永远只面对统一的 VaultEntry。

这种设计使数据库结构能够独立演进，而不会影响业务层。

---

## 2.3 Domain 与 Database 解耦

Domain Model 表达业务语义。

Database Entity 表达存储结构。

两者并不要求保持一致。

例如：

Database 中：

credentialBlob

Domain 中：

password

otp

notes

Database 不需要理解 Blob 内部结构。

Mapper 负责完成 Entity 与 Domain 之间的转换。

这种设计使数据库迁移不会影响业务代码，同时保证 Domain 模型始终保持清晰。

---

## 2.4 Blob 化存储

除 Metadata 外，其余敏感数据全部序列化为 Payload 后统一加密。

流程如下：

VaultCredential

↓

Serialization

↓

JSON

↓

AES-256-GCM

↓

credentialBlob (BLOB)

数据库无需了解 Blob 内部字段，也无需维护大量 Nullable 列。

这种方式具有以下优点：

• 减少数据库字段数量
• 降低 Schema 演进成本
• 统一加密流程
• 提高未来兼容性

---

## 2.5 Snapshot 而非 History

Passly 保存的是完整版本快照，而不是修改差异。

每次修改都会生成一个新的 Snapshot。

恢复历史版本时：

Snapshot

↓

Decrypt

↓

Restore

而不是：

Version1

↓

Diff

↓

Diff

↓

VersionN

这种设计恢复速度更快，逻辑也更加简单。

因此数据库采用 Snapshot，而非 History。

---

## 2.6 Blind Index 搜索

SQLCipher 无法直接进行全文搜索。

Passly 使用 Blind Index 实现零解密搜索。

每个关键词经过规范化后生成固定长度哈希：

Keyword

↓

Normalize

↓

SHA-256

↓

KeywordHash

搜索时：

Query

↓

Normalize

↓

Hash

↓

Lookup Index

整个过程无需解密 Credential。

数据库仅知道哈希值，而不知道真实关键词。

---

## 2.7 Bootstrap 独立

数据库启动前必须获得 DEK。

因此 Key Envelope 不能保存在 SQLCipher 数据库中。

否则会形成：

SQLCipher

↓

需要 DEK

↓

DEK 在 SQLCipher

循环依赖。

因此 BootstrapStore 独立于 SQLCipher。

BootstrapStore 负责保存：

• Key Envelope
• Bootstrap Metadata
• Migration State

数据库启动完成后便不再参与业务流程。

---

# 3. 数据生命周期

创建条目：

UI

↓

UseCase

↓

Repository

↓

Metadata

Credential

↓

Generate Blind Index

↓

Create Snapshot

↓

Record Activity

修改条目：

Repository

↓

Update Metadata

↓

Update Credential

↓

Rebuild Blind Index

↓

Create Snapshot

↓

Record Activity

删除条目：

Delete Metadata

↓

Delete Credential

↓

Delete Lookup Index

↓

Delete Attachments

↓

Record Activity

整个生命周期始终保持 Metadata 与 Credential 的一致性。

---

# 4. 安全设计

Passly 的安全模型包括多个层次。

第一层：

SQLCipher

保护整个数据库文件。

第二层：

AES-256-GCM

保护所有敏感 Payload。

第三层：

Android Keystore

保护 Master Key。

第四层：

Envelope Encryption

保护 DEK。

第五层：

Metadata 最小暴露原则。

即使攻击者获得数据库文件，也无法直接恢复 Credential 内容。

---

# 5. 性能设计

为了减少频繁解密带来的性能开销：

列表：

仅访问 Metadata。

搜索：

仅访问 Lookup Index。

排序：

仅访问 Metadata。

Autofill：

优先读取 Metadata，仅在用户选择候选项后解密对应 Credential。

详情：

按需解密 Credential。

这种设计使绝大多数用户操作无需进行数据库级解密。

---

# 6. 可扩展性

数据库设计预留未来能力：

• 多 Vault
• Passkey
• 附件
• 云同步
• 冲突解决
• 更多凭据类型
• 更多认证方式

新增功能通常只需增加新的 Payload 字段或独立表，而无需修改现有 Metadata 与 Credential 的核心结构。

---

# 7. 总结

Passly 数据库采用冷热分离、Blob 存储、Repository 聚合以及 Blind Index 搜索等设计思想，在保证 Zero Knowledge 的基础上兼顾性能与可扩展性。

数据库并不是 Domain 的镜像，而是围绕安全模型设计的数据持久化层。

整个数据库设计遵循以下原则：

• 最小暴露
• 按需解密
• Blob 化存储
• Domain 与 Database 解耦
• Repository 聚合
• Snapshot 保存历史
• Blind Index 搜索
• 面向未来扩展
```

下一篇为 **《Database Implementation（数据库实现）》**，详细描述每张表、字段、索引、约束、Mapper 和迁移策略。
