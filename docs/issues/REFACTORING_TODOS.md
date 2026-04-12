# Passly 项目待解决事项（精简版）

本文档仅保留**当前未完成**的重构与治理任务。  
已完成事项请查看：`docs/issues/completed.md`

**最后更新**: 2026-04-12

---

## 优先级定义

- `P0`：安全与数据完整性风险，优先最高。
- `P1`：核心架构持续重构项。
- `P2`：质量体系与工程化治理项。

---

## P0（必须优先）

### P0-03 明文备份导出风险治理（SAF 化）

- **现状**：紧急明文导出仍使用公共下载目录。
- **证据**：`app/src/main/java/com/aozijx/passly/core/backup/EmergencyBackupExporter.kt:51`
- **目标**：
  - 改为 SAF 用户显式选目录。
  - 强化导出前风险提示（明文泄露风险）。
  - 导出后提供最小化暴露指引（加密转存/删除建议）。
- **验收标准**：不再写入 `Environment.DIRECTORY_DOWNLOADS`。

### P0-04 数据库迁移与备份兼容自动化测试

- **现状**：核心链路无自动化保障。
- **证据**：`app/src/test` 与 `app/src/androidTest` 仍为示例测试。
- **目标**：
  - Room Migration 测试：覆盖 1->2、2->3、全链路升级。
  - Backup round-trip 测试：导出->导入一致性、错误密码、损坏文件。
- **验收标准**：CI 能自动执行并稳定通过核心迁移/备份测试。

---

## P1（架构后续）

### P1-08 VaultViewModel 第二阶段拆分（延续已完成第一阶段）

- **现状**：第一阶段已完成（见 `completed.md`），但 ViewModel 仍承载较多编排逻辑。
- **证据**：`app/src/main/java/com/aozijx/passly/features/vault/VaultViewModel.kt`
- **目标**：
  - 继续下沉可复用业务逻辑到 domain/usecase 层。
  - 收敛 UI 层可变状态，进一步统一事件驱动更新。
- **验收标准**：ViewModel 复杂度持续下降，跨模块逻辑边界更清晰。

---

## P2（质量与工程化）

### P2-01 单元/集成测试基线建设

- **现状**：核心模块测试覆盖不足。
- **建议优先顺序**：
  1. `features/main/internal`（自动锁、校验、DB 初始化流程）
  2. `features/vault/internal`（搜索过滤、详情协调状态）
  3. 备份与迁移集成测试
- **验收标准**：新增测试可覆盖关键分支与失败场景。

### P2-02 CI 质量门禁补全

- **现状**：当前主要依赖 CodeQL，缺少常规编译/测试门禁。
- **证据**：`.github/workflows/codeql.yml`
- **目标**：
  - 新增常规 CI：`compileVaultDebugKotlin`、`compileFullDebugKotlin`、unit tests。
  - 可选 nightly：instrumentation + migration + backup 回归。
- **验收标准**：PR 具备基础质量门禁后方可合并。

### P2-03 文案与错误提示统一（国际化）

- **现状**：多处 `Toast` 文案仍分散硬编码。
- **目标**：
  - 将关键业务提示收敛到资源文件（`strings.xml`）。
  - ViewModel 使用统一消息模型（资源 ID + 参数）而非直接拼接文本。
- **验收标准**：核心提示路径不再散落硬编码字符串。

### P2-04 Full/Main 双轨收敛治理

- **现状**：full 与 main 仍存在重复实现与长期分叉风险。
- **目标**：
  - 明确“共享逻辑”与“变体特有逻辑”边界。
  - 持续下沉可复用逻辑到 `main`，压缩 full 专属代码。
- **验收标准**：重复实现减少，变体差异可枚举、可维护。

---

## 执行建议（下一周期）

1. 先完成 `P0-03` 与 `P0-04`（安全与数据优先）。
2. 并行推进 `P2-02`（CI 门禁）与 `P2-01`（测试基线）。
3. 其后安排 `P2-03` / `P2-04`，最后做 `P1-08` 的第二阶段深拆。

---

## 任务模板

```markdown
### [ID] 标题
- 优先级：P0/P1/P2
- 位置：`path/to/file.kt:line`
- 问题：
- 目标：
- 验收标准：
- 状态：todo/doing/done
```
