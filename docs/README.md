# Passly 文档中心

文档以当前代码为准。平台行为同时核对 Android 官方文档；历史决策保留在 ADR 中。已批准但尚未实现的行为必须
明确标注，不能混入当前流程。

## 入门

- [开发与构建](getting-started/development.md)

## 架构

- [架构总览](architecture/overview.md)
- [包与模块边界](architecture/package-boundaries.md)
- [运行时流程](architecture/runtime-flows.md)
- [UI 宿主、导航与命名](architecture/ui-shell-and-naming.md)
- [错误处理](architecture/error-handling.md)
- [消息中心](architecture/message-center.md)
- [权限中心](architecture/permission-center.md)
- [日志记录、错误与 Trace](architecture/observability/record-and-trace.md)
- [日志运行时与加密存储](architecture/observability/runtime-and-storage.md)
- [日志导出格式](architecture/observability/export-formats.md)

## 数据

- [数据库](data/database.md)
- [设置存储](data/settings-storage.md)
- [数据映射](data/mapping.md)
- [内容渲染](data/content-rendering.md)

## 安全

- [安全总览](security/overview.md)
- [认证与恢复码](security/authentication.md)
- [密钥管理](security/key-management.md)
- [存储加密](security/storage-encryption.md)
- [Autofill 安全](security/autofill.md)
- [诊断安全](security/diagnostics.md)
- [敏感数据读取与剪贴板](security/sensitive-data-access.md)
- [远程内容](security/remote-content.md)
- [威胁模型](security/threat-model.md)

## 功能

- Vault：[列表与搜索](features/vault/list-and-search.md)、[修订历史](features/vault/entry-revision-history.md)、[编辑与写入架构（已批准，尚未实现）](features/vault/entry-editor-and-persistence.md)
- Autofill：[共享填充流程](features/autofill/fill-flow.md)、[Legacy Service](features/autofill/legacy-service.md)、[Credential Manager](features/autofill/credential-manager.md)、[保存语义](features/autofill/save-flow.md)、[Sealed Inbox（已批准，尚未实现）](features/autofill/sealed-inbox.md)、[验证矩阵](features/autofill/verification.md)
- Backup：[导入与导出](features/backup/import-export.md)、[归档格式](features/backup/archive-format.md)、[验证要求](features/backup/verification.md)
- [诊断日志查看与导出](features/diagnostics/viewer-and-export.md)
- [二维码扫描](features/scanner.md)
- [Wi-Fi 与 OTP 二维码展示](features/vault/qr-display.md)
- [动态口令 URI 导入与导出](features/vault/otp-uri.md)
- [动态口令创建](features/vault/otp-entry.md)
- [银行卡创建与有效期](features/vault/card-entry.md)
- [权限与消息](features/permissions-and-messages.md)

## 开发规范

- [Kotlin 规范](development/kotlin-style.md)
- [Compose 与 Material](development/compose-ui.md)
- [Presentation 状态管理](development/architecture-and-state.md)
- [测试与质量门禁](development/testing.md)
- [长期维护边界](development/maintenance-boundaries.md)
- [共享元素转场](development/shared-element-transitions.md)
- [文档维护约定](development/documentation.md)

## 决策与历史审查

- 架构决策记录见下表；决策解释“为什么”，主题文档解释“当前如何实现”。已发布 ADR 不删除、不复用编号，变化时新增 ADR 并建立替代关系。
- [2026-07 代码审查](reviews/2026-07-code-review.md)（仅适用于该审查时点）

| ADR | 状态 | 决策 |
| --- | --- | --- |
| [0001](decisions/ADR-0001-project-principles.md) | Accepted | 项目原则 |
| [0002](decisions/ADR-0002-envelope-encryption.md) | Accepted | 信封加密 |
| [0003](decisions/ADR-0003-dual-dek.md) | Superseded by 0019 | 双 DEK |
| [0004](decisions/ADR-0004-repository-is-decryption-boundary.md) | Accepted | Repository 解密边界 |
| [0005](decisions/ADR-0005-autofill-layer-boundaries.md) | Superseded by 0023 | Autofill 分层边界 |
| [0006](decisions/ADR-0006-resolvedcandidate-dto.md) | Superseded by 0023 | ResolvedCandidate 隔离 |
| [0007](decisions/ADR-0007-usecase-is-optional.md) | Accepted | UseCase 可选 |
| [0008](decisions/ADR-0008-runtime-threat-boundary.md) | Accepted | 运行时威胁边界 |
| [0009](decisions/ADR-0009-no-google-tink.md) | Accepted | 暂不引入 Google Tink |
| [0010](decisions/ADR-0010-repository-returns-plaintext.md) | Accepted | Repository 返回明文领域模型 |
| [0011](decisions/ADR-0011-threat-model-drives-design.md) | Accepted | 威胁模型驱动设计 |
| [0012](decisions/ADR-0012-random-nonce.md) | Accepted | 随机 Nonce |
| [0013](decisions/ADR-0013-vault-snapshot-model.md) | Superseded：备份由 0016、历史由 0015 约束 | Vault Snapshot 聚合模型 |
| [0014](decisions/ADR-0014-blind-index-search.md) | Accepted | Blind Index 检索 |
| [0015](decisions/ADR-0015-history-snapshot-strategy.md) | Accepted | 历史快照策略 |
| [0016](decisions/ADR-0016-backup-format.md) | 部分由 0022 替代；加密容器决策仍有效 | 版本化加密备份 |
| [0017](decisions/ADR-0017-recovery-code-envelope.md) | Accepted | 恢复码独立 Envelope |
| [0018](decisions/ADR-0018-lookup-metadata-strategy.md) | Accepted | Metadata/Lookup/Credential 分离 |
| [0019](decisions/ADR-0019-single-dek-derived-session-key.md) | Accepted | 单 DEK 与派生会话密钥 |
| [0020](decisions/ADR-0020-unified-lifecycle-safe-authentication.md) | Accepted | 生命周期安全的统一认证系统 |
| [0022](decisions/ADR-0022-keyed-backup-document.md) | Accepted | 稳定键值字段备份文档 |
| [0023](decisions/ADR-0023-typed-autofill-interactions.md) | Accepted | 类型化 Autofill interaction |
| [0024](decisions/ADR-0024-structured-observability-logger.md) | Implemented | 统一结构化 Logger |
| [0025](decisions/ADR-0025-sealed-autofill-save-inbox.md) | Accepted, pending | 锁定状态 Sealed Autofill Save Inbox |

## 状态标记

| 标记 | 含义 |
| --- | --- |
| 当前实现 | 已对照当前仓库，可作为实现与排障依据 |
| 已批准，尚未实现 | 已确认目标合同，不得当作现有能力 |
| 过渡中 | 同一主题同时包含明确分隔的当前行为与批准目标 |
| ADR | 解释决策及原因；状态和 supersession 决定是否仍有效 |
| 历史审查 | 只描述指定 revision/日期的证据，不定义当前实现 |
