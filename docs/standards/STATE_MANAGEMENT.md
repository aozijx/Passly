# State 与信息流管理规范

## 概述

本文档定义了 Passly 项目中基于 MVI 架构的 State 管理和信息流规范。

---

## 核心原则

1. **单一真相来源** - 使用不可变的 `data class` 作为 UI 状态的唯一来源
2. **状态分层** - 清晰分离 UI 状态和内部运行时状态
3. **事件驱动** - 通过统一的事件系统处理所有状态变更
4. **数据流单向** - UI → Event → ViewModel → State → UI
5. **类型安全** - 使用密封类（sealed class）确保事件和状态的类型安全

---

## 关键概念

| 概念 | 说明 | 示例 |
|------|------|------|
| UiState | UI 层状态，不可变 data class | `DetailUiState` |
| InternalState | ViewModel 内部运行时状态 | `DetailInternalState` |
| Event | 用户交互事件，密封类 | `DetailEvent` |
| Field | 可编辑字段枚举 | `DetailField` |

---

## 规范 1: 状态设计

### 1.1 UI 状态类（UiState）

**规则**:
- 必须使用不可变 `data class`
- 所有字段必须有默认值
- 按职责分组设计
- 禁止使用可变字段（`var`）

**示例**:
```kotlin
data class DetailUiState(
    val data: DetailDataUiState = DetailDataUiState(),
    val control: DetailControlUiState = DetailControlUiState(),
    val edit: DetailEditUiState = DetailEditUiState(),
    val display: DetailRevealUiState = DetailRevealUiState(),
    val draft: DetailDraftUiState = DetailDraftUiState(),
    val totp: DetailTotpUiState = DetailTotpUiState(),
    val icon: DetailIconUiState = DetailIconUiState()
)
```

### 1.2 状态分组原则

按业务职责分组（data, edit, draft, display 等），每组独立，便于局部更新。

### 1.3 内部状态（InternalState）

用于 ViewModel 内部运行时状态追踪，不直接用于 UI 渲染。

```kotlin
internal data class DetailInternalState(
    val credential: DetailCredentialInternalState = DetailCredentialInternalState(),
    val iconCache: DetailIconCacheInternalState = DetailIconCacheInternalState()
)
```

---

## 规范 2: 事件系统

### 2.1 事件定义

必须使用密封类（sealed class 或 sealed interface）

事件命名使用动词 + 名词结构

事件参数使用 val 声明

```kotlin
sealed interface DetailEvent {
    data class Initialize(val initialEntry: VaultEntry) : DetailEvent
    data class ToggleEditMode(val field: DetailField, val isEditing: Boolean) : DetailEvent
    data class UpdateDraft(val field: DetailField, val value: String) : DetailEvent
    // ...
}
```

### 2.2 字段枚举

使用 enum class 定义字段标识，完整覆盖所有可编辑字段。

```kotlin
enum class DetailField {
    TITLE, USERNAME, PASSWORD, CATEGORY, NOTES,
    DOMAIN, PACKAGE, TOTP, FAVORITE,
    TOTP_SECRET, TOTP_PERIOD, TOTP_DIGITS, TOTP_ALGORITHM,
    RECOVERY_CODES, PASSKEY_DATA, ID_NUMBER
}
```

---

## 规范 3: ViewModel 实现

### 3.1 状态管理

使用 MutableStateFlow<UiState> 管理 UI 状态，使用 copy() 进行不可变更新。

```kotlin
class DetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())
    
    fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.UpdateDraft -> {
                _uiState.update { current ->
                    current.copy(
                        draft = updateDraft(current.draft, event.field, event.value)
                    )
                }
            }
        }
    }
}
```

### 3.2 事件处理

- 所有状态变更必须通过事件处理
- 使用 when 表达式处理所有事件类型
- 批量更新相关状态，避免多次调用 _uiState.update

---

## 规范 4: UI 组件设计

### 4.1 Composable 函数签名

参数顺序：modifier → 数据源 → 回调 → 依赖 → 其他可选参数。

```kotlin
@Composable
fun CredentialSection(
    modifier: Modifier = Modifier,
    entry: VaultEntry,
    uiState: DetailUiState,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailEvent) -> Unit = {}
)
```

### 4.2 状态读取与事件发送

- 从 uiState 参数读取状态，禁止直接访问 ViewModel 状态
- 通过 onEvent 参数发送事件，禁止直接调用 ViewModel 方法

### 4.3 敏感数据处理

使用 display 状态存储解密后的明文，通过 ToggleDisclosure 事件控制显示/隐藏。

```kotlin
// 解密后存储到 display 状态
onEvent(DetailEvent.ToggleDisclosure(DetailField.PASSWORD, decrypted))

// UI 显示
val displayState = uiState.display
Text(displayState.plainPassword ?: "••••••")
```

---

## 规范 5: 工具函数

### 加密保存工具

```kotlin
internal fun saveEncrypted(
    newValue: String,
    oldValue: String?,
    onClose: () -> Unit,
    onSuccess: (String) -> Unit
) {
    if (newValue == oldValue) {
        onClose()
    } else {
        val encrypted = CryptoManager.encrypt(newValue)
        onSuccess(encrypted)
        onClose()
    }
}
```

---

## 规范 6: 数据流

单向数据流：用户交互 → Event → ViewModel → State → UI 刷新。

使用 derivedStateOf 计算派生状态，避免在 Composable 中创建新对象。

---

## 规范 7: 测试

详见测试规范。

---

## 规范 8: 文件组织

详见文件组织规范。

---

## 附录：实施检查清单

### 状态定义
- [ ] 使用不可变 `data class`
- [ ] 所有字段有默认值
- [ ] 按职责分组（data, edit, draft, display 等）
- [ ] 内部状态与 UI 状态分离

### 事件处理
- [ ] 所有状态变更通过 Event
- [ ] Event 使用密封类
- [ ] Field 枚举完整覆盖
- [ ] ViewModel 处理所有事件类型

### UI 组件
- [ ] Composable 接收 `uiState` 参数
- [ ] 添加 `onEvent` 回调参数
- [ ] 不直接修改状态
- [ ] 使用工具函数处理加密等逻辑

### ViewModel
- [ ] `_uiState` 使用 `MutableStateFlow`
- [ ] `onEvent` 方法处理所有事件
- [ ] 使用 `copy()` 进行不可变更新
- [ ] 批量更新避免多次调用

---

## 相关文档

- [文档中心](../README.md)
- [架构决策 - ADR](../architecture/ARCHITECTURE_DECISIONS.md)
- [开发者指南](../getting-started/DEVELOPER_GUIDE.md)
