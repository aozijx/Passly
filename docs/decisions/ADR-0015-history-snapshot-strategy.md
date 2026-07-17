# ADR-0015：History Snapshot 设计

状态：Accepted
日期：2026-07

---

## 背景

最初 History 使用 Diff。

例如：

password

oldValue

newValue

fieldName

changeType

这种方案类似 Git Diff。

随着字段越来越多：

- Login
- Passkey
- SSH
- Payment
- CustomField
- Attachment

Diff 维护成本越来越高。

恢复任意版本需要重放所有历史记录。

复杂度高。

---

## 决策

History 改为完整 Snapshot。

每一次修改：

VaultEntry

↓

VaultSnapshot

↓

AES-GCM

↓

snapshotBlob

↓

vault_history

History 不再关心哪些字段发生变化。

只保存完整快照。

---

## 恢复流程

History

↓

snapshotBlob

↓

解密

↓

VaultSnapshot

↓

VaultEntry

↓

覆盖当前版本

恢复过程无需计算 Diff。

---

## 数据结构

vault_history

包含：

- historyId
- entryId
- revision
- snapshotBlob
- createdAt

snapshotBlob 为完整加密快照。

---

## 优点

恢复速度快。

恢复逻辑简单。

无需维护字段差异。

新增字段无需修改 History。

天然支持未来：

- Passkey
- Attachment
- 新字段

---

## Activity

查看历史与操作日志分离。

History：

记录数据版本。

VaultActivity：

记录：

- Copy Password
- Autofill
- Create
- Update
- Delete
- Restore

两者职责独立。

---

## 最终决策

History 使用 Snapshot。

不再维护 Diff。