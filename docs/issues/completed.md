# Passly 已完成事项记录

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

## 待完成（后续阶段）

参见 `REFACTORING_TODOS.md`：

- **阶段 B**：P1-01（MainActivity 瘦身）、P1-02（VaultViewModel 拆分）、P1-03（SettingsViewModel 拆分）、P1-04（StateHolder 可变状态治理）、P1-05（Main Contract handleIntent 接入）、P0-03（明文备份 SAF 改造）
- **阶段 C**：P0-04（迁移与备份自动化测试）、P2-01（单元/集成测试基线）、P2-02（CI 质量门禁）、P2-03（文案国际化）、P2-04（Full/Main 双轨治理）
