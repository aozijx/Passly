# MVI 架构与命名

本文档定义 Passly 页面层的单向数据流分级、命名和边界。目标是让页面代码可以按复杂度选择合适的模式，而不是强制所有页面使用同一套模板。

## 核心原则

MVI 真正有价值的部分是：**单一状态源 + 单向数据流 + 明确的异步结果处理 + 副作用归属**。不是必须有
Intent.kt、Effect.kt、Reducer.kt 四件套。

## 分级架构

| 页面类型                 | 建议模式                                | 示例                                         |
|----------------------|-------------------------------------|--------------------------------------------|
| 纯展示页面                | Stateless Compose                   | 关于页面、静态说明                                  |
| Repository Flow 直接投影 | 简单 UDF（UiState + 语义化方法）             | Appearance、Interface、Notification Settings |
| 有少量操作但无状态机           | UiState + 语义化方法                     | 开关、排序、单项设置                                 |
| 多异步来源、复杂交互           | 完整 MVI（UiState + UiAction + Effect） | Auth、Vault、Backup、Recovery                 |
| 复杂状态转换               | MVI + Mutation + Reducer            | Vault 列表、备份流程、认证流程                         |

判断标准是**状态复杂度**，而不是事件数量。两个事件但每个都是简单的 Repository 投影，不需要完整 MVI。

## 简单 UDF 模式（推荐用于大多数 Settings 页面）

每个 ViewModel 方法直接对应一个 Repository 命令：

```kotlin
@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<AppearanceUiState> = settingsRepository.settings
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetThemeMode(mode))
        }
    }
}
```

数据流：UI 调用命令 -> Repository 更新 -> Flow 发出新状态 -> UI 重绘。这不违反单向数据流。

## 完整 MVI 模式（用于复杂页面）

当页面有输入状态、敏感数据清理、异步认证、并发限制、失败状态和多种交互路径时，使用完整 MVI。

### 标准目录

```text
feature/<feature>/
  contract/
    <Feature>UiState.kt
    <Feature>UiAction.kt       # 复杂页面才有
    <Feature>Effect.kt         # 有一次性副作用时才有
  presentation/
    <Feature>ViewModel.kt
    <Feature>Reducer.kt        # 复杂状态机才有
  ui/
    <Feature>Screen.kt
    <Feature>Content.kt
    components/
```

### 职责划分

| 目录 | 职责 | 不应该包含 |
| --- | --- | --- |
| `contract` | 页面可观察状态、用户事件、一次性副作用类型 | ViewModel、Repository、DAO、Composable |
| `presentation` | ViewModel、状态机、领域调用、effect 分发 | Compose UI、Android `Context`、数据库实现 |
| `ui` | Screen、Content、页面私有组件、资源映射 | DAO 调用、加密任务、跨页面业务编排 |
| `ui/components` | 只服务当前 feature 的小组件 | 全局通用 UI token |
| `core/ui/components` | 跨 feature 复用组件 | feature 专属文案和业务判断 |

### 命名规则

- 页面状态命名为 `<Feature>UiState`。
- 用户事件命名为 `<Feature>UiAction`（避免与 Android Intent 混淆），事件名使用用户动作或系统输入。
- 一次性副作用命名为 `<Feature>Effect`。
- ViewModel 命名为 `<Feature>ViewModel`，只暴露不可变 `StateFlow` / `SharedFlow`。
- Screen 命名为 `<Feature>Screen`，Content 命名为 `<Feature>Content`。

### Contract 边界

`UiState` 只能表达"当前 UI 如何渲染"：

- 可以包含页面 loading、展开项、错误类型、当前输入值和选择项；
- 不包含 `Context`、`NavController`、`ViewModel`、DAO、Repository；
- 不保存长期敏感明文。

`UiAction` 只能表达"UI 或宿主告诉 ViewModel 发生了什么"：

- 不传 lambda；
- 不传 Compose 类型；
- 不传已经格式化的 UI 文案；
- 可以传用户输入原始值、选中的枚举、返回手势等事件。

`Effect` 只能表达"一次性外部动作"：

- 导航、Toast、Snackbar、打开文件选择器、请求权限属于 effect；
- 可枚举结果必须使用 typed effect，不用裸字符串；
- 固定 UI 文案在 UI 层用资源映射，ViewModel 不硬编码中文。

### ViewModel 边界

ViewModel 是页面状态机，不是 UI 容器，也不是数据库编排层：

- 统一入口使用 `onAction(action)`；
- 持有私有 `MutableStateFlow`，向外暴露 `StateFlow`；
- 有一次性事件时，持有私有 `MutableSharedFlow`，向外暴露 `SharedFlow`；
- 只依赖 domain usecase、repository 接口、policy、service；
- 不直接依赖 DAO、Room entity、Compose、`NavController`；
- 不把认证、恢复模式、敏感字段读取的判断散落在 UI 中，优先进入 policy / usecase。

### Reducer（可选，复杂状态机才创建）

当状态转换满足以下条件时，提取为 Reducer：

- 状态分支较多（5+ 个 UiState 字段）；
- 多种异步结果汇入同一状态；
- 有非法状态转换需要阻止；
- 需要大量 reducer 单测；
- ViewModel 已经被状态转换代码淹没。

否则放在 ViewModel 文件底部即可，甚至不需要 reducer。

```kotlin
// 放在 ViewModel 文件底部或独立 Reducer.kt
private fun reduce(
    state: ExampleUiState,
    mutation: ExampleMutation,
): ExampleUiState = when (mutation) {
    ExampleMutation.Loading -> state.copy(isLoading = true)
    is ExampleMutation.Loaded -> state.copy(
        isLoading = false,
        items = mutation.items,
    )
    is ExampleMutation.Failed -> state.copy(
        isLoading = false,
        error = mutation.error,
    )
}
```

Reducer 消费 Mutation/Result，不直接消费 UiAction。UiAction 可能触发数据库、认证、加密等异步操作，不一定能直接产生新状态。

### 完整 MVI 数据流

```
UiAction
   |
ViewModel / ActionHandler
   |
UseCase / Repository
   |
Mutation / Result
   |
Reducer
   |
UiState
```

Effect 处理导航、文件选择、权限等一次性平台动作，不经过 Reducer。

### Compose 边界

页面级 `Screen` 可以拿 ViewModel 并收集状态：

```kotlin
@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ExampleContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
```

纯 UI `Content` 只接收状态和事件分发：

```kotlin
@Composable
fun ExampleContent(
    state: ExampleUiState,
    onAction: (ExampleUiAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

可复用组件不读取 ViewModel。组件参数应该是稳定、明确、可预览的 UI 输入。

## 当前迁移状态

认证页已按完整 MVI 整理：

```text
feature/auth/
  contract/
    AuthenticationIntent.kt
    AuthenticationUiState.kt
  presentation/
    AuthenticationViewModel.kt
  ui/
    AuthenticationScreen.kt
    host/
```

Vault 已迁移到 onIntent 统一入口：

```text
feature/vault/
  contract/
    VaultIntent.kt
    VaultUiState.kt
    VaultEffect.kt
  VaultViewModel.kt        # onIntent 统一入口
  VaultScreen.kt
```

Settings 简单页面使用 UDF 模式（语义化方法 + UiState）：

```text
feature/settings/general/
  NotificationSettingsContract.kt      # UiState
  NotificationSettingsViewModel.kt     # 语义化方法
  NotificationDetail.kt                # UI
  NotificationSettingsSection.kt       # UI 组件
```