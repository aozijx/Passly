# Passly 已完成事项记录

---

## 2026-04-12 — 命名规范重构（Rename Sweep）

### 内部类重命名（vault/internal + vault/）

- **目标**：消除模糊 `XxxSupport` / `XxxStateHolder` 后缀，统一命名约定：
  - `-Coordinator`：持有状态 + 协调多项逻辑
  - `-Helper`：无状态纯工具
  - `-State`：纯 Compose 状态持有
- **重命名映射**：

  | 旧名                                   | 新名                      | 文件位置              |
  |--------------------------------------|-------------------------|-------------------|
  | `VaultAutofillSupport`               | `AutofillCoordinator`   | `vault/internal/` |
  | `VaultCryptoSupport`                 | `CryptoHelper`          | `vault/internal/` |
  | `VaultDetailCoordinator`（P1-02 中间产物） | `DetailCoordinator`     | `vault/internal/` |
  | `VaultDetailStateHolder`             | `DetailState`           | `vault/internal/` |
  | `VaultEntryFileSupport`              | `EntryIconHelper`       | `vault/internal/` |
  | `VaultEntryLifecycleSupport`         | `EntryManager`          | `vault/internal/` |
  | `VaultSearchFilterStateHolder`       | `SearchFilterState`     | `vault/internal/` |
  | `VaultTotpCoordinator`（P1-02 中间产物）   | `TotpCoordinator`       | `vault/internal/` |
  | `VaultTotpSupport`                   | `TotpHelper`            | `vault/internal/` |
  | `VaultOnDemandQuerySupport`          | `VaultQueryCoordinator` | `vault/`          |

- **同步变化**：
  - `AutofillCoordinator` 将 `isAutofillEnabled()` 判断内联为 `refreshStatus(context)`，方法命名更直观。
  - `EntryManager` 将 `autoUpdateMissingIcons()` 改名为 `downloadMissingIcons()`，意图更清晰。
  - `VaultViewModel` 私有持有名同步简化：`autofillSupport` → `autofill`，`cryptoSupport` → `crypto`，`searchFilterState` → `searchFilter` 等。
  - `Toast.makeText` 调用改为直接传 `R.string.xxx` 资源 ID，移除冗余 `context.getString()`。
  - 新增 `R.string.vault_toast_save_icon_failed` 至三个 locale 的 `strings.xml`，替换原硬编码中文。
- **旧文件**：全部删除，无残留。

---

## 2026-04-12 — 阶段 B 核心重构（P1）续

### P1-02 — VaultViewModel 拆分第一阶段（提取内部协调器）

- **新增文件**: `app/src/main/java/com/aozijx/passly/features/vault/internal/DetailCoordinator.kt`（已按命名规范重命名）
  - 封装 `DetailState`（私有持有）。
  - 对外暴露只读属性：`addType`、`itemToDelete`、`coordinatorState`。
  - 提供方法：`setAddType`、`setItemToDelete`、`showDetail`、`showDetailForEdit`、`dismissDetail`、`showIconPicker`、`hideIconPicker`、`updateEntry`、`isViewingEntry`。
  - 私有 `update(transform)` 统一驱动 `VaultDetailCoordinatorState` 变更。
- **新增文件**: `app/src/main/java/com/aozijx/passly/features/vault/internal/VaultTotpCoordinator.kt`
  - 构造参数：`scope: CoroutineScope`、`codeGenerator`、`decryptSecret`。
  - 持有 `_states: MutableStateFlow<Map<Int, TotpState>>`，暴露 `states: StateFlow`。
  - 包装 `VaultTotpSupport`（内部持有，保留纯逻辑分离）。
  - 方法：`start(entriesProvider)`、`unlock`、`autoUnlock`、`removeEntry`。
- **修改**: `VaultAutofillSupport.kt`
  - 新增 `var isEnabled by mutableStateOf(false) internal set`，将自动填充启用态移入支持类自身。
