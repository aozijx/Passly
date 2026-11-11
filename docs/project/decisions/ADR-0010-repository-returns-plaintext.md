# ADR-0010: Repository 返回明文领域模型

- 状态：Accepted
- 日期：未记录

## 背景

Domain 与 Feature 若理解密文格式，就无法与 Data/Security 解耦。

## 决策

在已解锁会话内，Repository 返回完成业务所需的明文领域模型；密文 DTO 和 Entity 不越过 Data 边界。

## 后果

调用方更简单且可测试，但必须控制领域对象生命周期，避免缓存、日志和 SavedState 持久化敏感字段。

## 备选方案

未采用 Repository 返回密文并由每个消费者解密。
