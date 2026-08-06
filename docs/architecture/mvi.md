# MVI 架构与命名

本文档定义 Passly 页面层的 MVI 分级、命名和边界。目标是让页面代码可以按同一模式扩展，而不是把状态、事件、UI 组件和业务编排混在一个文件里。

## 标准目录

具备业务状态的功能优先使用以下目录：

```text
feature/<feature>/
  contract/
    <Feature>UiState.kt
    <Feature>Intent.kt
    <Feature>Effect.kt
  presentation/
    <Feature>ViewModel.kt
  ui/
    <Feature>Screen.kt
    components/
```

职责划分：

| 目录 | 职责 | 不应该包含 |
| --- | --- | --- |
| `contract` | 页面可观察状态、用户事件、一次性副作用类型 | ViewModel、Repository、DAO、Composable |
| `presentation` | ViewModel、状态机、领域调用、effect 分发 | Compose UI、Android `Context`、数据库实现 |
| `ui` | Screen、Content、页面私有组件、资源映射 | DAO 调用、加密任务、跨页面业务编排 |
| `ui/components` | 只服务当前 feature 的小组件 | 全局通用 UI token |
| `core/ui/components` | 跨 feature 复用组件 | feature 专属文案和业务判断 |

简单页面可以暂时保留扁平文件，但只要出现以下任一情况，就应拆成标准目录：

- 状态字段超过一个纯展示值；
- 有两个以上用户事件；
- 需要收集一次性导航、Toast、Snackbar、文件选择或权限结果；
- 页面里开始出现业务判断、认证边界或数据提交逻辑。

## 命名规则

- 页面状态命名为 `<Feature>UiState`。
- 用户事件命名为 `<Feature>Intent`，事件名使用用户动作或系统输入，例如 `PasswordChanged`、`SubmitClicked`、`BackPressed`。
- 一次性副作用命名为 `<Feature>Effect`，例如 `NavigateBack`、`ShowSnackbar`、`LaunchFilePicker`。
- ViewModel 命名为 `<Feature>ViewModel`，只暴露不可变 `StateFlow` / `SharedFlow`。
- Screen 命名为 `<Feature>Screen`，Content 命名为 `<Feature>Content`。

## Contract 边界

`UiState` 只能表达“当前 UI 如何渲染”：

- 可以包含页面 loading、展开项、错误类型、当前输入值和选择项；
- 不包含 `Context`、`NavController`、`ViewModel`、DAO、Repository；
- 不保存长期敏感明文。确实需要临时密码输入时，使用项目已有安全字符串类型，并在 ViewModel 清理。

`Intent` 只能表达“UI 或宿主告诉 ViewModel 发生了什么”：

- 不传 lambda；
- 不传 Compose 类型；
- 不传已经格式化的 UI 文案；
- 可以传用户输入原始值、选中的枚举、返回手势等事件。

`Effect` 只能表达“一次性外部动作”：

- 导航、Toast、Snackbar、打开文件选择器、请求权限属于 effect；
- 可枚举结果必须使用 typed effect，不用裸字符串；
- 固定 UI 文案在 UI 层用资源映射，ViewModel 不硬编码中文。

## ViewModel 边界

ViewModel 是页面状态机，不是 UI 容器，也不是数据库编排层：

- 统一入口使用 `onIntent(intent)`；
- 持有私有 `MutableStateFlow`，向外暴露 `StateFlow`；
- 有一次性事件时，持有私有 `MutableSharedFlow`，向外暴露 `SharedFlow`；
- 只依赖 domain usecase、repository 接口、policy、service；
- 不直接依赖 DAO、Room entity、Compose、`NavController`；
- 不把认证、恢复模式、敏感字段读取的判断散落在 UI 中，优先进入 policy / usecase。

## Compose 边界

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
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
```

纯 UI `Content` 只接收状态和事件分发：

```kotlin
@Composable
fun ExampleContent(
    state: ExampleUiState,
    onIntent: (ExampleIntent) -> Unit,
    modifier: Modifier = Modifier,
)
```

可复用组件不读取 ViewModel。组件参数应该是稳定、明确、可预览的 UI 输入。

## 当前迁移方向

新代码按标准目录创建。旧代码按以下优先级逐步收拢：

1. 先把 `UiState`、`Intent`、`Effect` 从 `presentation` 或 Screen 文件中移到 `contract`。
2. 再把 ViewModel 留在 `presentation`，屏幕和组件留在 `ui`。
3. 最后把跨页面通用组件上移到 `core/ui/components`，把 feature 私有组件留在 `feature/<feature>/ui/components`。

认证页已经按此规则整理为：

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
