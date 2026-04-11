# Passly 架构决策记录（ADR Lite）

本文档用于记录"长期有效、跨模块影响较大"的技术决策。

适用场景：

- 你准备修改加密、数据库、备份、Autofill、依赖注入等核心路径
- 你发现团队对同一问题反复讨论、结论不稳定
- 你需要给后续维护者留下"为什么这么做"的依据

---

## 1. 使用规则

### 1.0 ADR 索引与生命周期

当前 ADR 范围：`ADR-001` 至 `ADR-008`。

状态流转建议：`Proposed -> Accepted -> Superseded`。

- `Proposed`：可进入评审，但不作为强约束。
- `Accepted`：默认执行基线，改动需说明偏离原因。
- `Superseded`：保留历史上下文，并指向替代 ADR。

当某项改动触发 ADR 变化时，请同步更新：

1. 本文档对应 ADR 状态与内容。
2. `operations/CHANGE_PLAYBOOK.md` 中相应执行章节。

### 1.1 条目结构

每条 ADR 使用以下字段：

- `Status`：`Accepted` / `Superseded` / `Proposed`
- `Date`：YYYY-MM-DD
- `Context`：问题背景
- `Decision`：最终技术决策
- `Consequences`：正负影响
- `Revisit Trigger`：何时应重新评估
- `Related`：相关文件路径

### 1.2 状态说明

- `Accepted`：当前生效，默认遵循
- `Superseded`：被新决策替代，保留历史
- `Proposed`：讨论中，尚未落地

### 1.3 维护约定

1. 涉及高风险改动的 PR，建议在描述中引用 ADR 编号。
2. 若改动与已接受 ADR 冲突，先更新 ADR 再合并代码。
3. ADR 先求"清晰可执行"，不追求论文式完整。

---

## ADR-001 离线优先与零信任边界

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

Passly 的产品定位是本地隐私保险库，核心价值来自"用户数据不离开设备"。

### Decision

默认不引入联网同步能力；任何网络相关能力必须是非敏感且可关闭的辅助能力。

### Consequences

- 正向：隐私边界清晰，风险面显著降低。
- 代价：跨设备同步能力受限，需依赖加密备份迁移。

### Revisit Trigger

- 出现明确的多端同步产品需求，且能证明不破坏隐私边界。

### Related

- `README.md`
- `core/backup/BackupManager.kt`

---

## ADR-002 手写 AppContainer 作为依赖注入方案

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

项目规模中等，模块边界清晰，团队偏好可追踪、低魔法的依赖构建方式。

### Decision

使用手写 `AppContainer` 管理核心依赖，不强制引入 Hilt/Dagger。

### Consequences

- 正向：依赖关系可读性高，调试成本低。
- 代价：容器文件随模块增长会变长，需要定期整理分组。

### Revisit Trigger

- 依赖图复杂度显著提升，手工维护成本超过收益。

### Related

- `core/di/AppContainer.kt`

---

## ADR-003 数据层采用 Room + SQLCipher

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

应用存储敏感数据，需兼顾结构化查询能力与数据库级安全。

### Decision

持久化层使用 Room，底层启用 SQLCipher，结合字段级加密策略。

### Consequences

- 正向：查询能力与安全性兼顾。
- 代价：迁移与备份兼容成本更高，测试需要覆盖升级路径。

### Revisit Trigger

- 数据规模/查询模式变化导致当前方案瓶颈明显。

### Related

- `data/local/AppDatabase.kt`
- `core/crypto/CryptoManager.kt`

---

## ADR-004 数据库变更必须提供 Migration

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

用户数据为核心资产，破坏性迁移会造成不可逆损失。

### Decision

数据库版本升级时必须提供可执行 migration，不依赖 destructive 方式覆盖历史数据。

### Consequences

- 正向：升级稳定性更高，用户数据安全。
- 代价：开发成本增加，需要额外回归验证。

### Revisit Trigger

- 仅在明确不保留历史数据的实验分支可临时放宽。

### Related

- `data/local/AppDatabase.kt`
- `operations/CHANGE_PLAYBOOK.md`

---

## ADR-005 备份格式采用版本化兼容策略

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

备份文件承担跨设备迁移与灾难恢复职责，需要长期可读。

### Decision

备份导入导出采用版本字段与兼容解析逻辑；新版本不得无条件拒绝旧版本备份。

### Consequences

- 正向：跨版本恢复能力增强。
- 代价：导入代码复杂度增加，需持续维护兼容分支。

### Revisit Trigger

- 出现不可维护的历史负担时，可设计"分阶段废弃"策略。

### Related

- `core/backup/BackupManager.kt`
- `operations/CHANGE_PLAYBOOK.md`

---

## ADR-006 卡片样式参数集中到样式 Token

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

卡片样式散落在多个组件，导致改圆角/间距/背景时容易漏改。

### Decision

统一使用 `VaultCardStyleTokens` 管理可调样式参数；组件仅消费 token。

### Consequences

- 正向：样式改动集中、风险可控、设置预览更易对齐。
- 代价：初期需要一次性替换硬编码值。

### Revisit Trigger

- 若出现跨平台设计系统需求，可上提到更通用的 design token 层。

### Related

- `core/common/ui/VaultCardStyleTokens.kt`
- `features/vault/components/entries/VaultCardStyleRegistry.kt`

---

## ADR-007 设置页预览复用 Vault 真实渲染链

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

设置页单独手写预览 UI 会造成与真实列表样式漂移。

### Decision

设置页通过 `VaultCardStyleRegistry` 复用真实卡片渲染分支，避免重复实现。

### Consequences

- 正向：预览一致性更高，维护成本更低。
- 代价：预览态可能需要额外 mock/适配参数。

### Revisit Trigger

- 若设置页需要高度抽象化预览（非真实渲染），再评估拆分。

### Related

- `features/settings/components/CardStyleSettingsSection.kt`
- `features/vault/components/entries/VaultCardStyleRegistry.kt`

---

## ADR-008 Autofill 规则在 Engine 层，Presentation 仅负责展示

- **Status**: Accepted
- **Date**: 2026-04-05

### Context

Autofill 若将匹配逻辑与展示混写，容易造成维护困难与隐私边界模糊。

### Decision

匹配/排序/筛选规则集中在 `engine`，`presentation` 仅做 RemoteViews 展示组装。

### Consequences

- 正向：职责清晰，性能与隐私优化可独立推进。
- 代价：跨层调试需要更清晰日志。

### Revisit Trigger

- 若 Android 平台 API 变化导致调用链重构。

### Related

- `service/autofill/engine/`
- `service/autofill/presentation/`

---

## 2. 新增 ADR 模板（复制用）

```markdown
## ADR-XXX 标题

- **Status**: Proposed
- **Date**: YYYY-MM-DD

### Context

...

### Decision

...

### Consequences

- 正向：...
- 代价：...

### Revisit Trigger

...

### Related

- path/to/file
```
