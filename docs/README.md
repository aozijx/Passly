# Passly 文档中心

本文按“产品功能 → 平台合同 → 工程规范 → 项目记录”组织。先从对应功能总览进入，再沿链接查阅 Detail 流程和跨层规范。当前实现以代码为准；已批准但尚未实现的行为会单独标记。

## 快速开始

- [开发与构建](engineering/getting-started/development.md)
- [维护边界](engineering/development/maintenance-boundaries.md)
- [Kotlin 规范](engineering/development/kotlin-style.md)
- [测试与质量门禁](engineering/development/testing.md)
- [文档维护约定](engineering/development/documentation.md)

## 产品功能

### Vault

- [Vault 功能总览](product/vault/overview.md)
- [列表、搜索与排序](product/vault/list-and-search.md)
- [条目编辑器与写入](product/vault/entry-editor-and-persistence.md)
- [完整表单编辑](product/vault/full-form-editing.md)
- [修订历史](product/vault/entry-revision-history.md)
- [OTP 创建](product/vault/otp-entry.md)
- [OTP 运行时与复制](product/vault/otp-runtime.md)
- [OTP URI](product/vault/otp-uri.md)
- [银行卡资料](product/vault/card-entry.md)
- [卡片样式](product/vault/card-styles.md)
- [条目二维码](product/vault/qr-display.md)

### Autofill

- [Autofill 功能总览](product/autofill/overview.md)
- [共享填充流程](product/autofill/fill-flow.md)
- [Legacy Service](product/autofill/legacy-service.md)
- [Credential Manager](product/autofill/credential-manager.md)
- [保存语义](product/autofill/save-flow.md)
- [Sealed Inbox](product/autofill/sealed-inbox.md)
- [验证矩阵](product/autofill/verification.md)

### Backup

- [Backup 工作流总览](product/backup/backup-workflows.md)
- [快照、SAF 目录与目标 URI](product/backup/storage-and-permissions.md)
- [导出](product/backup/export.md)
- [操作状态、密码与页面生命周期](product/backup/operation-lifecycle.md)
- [导入规划、事务与文件恢复](product/backup/import.md)
- [Passly Archive v1 协议](product/backup/passly-archive-v1.md)
- [验证要求](product/backup/verification.md)

### Settings 与系统入口

- [设置功能总览](product/settings/overview.md)
- [通知设置](product/settings/notification-settings.md)
- [权限管理](product/settings/permission-management.md)

### Diagnostics

- [诊断日志总览](product/diagnostics/overview.md)
- [查看与管理](product/diagnostics/viewer.md)
- [导出](product/diagnostics/export.md)

### Scanner

- [二维码扫描总览](product/scanner/overview.md)
- [输入与生命周期](product/scanner/input-lifecycle.md)
- [结果交接](product/scanner/result-handoff.md)

## 平台合同

### Architecture 与跨层协作

- [架构总览](platform/architecture/overview.md)
- [层间契约与通信](platform/architecture/layer-interactions.md)
- [包与模块边界](platform/architecture/package-boundaries.md)
- [ViewModel 依赖与 owner 边界](platform/architecture/viewmodel-ownership.md)
- [运行时流程](platform/architecture/runtime-flows.md)
- [UI 宿主、导航与命名](platform/architecture/ui-shell-and-naming.md)
- [条目类型与凭据定义](platform/architecture/entry-type-registration.md)
- [错误处理](platform/architecture/error-handling.md)
- [消息中心](platform/architecture/message-center.md)
- [权限中心](platform/architecture/permission-center.md)
- [日志、错误与 Trace](platform/architecture/observability/record-and-trace.md)
- [日志运行时与加密存储](platform/architecture/observability/runtime-and-storage.md)
- [日志导出格式](platform/architecture/observability/export-formats.md)
- [错误码目录（生成）](platform/architecture/observability/error-codes.md)

### Data 与持久化

- [Data 层总览](platform/data/overview.md)
- [数据库与 Session](platform/data/database.md)
- [数据映射](platform/data/mapping.md)
- [设置存储](platform/data/settings-storage.md)
- [内容渲染](platform/data/content-rendering.md)

### Security

