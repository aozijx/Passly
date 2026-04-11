# Passly 项目待解决事项与重构清单

本文档用于追踪当前项目的技术债、架构重构项、稳定性风险与文档修复项。

**基线时间**: 2026-04-12  
**来源**: 基于当前仓库代码与文档扫描（main/full 变体、docs、CI 配置）  
**目标**: 让待办可直接执行（有优先级、有落点、有验收标准）

---

## 0. 优先级与执行规则

- `P0`：安全/数据完整性/关键功能可用性风险，必须优先处理。
- `P1`：架构重构与跨模块技术债，影响后续开发效率与缺陷率。
- `P2`：质量体系与可维护性治理，防止问题反复出现。
- `P3`：一致性与清理优化项。

执行建议：每个任务都至少包含“代码改造 + 编译验证 +（必要时）文档同步”。

---

## 1. 总览（按优先级）

## 1.1 P0（立即处理）

1. `P0-01` Main 主流程 Effect 未被 UI 消费，错误/提示链路不完整。  
2. `P0-02` 启动阶段认证与数据库初始化存在竞态，可能在 DB 失败时先弹认证。  
3. `P0-03` 紧急明文备份写入公共下载目录，存在敏感信息暴露风险。  
4. `P0-04` 数据库迁移与备份兼容缺少自动化测试覆盖。  
5. `P0-05` `FaviconUtils` 内部 `runBlocking` 阻塞式调用，线程模型不清晰。

## 1.2 P1（本轮重构核心）

6. `P1-01` `MainActivity` 体量过大，混合 UI、认证、安全策略、传感器逻辑。  
7. `P1-02` `VaultViewModel` 职责过重，Support/StateHolder 组合复杂。  
8. `P1-03` `SettingsViewModel` 状态与流程混杂（设置流 + 备份交互态）。  
9. `P1-04` `VaultDetailStateHolder`/`VaultSearchFilterStateHolder` 仍有可变 UI 状态。  
10. `P1-05` `features/main/contract` 与实际使用不一致（`handleIntent` 未接入）。  
11. `P1-06` `core/qr/ScannerViewModel` 位于 core 层，分层边界偏移。  
12. `P1-07` 目录命名拼写问题：`topbar/compoents`。

## 1.3 P2（质量体系）

13. `P2-01` 测试目录仍是示例用例，核心流程缺少单元/集成测试。  
14. `P2-02` CI 仅有 CodeQL，缺少常规编译矩阵与测试门禁。  
15. `P2-03` 文案与错误提示分散在 ViewModel/Composable 中，国际化与一致性不足。  
16. `P2-04` Full/Main 双轨功能存在重复实现与长期分叉风险。

## 1.4 P3（文档与一致性）

17. `P3-01` 文档中的 `VaultCardStyleTokens` 路径已过期（`core/common/ui` -> `core/designsystem/model`）。  
18. `P3-02` Full 变体仍有 TODO 占位逻辑未闭环。

---

## 2. 详细任务清单（可执行版）

### P0-01 Main Effect 未消费（功能链路断点）

- **问题**：`MainViewModel` 已产出 `MainEffect`，但 `MainActivity` 未收集，导致 `ShowError/ShowToast/LockedByTimeout/NavigateToVault` 无统一出口。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/main/MainViewModel.kt:46-48, 250-252`
  - `app/src/main/java/com/aozijx/passly/features/main/MainViewModel.kt:55`（`handleIntent` 存在）
  - `MainActivity` 中无 `effect(s).collect` 相关代码（检索为空）。
- **改造步骤**：
  - 在 `MainActivity` `setContent` 中新增 `LaunchedEffect` 收集 `viewModel.effects`。
  - 建立统一分发：`ShowError/ShowToast` -> Toast/Snackbar，`LockedByTimeout` -> 锁屏提示，`NavigateToVault` -> 仅用于状态驱动确认。
  - 统一保留 `effects` 或 `effect` 单一出口，移除别名重复暴露。
- **验收标准**：
  - 人工验证：认证失败、数据库错误、自动锁定均有可见反馈。
  - 编译通过：`:app:compileVaultDebugKotlin`、`:app:compileFullDebugKotlin`。
- **建议工期**：0.5 天。

### P0-02 启动认证与 DB 初始化竞态

- **问题**：`MainActivity` 依据 `databaseError == null` 立即请求认证，但 `MainUiState` 初始值中 `databaseError` 本就是 `null`，DB 初始化尚未完成时也会触发认证。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/MainActivity.kt:106-109`
  - `app/src/main/java/com/aozijx/passly/features/main/contract/MainUiState.kt:10`（`isDatabaseInitializing`）
