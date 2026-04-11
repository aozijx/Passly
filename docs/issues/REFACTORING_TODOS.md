# Passly 项目待重构事项

本文档追踪项目中需要重构的 ViewModel、UseCase 及相关代码。

**最后更新**: 2026-04-11

---

## 优先级说明

- 🔴 **高优先级** - 架构问题或严重代码异味
- 🟡 **中优先级** - 代码组织或职责分离问题
- 🟢 **低优先级** - 代码风格或可优化项

---

## ViewModel 重构

### 🔴 1. VaultViewModel 职责过重

**位置**: `features/vault/VaultViewModel.kt`

**问题**:
- 包含了过多辅助类（7 个 Support 类）
- 直接管理多个 StateHolder
- 存在可变状态 (`var isSearchActive`, `var isMoreMenuExpanded` 等)
- 混合了搜索、过滤、详情、TOTP、文件处理等多个职责

**当前依赖**:
```kotlin
private val vaultUseCases = AppContainer.vaultUseCases
private val autofillSupport = VaultAutofillSupport()
private val cryptoSupport = VaultCryptoSupport()
private val totpSupport = VaultTotpSupport()
private val entryFileSupport = VaultEntryFileSupport()
private val entryLifecycleSupport = VaultEntryLifecycleSupport(...)
private val onDemandQuerySupport = VaultOnDemandQuerySupport(...)
private val searchFilterState = VaultSearchFilterStateHolder()
private val detailState = VaultDetailStateHolder()
```

**建议重构方案**:
1. 拆分为多个专用 ViewModel:
   - `VaultListViewModel` - 列表展示和查询
   - `VaultSearchViewModel` - 搜索和过滤
   - `VaultDetailViewModel` - 详情处理（已有独立的 DetailViewModel，需确认职责边界）
2. 将 Support 类提升为 UseCase 或 Repository
3. 使用不可变 StateFlow 替代可变 var

**影响范围**: 大（需要调整整个 Vault 模块结构）

---

### 🟡 2. SettingsViewModel 状态管理复杂

**位置**: `features/settings/SettingsViewModel.kt`

**问题**:
- 存在多个 data class 状态（`CoreSettingsFlowState`, `InteractionSettingsFlowState` 等）
- 状态分组过多（4 个中间状态类）
- 依赖 `BackupActionSupport` 内部类

**当前状态类**:
```kotlin
data class SettingsUiState(...) // 主状态
private data class CoreSettingsFlowState(...)
private data class InteractionSettingsFlowState(...)
private data class SecurityAndStyleFlowState(...)
private data class AutofillAndSwipeFlowState(...)
```

**建议重构方案**:
1. 简化状态分组，合并相关状态
2. 将 `BackupActionSupport` 提升为独立的 UseCase
3. 考虑使用 MVI 统一事件处理

**影响范围**: 中

---

### 🟡 3. DetailViewModel 职责边界不清晰

**位置**: `features/detail/DetailViewModel.kt`

**问题**:
- `onEvent` 方法返回 `VaultEntry?`，调用方需要处理返回值，不符合纯 MVI 模式
- 混合了状态更新和实体返回
- 缺少独立的事件处理结果反馈机制

**当前签名**:
```kotlin
fun onEvent(event: DetailEvent): VaultEntry?
```

**建议重构方案**:
1. 改为纯事件处理，不返回实体
2. 通过 StateFlow 状态变化反馈结果
3. 或使用 Result/Either 类型封装操作结果

**影响范围**: 中（需要调整调用方逻辑）

---

### 🟢 4. MainViewModel 功能单一

**位置**: `MainViewModel.kt`

**问题**:
- 功能过于简单，可能不需要独立 ViewModel
- 可能只是简单的导航中转

**建议重构方案**:
- 评估是否可以合并到 MainActivity 或使用 Navigation Component 替代

**影响范围**: 小

---

### 🟢 5. ScannerViewModel 待评估

**位置**: `core/qr/ScannerViewModel.kt`

**问题**:
- 位于 `core` 层，但 ViewModel 应该在 `features` 层
- 职责边界不清晰

**建议重构方案**:
- 移动到 `features/scanner/` 目录
- 明确职责（仅扫码 UI 状态管理）

**影响范围**: 小

---

## UseCase 重构

### 🟡 6. VaultUseCases 和 DetailUseCases 职责重叠

**位置**: 
- `domain/usecase/vault/VaultUseCases.kt`
- `domain/usecase/detail/DetailUseCases.kt`

**问题**:
- 两者都包含 `getEntryById`, `updateEntry`, `downloadFavicon`
- 职责划分不清晰（Vault 主页 vs 详情页）
- 可能存在重复调用

**当前重叠**:
```kotlin
// VaultUseCases
val getEntryById = GetEntryByIdUseCase(vaultRepository)
val updateEntry = UpdateEntryUseCase(vaultRepository)
val downloadFavicon = DownloadFaviconUseCase(faviconRepository)

// DetailUseCases
val getEntryById = GetEntryByIdUseCase(vaultRepository)
val updateEntry = UpdateEntryUseCase(vaultRepository)
val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
```