- [安全总览](platform/security/overview.md)
- [威胁模型](platform/security/threat-model.md)
- [认证与恢复码](platform/security/authentication.md)
- [密钥管理](platform/security/key-management.md)
- [存储加密](platform/security/storage-encryption.md)
- [Autofill 安全](platform/security/autofill.md)
- [诊断安全](platform/security/diagnostics.md)
- [敏感数据与剪贴板](platform/security/sensitive-data-access.md)
- [远程内容](platform/security/remote-content.md)
- [全仓安全扫描报告（2026-11）](platform/security/2026-11-06-repository-scan.md)

## 工程规范

- [状态管理与 ViewModel](engineering/development/architecture-and-state.md)
- [Compose 与 Material](engineering/development/compose-ui.md)
- [共享元素转场](engineering/development/shared-element-transitions.md)

## 项目记录

### 已发布决策

ADR 记录决策原因，平台/功能专题页说明当前实现。已发布 ADR 保留编号；决策变化时新增 ADR 并建立替代关系。

| ADR | 状态 | 决策 |
| --- | --- | --- |
| [0001](project/decisions/ADR-0001-project-principles.md) | Accepted | 项目原则 |
| [0002](project/decisions/ADR-0002-envelope-encryption.md) | Accepted | 信封加密 |
| [0003](project/decisions/ADR-0003-dual-dek.md) | Superseded by 0019 | 双 DEK |
| [0004](project/decisions/ADR-0004-repository-is-decryption-boundary.md) | Accepted | Repository 解密边界 |
| [0005](project/decisions/ADR-0005-autofill-layer-boundaries.md) | Superseded by 0023 | Autofill 分层边界 |
| [0006](project/decisions/ADR-0006-resolvedcandidate-dto.md) | Superseded by 0023 | ResolvedCandidate 隔离 |
| [0007](project/decisions/ADR-0007-usecase-is-optional.md) | Accepted | UseCase 可选 |
| [0008](project/decisions/ADR-0008-runtime-threat-boundary.md) | Accepted | 运行时威胁边界 |
| [0009](project/decisions/ADR-0009-no-google-tink.md) | Accepted | 暂不引入 Google Tink |
| [0010](project/decisions/ADR-0010-repository-returns-plaintext.md) | Accepted | Repository 返回明文领域模型 |
| [0011](project/decisions/ADR-0011-threat-model-drives-design.md) | Accepted | 威胁模型驱动设计 |
| [0012](project/decisions/ADR-0012-random-nonce.md) | Accepted | 随机 Nonce |
| [0013](project/decisions/ADR-0013-vault-snapshot-model.md) | Superseded：备份由 0016、历史由 0015 约束 | Vault Snapshot 聚合模型 |
| [0014](project/decisions/ADR-0014-blind-index-search.md) | Accepted | Blind Index 检索 |
| [0015](project/decisions/ADR-0015-history-snapshot-strategy.md) | Accepted | 历史快照策略 |
| [0016](project/decisions/ADR-0016-backup-format.md) | 部分由 0022 替代；加密容器决策仍有效 | 版本化加密备份 |
| [0017](project/decisions/ADR-0017-recovery-code-envelope.md) | Accepted | 恢复码独立 Envelope |
| [0018](project/decisions/ADR-0018-lookup-metadata-strategy.md) | Accepted | Metadata/Lookup/Credential 分离 |
| [0019](project/decisions/ADR-0019-single-dek-derived-session-key.md) | Accepted | 单 DEK 与派生会话密钥 |
| [0020](project/decisions/ADR-0020-unified-lifecycle-safe-authentication.md) | Accepted | 生命周期安全的统一认证系统 |
| [0022](project/decisions/ADR-0022-keyed-backup-document.md) | Accepted | 稳定键值字段备份文档 |
| [0023](project/decisions/ADR-0023-typed-autofill-interactions.md) | Accepted | 类型化 Autofill interaction |
| [0024](project/decisions/ADR-0024-structured-observability-logger.md) | Implemented | 统一结构化 Logger |
| [0025](project/decisions/ADR-0025-sealed-autofill-save-inbox.md) | Accepted, implemented | 锁定状态 Sealed Autofill Save Inbox |

