# ADR-00014：Blind Index 检索架构

状态：Accepted
日期：2026-07
决策者：Passly Team

---

## 背景

Passly 是一款 Zero-Knowledge、Offline First 的密码管理器。

数据库中的敏感信息（标题、用户名、密码、URL 等）均经过 AES-256-GCM 加密，不允许以明文形式建立索引。

然而应用仍然需要支持：

- 密码列表搜索
- 自动填充匹配
- URL 匹配
- App Package 匹配
- 快速定位条目

因此需要设计一种既支持检索，又尽量减少信息泄露的方案。

---

## 备选方案

### 方案一：SQLite LIKE

例如：

SELECT * FROM vault_metadata
WHERE title LIKE '%google%'

优点：

- 实现简单
- 无需额外索引

缺点：

- 必须保存明文标题
- 无法满足 Zero Knowledge
- 数据库泄露后全部可见

最终放弃。

---

### 方案二：SQLite FTS5

建立全文索引。

优点：

- 搜索性能优秀
- 支持排序

缺点：

- 需要保存大量明文 Token
- 会泄露词频
- 会泄露分词结果
- 会泄露关键词数量

对于密码管理器而言泄露信息过多。

最终放弃。

---

### 方案三：Blind Index（最终采用）

建立独立索引表。

search_index

每个可搜索字段：

normalize

↓

SearchKey

↓

HMAC-SHA256

↓

keywordHash

↓

Database

数据库仅保存固定长度 Hash。

不会保存任何明文。

---

## SearchKey

SearchKey 不直接使用 DEK。

启动时：

DEK

↓

HMAC-SHA256

↓

SearchKey

↓

Blind Index

这样未来即使调整数据库加密方式，也不会影响索引生成。

---

## 检索流程

用户输入：

Google

↓

normalize

↓

HMAC(SearchKey)

↓

keywordHash

↓

search_index

↓

entryId

↓

vault_metadata

↓

列表展示

↓

点击

↓

vault_credentials

↓

解密密码

整个过程中不会扫描所有 Blob。

---

## 自动填充

Autofill 使用同一套索引。

匹配字段包括：

- associatedDomain
- associatedAppPackage
- uri
- title
- username
- email

无需额外建立 Autofill 专用索引。

---

## 数据库结构

search_index

字段：

- indexId
- entryId
- keywordHash
- field
- weight

其中：

field

用于区分：

- title
- username
- email
- domain
- package

weight

用于搜索排序。

例如：

title > username > domain

无需解密即可完成排序。

---

## 安全性

Blind Index 不能隐藏：

- 是否存在重复关键字
- Hash 数量

但能够避免：

- 明文泄露
- 词频统计
- FTS Token 泄露

满足 Passly 对 L2/L3 威胁模型的设计目标。

---

## 最终决策

采用独立 Blind Index。

不使用 LIKE。

不使用 FTS。

统一作为：

- 密码列表搜索
- 自动填充匹配
- URL 匹配
- App Package 匹配

的唯一检索入口。