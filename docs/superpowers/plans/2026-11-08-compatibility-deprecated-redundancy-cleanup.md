# 兼容与冗余清理审计

本文只跟踪尚未完成的全仓声明与重复规则审计。已完成清理及留存合同见[审计记录](../../reviews/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)；当前 Backup 边界见[工作流总览](../../features/backup/backup-workflows.md)和[验证要求](../../features/backup/verification.md)。

## Task 1：完成全仓未引用声明与重复规则审计

- [ ] 按生产 owner 检索各模块 Kotlin/Java 未引用声明、重复策略及测试专用生产接口；对每个候选闭合生产调用、DI/注册、持久化或协议、测试和正式文档证据。只将已证明冗余的候选移除；有效合同或缺少证据的候选记为具体后续项，不按搜索命中直接删除。
- [ ] 更新对应正式主题文档和本审查记录，移除已完成事项；将本任务改成仅包含剩余候选，或在审计闭合后删除本计划并修复索引。

范围包括 `app`、`domain`、`data`、`security`、`runtime`、`core` 和 `buildSrc` 当前生产源集。既有工作区改动需先排除；本计划不包含构建或测试验收，除非用户另行要求。
