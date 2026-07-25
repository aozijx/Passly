# ADR-0005: Autofill Pipeline 分层边界

- 状态：Accepted
- 日期：未记录

## 背景

Autofill 同时涉及 Android Service、候选检索、Vault 解锁和系统响应构建，容易形成跨层大类。

## 决策

系统入口只解析请求并编排；`CandidateResolver` 依赖 Domain Repository 契约；Data
负责检索和解密；ResponseFactory 只消费最小候选 DTO。

## 后果

Android 类型留在系统/Feature 边界，候选解析可单测，Service 不依赖 Entity/DAO。

## 备选方案

未采用 Service 直接查询 Room 并解密的快捷实现。
