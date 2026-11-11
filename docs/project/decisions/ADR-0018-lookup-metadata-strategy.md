# ADR-0018: 分离 Metadata、Lookup 与 Credential

- 状态：Accepted
- 日期：未记录

## 背景

列表、搜索和详情具有不同性能与敏感度；把所有字段放在单表会迫使列表读取大密文，并诱导明文搜索列。

## 决策

`vault_metadata` 保存列表元数据，`vault_credentials` 保存加密业务负载，`lookup_index` 保存 Blind
Index。Repository 按用例聚合，Domain 不感知三表结构。

## 后果

列表和搜索更轻量，敏感字段边界更清晰；写入需要事务维护三者一致性，删除必须清理关联索引和附件。

## 备选方案

未采用单一宽表与 Feature 直接拼接多 DAO。
