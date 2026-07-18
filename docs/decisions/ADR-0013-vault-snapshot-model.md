# ADR-0013: 使用 Vault Snapshot 聚合模型

- 状态：Accepted
- 日期：未记录

## 背景

历史、备份、导出和恢复都需要表达某一时点的完整条目，分别定义格式容易漂移。

## 决策

用 Vault Snapshot 语义统一聚合 metadata、credential 与相关附件。数据库 Entity、Domain model 和备份 DTO
仍由 Mapper 隔离，并各自版本化。

## 后果

流程复用更强、恢复语义一致；聚合对象可能较大，需要限制附件大小并避免在列表路径构造。

## 备选方案

未采用每个功能维护一套独立导出结构。