- **改造步骤**：
  - 启动认证条件改为：`!isDatabaseInitializing && databaseError == null && !isAuthorized`。
  - 首次进入仅在 DB 初始化完成后触发一次认证。
  - 若 DB 错误，直接进入错误对话流程，不触发认证。
- **验收标准**：
  - 模拟 DB 初始化失败时，不弹生物认证。
  - 正常路径只认证一次，无重复弹窗。
- **建议工期**：0.5 天。

### P0-03 明文紧急备份外泄风险

- **问题**：紧急/明文备份直接写入公共下载目录，其他应用/用户可访问，且包含高敏感字段（即使是“应急”场景，仍需更强约束）。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/core/backup/EmergencyBackupExporter.kt:51`
- **改造步骤**：
  - 改为 SAF 目录选择（用户显式目标目录）。
  - 导出前强提醒“明文风险 + 建议立即加密转存/删除”。
  - 导出后追加可选“打开目录 / 复制路径 / 立即分享加密渠道”流程。
  - 评估保留 `exportPlainBackup` 的权限门槛（例如强制二次生物认证已完成，且短时有效）。
- **验收标准**：
  - 不再直接写 `Environment.DIRECTORY_DOWNLOADS`。
  - 导出路径可控，风险提示完整。
- **建议工期**：1-2 天。

### P0-04 数据库迁移与备份兼容测试缺失

- **问题**：核心数据链路（Migration + Backup）无自动化回归保障。
- **证据**：
  - `app/src/test` 与 `app/src/androidTest` 仅示例测试。
  - 未发现 `MigrationTest`、`BackupManager` 相关测试。
- **改造步骤**：
  - 新增 Room Migration instrumentation tests（至少覆盖 1->2、2->3 和最终版本）。
  - 新增 BackupManager round-trip tests（导出->导入->字段校验，含错误密码/损坏文件）。
  - 在 CI 中执行这组测试（可分 nightly 与 PR 快速集）。
- **验收标准**：
  - 迁移路径和备份导入导出具备自动化通过记录。
- **建议工期**：2-3 天。

### P0-05 `FaviconUtils` 使用 `runBlocking`

- **问题**：在工具方法中 `runBlocking` 执行 Coil 请求，虽位于 IO 路径中，但会增加阻塞语义与维护风险。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/core/media/FaviconUtils.kt:11, 172`
- **改造步骤**：
  - 将 `downloadFaviconWithCoil` 改为 `suspend`。
  - 移除 `runBlocking`，使用协程链路自然挂起。
  - 增加超时与可取消支持，避免长时间网络阻塞。
- **验收标准**：
  - `FaviconUtils` 不再包含 `runBlocking`。
  - 下载失败路径日志与返回码保持兼容。
- **建议工期**：0.5 天。

---

### P1-01 `MainActivity` 过重（355 行）

- **问题**：单文件混合过多职责，维护和测试困难。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/MainActivity.kt`（约 355 行）
- **改造步骤**：
  - 提取：`MainSecurityController`（认证/锁屏）、`MainSensorController`（翻转锁屏）、`MainUiCoordinator`（页面切换）。
  - Activity 只保留生命周期与顶层导航壳。
- **验收标准**：
  - Activity 缩减到 <220 行。
  - 各控制器可独立单测（至少纯 Kotlin 逻辑部分）。
- **建议工期**：1-2 天。

### P1-02 `VaultViewModel` 过载（313 行 + 多 Support）

- **问题**：搜索、详情、TOTP、自动填充、文件逻辑同时聚集，复杂度高。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/vault/VaultViewModel.kt`
  - `private val xxxSupport = ...` 多个实例化集中。
- **改造步骤**：
  - 第一阶段：按“列表查询 / 条目生命周期 / TOTP / 自动填充状态”拆分内部协调器。
  - 第二阶段：逐步将 Support 升级为 UseCase 或 domain service。
- **验收标准**：
  - ViewModel 行数显著下降，主要方法按职责分组清晰。
  - `mutableStateOf` 的关键业务态改为单向 StateFlow。
- **建议工期**：2-4 天。

### P1-03 `SettingsViewModel` 复合态过多（328 行）

