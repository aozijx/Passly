# ADR-0006: 使用 ResolvedCandidate 隔离领域模型

- 状态：Accepted
- 日期：未记录

## 背景

完整 Vault 领域对象包含比一次 Autofill 响应更多的信息。

## 决策

在候选解析边界创建 `ResolvedCandidate`，只携带本次匹配和填充所需字段，ResponseFactory 不接收完整领域模型。

## 后果

减少敏感数据传播和系统 API 耦合，但需要显式转换并管理 DTO 的短生命周期。

## 备选方案

未采用把 Domain model 直接传入 Android 响应构建器。