- **修改**: `VaultViewModel.kt`（311 行 → 195 行）
  - 移除 `_totpStates`、`totpSupport`、`detailState`、`updateDetailCoordinator` 等直接状态持有与私有工具方法。
  - 以 `detailCoordinator`、`totpCoordinator` 替代，Init 块改为 `totpCoordinator.start { vaultItems.value }`。
  - `isAutofillEnabled` 改为 `autofillSupport.isEnabled` 代理；`updateAutofillStatus` 写入 `autofillSupport.isEnabled`。
  - 修复 P1-04 遗留编译 bug：`addItem` 中 `addType = AddType.NONE` × 2 → `setAddType(AddType.NONE)`；`confirmDelete` 中 `itemToDelete = null` → `detailCoordinator.setItemToDelete(null)`。
  - 所有 detail/TOTP 方法改为单行委托；公共 API 签名完全向后兼容。
- **关联**: `REFACTORING_TODOS.md` P1-02

---

## 2026-04-12 — 阶段 B 核心重构（P1）

### P1-05 — Main Contract handleIntent 接入

- **文件**: `MainActivity.kt`, `MainViewModel.kt`
- **改动**:
  - 删除 `MainViewModel` 中未使用的 `effect: Flow<MainEffect>` 别名（同时移除无用 `Flow` import）。
  - `MainActivity` 中所有直接 VM 方法调用统一改为 `handleIntent`：
    - `viewModel.lock()` → `handleIntent(MainIntent.Lock)`（onSensorChanged）
    - `viewModel.updateInteraction()` → `handleIntent(MainIntent.UpdateInteraction)`（onUserInteraction）
    - `viewModel.checkAndLock()` → `handleIntent(MainIntent.CheckAndLock)`（onResume）
    - `viewModel.exportEmergencyBackup(ctx)` → `handleIntent(MainIntent.ExportEmergencyBackup(ctx))`
    - `viewModel.exportPlainBackup(this)` → `handleIntent(MainIntent.ExportPlainBackup(this))`
  - `viewModel.authenticate(...)` 保留直接调用（含复杂回调参数，不适合通过 Intent 传递）。
- **关联**: `REFACTORING_TODOS.md` P1-05

---

### P1-01 — MainActivity 瘦身（提取 MainSensorController）

- **新增文件**: `app/src/main/java/com/aozijx/passly/features/main/MainSensorController.kt`
  - 实现 `SensorEventListener`，封装加速度计注册/注销与翻转触发逻辑。
  - `isFlipLockEnabled`、`isFlipExitAndClearStackEnabled` 状态移至控制器。
  - `onFlipLock` 回调由 `MainActivity` 传入，触发 lock + UI 折叠 + 可选退栈。
- **修改**: `MainActivity.kt`
  - 移除 `SensorEventListener` 接口实现。
  - 移除 `sensorManager`、`accelerometer`、`isFlipLockEnabled`、`isFlipExitAndClearStackEnabled` 4 个字段。
  - 移除 `registerSensor()`、`unregisterSensor()`、`onSensorChanged()`、`onAccuracyChanged()` 4 个方法。
  - 改为 `private val sensorController by lazy { MainSensorController(...) }`，所有传感器操作委托给控制器。
  - 移除 5 条 sensor 相关 import。
- **关联**: `REFACTORING_TODOS.md` P1-01

---

### P1-03 — SettingsViewModel 备份逻辑拆分（提取 SettingsBackupCoordinator）

- **新增文件**: `app/src/main/java/com/aozijx/passly/features/settings/SettingsBackupCoordinator.kt`
  - 持有全部备份交互状态（8 个 `mutableStateOf` 字段：`backupMessage`、`isExporting`、`showBackupPasswordDialog`、`backupUri`、`backupPassword`、`importMode`、`includeImagesInBackup`、`backupExportFallbackFileName`）。
  - 持有全部备份业务方法：`startExport`、`startImport`、`processBackupAction`、`dismissBackupPasswordDialog`、`clearBackupMessage`、`testBackupDirectoryWritePermission` 等。
