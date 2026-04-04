# Passly 开发者项目文档

本文档面向 **新加入开发者** 和 **准备做功能改造/重构的同学**，目标是：

- 快速建立项目全局认知
- 知道每类需求应该改哪里
- 知道哪些地方是高风险（加密、备份、数据库、Autofill）
- 在本地高效验证改动

---

## 文档矩阵（先看这个）

| 文档 | 作用 | 什么时候看 |
| --- | --- | --- |
| `docs/INDEX.md` | 文档统一导航入口 | 不确定先读哪份文档时 |
| `README.md` | 项目入口与最小上手信息 | 第一次进入项目时 |
| `docs/PROJECT_STRUCTURE.md` | 目录结构与模块职责速查 | 需要快速定位代码落点时 |
| `docs/DEVELOPER_GUIDE.md` | 架构认知、模块定位、常见开发路径 | 准备开始开发或重构前 |
| `docs/ARCHITECTURE_DECISIONS.md` | 关键技术决策与“为什么这样做” | 设计方案评审、跨模块改动前 |
| `docs/CHANGE_PLAYBOOK.md` | 高风险改动执行清单（步骤/验证/回滚） | 准备提交数据库/备份/样式/Autofill改动时 |

推荐顺序：`README.md` -> `docs/INDEX.md` -> `docs/DEVELOPER_GUIDE.md` -> `docs/ARCHITECTURE_DECISIONS.md` -> `docs/CHANGE_PLAYBOOK.md`

---

## 文档更新基线

当代码改动影响“架构决策”或“执行步骤”时，文档同步遵循：

1. 先判断是否触发 ADR 变更（`docs/ARCHITECTURE_DECISIONS.md`）。
2. 再更新执行清单（`docs/CHANGE_PLAYBOOK.md`）。
3. 最后检查导航与入口是否需要调整（`docs/INDEX.md` / `README.md`）。

PR 建议至少包含以下一项文档更新：

- 新决策（或决策状态变化）
- 新的验证步骤/回滚步骤
- 新增模块的导航入口

---

## 1. 项目定位与原则

Passly 是一款离线优先的隐私保险库应用，核心原则：

1. **安全优先**：敏感数据加密存储，最小暴露面。
2. **离线优先**：不依赖云端和网络请求。
3. **分层清晰**：按 Clean Architecture + Feature 包组织。
4. **可维护性优先**：策略、映射、DI、样式配置集中管理。

你在做改动时，优先问自己两件事：

- 这个改动是否影响加密/备份/数据库兼容？
- 这个逻辑应该属于 `domain/usecase` 还是 `feature UI`？

---

## 2. 快速上手（Windows / PowerShell）

### 2.1 环境要求

- Android Studio Otter (2024.2.2)+
- JDK 21
- Gradle 8.13+
- Android 12+ (API 31+)

