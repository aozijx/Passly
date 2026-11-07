# Domain 内聚与官方规范对齐

状态：已实施，本文保留为设计背景；最终迁移与验证记录见[Domain 职责凝聚迁移审查](../../reviews/2026-11-07-domain-cohesion-migration.md)。

## 意图

按真实业务所有权收敛 `:domain` 的跨业务契约，并让 Passly 正式规范准确区分 Android/Kotlin 官方建议与项目自身的规则。目标是减少可避免的依赖方向和调用方负担，同时保留认证、持久化、备份及安全会话的必要语义边界。

## 设计启动时证据（2026-11-07）

- `:domain` 是单一 JVM library，包含 `access`、`autofill`、`backup`、`clipboard`、`entry`、`sensitive`、`settings` 七个顶层业务包；生产源码约 170 个 Kotlin 文件。文件数是审查清单，不是拆分依据。
- 当时 Domain 内部有双向交叉合同：`access` 与 `entry` 通过授权范围、凭据访问端口和 `AuthorizationPermit` 相互引用；`entry` 与 `autofill` 通过 `EntryCreation.createFromAutofill(..., AutofillInboxDraftId)` 相互引用；`access` 的 `AutofillInboxRecordStore` 又引用 Autofill 模型。后两项已由本设计推进迁移，当前合同见上方迁移审查记录。
- `EntryCreation` 的普通入口供 Vault/扫码流程使用；Autofill 使用其专用入口。应用实现额外处理 Autofill 会话访问、Inbox 稳定身份和幂等持久化。Domain 已另有面向 Autofill 的 `AutofillEntryWriter` 持久化端口。
- `backup` 依赖 `entry` 模型表达快照，现有工作区已经有未提交的 Backup Data port 变更与设计/计划。本任务必须把它们当作当前工作保留，不重写或覆盖；`backup → entry` 暂时视为有明确业务理由的候选保留边。
- 其余已查明的边包括：`settings → entry`（`LibraryViewSettings` 持有 Entry 排序和层级查询模式）、`autofill → entry/access/settings`（候选/条目身份、认证失败结果和 Autofill 展示策略）、`entry/access → sensitive`（共享的敏感字段标识/值类型）。这些都需按类型用途判定，不以静态图中的边本身作为问题。
- `SourceBoundaryPolicy` 已有按源码路径、禁止前缀与精确类型定义层边界的机制；`SourceBoundaryVerifier` 会检查 import 与未导入的完全限定名，并保留字符串模板 `${...}` 表达式扫描。现有测试已覆盖点号续行、转义包段、注释/字符串 decoy、raw string、嵌套模板和转义 `$`，因此 Domain 内部禁边可用窄规则扩展，不需要另写解析器。
- `package-boundaries.md` 和 `maintenance-boundaries.md` 已表达若干内聚、依赖倒置和不机械添加 UseCase 的本地规则；`kotlin-style.md` 提到遵循官方风格，但没有列出直接官方依据。当前常量位置细则属于 Passly 本地约定。
- 当前工作区还包含 Autofill 设置、架构 verifier、Backup、扫描器、详情页等未提交修改；实现前须重新检查工作区并避开无关改动。

已确认边的初步判定如下，实施前仍须以实现和消费者闭包复核：

| 跨包边 | 初步决定 | 证据/判定理由 |
| --- | --- | --- |
| `access ↔ entry` | 保留 | 授权范围精确绑定 `EntryId` / `SensitiveFieldKey`；Entry 敏感读取/修订端口接收 opaque `AuthorizationPermit`。不能用通用 ID 或复制敏感键削弱授权合同。 |
| `entry → autofill` | 调整 | `EntryCreation` 只有 Autofill 专用入口引用 `AutofillInboxDraftId`；把入口移到 Autofill 能力合同后，Entry 基础创建端口不再依赖 Autofill。 |
| `access → autofill` | 候选迁移 | `AutofillInboxRecordStore` 定义队列入队、读取、移除和隔离，并直接使用 Autofill record/id 模型；确认 Security/Data 消费边界后归到 `domain.autofill.port`。 |
| `autofill → entry` | 保留 | Autofill 候选、凭据和写入能力处理 Entry ID、Entry 模型及查询投影。 |
| `autofill → access` | 保留 | Autofill interaction 的失败、取消语义使用认证结果合同。 |
| `autofill → settings` | 保留 | Autofill 流程读取用于传统 Autofill 输出的偏好模型。 |
| `settings → entry` | 保留 | Vault 列表偏好显式包含 `EntrySort` 和 `EntryHierarchyDisplayMode`，并直接驱动 Entry 查询/DataStore 映射。 |
| `backup → entry` | 保留 | Backup 快照定义就是 Entry、EntryLink 与附件的持久化中立表示。 |
| `entry/access → sensitive` | 保留 | 两个领域边界共同使用敏感字段 key/value 契约；需继续确认无平台/加密实现泄漏。 |

## 目标

1. 逐条审查 `:domain` 七个顶层包之间的生产依赖。对每条跨包边追踪模型/端口的定义、实现、调用者、测试、DI 与持久化影响，标记为保留、调整或延期，并写出理由。
2. 移除有证据表明所有权不当的跨包 API。优先验证两个已确认候选：
   - 将 Autofill 专用的创建能力从 `domain.entry.port.EntryCreation` 分离到 Autofill 所有的 Domain 端口，让普通 Entry 创建合同不再引用 `AutofillInboxDraftId`；保留共享的创建校验/物化规则、会话门禁、稳定 ID 与 Inbox 幂等语义。
   - 审查 `AutofillInboxRecordStore` 放在 `domain.access.port` 是否符合它的 Inbox 队列职责；只有在追踪 security/data 实现和所有消费者后确认 Autofill 才是实际合同 owner，才迁移它。
