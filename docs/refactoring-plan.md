# Passly 项目文件结构重构方案

## 📋 当前问题诊断

| # | 问题文件                         | 症状                                            | 严重度  |
|---|------------------------------|-----------------------------------------------|------|
| 1 | `AppDatabase.kt`             | Room 定义 + 加密口令管理 + 迁移 + DatabaseConfig，全在一个文件 | 🔴 高 |
| 2 | `VaultDao.kt`                | 20+ 方法混杂 CRUD / 搜索 / 摘要 / 自动填充 / 历史           | 🟡 中 |
| 3 | `SpecificEntryStrategies.kt` | 9 种条目策略全挤在 400+ 行的单文件                         | 🔴 高 |
| 4 | `VaultUseCasesImpl.kt`       | 15 个 UseCase 类堆在一个文件                          | 🟡 中 |
| 5 | `BackupManager.kt`           | 导出 / 导入 / 加密 / JSON 序列化全在一起                   | 🟡 中 |
| 6 | `core/common/ui/`            | UI Token 与非 UI 通用类型混放                         | 🟢 低 |
| 7 | `EntryTypeResolver.kt`       | `FieldKey` 枚举嵌在 Resolver 逻辑里                  | 🟢 低 |

---

## 🏗️ 目标结构 (重构后)

```
com.aozijx.passly/
├── AppContext.kt                         ← 不变
├── MainActivity.kt                       ← 不变
├── MainViewModel.kt                      ← 不变
│
├── core/
│   ├── common/
│   │   └── EntryType.kt                  ← 不变
│   ├── crypto/
│   │   └── CryptoManager.kt              ← 不变
│   ├── di/
│   │   ├── AppContainer.kt               ← 瘦身：仅做门面
│   │   ├── DataModule.kt                 ← 新建：DB / DAO / Repository 注册
│   │   └── DomainModule.kt               ← 新建：UseCases / Strategy 注册
│   ├── logging/                           ← 不变
│   ├── media/                             ← 不变
│   ├── platform/                          ← 不变
│   ├── qr/                                ← 不变
│   ├── security/
│   │   ├── DatabasePassphraseManager.kt   ← 从 AppDatabase.kt 提取
│   │   └── otp/                           ← 不变
│   ├── storage/                           ← 不变
│   ├── backup/
│   │   ├── BackupManager.kt              ← 瘦身：仅保留编排逻辑
│   │   ├── BackupCrypto.kt               ← 提取：密钥派生 / 流读取
│   │   ├── BackupSerializer.kt           ← 提取：JSON 读写
│   │   └── BackupFieldEncryptor.kt       ← 提取：字段加解密
│   └── designsystem/
│       ├── base/                          ← 不变
│       ├── model/
│       │   ├── AddType.kt                ← 从 core/common/ui/ 迁入
│       │   ├── VaultTab.kt               ← 从 core/common/ui/ 迁入
│       │   ├── VaultCardStyle.kt         ← 从 core/common/ui/ 迁入
│       │   └── FieldLabel.kt             ← 从 EntryTypeResolver.kt 提取
│       ├── tokens/
│       │   └── VaultCardStyleTokens.kt   ← 从 core/common/ui/ 迁入
│       ├── state/                         ← 不变
│       └── utils/
│           ├── EntryTypeFieldMapper.kt    ← 不变
│           └── EntryTypeResolver.kt       ← 瘦身：仅保留逻辑
│
├── data/
│   ├── entity/                            ← 不变 (Room 约束)
│   ├── local/
│   │   ├── AppDatabase.kt                ← 瘦身：仅 @Database + getDatabase()
│   │   ├── DatabaseConfig.kt             ← 从 AppDatabase.kt 提取
│   │   ├── Converters.kt                 ← 不变
│   │   ├── config/                        ← 不变
│   │   ├── dao/
│   │   │   ├── VaultEntryDao.kt          ← 拆分：条目 CRUD / 查询 / 摘要
│   │   │   └── VaultHistoryDao.kt        ← 拆分：历史记录操作
│   │   └── migration/
│   │       ├── Migration1To2.kt          ← 从 AppDatabase.kt 提取
│   │       └── Migration2To3.kt          ← 从 AppDatabase.kt 提取
│   ├── mapper/
│   │   └── VaultEntryMapper.kt            ← 不变
│   └── repository/                        ← 不变
│
├── domain/
│   ├── mapper/                            ← 不变
│   ├── model/
│   │   ├── ... (全部不变)
│   │   └── FieldKey.kt                   ← 从 EntryTypeResolver.kt 提取
│   ├── policy/                            ← 不变
│   ├── repository/                        ← 不变
│   ├── strategy/
│   │   ├── EntryTypeStrategy.kt           ← 保留接口定义
│   │   ├── EntryTypeStrategyRegistry.kt   ← 不变
│   │   └── impl/
│   │       ├── PasswordEntryStrategy.kt   ┐
│   │       ├── WiFiEntryStrategy.kt       │
│   │       ├── BankCardEntryStrategy.kt   │
│   │       ├── SshKeyEntryStrategy.kt     │ 一个策略一个文件
│   │       ├── SeedPhraseEntryStrategy.kt │ (从 SpecificEntryStrategies.kt 拆分)
│   │       ├── PasskeyEntryStrategy.kt    │
│   │       ├── RecoveryCodeEntryStrategy.kt│
│   │       ├── TotpEntryStrategy.kt       │
│   │       └── IdCardEntryStrategy.kt     ┘
│   └── usecase/
│       ├── vault/
│       │   ├── VaultUseCases.kt           ← 接线类不变
│       │   ├── ObserveAllEntriesUseCase.kt        ┐
│       │   ├── ObserveAllEntrySummariesUseCase.kt │
│       │   ├── GetEntriesByCategoryUseCase.kt     │
│       │   ├── SearchEntriesUseCase.kt            │ 一个用例一个文件
│       │   ├── InsertEntryUseCase.kt              │ (从 VaultUseCasesImpl.kt 拆分)
│       │   ├── UpdateEntryUseCase.kt              │
│       │   ├── DeleteEntryUseCase.kt              │
│       │   ├── GetTotpCodeUseCase.kt              │
│       │   ├── DownloadFaviconUseCase.kt          │
│       │   └── ...                                ┘
│       ├── settings/                      ← 不变
│       └── userconfig/                    ← 不变
│
├── features/                              ← 整体不变 (已经按功能分好)
│   ├── vault/                             ← VaultViewModel + 7 个 internal 支持类，够好了
│   ├── detail/                            ← sections/ 按类型分文件，很好
│   ├── settings/                          ← 不变
│   └── scanner/                           ← 不变
│
├── service/                               ← 不变
│   └── autofill/
│
└── ui/                                    ← 不变
```