### 设计、实施与验收

日期保留在索引层，文件按 `project/work/{specs,plans}` 或 `project/reviews` 保存。当前实现合同回到上方唯一主题页；计划不作为实现事实来源。

#### 2026-11 设计

- [Typed Diagnostic Events 设计](project/work/specs/2026-11-04-typed-diagnostic-events-design.md)

#### 2026-11 实施与待办

- [ViewModel/Scanner 审查 TODO](project/work/plans/2026-11-10-viewmodel-review-todo.md)
- [应用架构与并发设计 TODO](project/work/plans/2026-11-06-app-architecture-concurrency-todo.md)
- [Diagnostics 写入与 flush 可靠性跟进](project/work/plans/2026-11-06-passly-diagnostics-reliability-followup.md)
- [Scanner 相机取景框设备验收](project/work/plans/2026-11-06-scanner-entry-and-framing.md)
- [OTP 审查待办](project/work/plans/2026-11-06-passly-otp-review-followups.md)
- [Security 审查待办](project/work/plans/2026-11-06-passly-security-scan-followups.md)
- [Vault 搜索审查待办](project/work/plans/2026-11-06-passly-vault-search-review-followups.md)
- [Autofill 后续清单](project/work/plans/2026-11-07-passly-autofill-followups.md)
- [详情页密码关系设备验收](project/work/plans/2026-11-08-related-password-management.md)
- [兼容与冗余清理审计](project/work/plans/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)

#### 2026-11 审查记录

- [认证提交后维护与取消恢复审查](project/reviews/2026-11-11-authentication-post-commit-recovery.md)
- [全项目测试逻辑清理审查](project/reviews/2026-11-11-test-logic-maintenance-review.md)
- [OTP Runtime owner 与缓存失效审查](project/reviews/2026-11-11-otp-runtime-owner-and-invalidation.md)
- [通知权限未启动结果与重试审查](project/reviews/2026-11-11-notification-permission-not-started.md)
- [App Password 对话框生命周期审查](project/reviews/2026-11-11-app-password-dialog-lifecycle.md)
- [完整表单编辑 owner 与失败交接审查](project/reviews/2026-11-11-full-form-editing-boundaries.md)
- [银行卡完整表单编辑保留付款平台审查](project/reviews/2026-11-11-card-payment-platform-preservation.md)
- [Backup 目录设置失败处理审查](project/reviews/2026-11-08-backup-directory-settings-failure-feedback.md)
- [Database Reset 成功反馈计时器审查](project/reviews/2026-11-11-database-reset-feedback.md)
- [Android 系统通知网关平台边界审查](project/reviews/2026-11-11-system-notification-gateway.md)
- [设置资源归属与本地化审查](project/reviews/2026-11-08-app-settings-resource-cohesion.md)
- [文档分类与路径重组验收](project/reviews/2026-11-11-functional-docs-reorganization.md)
- [Autofill 宿主与字段交付验收](project/reviews/2026-11-08-autofill-host-validation.md)
- [兼容、弃用与冗余清理审查](project/reviews/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)
- [Domain 边界与 Entry 标签策略审查](project/reviews/2026-11-07-domain-boundary-scan.md)
- [Backup Data Port 迁移审查记录](project/reviews/2026-11-07-backup-data-port-migration.md)
- [Security 模块边界迁移审查记录](project/reviews/2026-11-07-security-boundaries-migration.md)
- [Domain 职责凝聚迁移审查记录](project/reviews/2026-11-07-domain-cohesion-migration.md)
- [Vault 卡片样式设置实施审查](project/reviews/2026-11-07-vault-card-style-settings.md)
- [Session 关闭与 DEK 擦除审查](project/reviews/2026-11-07-session-close-and-dek-wipe.md)
- [Vault Presentation 边界审查](project/reviews/2026-11-08-vault-presentation-boundaries.md)
- [硬件 Keystore 真机验收](project/reviews/2026-11-03-hardware-keystore-verification.md)

#### 2026-10 项目记录

- [深度维护 TODO](project/work/plans/2026-10-30-passly-maintainability-todo.md)
- [历史、界面与输入边界交付记录](project/reviews/2026-10-27-history-ui-delivery.md)
