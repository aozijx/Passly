# ADR-0004: Repository 作为解密边界

- 状态：Accepted
- 日期：未记录

## 背景

若 DAO、ViewModel、Service 和 UI 都能自行解密，密钥传播与错误处理无法审计。

## 决策

Data Repository 与 Security 服务的协作点是持久化密文转换为明文领域模型的唯一边界。DAO 只处理
Entity/密文，Feature 不接触加密细节。

## 后果

Repository 需要承担 Mapper、会话检查和密码学错误映射；换来更小的密钥访问面与可测试契约。

## 备选方案

未采用调用方延迟解密，因为会把安全职责扩散到所有消费者。