### 2.2 常用命令

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat clean :app:assembleDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:compileFullDebugKotlin
```

> 说明：本项目存在多变体，编译 Kotlin 时建议显式使用变体任务（如 `compileFullDebugKotlin`）。

### 2.3 日志排查

```powershell
adb logcat -s PasslyTag
```

或使用应用内部日志模块：`app/src/main/java/com/aozijx/passly/core/logging/Logcat.kt`。

---

## 3. 架构总览（你需要先建立这个地图）

## 3.1 分层结构

- `core/`：跨模块基础能力（加密、备份、DI、设计系统、日志等）
- `data/`：本地数据实现（Room/DataStore、实体、Mapper、Repository 实现）
- `domain/`：业务抽象和用例（Model、Strategy、Repository 接口、UseCase）
- `features/`：业务页面和交互（vault/detail/settings/scanner）
- `service/`：系统服务能力（Autofill 等）
- `ui/`：全局主题

## 3.2 请求流（典型）

UI 事件 -> ViewModel -> UseCase -> Repository 接口 -> Data Repository 实现 -> Local DB/DataStore

## 3.3 依赖注入

项目不使用 Hilt/Dagger，统一由 `AppContainer` 手工构建：

- `app/src/main/java/com/aozijx/passly/core/di/AppContainer.kt`

新增全局依赖时请优先在该处集中接线，避免散落构造。

---

## 4. 关键文件导航（建议阅读顺序）

1. `README.md`
2. `AGENTS.md`
3. `app/src/main/java/com/aozijx/passly/core/di/AppContainer.kt`
4. `app/src/main/java/com/aozijx/passly/data/local/AppDatabase.kt`
5. `app/src/main/java/com/aozijx/passly/core/crypto/CryptoManager.kt`
6. `app/src/main/java/com/aozijx/passly/core/backup/BackupManager.kt`
7. `app/src/main/java/com/aozijx/passly/features/vault/VaultViewModel.kt`
8. `app/src/main/java/com/aozijx/passly/domain/strategy/EntryTypeStrategyRegistry.kt`
9. `app/src/main/java/com/aozijx/passly/data/mapper/VaultEntryMapper.kt`
10. `app/src/main/java/com/aozijx/passly/service/autofill/` 下关键文件

---

## 5. 你改不同功能时，应该改哪里

## 5.1 新增条目类型（Strategy）

1. 在 `domain/strategy/impl/` 新建策略实现
2. 在 `domain/strategy/impl/SpecificEntryStrategies.kt` 注册
3. 需要持久化字段时改 `data/entity` + `data/mapper/VaultEntryMapper.kt`
4. 需要 UI 字段时改对应 feature 页面组件

## 5.2 数据库字段变更

1. 修改实体
2. 提升 DB 版本：`data/local/AppDatabase.kt`
3. 添加 migration
4. 验证备份导入导出兼容：`core/backup/BackupManager.kt`

## 5.3 设置项/开关逻辑

- 配置源：`data/local/AppPrefs.kt`
- 仓库实现：`data/repository/SettingsDataRepository.kt`
- UI 页面：`features/settings/`

## 5.4 Vault 列表卡片样式

- 样式分发：`features/vault/components/entries/VaultCardStyleRegistry.kt`
- 样式实现：`PasswordStyleVaultItem.kt`、`TotpStyleVaultItem.kt`、`core/designsystem/base/VaultItem.kt`
- 统一样式 token：`core/common/ui/VaultCardStyleTokens.kt`
- 设置页配置：`features/settings/internal/CardStyleSettingsConfig.kt`

---

## 6. UI 样式系统（重点，最近重构过）

为了让“圆角、间距、背景 alpha、字体尺寸”等可集中调优，已引入统一 token：

- `app/src/main/java/com/aozijx/passly/core/common/ui/VaultCardStyleTokens.kt`

建议规则：

1. **跨卡片共用参数** 放 `Base`
2. **样式特有参数** 放 `Password` / `Totp`
3. **行为常量**（如阈值、状态标签）保留在对应组件本地，不混入纯样式 token

这样后续你要统一“更圆”“更紧凑”“深色背景更柔和”时，只改 token 文件即可。

---

## 7. Detail 模块快速理解

入口 ViewModel：

- `app/src/main/java/com/aozijx/passly/features/detail/DetailViewModel.kt`

职责：

- 处理 `DetailEvent`（初始化、同步、标题编辑、收藏切换）
- 通过 `DetailEntryAnalyzer` 分析条目策略状态
- 管理 `DetailUiState`（标题编辑态、策略摘要、校验错误等）

关键点：

- `Initialize` 会尝试拉取最新 entry 再刷新状态
- `SaveTitle` 只返回有变化的 `VaultEntry`（供外层持久化）
- `ToggleFavorite` 仅做状态层 copy，持久化由调用方决定

---

## 8. 安全与高风险改动红线

以下改动必须做“联动审查”：

1. **加密存储逻辑**：`CryptoManager`、数据库 passphrase、备份格式
2. **数据库 schema**：migration + 历史数据兼容
3. **备份格式版本**：导入导出向后兼容策略
4. **Autofill 查询/匹配**：性能阈值与隐私边界
5. **新增网络能力**：与项目“离线优先”原则冲突，需专项评估

---

## 9. 常见开发任务 SOP（可直接照做）

## 9.1 新增 UseCase

1. 在 `domain/usecase/` 增加 usecase
2. 若需要数据访问，扩展 `domain/repository` 接口
3. 在 `data/repository` 实现
4. 在 `AppContainer` 接线
5. 页面侧 ViewModel 只调用 usecase

## 9.2 新增设置项

1. `AppPrefs` 增加 key 与默认值
2. `SettingsDataRepository` 增加读写 API
3. `SettingsViewModel` 衔接事件与状态
4. `features/settings/components` 增加 UI

## 9.3 调整列表卡片视觉

1. 优先改 `VaultCardStyleTokens.kt`
2. 必要时调整具体组件内部结构
3. 设置页预览通过 `VaultCardStyleRegistry.RenderPreviewVaultItem` 回归

---

## 10. 调试与排障建议

### 10.1 先看这三类问题

1. **状态不同步**：ViewModel 的 StateFlow 更新时机
2. **样式不一致**：是否存在未接入 token 的硬编码值
3. **性能波动**：Autofill/TOTP 是否引入额外重计算

### 10.2 排障顺序

1. 复现路径固定
2. 观察日志
3. 缩小到单层（UI / ViewModel / UseCase / Repo）
4. 最后才做跨层重构

---

## 11. CI 与质量门槛

- CI 使用 GitHub Actions + CodeQL
- CodeQL 采用 `build-mode: none`
- 本地最低建议执行：

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
```

---

## 12. 新人 30 分钟上手路径

1. 阅读 `README.md`
2. 阅读 `AGENTS.md`
3. 打开 `AppContainer.kt` 了解依赖组织
4. 跑 `:app:compileFullDebugKotlin`
5. 看 `VaultViewModel.kt` + `DetailViewModel.kt`
6. 看卡片渲染链：`VaultCardStyleRegistry.kt` -> 具体卡片组件
7. 看设置页样式配置：`CardStyleSettingsConfig.kt`

---

## 13. 代码风格与改动建议

- 小步改动，单点验证
- 先抽“通用配置/常量”，再改逻辑
- 谨慎修改高风险模块（加密、备份、数据库、autofill）
- 尽量让 UI 组件无业务副作用

---

## 14. 后续可继续完善（建议）

1. 增加最小化回归清单模板（发版前执行）
2. 增加术语表（中英对照，降低沟通成本）
3. 增加“新功能开发模板”（需求 -> 设计 -> 落地 -> 验证）

---
