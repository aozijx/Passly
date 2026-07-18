# 架构决策记录

ADR 记录“为什么选择”，主题文档记录“当前如何实现”。已发布 ADR 不删除、不复用编号；决策变化时新增
ADR，并在双方状态中建立替代关系。

## 统一格式

每份 ADR 包含状态、日期、背景、决策、后果、备选方案和关联资料。旧记录没有可靠日期时写“未记录”，不猜测。

## 索引

| 编号                                                    | 状态         | 决策                            |
|-------------------------------------------------------|------------|-------------------------------|
| [0001](ADR-0001-project-principles.md)                | Accepted   | 项目原则                          |
| [0002](ADR-0002-envelope-encryption.md)               | Accepted   | 信封加密                          |
| [0003](ADR-0003-dual-dek.md)                          | Superseded | 双 DEK（由 0019 替代）              |
| [0004](ADR-0004-repository-is-decryption-boundary.md) | Accepted   | Repository 是解密边界              |
| [0005](ADR-0005-autofill-layer-boundaries.md)         | Accepted   | Autofill 分层边界                 |
| [0006](ADR-0006-resolvedcandidate-dto.md)             | Accepted   | ResolvedCandidate 隔离          |
| [0007](ADR-0007-usecase-is-optional.md)               | Accepted   | UseCase 可选                    |
| [0008](ADR-0008-runtime-threat-boundary.md)           | Accepted   | 运行时威胁边界                       |
| [0009](ADR-0009-no-google-tink.md)                    | Accepted   | 暂不引入 Google Tink              |
| [0010](ADR-0010-repository-returns-plaintext.md)      | Accepted   | Repository 返回明文领域模型           |
| [0011](ADR-0011-threat-model-drives-design.md)        | Accepted   | 威胁模型驱动设计                      |
| [0012](ADR-0012-random-nonce.md)                      | Accepted   | 随机 Nonce                      |
| [0013](ADR-0013-vault-snapshot-model.md)              | Accepted   | Vault Snapshot 聚合模型           |
| [0014](ADR-0014-blind-index-search.md)                | Accepted   | Blind Index 检索                |
| [0015](ADR-0015-history-snapshot-strategy.md)         | Accepted   | 历史快照策略                        |
| [0016](ADR-0016-backup-format.md)                     | Accepted   | 版本化加密备份                       |
| [0017](ADR-0017-recovery-code-envelope.md)            | Accepted   | 恢复码独立 Envelope                |
| [0018](ADR-0018-lookup-metadata-strategy.md)          | Accepted   | Metadata/Lookup/Credential 分离 |
| [0019](ADR-0019-single-dek-derived-session-key.md)    | Accepted   | 单 DEK 与派生会话密钥                 |
