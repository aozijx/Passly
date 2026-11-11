# ADR-0013: 使用 Vault Snapshot 聚合模型

- 状态：Superseded（备份部分由 ADR-0016 替代；历史部分由 ADR-0015 约束）
- 日期：未记录

## 背景

历史曾计划与备份共用 Vault Snapshot，但两者的兼容周期和内容边界不同。

## 决策

历史记录可以使用内部 Entry Snapshot；长期备份使用独立、冻结的 v1 wire model，
不再复用历史 Snapshot、Room Entity 或数据库 Payload。

## 后果

避免历史模型演进无意破坏备份。两者允许通过 Mapper 共享业务转换，但不共享序列化协议。

## 备选方案

未采用每个功能维护一套独立导出结构。