**建议重构方案**:
1. 合并为统一的 `VaultUseCases`
2. 按功能而非页面划分 UseCase
3. 或明确区分：Vault 列表操作用例 vs Detail 编辑用例

**影响范围**: 中

---

### 🟡 7. AutofillUseCases 组织松散

**位置**: `domain/usecase/autofill/`

**问题**:
- `AutofillUseCases.kt` 聚合类存在，但实现类分散
- 缺少统一的入口

**当前结构**:
```
autofill/
├── AutofillUseCases.kt (聚合)
└── impl/
    ├── UpdateUsageStatsUseCase.kt
    ├── SaveOrUpdateEntryUseCase.kt
    ├── GetEntryByIdUseCase.kt
    ├── GetEntriesByIdsUseCase.kt
    └── FindMatchingCandidatesUseCase.kt
```

**建议重构方案**:
- 保持当前结构，但需确保所有 UseCase 都通过聚合类访问

**影响范围**: 小

---

### 🟢 8. SettingsUseCases 待检查

**位置**: `domain/usecase/settings/SettingsUseCases.kt`

**问题**:
- 需要评估是否所有设置操作都已封装为 UseCase
- 可能存在直接调用 Repository 的情况

**建议重构方案**:
- 审查 SettingsViewModel 中是否所有数据访问都通过 UseCase

**影响范围**: 小

---

## 内部类和支持类

### 🟡 9. VaultDetailStateHolder 使用可变状态

**位置**: `features/vault/internal/VaultDetailStateHolder.kt`

**问题**:
- 使用 `var ... by mutableStateOf(...)` 
- 不符合不可变状态原则

**当前代码**:
```kotlin
var addType by mutableStateOf(AddType.NONE)
var detailItem by mutableStateOf<VaultEntry?>(null)
var itemToDelete by mutableStateOf<VaultEntry?>(null)
var showIconPicker by mutableStateOf(false)
// ...
```

**建议重构方案**:
1. 改为不可变 data class + StateFlow
2. 通过事件驱动状态变化

**影响范围**: 中

---

### 🟡 10. VaultSearchFilterStateHolder 待评估

**位置**: `features/vault/internal/VaultSearchFilterStateHolder.kt`

**问题**:
- 需要评估是否符合不可变状态原则
- 可能存在类似 StateHolder 的问题

**建议重构方案**:
- 参照 StateHolder 重构方案

**影响范围**: 中

---

## 架构层面问题

### 🟡 11. Support 类职责不统一

**问题**:
- 部分 Support 类应该提升为 UseCase
- 部分 Support 类应该合并到 Repository
- 命名不统一（Support/Helper/Manager 混用）

**当前 Support 类**:
- `VaultAutofillSupport`
- `VaultCryptoSupport`
- `VaultTotpSupport`
- `VaultEntryFileSupport`
- `VaultEntryLifecycleSupport`
- `VaultOnDemandQuerySupport`
- `BackupActionSupport`

**建议重构方案**:
1. 制定 Support 类命名和使用规范
2. 逐步迁移到 UseCase 或 Repository 层
3. 统一命名约定（UseCase/Repository/Manager）

**影响范围**: 大

---

### 🟢 12. Repository 接口和实现边界

**问题**:
- 需要审查 Repository 是否过度暴露底层实现
- UseCase 是否过度依赖 Repository

**建议重构方案**:
- 审查 Repository 接口设计
- 确保 UseCase 不直接访问数据源

**影响范围**: 中

---

## 已完成重构（参考）

- ✅ Strategy 文件拆分（9 个独立策略文件）
- ✅ UseCase 文件拆分（15 个独立用例文件）
- ✅ 数据库迁移链收敛
- ✅ 备份系统重构（V1-Reset 架构）

---

## 重构优先级建议

### Phase 1 - 高优先级（下一迭代）
1. VaultViewModel 拆分
2. VaultUseCases 和 DetailUseCases 合并/重划分

### Phase 2 - 中优先级（下下迭代）
3. SettingsViewModel 简化
4. DetailViewModel 纯 MVI 化
5. StateHolder 不可变化

### Phase 3 - 低优先级（后续优化）
6. Support 类统一规范
7. 其他 ViewModel 评估和调整

---

## 新增问题模板

```markdown
### 🟡 [优先级] 问题标题
- **位置**: `文件路径`
- **问题**: 问题描述
- **当前代码**: （可选）关键代码片段
- **建议重构方案**: 解决方案
- **影响范围**: 大/中/小
```

---

## 相关链接

- [架构决策](../architecture/ARCHITECTURE_DECISIONS.md)
- [开发者指南](../getting-started/DEVELOPER_GUIDE.md)
- [状态管理规范](../standards/STATE_MANAGEMENT.md)