- **问题**：设置流、备份弹窗态、导入导出流程、消息状态混杂。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/settings/SettingsViewModel.kt`
- **改造步骤**：
  - 抽 `SettingsBackupCoordinator`（备份相关临时状态与流程）。
  - `SettingsViewModel` 仅保留设置读写与事件路由。
  - 对 `backupMessage/showBackupPasswordDialog/isExporting/backupPassword` 改造为统一 UiState 子块。
- **验收标准**：
  - 备份流程可独立测试。
  - 设置主状态和备份流程状态边界清晰。
- **建议工期**：1-2 天。

### P1-04 StateHolder 可变状态治理

- **问题**：`VaultDetailStateHolder`、`VaultSearchFilterStateHolder` 使用 `mutableStateOf var`，弱化单向数据流约束。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/vault/internal/VaultDetailStateHolder.kt`
  - `app/src/main/java/com/aozijx/passly/features/vault/internal/VaultSearchFilterStateHolder.kt`
- **改造步骤**：
  - 引入 `data class` + `MutableStateFlow`。
  - 状态修改统一通过事件方法。
- **验收标准**：
  - 关键状态字段不再暴露为外部可变 `var`。
- **建议工期**：1-2 天。

### P1-05 Main Contract 与实际调用不一致

- **问题**：`MainIntent` 已定义，但 `handleIntent` 未被调用；`RetryDatabaseInitialization`、`LockedByTimeout` 等未在 UI 闭环。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/main/MainViewModel.kt:55`
  - 检索 `handleIntent(` 仅定义无调用。
- **改造步骤**：
  - 统一入口：UI 全部通过 `handleIntent` 驱动，或删去未使用 contract。
  - 建立 contract 使用规范（与 `DetailContract` 对齐）。
- **验收标准**：
  - Main 模块 contract 使用路径唯一且完整。
- **建议工期**：0.5-1 天。

### P1-06 `core/qr/ScannerViewModel` 分层归位

- **问题**：ViewModel 位于 core 层，与分层约定（feature 层承载 UI 状态）不一致。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/core/qr/ScannerViewModel.kt`
- **改造步骤**：
  - 迁移至 `features/scanner`，core 保留纯工具能力（如 `QrCodeUtils`）。
- **验收标准**：
  - core 层不再包含 UI ViewModel。
- **建议工期**：0.5 天。

### P1-07 目录拼写与包路径规范化

- **问题**：`topbar/compoents` 拼写错误，降低可读性与检索效率。
- **证据**：
  - `app/src/main/java/com/aozijx/passly/features/vault/components/topbar/compoents`
- **改造步骤**：
  - 重命名为 `components`，统一 import。
- **验收标准**：
  - 无 `compoents` 残留路径。
- **建议工期**：0.5 天。

---

### P2-01 自动化测试基线建设

- **问题**：核心功能缺乏测试保护网。
- **证据**：
  - `app/src/test/java/com/example/passly/ExampleUnitTest.kt`
  - `app/src/androidTest/java/com/example/passly/ExampleInstrumentedTest.kt`
- **改造步骤**：
  - 单元测试优先级：`MainAutoLockCoordinator`、`MainValidationSupport`、`VaultSearchFilterStateHolder`。
  - 集成测试优先级：数据库迁移、备份导入导出、Autofill candidate 匹配。
- **验收标准**：
  - PR 最低可运行测试集明确。
- **建议工期**：2-5 天（可分阶段）。

### P2-02 CI 质量门禁补全

- **问题**：当前仅 CodeQL，且构建流程偏安全扫描导向，缺常规质量门禁。
- **证据**：
  - `.github/workflows/codeql.yml`
- **改造步骤**：
  - 新增 `build.yml`：`compileVaultDebugKotlin` + `compileFullDebugKotlin` + unit tests。
  - 可选 nightly：instrumentation + migration + backup round-trip。
- **验收标准**：
  - PR 必过“编译双变体 + 单测”后可合并。
- **建议工期**：0.5-1 天。

### P2-03 文案国际化与提示统一

- **问题**：大量硬编码提示分散在 UI/ViewModel，国际化和一致性弱。
- **证据**：
  - 例如 `MainActivity.kt:74,145`、`MainViewModel.kt:150,162,174` 等多处。
- **改造步骤**：
  - 建立 `UiMessage`（资源 ID + 参数）模型。
  - 将 ViewModel 文案迁移到 `strings.xml`。
  - 对“复制成功/导出提示/错误提示”统一模板。
- **验收标准**：
  - 业务关键提示不再硬编码字符串。
- **建议工期**：1-2 天。

### P2-04 Full/Main 双轨重复实现治理

- **问题**：Full 与 Main 存在重复概念实现，长期易分叉。
- **证据**：
  - 同名类重复：`SettingsViewModel`、`SettingsUiState` 等（main/full 各一套）。
  - Full 路径仍有 `ui/screens/*` 旧组织，Main 为 `features/*` 新组织。
- **改造步骤**：
  - 制定“共享模块 vs 变体特有”边界清单。
  - 能共享的下沉至 `main`，变体差异仅留最小壳层。
- **验收标准**：
  - 重复业务逻辑显著减少，变体差异点可枚举。
- **建议工期**：3-6 天（分批）。

---

### P3-01 文档路径漂移修复

- **问题**：文档仍引用旧路径 `core/common/ui/VaultCardStyleTokens.kt`。
- **证据**：
  - `docs/getting-started/DEVELOPER_GUIDE.md:155,164`
  - `docs/architecture/ARCHITECTURE_DECISIONS.md:224`
  - `docs/operations/CHANGE_PLAYBOOK.md:130`
- **改造步骤**：
  - 全量替换为 `core/designsystem/model/VaultCardStyleTokens.kt`。
  - 同步检查 README/INDEX/Playbook 交叉链接。
- **验收标准**：
  - 文档引用路径可直接定位到真实文件。
- **建议工期**：0.5 天。

### P3-02 Full 变体 TODO 闭环

- **问题**：存在 TODO 占位逻辑，功能不完整。
- **证据**：
  - `app/src/full/java/com/aozijx/passly/ui/screens/detail/DetailScreen.kt:57`
  - `app/src/full/java/com/aozijx/passly/ui/screens/home/HomeScreen.kt:82`
  - `app/src/full/java/com/aozijx/passly/ui/screens/login/LoginScreen.kt:142`
- **改造步骤**：
  - 对每个 TODO 明确“删掉/补齐/延后”的处理结果。
- **验收标准**：
  - TODO 具备 issue 编号或实现完成。
- **建议工期**：0.5-1 天。

---

## 3. 与 ADR 的对齐检查（必须纳入评审）

### ADR-001 离线优先（重点）

当前需重点复核的联网点：

- `app/src/full/java/com/aozijx/passly/ui/screens/settings/SettingsViewModel.kt:272-285`（远程日志拉取）
- `app/src/full/java/com/aozijx/passly/ui/screens/home/HomeViewModel.kt:53-65,94-103`（网络图片与链接）
- `app/src/main/java/com/aozijx/passly/core/media/FaviconUtils.kt:42-43,125-127`（favicon 网络抓取）

处理建议：

- 明确哪些属于“可关闭的非敏感辅助能力”，并补充开关与文档说明。
- 若策略变化，先更新 ADR 再改实现。

### ADR-003/004/005 数据、迁移、备份

- 数据库迁移必须可回归验证（见 `P0-04`）。
- 备份兼容必须有自动化测试保障（见 `P0-04`）。
- 明文导出风险需降低（见 `P0-03`）。

---

## 4. 推荐执行计划（3 个阶段）

### 阶段 A（1 周，止血）

- [ ] 完成 `P0-01`、`P0-02`、`P0-05`
- [ ] 建立最小 CI 编译门禁（`P2-02` 部分）
- [ ] 修复文档路径漂移（`P3-01`）

### 阶段 B（1-2 周，核心重构）

- [ ] 完成 `P1-01`、`P1-03`、`P1-05`
- [ ] 推进 `P1-02` 第一阶段拆分
- [ ] 完成 `P0-03` 明文备份导出改造

### 阶段 C（持续，质量固化）

- [ ] 完成 `P0-04` + `P2-01` 测试体系
- [ ] 完成 `P2-04` 双轨治理
- [ ] 清理 `P3-02` TODO 占位

---

## 5. 任务模板（新增事项时使用）

```markdown
### [ID] 标题
- 优先级：P0/P1/P2/P3
- 类型：Security / Refactor / Quality / Docs
- 位置：`path/to/file.kt:line`
- 问题：
- 改造步骤：
- 验收标准：
- 预计工期：
```

---

## 6. 下次更新建议

下次更新该文档时，请同步补充：

- 每个任务当前状态（`todo / doing / done`）
- 对应 PR 编号
- 是否触发 ADR 更新
