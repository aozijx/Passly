# ADR-0005: Autofill Pipeline 分层边界

- 状态：Accepted
- 日期：未记录

## 背景

Autofill 同时涉及 Android Service、候选检索、Vault 解锁和系统响应构建，容易形成跨层大类。

## 决策

两个系统入口只解析请求并编排；`FillRequestDispatcher` 原子读取当前自动填充设置；
`CandidateResolver` 依赖 Domain Repository 契约；Data 先通过 Blind Index 获取 ID，
只解密命中的条目并进行精确关联校验；ResponseFactory 只消费最小候选 DTO。

条目 ID 在整个链路中保持字符串 UUID。Credential Manager 查询阶段只创建候选元数据，
完成阶段必须使用被选择的 ID 重新查询、重新校验调用方，并根据策略认证。

## 后果

Android 类型留在系统/Feature 边界，候选解析可单测，Service 不依赖 Entity/DAO。
传统 Autofill 与 Credential Manager 可以在 Android 14+ 同时启用；Passkey 在完整实现前不发布。

## 备选方案

未采用 Service 直接查询 Room 并解密的快捷实现。
