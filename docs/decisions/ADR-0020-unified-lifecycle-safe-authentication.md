# ADR-0020: 采用生命周期安全的统一认证系统

- 状态：Accepted
- 日期：2026-07-18

## 背景

旧 Gateway、Repository、PromptLauncher 和多个 ViewModel 消息通道分别编排认证，造成 Activity 泄漏风险、取消事件
双 Toast、Autofill Host 竞态和会话状态分裂。Native Argon2id 也不能依赖协程取消立即停止。

## 决策

以纯 Kotlin `AuthenticationManager` 作为唯一入口，以弱引用 Host lease 适配各 Activity，并用专用单线程 KDF runner、
`VaultSessionController`、原子生物识别轮换 journal 和 Activity-retained 恢复码草稿实现认证。用户取消是无消息结果；
其他失败由 correlation ID 去重的统一 presenter 发布。

## 后果

调用方不再持有 Launcher、Cipher 或安全实现，Autofill 请求所有权明确，取消后 Native 结果不会提交。代价是 Host、轮换、
草稿和会话状态机需要独立故障注入测试；恢复码草稿在进程死亡后按设计失效。

## 备选方案

未继续修补旧 Gateway，也未让 Singleton 持有 Activity；未把 CryptoObject 保存到可恢复状态。

## 关联

[统一认证](../security/authentication.md)、[ADR-0017](ADR-0017-recovery-code-envelope.md)
