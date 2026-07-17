# ADR-0016：备份格式设计

状态：Accepted
日期：2026-07

---

## 背景

备份不仅用于导出。

未来还需要支持：

- 导入
- 多设备迁移
- 云同步
- History
- Restore

因此不能直接导出数据库。

需要稳定的数据交换模型。

---

## 决策

定义独立模型：

VaultSnapshot

作为唯一备份格式。

数据库 Entity 不参与导出。

---

## VaultSnapshot

包含：

- id
- vaultId
- entryType
- schemaVersion
- deletedAt
- createdAt
- updatedAt
- lastUsedAt
- metadata
- credential
- attachments

其中：

metadata

保存：

标题

分类

标签

图标

URI

等。

credential

保存：

用户名

密码

Passkey

TOTP

SSH

Payment

等敏感信息。

attachments

保存附件元数据。

文件内容独立归档。

---

## Schema Version

统一使用：

BackupSchema.VERSION

管理备份版本。

未来修改格式：

Version++

即可完成 Migration。

---

## 导出流程

VaultEntry

↓

VaultSnapshot

↓

JSON

↓

AES-GCM

↓

Backup File

---

## 导入流程

Backup

↓

JSON

↓

VaultSnapshot

↓

VaultEntry

↓

Repository

↓

Database

整个过程与数据库结构解耦。

---

## 优点

数据库迁移不会影响备份。

未来支持：

- 多 Vault
- 跨平台
- 云同步

无需重新设计格式。

History 也可以直接复用 Snapshot。

---

## 最终决策

VaultSnapshot 是唯一导入导出模型。

不直接导出数据库。