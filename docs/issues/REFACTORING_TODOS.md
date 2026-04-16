# Passly 项目待解决事项（精简版）

本文档仅保留**当前未完成**的重构与治理任务。  
已完成事项请查看：`docs/issues/completed.md`

**最后更新**: 2026-04-16

---

## 优先级定义

- `P0`：安全与数据完整性风险，优先最高。
- `P1`：核心架构持续重构项。
- `P2`：质量体系与工程化治理项。

---

## P0（必须优先）

### P0-03 紧急导出路径 SAF 残留（收尾）

- **现状**：明文导出已接入 SAF，但紧急导出仍走 `DIRECTORY_DOWNLOADS`。
- **证据**：`EmergencyBackupExporter.kt:84` — `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)`
- **目标**：紧急导出也改为 SAF 或配置目录写入，彻底消除公共目录依赖。
- **验收标准**：全项目无 `DIRECTORY_DOWNLOADS` 引用。
- **详细步骤**：
  1. 在 `MainViewModel` / `MainIntent` 中新增 `ExportEmergencyBackupToUri(uri)` 意图。
  2. `EmergencyBackupExporter` 新增 `exportEmergencyToUri(context, uri)` 方法，复用 `writeEntriesToJson`。
  3. `MainActivity` 注册 `CreateDocument` launcher 处理紧急导出路径选择。
  4. 删除 `exportPlainJson` 中的 `DIRECTORY_DOWNLOADS` 分支，或将其改为 fallback 提示用户选择目录。
  5. 全项目搜索 `DIRECTORY_DOWNLOADS` 确认零残留。
- **建议**：
  - 紧急导出的设计初衷是"数据库损坏时的逃生通道"，此时 SAF 可能不可用（Activity 未正常启动）。建议保留一个 `catch` 分支：SAF 失败时降级写入 app-specific 目录（`context.getExternalFilesDir`），而非公共 Downloads。
- **隐患**：
  - 紧急导出触发时机通常是数据库异常，此时 UI 栈可能不完整，`CreateDocument` launcher 可能无法正常弹出。需确保降级路径可用。
  - Android 11+ scoped storage 限制下，`DIRECTORY_DOWNLOADS` 写入本身已受限，部分设备上紧急导出可能静默失败——这是当前的隐性 bug。

### P0-04 迁移与备份测试补全（收尾）

- **现状**：基础测试已就位（`DatabaseMigrationTest` + `BackupRoundTripTest`），但覆盖不足。
- **证据**：`app/src/androidTest/` 仅 2 个测试类。
- **目标**：
  - 补充 1→3 全链路迁移测试（跳版本升级）。
  - 补充备份 round-trip 的负向用例：错误密码、损坏文件、空数据库。
  - 验证导出 JSON 可被重新导入且数据一致。
- **验收标准**：CI 自动执行，核心迁移/备份路径正向 + 负向覆盖。
- **详细步骤**：
  1. `DatabaseMigrationTest` 新增 `migrate_1_to_3_directUpgrade` 用例。
  2. `BackupRoundTripTest` 新增：
     - `export_emptyDatabase_producesEmptyArray`
     - `import_corruptedJson_returnsFailure`
     - `import_wrongPassword_returnsFailure`（加密备份场景）
     - `export_then_import_dataConsistency`（完整 round-trip）
  3. 在 `build.gradle.kts` 的 `androidTest` 中确认 Room testing 依赖版本与主模块一致。
  4. 将测试纳入 CI workflow（见 P2-02）。
- **建议**：
  - 优先补 round-trip 一致性测试，这是备份功能的核心保障。
  - 考虑使用 `@LargeTest` 标注耗时用例，避免拖慢日常 CI。
- **隐患**：
  - 当前测试使用真实 SQLCipher 数据库，CI 环境需确保 native so 可用（ARM emulator 或 x86 兼容）。
  - 跳版本迁移（1→3）如果 Room 的 `addMigrations` 顺序有误，可能导致生产环境升级崩溃但测试未覆盖。

---

## P1（架构后续）

### P1-08 VaultViewModel 第二阶段拆分

- **现状**：第一阶段已完成，VM 从 311 行降至 218 行，已提取 10 个 internal 协调器。但仍承载 Tab 切换、设置监听、条目 CRUD 编排等逻辑。
- **证据**：`VaultViewModel.kt`（218 行）、`vault/internal/`（10 个文件）
- **目标**：
  - 继续下沉可复用业务逻辑到 domain/usecase 层。
  - 收敛 UI 层可变状态，进一步统一事件驱动更新。
- **验收标准**：ViewModel 复杂度持续下降，跨模块逻辑边界更清晰。
- **详细步骤**：
  1. 审计 VM 中剩余方法，识别可下沉到 usecase 层的逻辑（如条目 CRUD、批量操作）。
  2. 将设置监听逻辑（`isAutoDownloadIcons`、`visibleVaultTabs`）收敛为独立 `SettingsObserver` 或合并到现有 coordinator。
  3. 评估是否引入 `VaultUiState` sealed class 统一驱动 UI，替代当前多个独立 StateFlow。
  4. 提取后编写委托测试验证行为不变。
- **建议**：
  - 218 行已经是合理范围，不要为拆而拆。优先拆对测试有显著收益的部分。
  - 如果引入 sealed UiState，注意 recomposition 范围——过大的 state 对象会导致不必要的重组。
- **隐患**：
  - 过度拆分会导致间接调用链过长，调试困难。当前 10 个 internal 文件已接近上限，新增前先评估是否可合并。
  - `VaultQueryCoordinator` 已承载 Tab→Filter 映射，若再叠加设置监听，可能变成新的 God Object。

---

## P2（质量与工程化）

### P2-01 单元/集成测试基线建设

