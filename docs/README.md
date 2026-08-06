# Passly 文档中心

本文档以当前代码为准。历史决策保留在 ADR 中；当 ADR 与现有实现不同，应由新的 ADR 明确替代关系，而不是改写历史。

## 入门与开发

- [开发与构建](getting-started/development.md)
- [测试与质量门禁](development/testing.md)
- [Kotlin 与 Compose 风格规范](development/kotlin-compose-style.md)
- [文档维护约定](development/documentation.md)

## 架构

- [架构总览](architecture/overview.md)
- [UI 宿主、导航与命名](architecture/ui-shell-and-naming.md)
- [MVI 架构与命名](architecture/mvi.md)
- [包边界](architecture/package-boundaries.md)
- [运行时流程](architecture/runtime-flows.md)

## 数据与格式

- [数据库](data/database.md)
- [设置存储](data/settings-storage.md)
- [备份格式](data/backup-format.md)

## 安全

- [安全总览](security/overview.md)
- [认证与恢复码](security/authentication.md)
- [安全诊断](security/diagnostics.md)
- [密钥管理](security/key-management.md)
- [存储加密](security/storage-encryption.md)
- [自动填充安全](security/autofill.md)
- [敏感数据读取与剪贴板](security/sensitive-data-access.md)
- [威胁模型](security/threat-model.md)

## 功能

- [权限与消息](features/permissions-and-messages.md)
- [Credential Manager Provider](features/credential-manager.md)

## 决策与审查

- [架构决策记录](decisions/README.md)
- [2026-07 代码审查](reviews/2026-07-code-review.md)

## 文档状态

| 标记   | 含义                  |
|------|---------------------|
| 当前实现 | 已从仓库代码核对，可作为开发依据    |
| 决策   | 说明长期约束，具体状态以 ADR 为准 |
| 待办   | 已发现但尚未实现，不应写成现有能力   |