---

## 🚀 分阶段执行

### Phase 1 — 零风险提取 (纯拆文件，不改逻辑)

> **目标：** 减少单文件行数，一个公共类一个文件。
> **风险：** 仅 import 路径变化。

| 操作                         | 原文件                          | 新文件                                     |
|----------------------------|------------------------------|-----------------------------------------|
| 提取 `DatabaseConfig` object | `AppDatabase.kt`             | `data/local/DatabaseConfig.kt`          |
| 提取 `FieldKey` enum         | `EntryTypeResolver.kt`       | `domain/model/FieldKey.kt`              |
| 提取 `FieldLabel` data class | `EntryTypeResolver.kt`       | `core/designsystem/model/FieldLabel.kt` |
| 拆分 9 个策略类                  | `SpecificEntryStrategies.kt` | `domain/strategy/impl/` 下 9 个文件         |
| 拆分 15 个 UseCase 类          | `VaultUseCasesImpl.kt`       | `domain/usecase/vault/` 下各自独立文件         |

### Phase 2 — 数据层重构 (AppDatabase 瘦身)

> **目标：** AppDatabase.kt 从 150 行降到 ~40 行。
> **风险：** 低，仅移动代码位置；Room 注解和运行时行为不变。

| 操作                             | 说明                                                                 |
|--------------------------------|--------------------------------------------------------------------|
| 提取 `DatabasePassphraseManager` | Keystore 口令生成/加解密 → `core/security/`                               |
| 提取 Migration 到独立文件             | `Migration1To2.kt` / `Migration2To3.kt` → `data/local/migration/`  |
| 拆分 `VaultDao`                  | → `VaultEntryDao` (条目操作) + `VaultHistoryDao` (历史操作)                |
| 更新 `AppDatabase`               | 添加 `abstract fun vaultHistoryDao()`；`VaultDataRepository` 接收两个 DAO |