- **现状**：核心模块测试覆盖不足。
- **建议优先顺序**：
  1. `features/main/internal`（自动锁、校验、DB 初始化流程）
  2. `features/vault/internal`（搜索过滤、详情协调状态）
  3. 备份与迁移集成测试（已有基线，需补全负向用例）
- **验收标准**：新增测试可覆盖关键分支与失败场景。
- **详细步骤**：
  1. 为 `MainAutoLockCoordinator` 编写单元测试：超时触发、交互重置、配置变更。
  2. 为 `SearchFilterState` 编写测试：搜索激活/取消、过滤组合、边界条件。
  3. 为 `DetailCoordinator` 编写测试：状态转换、并发操作。
  4. 为 `CryptoHelper` 编写测试：加解密对称性、错误密钥处理。
  5. 建立测试命名规范与目录结构约定。
- **建议**：
  - Coordinator 类测试优先——它们承载核心业务逻辑且与 Android 框架解耦，易于测试。
  - 使用 Turbine 测试 StateFlow/SharedFlow 行为。
- **隐患**：
  - `CryptoHelper` 依赖 SQLCipher native 库，纯 JVM 单元测试可能无法运行，需要 instrumented test 或 mock。
  - 不要追求覆盖率数字，优先覆盖有实际回归风险的路径。

### P2-02 CI 质量门禁补全

- **现状**：仅有 CodeQL，缺编译/测试门禁。
- **证据**：`.github/workflows/codeql.yml`
- **目标**：
  - 新增常规 CI：`compileVaultDebugKotlin`、`compileFullDebugKotlin`、unit tests。
  - 可选 nightly：instrumentation + migration + backup 回归。
- **验收标准**：PR 具备基础质量门禁后方可合并。
- **详细步骤**：
  1. 新增 `.github/workflows/ci.yml`，触发条件 `push` + `pull_request` 到 `main`/`dev`。
  2. Job 1：Lint + Compile（`./gradlew lintVaultDebug compileVaultDebugKotlin compileFullDebugKotlin`）。
  3. Job 2：Unit Tests（`./gradlew testVaultDebugUnitTest`）。
  4. Job 3（nightly/manual）：Instrumented Tests（需 Android emulator action）。
  5. 配置 branch protection rules 要求 Job 1 + Job 2 通过。
- **建议**：
  - 先保证 compile + unit test 门禁稳定，再逐步加入 instrumented test。
  - 使用 Gradle build cache 加速 CI，首次运行预计 5-8 分钟。
- **隐患**：
  - SQLCipher native 依赖可能导致 CI 构建需要额外配置（NDK 或预编译 so）。
  - Full variant 依赖 Google Play Services 等外部 SDK，CI 环境需确保 license 接受。

### P2-03 文案与错误提示统一（国际化）

- **现状**：多处 `Toast` 文案仍分散硬编码。
- **目标**：
  - 将关键业务提示收敛到 `strings.xml`。
  - ViewModel 使用统一消息模型（资源 ID + 参数）而非直接拼接文本。
- **验收标准**：核心提示路径不再散落硬编码字符串。
- **详细步骤**：
  1. 全项目搜索 `Toast.makeText` + `getString` 之外的硬编码字符串，建立清单。
  2. 按模块批量迁移到 `strings.xml`（zh/en/ja 三语）。
  3. 在 ViewModel 层定义 `UiMessage(resId, args)` 模型，替代 `String` 类型的 toast 消息。
  4. 逐步将 `MainEffect.ShowToast(text)` 改为 `ShowToast(resId)`。
- **建议**：
  - 已有部分迁移先例（`R.string.vault_toast_save_icon_failed`），沿用相同模式。
  - 不要一次性全改，按 feature 模块分批推进。
- **隐患**：
  - 资源 ID 方式下，带动态参数的消息需要 `getString(resId, arg1, arg2)` 格式化，模型设计需考虑参数传递。
  - 三语维护成本：每新增一条字符串需同步更新 3 个 `strings.xml`，容易遗漏。

### P2-04 Full/Main 双轨收敛治理

- **现状**：full 与 main 仍存在重复实现与长期分叉风险。
- **目标**：
  - 明确"共享逻辑"与"变体特有逻辑"边界。
  - 持续下沉可复用逻辑到 `main`，压缩 full 专属代码。
- **验收标准**：重复实现减少，变体差异可枚举、可维护。
- **详细步骤**：
  1. 对比 `src/main` 与 `src/full` 目录，列出重复文件与差异点。
  2. 识别可统一的逻辑（如 UI 组件、数据层），提取到 `main`。
  3. Full 专属功能（扫码、云同步等）通过接口注入，main 提供空实现。
  4. 建立变体差异文档，明确哪些代码属于 full-only。
- **建议**：
  - 先治理风险最高的重复——数据层和安全相关逻辑不应有两份实现。
  - 使用 `expect`/`actual` 风格的接口隔离，而非文件级复制。
- **隐患**：
  - 收敛过程中可能影响 full variant 的独有功能（如 Scanner），需逐一验证。
  - 如果 full variant 的 UI 与 main 差异较大，强行统一可能引入条件分支地狱。

---

## 执行建议（下一周期）

1. **立即**：收尾 `P0-03`（紧急导出 SAF 残留，约 1-2 小时），这是当前唯一的安全隐患。
2. **本周**：补全 `P0-04` 负向测试用例 + 搭建 `P2-02` CI 基础门禁（二者可并行）。
3. **下周**：推进 `P2-01` 测试基线（从 Coordinator 类开始）。
4. **后续**：`P2-03`（国际化）/ `P2-04`（双轨治理）按需穿插。
5. **P1-08** 暂缓——218 行 VM 已在合理范围，待测试基线建立后再评估是否值得继续拆分。

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