- **修改**: `SettingsViewModel.kt`（328 行 → 195 行）
  - 备份相关 import、字段、方法全部移除。
  - 新增 `val backup = SettingsBackupCoordinator(...)` 公开属性。
  - `testBackupDirectoryWritePermission` 改为单行委托。
- **更新调用方**:
  - `SettingsScreen.kt`：`viewModel.backupMessage` → `viewModel.backup.backupMessage`
  - `VaultDialogs.kt`：`settingsViewModel.showBackupPasswordDialog` → `.backup.showBackupPasswordDialog`
  - `BackupPasswordDialog.kt`：所有 `settingsViewModel.xxx` → `settingsViewModel.backup.xxx`（13 处）
  - `VaultScreen.kt`：`startExport`、`startImport`、`tryStartExportInConfiguredDirectory`、`nextBackupFileName` → `.backup.xxx`
- **关联**: `REFACTORING_TODOS.md` P1-03

---

### P1-04 — StateHolder 可变状态治理

- **修改**: `VaultSearchFilterStateHolder.kt`
  - `isSearchActive`、`isMoreMenuExpanded` 加 `private set`，禁止外部直接赋值。
  - 新增 `expandMoreMenu(expanded: Boolean)` 方法。
- **修改**: `VaultDetailStateHolder.kt`
  - `addType`、`detailCoordinatorState`、`itemToDelete` 改为 `internal set`（仅 `VaultViewModel` 可赋值）。
- **修改**: `VaultViewModel.kt`
  - `var isSearchActive`/`var isMoreMenuExpanded` 改为 `val`（只读），移除无用 setter。
  - 新增 `expandMoreMenu(Boolean)`、`setAddType(AddType)`、`setItemToDelete(VaultEntry?)` 方法。
  - 内部 `isMoreMenuExpanded = false` 改为 `expandMoreMenu(false)`。
- **更新调用方**（6 个文件）:
  - `AddPassword.kt`、`AddTwoFADialog.kt`、`VaultFab.kt`、`VaultScreen.kt`：`viewModel.addType = X` → `viewModel.setAddType(X)`
  - `VaultDialogs.kt`：`vaultViewModel.itemToDelete = null` → `vaultViewModel.setItemToDelete(null)`
  - `VaultTopBar.kt`：`vaultViewModel.isMoreMenuExpanded = true/false` → `vaultViewModel.expandMoreMenu(true/false)`
- **关联**: `REFACTORING_TODOS.md` P1-04

---

## 2026-04-12 — 阶段 A 止血（P0/P1/P3）

### P0-01 — MainEffect 接入

- **文件**: `app/src/main/java/com/aozijx/passly/MainActivity.kt`
- **改动**:
  - 在 `setContent` 内新增 `LaunchedEffect(Unit)` 收集 `viewModel.effects`。
  - `ShowToast` → `Toast.LENGTH_SHORT`；`ShowError` → `Toast.LENGTH_LONG`。
  - `LockedByTimeout` → 折叠 `showDetail` / `showSettings`。
  - `NavigateToVault` → 由 `uiState.isAuthorized` 状态驱动 UI，无需额外操作。
  - 同步移除原先因双重 Toast 问题而存在的两个备份文件状态驱动 `LaunchedEffect`（`emergencyBackupFile`、`plainBackupFile`）。
- **关联**: `REFACTORING_TODOS.md` P0-01

---

### P0-02 — 启动认证竞态修复

- **文件**: `app/src/main/java/com/aozijx/passly/MainActivity.kt`
- **改动**:
  - 删除 `onCreate` 中 `if (databaseError == null) requestAuthentication()` 的主动同步调用。
  - 改为 `LaunchedEffect(isDatabaseInitializing, databaseError, isAuthorized)` 三键状态驱动：仅在 `!isDatabaseInitializing && databaseError == null && !isAuthorized` 时触发认证。
  - 确保 DB 初始化完成前不触发生物认证，DB 出错时直接进入错误对话流程。
- **关联**: `REFACTORING_TODOS.md` P0-02

---