### Phase 3 — BackupManager 拆分

> **目标：** 单一职责，方便独立测试。
> **风险：** 中等，需确保导入/导出兼容性不变。

| 新文件                       | 职责                                                                                |
|---------------------------|-----------------------------------------------------------------------------------|
| `BackupCrypto.kt`         | `deriveKey()`、`readSingleByteOrThrow()`、`readFullyOrThrow()`、`mapImportFailure()` |
| `BackupSerializer.kt`     | `writeEntry()`、`readEntry()`、`JsonReader` 扩展函数                                    |
| `BackupFieldEncryptor.kt` | `encryptEntryFields()`、`decryptField()`                                           |
| `BackupManager.kt`        | 仅保留 `exportBackup()` / `importBackup()` / `getBackupData()` 编排流程                  |

### Phase 4 — UI/DI 整理 (可选，收尾)

> **目标：** 包路径语义清晰。
> **风险：** 极低，仅文件移动。

| 操作                            | 说明                                                                                                            |
|-------------------------------|---------------------------------------------------------------------------------------------------------------|
| 迁移 `core/common/ui/` 下的 UI 类型 | `VaultCardStyleTokens` → `designsystem/tokens/`；`AddType`/`VaultTab`/`VaultCardStyle` → `designsystem/model/` |
| 清空并删除 `core/common/ui/`       | 确保无残余引用                                                                                                       |
| 拆分 `AppContainer.kt`          | → `DataModule.kt` + `DomainModule.kt`，`AppContainer` 做门面委托                                                    |

---

## ❌ 不要动的部分

| 文件/结构                                            | 原因                                         |
|--------------------------------------------------|--------------------------------------------|
| `VaultEntryEntity.kt` / `VaultEntry.kt` (40+ 字段) | Room 单表设计的必然结果，拆表需 schema 迁移 + 备份格式变更，风险极高 |
| `VaultEntryMapper.kt` (120+ 行)                   | 虽然冗长但机械正确，1:1 字段映射是最安全的模式                  |
| `VaultViewModel.kt` (370 行)                      | 已通过 7 个 internal 支持类良好拆分                   |
| `features/detail/sections/`                      | 一个入口类型一个 Section 文件，已是理想模式                 |
| `features/vault/internal/`                       | 7 个专职支持类，职责清晰                              |
| 手动 DI (`AppContainer`)                           | 单模块 app 无需引入 Hilt/Koin                     |
| `SettingsUseCases.kt` / `UserConfigUseCases.kt`  | 薄门面类，拆分无意义                                 |

---

## 📏 命名规范

| 类型         | 规范                             | 示例                         |
|------------|--------------------------------|----------------------------|
| 策略实现       | `{EntryType名}EntryStrategy.kt` | `BankCardEntryStrategy.kt` |
| UseCase    | `{动词}{名词}UseCase.kt`           | `InsertEntryUseCase.kt`    |
| DAO        | `{领域}Dao.kt`                   | `VaultEntryDao.kt`         |
| Migration  | `Migration{From}To{To}.kt`     | `Migration2To3.kt`         |
| Backup 协作者 | `Backup{职责}.kt`                | `BackupSerializer.kt`      |
| DI 模块      | `{Layer}Module.kt`             | `DataModule.kt`            |

---

## ⚡ 执行优先级建议

```
Phase 1 (策略/UseCase 拆文件)  ←  立即做，收益最大，风险最小
  ↓
Phase 2 (AppDatabase 瘦身)     ←  紧接着做，消除最大的 god-file
  ↓
Phase 3 (BackupManager 拆分)   ←  有空做，提升可测试性
  ↓
Phase 4 (UI/DI 整理)           ←  收尾，非紧急
```