3. 对 `access ↔ entry` 授权与敏感字段合同作证据化决定；不得为消除静态依赖环而弱化授权绑定、复制模型或引入通用 ID/转发层。
4. 更新正式架构/维护规范：说明 Passly `:domain` 的职责、官方 Android Domain layer 定义与本项目契约模块的差别、UseCase 何时值得建立、模块化的高内聚低耦合原则、API/实现模块关系，以及官方 Kotlin 风格与本地附加规则的边界。官方来源优先链接 Android Developers 与 Kotlin 官方文档。
5. 在现有边界检查上补足可自动验证的 Domain 包依赖规则，但仅针对设计确认的方向；不要把合法授权合同、Backup 快照或安全队列合同用全局 allowlist 隐藏。

## 非目标

- 本阶段不把 `:domain` 自动拆成多个 Gradle module。是否拆分应在合法包依赖收敛后，根据剩余 API 图、独立变更率、编译隔离收益与构建成本另行评估。
- 不改变 Entry、Autofill、认证、备份、数据库、Proto 或外部格式的产品行为和持久化合同。
- 不为每个 Repository 建一对一 UseCase，不按目录大小、类型数量或复用次数机械移动代码。
- 不重写工作区中已存在的未提交修改，不暂存或提交 `.agents/`、本地 `AGENTS.md` 或任何文件。

## 考虑过的路径

| 路径 | 取舍 |
| --- | --- |
| **按业务所有权审查并收敛包/API 边界（用户选择）** | 保持单一 `:domain`，保留合法共享合同，切断有证据的错误依赖方向，并在规范和 verifier 中固化结论。能直接改善目标，同时不预设 Gradle 结构。 |
| 立即拆成多个 Gradle Domain modules | 当前 `access ↔ entry` 和多个跨包合同使模块切割边界尚不清楚；先拆会要求复制/搬迁合同或制造模块间桥接，不能证明高内聚。暂不选择。 |
| 只补规范与审查清单 | 可澄清官方建议，但会保留已发现的 `EntryCreation` API 交叉引用，未完成内聚目标。暂不选择。 |

## 最终实施方案

Passly 的 `:domain` 保持为纯 Kotlin 领域模型、规则与能力端口模块；Data、Security、Feature 或 App 在其真实机制 owner 内实现合同。Android 架构指南中 UI → Domain → Data 的图描述其可选 UseCase 层，不作为 Passly 的强制依赖模板。

Autofill 创建从通用 `EntryCreation` 拆入 `domain.autofill.port.AutofillEntryCreation`，`AutofillInboxRecordStore` 也移至 Autofill 所有的 Domain port；App `CreateEntryUseCase` 继续共享纯校验/物化规则，并分别实现和绑定普通创建与 Autofill 创建合同。明确有语义依据的 `access ↔ entry`、`settings → entry`、`backup → entry` 及其他跨包边保留。最终调用方、边界 verifier 与验收证据见[迁移审查记录](../../reviews/2026-11-07-domain-cohesion-migration.md)。

正式规范归位：`package-boundaries.md` 记录模块/包依赖方向和当前合法边；`maintenance-boundaries.md` 记录如何决策内聚、何时拆分与何时保留跨域合同；`kotlin-style.md` 记录官方 Kotlin 样式链接及本地额外约束。避免在多个规范重复维护相同细节。

官方依据：

- [Android Domain layer](https://developer.android.com/topic/architecture/domain-layer)：Android 指南将 Domain layer 定义为可选层，并说明其他架构对该术语有不同定义；UseCase 聚焦一个动作，只在复杂或可复用业务逻辑有收益时引入。
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)：大型应用在跨 ViewModel 复用或简化复杂 ViewModel 业务时推荐 Domain layer；该指南把建议按强度分级，而非视作刚性规则。
- [Common modularization patterns](https://developer.android.com/topic/modularization/patterns)：没有唯一模块化策略，建议高内聚低耦合；API/抽象模块与实现模块可分别依赖合同。
- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)：记录包/文件命名、常量命名、类内组织与代码风格；文件顶部声明区的具体要求保留为 Passly 本地规则，不冒称官方强制项。

## 原实施约束与验收（历史）

- 保留当前所有用户工作区改动；修改前后逐项比较目标路径状态。禁止覆盖现有 `domain/backup`、Autofill 设置及正式文档修改。
- 所有移出的操作都必须更新生产调用方、测试与 DI 绑定；不留下旧 API、默认 fallback 或仅供测试的旁路。
- 安全/认证变化要验证取消、授权范围、持久化失败、幂等和 fatal `Error` 传播；不把 `Throwable`/`Error` 映射成普通可重试失败。
- Domain package-boundary verifier 在现有源码规则上增加路径限定的禁边，覆盖导入语句与完全限定名；复用并保留现有模板/注释/字符串解析合同，不得因相似包名或字符串字面量误判。新增规则的测试应沿用同一组 decoy、模板与转义场景。
- 验收按变更影响运行 Domain 与目标应用模块测试、`verifyArchitecture` 和 `git diff --check`；不运行或声称未执行的设备验证。
- 变更只留在工作区，不暂存、不提交、不推送。