### P0-05 — FaviconUtils `runBlocking` 消除

- **文件**: `app/src/main/java/com/aozijx/passly/core/media/FaviconUtils.kt`
- **改动**:
  - `downloadFaviconWithCoil` 由 `private fun` 改为 `private suspend fun`。
  - 移除 `val result = runBlocking { imageLoader.execute(request) }`，直接 `val result = imageLoader.execute(request)` 自然挂起。
  - 删除无用 `import kotlinx.coroutines.runBlocking`。
  - 调用链（`downloadAndSaveFavicon` → `FaviconDataRepository` → `VaultEntryLifecycleSupport`）本已全在 suspend/IO 上下文，无需额外改动。
- **关联**: `REFACTORING_TODOS.md` P0-05

---

### P1-06 — ScannerViewModel 分层归位

- **文件变动**:
  - 新建 `app/src/main/java/com/aozijx/passly/features/scanner/ScannerViewModel.kt`（package 改为 `features.scanner`，显式添加 `import com.aozijx.passly.core.qr.QrCodeUtils`）。
  - 更新 `app/src/full/java/com/aozijx/passly/ui/screens/scanner/ScannerScreen.kt` import：`core.qr.ScannerViewModel` → `features.scanner.ScannerViewModel`。
  - 删除 `app/src/main/java/com/aozijx/passly/core/qr/ScannerViewModel.kt`。
  - `features/scanner/VaultScanner.kt` 与 `ScannerViewModel` 同包，linter 自动移除显式 import，无需处理。
  - `core/qr/` 目录现仅保留 `QrCodeUtils.kt`，符合 core 层纯工具定位。
- **关联**: `REFACTORING_TODOS.md` P1-06

---

### P1-07 — 目录拼写修复 `compoents` → `components`

- **文件变动**:
  - `features/vault/components/topbar/compoents/` 下 4 个文件 package 声明由 `topbar.compoents` 改为 `topbar.components`：
    - `CustomExportMenuItem.kt`
    - `VaultDropdownMenu.kt`
    - `VaultSearchBar.kt`
    - `VaultTabRow.kt`
  - `features/vault/components/topbar/VaultTopBar.kt` 中 3 处 import 同步更新。
  - 目录重命名：`compoents/` → `components/`。
  - 全项目 `compoents` 字符串残留为零（已验证）。
- **关联**: `REFACTORING_TODOS.md` P1-07

---

### P3-01 — 文档路径漂移修复

- **文件变动**（共 4 处替换）:
  - `docs/getting-started/DEVELOPER_GUIDE.md`（第 155、164 行）
  - `docs/architecture/ARCHITECTURE_DECISIONS.md`（第 224 行）
  - `docs/operations/CHANGE_PLAYBOOK.md`（第 130 行）
- **改动**: 旧路径 `core/common/ui/VaultCardStyleTokens.kt` → 实际路径 `core/designsystem/model/VaultCardStyleTokens.kt`。
- **关联**: `REFACTORING_TODOS.md` P3-01

---

### P3-02 — Full 变体 TODO 闭环

- **文件变动**:
  - `app/src/full/java/com/aozijx/passly/ui/screens/detail/DetailScreen.kt`
    - 删除 `TopBarConfig.actions` 中的搜索 `IconButton`（`DetailViewModel` 无搜索能力）。
    - 移除随之无用的 `import Icons.Default.Search` 和 `import IconButton`。
  - `app/src/full/java/com/aozijx/passly/ui/screens/home/HomeScreen.kt`
    - 删除 `TopBarConfig.actions` 中的通知 `IconButton`（无通知页面）。
    - `Icons` / `IconButton` 在文件其他位置仍有使用，import 保留。
  - `app/src/full/java/com/aozijx/passly/ui/screens/login/LoginScreen.kt`
    - 删除注册 `TextButton`（Passly 为本地密码管理器，无注册概念）。
    - 移除随之无用的 `import TextButton`。
- **关联**: `REFACTORING_TODOS.md` P3-02

---