**每个 Phase 完成后都做一次编译验证：**
```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
```

```tree
com.aozijx.passly/
├── AppContext.kt                          # (unchanged)
├── MainActivity.kt                        # (unchanged)
├── MainViewModel.kt                       # (unchanged)
│
├── core/
│   ├── common/
│   │   ├── EntryType.kt                   # (unchanged)
│   │   ├── EntryCategory.kt              # ← extract from EntryType.kt (currently embedded)
│   │   ├── AutofillUiMode.kt             # (unchanged)
│   │   └── SwipeActionType.kt            # (unchanged)
│   ├── crypto/
│   │   └── CryptoManager.kt              # (unchanged)
│   ├── di/
│   │   ├── AppContainer.kt               # ← keep, but split into sections via internal objects
│   │   ├── DataModule.kt                 # ← extracted: DB, DAOs, repositories
│   │   └── DomainModule.kt              # ← extracted: use cases, strategies
│   ├── logging/
│   │   └── Logcat.kt                     # (unchanged)
│   ├── media/
│   │   ├── FaviconUtils.kt              # (unchanged)
│   │   ├── IconPathResolver.kt           # (unchanged)
│   │   └── PickPhoto.kt                 # (unchanged)
│   ├── platform/
│   │   └── ClipboardUtils.kt            # (unchanged)
│   ├── qr/
│   │   └── QrCodeUtils.kt               # (unchanged)
│   ├── security/
│   │   ├── DatabasePassphraseManager.kt  # ← extracted from AppDatabase.kt companion
│   │   └── otp/
│   │       ├── TwoFAUtils.kt            # (unchanged)
│   │       └── TotpUtils.kt             # (unchanged)
│   └── storage/
│       └── VaultFileUtils.kt            # (unchanged)
│
├── data/
│   ├── entity/
│   │   ├── VaultEntryEntity.kt           # (unchanged — Room constraint)
│   │   └── VaultHistoryEntity.kt         # (unchanged)
│   ├── local/
│   │   ├── AppDatabase.kt               # ← slimmed: only @Database class + getDatabase()
│   │   ├── DatabaseConfig.kt            # ← extracted object from AppDatabase.kt
│   │   ├── migration/
│   │   │   ├── Migration1To2.kt         # ← extracted from AppDatabase.kt
│   │   │   └── Migration2To3.kt         # ← extracted from AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── VaultEntryDao.kt         # ← split: CRUD + query for entries
│   │   │   └── VaultHistoryDao.kt       # ← split: history-specific methods
│   │   ├── Converters.kt                # (unchanged)
│   │   └── config/
│   │       └── UserConfigFileStore.kt   # (unchanged)
│   ├── local/prefs/
│   │   └── AppPrefs.kt                  # ← moved from data/local/
│   ├── mapper/
│   │   └── VaultEntryMapper.kt          # (unchanged)
│   └── repository/
│       ├── VaultDataRepository.kt        # (unchanged)
│       ├── AutofillServiceDataRepository.kt # (unchanged)
│       ├── FaviconDataRepository.kt      # (unchanged)
│       ├── SettingsDataRepository.kt     # (unchanged)
│       └── UserConfigDataRepository.kt   # (unchanged)
│
├── domain/
│   ├── mapper/
│   │   └── VaultSummaryMapper.kt         # (unchanged)
│   ├── model/
│   │   ├── VaultEntry.kt                 # (unchanged — mirrors entity by design)
│   │   ├── VaultHistory.kt               # (unchanged)
│   │   ├── VaultSummary.kt               # (unchanged)
│   │   ├── TotpConfig.kt                 # (unchanged)
│   │   ├── UserConfig.kt                 # (unchanged)
│   │   ├── AutofillCandidate.kt          # (unchanged)
│   │   ├── FaviconOutcome.kt             # (unchanged)
│   │   └── FieldKey.kt                  # ← extracted enum from EntryTypeResolver.kt
│   ├── policy/
│   │   └── AutofillTitlePolicy.kt        # (unchanged)
│   ├── repository/
│   │   └── (all 5 interfaces unchanged)
│   ├── strategy/
│   │   ├── EntryTypeStrategy.kt          # ← keep interface + FieldGroup/FieldDefinition/FieldType/ValidationResult
│   │   ├── EntryTypeStrategyFactory.kt  # ← extracted from EntryTypeStrategy.kt
│   │   ├── EntryTypeStrategyRegistry.kt  # (unchanged)
│   │   └── impl/
│   │       ├── PasswordEntryStrategy.kt  # ← 1 file per strategy (9 files total)
│   │       ├── WiFiEntryStrategy.kt
│   │       ├── BankCardEntryStrategy.kt
│   │       ├── SshKeyEntryStrategy.kt
│   │       ├── SeedPhraseEntryStrategy.kt
│   │       ├── PasskeyEntryStrategy.kt
│   │       ├── RecoveryCodeEntryStrategy.kt
│   │       ├── TotpEntryStrategy.kt
│   │       └── IdCardEntryStrategy.kt
│   └── usecase/
│       ├── vault/
│       │   ├── VaultUseCases.kt          # (unchanged wiring class)
│       │   ├── ObserveAllEntriesUseCase.kt       # ← 1 file per use case
│       │   ├── ObserveAllEntrySummariesUseCase.kt
│       │   ├── GetEntriesByCategoryUseCase.kt
│       │   ├── GetEntrySummariesByCategoryUseCase.kt
│       │   ├── SearchEntriesUseCase.kt
│       │   ├── SearchEntrySummariesUseCase.kt
│       │   ├── GetCategoriesUseCase.kt
│       │   ├── GetHistoryByEntryIdUseCase.kt
│       │   ├── GetEntryByIdUseCase.kt
│       │   ├── InsertEntryUseCase.kt
│       │   ├── UpdateEntryUseCase.kt
│       │   ├── DeleteEntryUseCase.kt
│       │   ├── DeleteAllEntriesUseCase.kt
│       │   ├── GetTotpCodeUseCase.kt
│       │   └── DownloadFaviconUseCase.kt
│       ├── settings/
│       │   └── SettingsUseCases.kt       # (unchanged)
│       └── userconfig/
│           └── UserConfigUseCases.kt     # (unchchanged)
│
├── features/
│   ├── vault/                             # (internal structure unchanged — already well-decomposed)
│   ├── detail/                            # (unchanged)
│   ├── settings/                          # (unchanged)
│   └── scanner/                           # (unchanged)
│
├── service/
│   ├── autofill/                          # (unchanged)
│   └── backup/                            # (unchanged)
│
├── core/backup/
│   ├── BackupManager.kt                  # ← slimmed: orchestration only (export/import flows)
│   ├── BackupCrypto.kt                   # ← extracted: deriveKey, cipher init, read helpers
│   ├── BackupSerializer.kt              # ← extracted: writeEntry, readEntry, JSON extensions
│   └── BackupFieldEncryptor.kt          # ← extracted: encryptEntryFields, decryptField
│
└── core/designsystem/
├── base/                              # (unchanged)
├── components/                        # (unchanged)
├── fields/                            # (unchanged)
├── icons/                             # (unchanged)
├── sections/                          # (unchanged)
├── state/                             # (unchanged)
├── tokens/
│   └── VaultCardStyleTokens.kt       # ← moved from core/common/ui/
├── model/
│   ├── VaultCardStyle.kt             # ← moved from core/common/ui/UiTypes.kt
│   ├── AddType.kt                    # ← moved from core/common/ui/UiActionTypes.kt (+ ImageType)
│   ├── VaultTab.kt                   # ← moved from core/common/ui/UiStateTypes.kt (+ DeleteState, EditMode)
│   └── FieldLabel.kt                 # ← extracted from EntryTypeResolver.kt
├── utils/
│   ├── EntryTypeFieldMapper.kt       # (unchanged)
│   └── EntryTypeResolver.kt          # ← slimmed: logic only, FieldKey/FieldLabel extracted
└── widgets/                           # (unchanged)
```

