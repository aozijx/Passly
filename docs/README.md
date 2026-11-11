# Passly 文档中心

文档以当前代码为准。按开发流程阅读：先接入项目并理解边界，再查阅实现合同，按功能主题开发，最后验证并归档交付记录。已批准但尚未实现的行为必须明确标注，不能混入当前流程。

## 1. 开发准备

- [开发与构建](getting-started/development.md)
- [维护边界](development/maintenance-boundaries.md)
- [文档维护约定](development/documentation.md)

## 2. 理解系统合同

### 架构与平台边界

- [架构总览](architecture/overview.md)
- [条目类型与凭据注册](architecture/entry-type-registration.md)
- [包与模块边界](architecture/package-boundaries.md)
- [ViewModel 依赖与 owner 边界](architecture/viewmodel-ownership.md)
- [运行时流程](architecture/runtime-flows.md)
- [UI 宿主、导航与命名](architecture/ui-shell-and-naming.md)
- [错误处理](architecture/error-handling.md)
- [消息中心](architecture/message-center.md)
- [权限中心](architecture/permission-center.md)
- [日志、错误与 Trace](architecture/observability/record-and-trace.md)
- [日志运行时与加密存储](architecture/observability/runtime-and-storage.md)
- [日志导出格式](architecture/observability/export-formats.md)
- [错误码目录（生成）](architecture/observability/error-codes.md)

### 数据与安全

- [数据库](data/database.md)
- [设置存储](data/settings-storage.md)
- [数据映射](data/mapping.md)
- [内容渲染](data/content-rendering.md)
- [安全总览](security/overview.md)
- [威胁模型](security/threat-model.md)
- [认证与恢复码](security/authentication.md)
- [密钥管理](security/key-management.md)
- [存储加密](security/storage-encryption.md)
- [Autofill 安全](security/autofill.md)
- [诊断安全](security/diagnostics.md)
- [敏感数据与剪贴板](security/sensitive-data-access.md)
- [远程内容](security/remote-content.md)

#### 2026-11 安全扫描

- [全仓安全扫描报告](security/2026-11-06-repository-scan.md)

### 已发布决策

ADR 说明决策原因，主题页说明当前实现。已发布 ADR 保留原编号；决策变化时新增 ADR 并建立替代关系。

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
| [0025](decisions/ADR-0025-sealed-autofill-save-inbox.md) | Accepted, implemented | 锁定状态 Sealed Autofill Save Inbox |

## 3. 实现功能主题

### Vault

- [列表与搜索](features/vault/list-and-search.md)
- [修订历史](features/vault/entry-revision-history.md)
- [编辑与写入](features/vault/entry-editor-and-persistence.md)
- [完整表单编辑](features/vault/full-form-editing.md)
- [OTP 创建](features/vault/otp-entry.md)
- [OTP 运行时与复制](features/vault/otp-runtime.md)
- [OTP URI](features/vault/otp-uri.md)
- [二维码](features/vault/qr-display.md)
- [银行卡](features/vault/card-entry.md)
- [卡片样式设置与解析](features/vault/card-styles.md)

### Autofill

- [共享流程](features/autofill/fill-flow.md)
- [Legacy Service](features/autofill/legacy-service.md)
- [Credential Manager](features/autofill/credential-manager.md)
- [保存语义](features/autofill/save-flow.md)
- [Sealed Inbox](features/autofill/sealed-inbox.md)
- [验证矩阵](features/autofill/verification.md)

### Backup

- [Backup 工作流总览](features/backup/backup-workflows.md)
- [Backup 快照、SAF 目录与目标 URI](features/backup/storage-and-permissions.md)
- [Backup 导出](features/backup/export.md)
- [Backup 操作状态、密码与页面生命周期](features/backup/operation-lifecycle.md)
- [Backup 导入规划、事务与文件恢复](features/backup/import.md)
- [Passly Archive v1 协议](features/backup/passly-archive-v1.md)
- [验证要求](features/backup/verification.md)

### 设置与系统集成

- [通知设置](features/settings/notification-settings.md)
- [权限管理](features/settings/permission-management.md)

### 系统工具
#### 诊断日志

- [总览](features/diagnostics/overview.md)
- [查看器](features/diagnostics/viewer.md)
- [导出流程](features/diagnostics/export.md)

#### 二维码扫描

- [总览](features/scanner/overview.md)
- [输入生命周期](features/scanner/input-lifecycle.md)
- [结果交接](features/scanner/result-handoff.md)

## 4. 验证、维护与交付

### 开发约定与质量门禁

- [Kotlin 规范](development/kotlin-style.md)
- [Compose 与 Material](development/compose-ui.md)
- [状态管理](development/architecture-and-state.md)
- [测试与质量门禁](development/testing.md)
- [共享元素转场](development/shared-element-transitions.md)

### 活动设计与待办

活动文档按 `YYYY-MM` 分组；每项开放工作只在一个计划中维护。当前实现合同留在上方的主题页，完成后从活动计划移除；仅需长期保留的验收依据归入审查记录。

#### 2026-11

##### 设计

- [Typed Diagnostic Events 设计](superpowers/specs/2026-11-04-typed-diagnostic-events-design.md)
- [Scanner 入口与 Framing 设计](superpowers/specs/2026-11-06-scanner-entry-and-framing-design.md)

##### 实施与待办

- [应用架构与并发设计 TODO](superpowers/plans/2026-11-06-app-architecture-concurrency-todo.md)
- [Diagnostics 写入与 flush 可靠性跟进](superpowers/plans/2026-11-06-passly-diagnostics-reliability-followup.md)
- [Scanner 入口与 Framing 实施计划](superpowers/plans/2026-11-06-scanner-entry-and-framing.md)
- [OTP 审查待办](superpowers/plans/2026-11-06-passly-otp-review-followups.md)
- [Security 审查待办](superpowers/plans/2026-11-06-passly-security-scan-followups.md)
- [Vault 搜索审查待办](superpowers/plans/2026-11-06-passly-vault-search-review-followups.md)
- [Autofill 后续清单](superpowers/plans/2026-11-07-passly-autofill-followups.md)
- [Vault App Picker 状态切换](superpowers/plans/2026-11-08-vault-app-picker-state-cutover.md)
- [详情页密码关系设备验收](superpowers/plans/2026-11-08-related-password-management.md)
- [兼容与冗余清理审计](superpowers/plans/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)

##### 验收记录

- [Autofill 宿主与字段交付验收](reviews/2026-11-08-autofill-host-validation.md)
- [兼容、弃用与冗余清理审查](reviews/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)
- [硬件 Keystore 真机验收](reviews/2026-11-03-hardware-keystore-verification.md)
- [Domain 边界与 Entry 标签策略审查](reviews/2026-11-07-domain-boundary-scan.md)
- [Backup Data Port 迁移审查记录](reviews/2026-11-07-backup-data-port-migration.md)
- [Security 模块边界迁移审查记录](reviews/2026-11-07-security-boundaries-migration.md)
- [Domain 职责凝聚迁移审查记录](reviews/2026-11-07-domain-cohesion-migration.md)
- [Vault 卡片样式设置实施审查](reviews/2026-11-07-vault-card-style-settings.md)
- [Session 关闭与 DEK 擦除审查](reviews/2026-11-07-session-close-and-dek-wipe.md)
- [Vault Presentation 边界审查](reviews/2026-11-08-vault-presentation-boundaries.md)

#### 2026-10

- [深度维护 TODO](superpowers/plans/2026-10-30-passly-maintainability-todo.md)
- [历史、界面与输入边界交付记录](reviews/2026-10-27-history-ui-delivery.md)