## 2026-04-16 — Compose Navigation 与全局架构重构

### Navigation — Jetpack Compose Navigation 全局导航引入

- `PasslyNavHost` 统一管理 Vault / Detail / Settings 路由，`AppRoute` 定义路由表。
- `MainActivity` 移除 `showSettings`/`showDetail` 等手动状态，改由 `isAuthorized` + NavHost 驱动。
- Detail 页支持 `entryId` 路由参数动态加载。
- 新增 `PasslyNavigationAnim` 标准化转场动画。
- **关联**: `08750b7`

### Detail 重构 — UI 解耦与安全性增强

- `DetailScreen` 及子组件重构为无状态，移除对 VM 直接依赖，改用回调机制。
- 各 Section 直接集成 `CryptoManager` 解密，支持单次验证批量解密。
- `DetailViewModel.onCleared` 主动擦除敏感状态；`DisposableEffect` 确保页面销毁清理。
- `EntryEditState`/`TotpEditState` 迁移至 `features.detail.internal`。
- 修复 AutofillAuthActivity 在 `BOTTOM_SHEET` 模式的 FillResponse 兼容问题。
- **关联**: `28220e2`, `fca0266`, `11f7804`

### 状态模型跨模块解耦

- 删除 `core.designsystem.state`/`model` 通用 UI 状态，按功能模块重组。
- `AddType`/`VaultTab` → `features.vault`；`TotpState` 保留为唯一跨模块共享状态。
- `ImageType` → `core.media`；清理 `DeleteState`/`EditMode` 等无用枚举。
- **关联**: `abc6660`

### MainViewModel 架构优化

- 新增 `MainAutoLockCoordinator`，封装 `AutoLockScheduler`，通过 `SharedFlow` 订阅锁定事件。
- 新增 `MainNotificationPermissionController`，解耦通知权限逻辑。
- `handleIntent` 统一处理 `UpdateInteraction`/`CheckAndLock`。
- **关联**: `01f0fac`, `5a8fde7`

### Vault 滑动操作与字段访问重构

- `SwipeActionType` 移除 `EDIT`，简化滑动操作。
- 引入 `FieldKey` 枚举驱动 `EntryTypeStrategy.getFieldValue`，统一复制路径。
- TOTP 条目优先复制动态验证码。
- **关联**: `18a9d47`, `ad97e6e`

### P0-03 — 明文备份 SAF 化（部分完成）

- 新增 SAF `CreateDocument` 合约，支持用户自选导出目录。
- `EmergencyBackupExporter` 新增 `exportPlainBackupToUri` 流式写入方法。
- `PlainExportDialog` 增加详细风险提示。
- **未闭环**：紧急导出路径 `exportPlainJson` 仍使用 `DIRECTORY_DOWNLOADS`（见 `EmergencyBackupExporter.kt:84`）。
- **关联**: `6273812`

### P0-04 — 数据库迁移与备份集成测试（基本完成）

- 新增 `DatabaseMigrationTest`：覆盖 1→2（索引）、2→3（新字段）迁移路径。
- 新增 `BackupRoundTripTest`：验证明文 JSON 导出完整性与 Boolean 序列化。
- Room Schema 导出 v2，`androidTest` 关联 `schemas` 资源目录。
- **关联**: `70e2070`

### Vault Pager 同步与 Tab 可见性控制

- `VaultScreen` 监听 `settledPage` 替代 `currentPage`，避免动画中间态触发更新。
- `VaultTab` 重构：`settingsKey` 持久化 + `isToggleable` 属性。
- 新增 `VaultTabsSettingsSection`，用户可自定义 Tab 可见性。
- 新增"自动下载图标"设置开关。
- `AddType`/`VaultTab` 迁移至 `features.vault.model`。
- `VaultViewModel` 移除 `decryptMultiple`/`unlockTotp`/`showDetailForEdit` 冗余方法。
- **关联**: `2b1a7d0`, `8c68c6b`

---

## 待完成（后续阶段）

参见 `REFACTORING_TODOS.md`。