---

## 🔧 进阶优化（Support -> UseCase -> ViewModel）

> 适用时机：完成当前 Phase 1-4 后，进一步降低 `VaultViewModel` 复杂度并提升测试性。

### 1) Support 到 UseCase 的闭环

**目标**
- Support 只负责 UI 状态映射与协程调度。
- 业务规则统一下沉到 `domain/usecase/vault/`。

**执行清单**
- [ ] 审查 `features/vault/internal/*Support`，标记“业务规则代码块”。
- [ ] 将可下沉逻辑迁移到 UseCase：
  - 例如条目新增后 favicon 下载与更新链路，迁移为独立 UseCase。
  - 例如删除前资源清理策略，迁移为独立 UseCase。
- [ ] Support 层仅保留：
  - 输入参数组装
  - Flow/State 映射
  - 协程调用时序

**验收标准**
- [ ] Support 内不直接编写业务决策（if/when 业务规则分支显著减少）。
- [ ] 新业务需求优先改 UseCase，Support 无需新增领域规则。
- [ ] 关键逻辑可在 domain 层做单测（不依赖 Android Context）。

---

### 2) StateHolder 粒度控制（引入 ActionProcessor 思路）

**目标**
- 当 `VaultViewModel` 超过 300 行时，拆分“状态持有”与“事件处理”。
- `ViewModel` 仅做 State/Effect 出口与依赖编排。

