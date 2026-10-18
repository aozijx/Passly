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
- [可观测性与日志](architecture/observability/README.md)

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

- [Vault](features/vault/README.md)
- [Autofill 与 Credential Manager](features/autofill/README.md)
- [Backup](features/backup/README.md)
- [诊断日志](features/diagnostics/README.md)
- [二维码扫描](features/scanner.md)
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

- [架构决策记录](decisions/README.md)
- [2026-07 代码审查](reviews/2026-07-code-review.md)（仅适用于该审查时点）

## 状态标记

| 标记 | 含义 |
| --- | --- |
| 当前实现 | 已对照当前仓库，可作为实现与排障依据 |
| 已批准，尚未实现 | 已确认目标合同，不得当作现有能力 |
| 过渡中 | 同一主题同时包含明确分隔的当前行为与批准目标 |
| ADR | 解释决策及原因；状态和 supersession 决定是否仍有效 |
| 历史审查 | 只描述指定 revision/日期的证据，不定义当前实现 |
