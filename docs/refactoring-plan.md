
# Passly 项目文件结构重构方案

## 📋 当前问题诊断

| # | 问题文件 | 症状 | 严重度 |
|---|---------|------|--------|
| 1 | `AppDatabase.kt` | Room 定义 + 加密口令管理 + 迁移 + DatabaseConfig，全在一个文件 | 🔴 高 |
| 2 | `VaultDao.kt` | 20+ 方法混杂 CRUD / 搜索 / 摘要 / 自动填充 / 历史 | 🟡 中 |
| 3 | `SpecificEntryStrategies.kt` | 9 种条目策略全挤在 400+ 行的单文件 | 🔴 高 |
| 4 | `VaultUseCasesImpl.kt` | 15 个 UseCase 类堆在一个文件 | 🟡 中 |
| 5 | `BackupManager.kt` | 导出 / 导入 / 加密 / JSON 序列化全在一起 | 🟡 中 |
| 6 | `core/common/ui/` | UI Token 与非 UI 通用类型混放 | 🟢 低 |
| 7 | `EntryTypeResolver.kt` | `FieldKey` 枚举嵌在 Resolver 逻辑里 | 🟢 低 |

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

| 操作 | 原文件 | 新文件 |
|------|--------|--------|
| 提取 `DatabaseConfig` object | `AppDatabase.kt` | `data/local/DatabaseConfig.kt` |
| 提取 `FieldKey` enum | `EntryTypeResolver.kt` | `domain/model/FieldKey.kt` |
| 提取 `FieldLabel` data class | `EntryTypeResolver.kt` | `core/designsystem/model/FieldLabel.kt` |
| 拆分 9 个策略类 | `SpecificEntryStrategies.kt` | `domain/strategy/impl/` 下 9 个文件 |
| 拆分 15 个 UseCase 类 | `VaultUseCasesImpl.kt` | `domain/usecase/vault/` 下各自独立文件 |

### Phase 2 — 数据层重构 (AppDatabase 瘦身)

> **目标：** AppDatabase.kt 从 150 行降到 ~40 行。
> **风险：** 低，仅移动代码位置；Room 注解和运行时行为不变。

| 操作 | 说明 |
|------|------|
| 提取 `DatabasePassphraseManager` | Keystore 口令生成/加解密 → `core/security/` |
| 提取 Migration 到独立文件 | `Migration1To2.kt` / `Migration2To3.kt` → `data/local/migration/` |
| 拆分 `VaultDao` | → `VaultEntryDao` (条目操作) + `VaultHistoryDao` (历史操作) |
| 更新 `AppDatabase` | 添加 `abstract fun vaultHistoryDao()`；`VaultDataRepository` 接收两个 DAO |

### Phase 3 — BackupManager 拆分

> **目标：** 单一职责，方便独立测试。
> **风险：** 中等，需确保导入/导出兼容性不变。

| 新文件 | 职责 |
|--------|------|
| `BackupCrypto.kt` | `deriveKey()`、`readSingleByteOrThrow()`、`readFullyOrThrow()`、`mapImportFailure()` |
| `BackupSerializer.kt` | `writeEntry()`、`readEntry()`、`JsonReader` 扩展函数 |
| `BackupFieldEncryptor.kt` | `encryptEntryFields()`、`decryptField()` |
| `BackupManager.kt` | 仅保留 `exportBackup()` / `importBackup()` / `getBackupData()` 编排流程 |

### Phase 4 — UI/DI 整理 (可选，收尾)

> **目标：** 包路径语义清晰。
> **风险：** 极低，仅文件移动。

| 操作 | 说明 |
|------|------|
| 迁移 `core/common/ui/` 下的 UI 类型 | `VaultCardStyleTokens` → `designsystem/tokens/`；`AddType`/`VaultTab`/`VaultCardStyle` → `designsystem/model/` |
| 清空并删除 `core/common/ui/` | 确保无残余引用 |
| 拆分 `AppContainer.kt` | → `DataModule.kt` + `DomainModule.kt`，`AppContainer` 做门面委托 |

---

## ❌ 不要动的部分

| 文件/结构 | 原因 |
|-----------|------|
| `VaultEntryEntity.kt` / `VaultEntry.kt` (40+ 字段) | Room 单表设计的必然结果，拆表需 schema 迁移 + 备份格式变更，风险极高 |
| `VaultEntryMapper.kt` (120+ 行) | 虽然冗长但机械正确，1:1 字段映射是最安全的模式 |
| `VaultViewModel.kt` (370 行) | 已通过 7 个 internal 支持类良好拆分 |
| `features/detail/sections/` | 一个入口类型一个 Section 文件，已是理想模式 |
| `features/vault/internal/` | 7 个专职支持类，职责清晰 |
| 手动 DI (`AppContainer`) | 单模块 app 无需引入 Hilt/Koin |
| `SettingsUseCases.kt` / `UserConfigUseCases.kt` | 薄门面类，拆分无意义 |

---

## 📏 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 策略实现 | `{EntryType名}EntryStrategy.kt` | `BankCardEntryStrategy.kt` |
| UseCase | `{动词}{名词}UseCase.kt` | `InsertEntryUseCase.kt` |
| DAO | `{领域}Dao.kt` | `VaultEntryDao.kt` |
| Migration | `Migration{From}To{To}.kt` | `Migration2To3.kt` |
| Backup 协作者 | `Backup{职责}.kt` | `BackupSerializer.kt` |
| DI 模块 | `{Layer}Module.kt` | `DataModule.kt` |

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
│           └── UserConfigUseCases.kt     # (unchanged)
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