**建议结构**
- `VaultSearchFilterStateHolder`：仅保存筛选/搜索状态。
- `VaultDetailStateHolder`：仅保存详情弹窗与编辑启动标记。
- `VaultActionProcessor`（新增）：集中处理 UI Action -> 状态更新/UseCase 调用。

**执行清单**
- [ ] 定义 `VaultAction`（如 `SearchChanged`、`QuickDelete`、`ShowDetailForEdit`）。
- [ ] 在 `VaultActionProcessor` 中实现 action 分发。
- [ ] `VaultViewModel` 中保留 `onAction(action)` 入口与状态暴露。

**验收标准**
- [ ] `VaultViewModel` 行数下降，方法数量明显收敛。
- [ ] UI 事件处理路径可追踪（入口统一）。
- [ ] 事件处理与状态持有不再耦合在同一方法内。

---

### 3) 依赖注入语义化（按需初始化）

**目标**
- 减少 `ViewModel` 构造时一次性初始化开销。
- 将 Support 实例改为“按需创建”。

**执行清单**
- [ ] `VaultViewModel` 内的 Support 改为 `by lazy` 初始化。
- [ ] 仅在对应功能首次触发时创建对象（如 TOTP/Autofill 支持类）。
- [ ] 保持 `AppContainer` 只管理跨模块共享依赖，页面级 helper 不做全局单例。

**验收标准**
- [ ] 首屏进入 `VaultScreen` 时初始化对象数量下降。
- [ ] 功能行为不变（无懒加载时序问题）。
- [ ] 不引入循环依赖。

---

### 4) `VaultEntryLifecycleSupport` 拆分

**目标**
- 降低单类职责密度，提升可测试性。

**建议拆分**
- `VaultEntryAddSupport`：新增条目/新增后附加动作（如 favicon）。
- `VaultEntryEditSupport`：更新条目/图标保存与路径处理。
- `VaultEntryDeleteSupport`：删除条目/关联资源清理。

**执行清单**
- [ ] 从现有 `VaultEntryLifecycleSupport` 抽出 Add/Edit/Delete 三个支持类。
- [ ] `VaultViewModel` 仅按需注入并调用对应支持类。
- [ ] 为每个支持类补最少 1 个行为测试（成功路径）。

**验收标准**
- [ ] 每个 Support 文件聚焦单一职责（方法数可控）。
- [ ] 删除/更新/新增问题定位更快（调用链清晰）。
- [ ] 变更影响面缩小，回归成本下降。

---

### 建议执行顺序（进阶）

```text
Step A: 拆 LifecycleSupport（收益最高，风险可控）
  -> Step B: Support 业务下沉到 UseCase
  -> Step C: 引入 VaultActionProcessor
  -> Step D: 全量按需初始化与性能回归
```

每一步完成后建议执行：

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
```
