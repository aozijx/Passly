# Kotlin 与 Compose 风格规范

本文档定义 Passly 的 Kotlin、Compose、MVI、主题系统和安全边界编码约定。目标是让代码在多人维护时保持一致、可审查、可自动化检查。

标记说明：

- 必须：违反后应在当前改动中修正，或在 PR/提交说明中解释原因。
- 推荐：默认遵守；只有明确收益时才偏离。
- 禁止：不得在新代码中引入。

## Kotlin 基础风格

### 格式

必须遵循 Kotlin 官方风格和 Android Kotlin 风格：

- 4 空格缩进。
- 一个文件只放高度相关的类型；大型页面、契约、组件应拆文件。
- 文件名与主要公开类型一致。
- 包名与目录一致。
- import 由 IDE 或格式化工具排序，不手工分组制造差异。
- 推荐使用尾逗号，降低多行参数、枚举、data class 的 diff 噪音。
- 单行过长时优先按语义换行，而不是压缩变量名。

推荐后续用 `.editorconfig`、ktlint 或 spotless 固化格式；格式化改动应单独提交，避免和功能变更混在一起。

### 命名

必须：

- 类、接口、枚举：`PascalCase`。
- 函数、属性、局部变量：`camelCase`。
- 常量：`UPPER_SNAKE_CASE`，仅用于真正编译期常量。
- Boolean 属性使用明确语义：`isEnabled`、`hasFullAccess`、`canSubmit`。
- 安全边界类使用显式后缀：`Policy`、`Gate`、`Guard`、`Provisioner`、`UseCase`。

禁止：

- 用模糊命名隐藏权限差异，例如 `getById()` 返回高敏感明文。
- 用 `Manager` 承载过多无关职责；如果方法开始跨数据、UI、认证多个边界，应拆成 repository/usecase/policy。
- 在 ViewModel 中出现 `dao`、`room` 等数据层命名依赖。

### 空值和错误处理

必须：

- 领域层返回明确结果类型，例如 `AppResult`、sealed failure、typed state。
- UI 层只显示错误，不决定安全策略。
- 不吞异常；如果异常是可恢复流程的一部分，应转换成明确 failure。

推荐：

- 非空默认值优先于 nullable 状态。
- nullable 只表达真实的“不存在”，不要用作多状态 flag。
- 对安全相关失败保留结构化错误码，UI 再映射文案。

## 敏感数据和内存规则

必须：

- 密码、恢复码、token、OTP secret 等敏感输入优先使用 `CharArray`、`SecureString` 或等价安全包装。
- 传入认证/加密层的 `CharArray` 所有权必须清晰：调用方转移所有权后不得继续读取。
- 使用完的敏感数组必须 wipe。
- 高敏感字段读取方法必须在命名上暴露风险，例如 `loadHighSensitivitySecret`、`getSecretBlobById`。

禁止：

- 在普通 summary 查询中返回高敏感明文。
- 在日志、telemetry、异常 message、Toast 中输出密钥、密码、恢复码、token。
- 在 ViewModel 中长期保存敏感明文字符串。

恢复码特殊规则：

- 恢复码不是日常解锁方式。
- 恢复码只能进入受限恢复模式。
- 恢复模式允许重建主认证方式，或导出新的加密备份。
- 恢复模式不得查看、复制、自动填充、搜索普通 Vault 明文。
- 恢复模式下重设应用密码后必须锁定，让用户用新密码重新进入普通会话。

## 架构分层

推荐分层：

```text
feature/*        UI、ViewModel、页面 contract、页面内组件
domain/*         领域模型、策略接口、usecase、repository 接口
data/*           Room、Proto、文件、mapper、repository 实现
security/*       加密、认证执行器、密钥管理、信封实现
core/ui/*        跨页面通用 UI token 和组件
core/*           与业务无关的基础设施
```

必须：

- ViewModel 只依赖 usecase、repository 接口、policy、service，不直接依赖 DAO。
- DAO 只负责数据访问，不承载组合业务流程。
- Repository 负责数据聚合、明文解密边界和数据模型转换。
- UseCase 负责跨 repository 的业务动作和事务编排。
- Policy 负责可复用判断，不把 `if (isRecoveryMode)` 散落到多个 ViewModel。

禁止：

- 在 UI 层绕过 repository 直接读数据库。
- 在 DAO 层决定认证权限。
- 在 Composable 中发起数据库或加密任务。

## MVI 约定

每个具备业务状态的页面优先使用 MVI：

```text
feature/example/
  ExampleScreen.kt
  ExampleViewModel.kt
  contract/
    ExampleUiState.kt
    ExampleIntent.kt
    ExampleEffect.kt
  components/
```

必须：

- `UiState` 是页面可渲染状态，不包含 Android `Context`、`NavController`、`ViewModel`。
- `Intent` 表示用户动作或生命周期输入。
- `Effect` 表示一次性事件，例如导航、文件选择、Toast、Snackbar。
- ViewModel 暴露 `StateFlow<UiState>` 和 `SharedFlow<Effect>`。
- ViewModel 提供一个入口处理事件：`onIntent(intent)` 或项目内已统一的等价命名。

推荐：

```kotlin
data class ExampleUiState(
    val isLoading: Boolean = false,
    val error: ExampleError? = null,
)

sealed interface ExampleIntent {
    data object RetryClicked : ExampleIntent
}

sealed interface ExampleEffect {
    data object Saved : ExampleEffect
}
```

禁止：

- ViewModel 向 UI 直接暴露可变 Flow。
- ViewModel 传出硬编码 UI 文案，尤其是中文字符串。
- Effect 使用裸字符串表达可枚举结果；应使用 typed effect，UI 层映射资源。

## Compose API 风格

### 参数顺序

页面级 Composable 推荐：

```kotlin
@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

纯 UI Content 推荐：

```kotlin
@Composable
fun ExampleContent(
    state: ExampleUiState,
    onIntent: (ExampleIntent) -> Unit,
    modifier: Modifier = Modifier,
)
```

基础组件推荐：

```kotlin
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
)
```

必须：

- `modifier` 是第一个可选参数。
- 事件回调命名为 `onXxx`。
- Composable 不直接创建或持有业务对象。
- 可复用组件不读取 ViewModel。
- `remember` 只保存 UI 本地状态。
- `LaunchedEffect` 只处理 effect 收集、一次性启动、动画等 UI 副作用。

禁止：

- 在 Composable 中写复杂业务分支。
- 在组件内部硬编码大量间距、圆角、颜色。
- 在 feature 组件中偷偷依赖全局导航。

## 主题、颜色、圆角、间距和动效

Passly 的 UI 应优先使用主题 token，而不是组件内散落常量。

推荐 token 结构：

```text
AppThemeTokens
AppColorTokens
AppShapeTokens
AppSpacingTokens
AppMotionTokens
AppExpressiveTokens
```

必须：

- 页面和组件优先读取项目主题 token。
- 圆角、组内间距、内容 padding 等可配置项应来自同一个事件源/设置源。
- `RoundedGroup`、输入框、BottomSheet、卡片、菜单等基础组件应共享 shape/spacing token。
- Material Expressive 开关只能影响允许表达性增强的动效、形状和视觉强调，不应改变业务行为。

推荐：

- 主题色允许由多个颜色组成，例如 primary、secondary、tertiary、surface、surfaceVariant、error。
- 手动主题色应转换成完整 color scheme，而不是只替换一个 primary。
- 组件默认使用 `MaterialTheme.colorScheme`，项目增强使用 `LocalAppThemeTokens` 或等价 CompositionLocal。
- 圆角拆分为至少：
  - container outer corner；
  - item inner corner；
  - top-only corner；
  - bottom-only corner；
  - standalone corner。
- 动效拆分为：
  - 是否启用 expressive；
  - 是否启用组件增强动画；
  - 是否降低动态效果。

禁止：

- 到处写 `16.dp`、`24.dp` 作为永久组件参数。
- 在业务页面复制 shape 计算。
- 让 Material Expressive 开关决定认证、数据库、导出等业务逻辑。

## 输入框和表单

必须：

- 输入框使用统一的 `AppTextField` 或更具体封装。
- 密码输入使用统一 `PasswordInput`/`AppPasswordField`，不要每页重复实现显隐、错误、清理逻辑。
- 所有输入提交前做必要 trim；密码类字段是否 trim 必须由业务明确决定。
- 错误状态由 `UiState` 驱动。

推荐：

- 一个字段一个小组件，多个字段通过页面或 section 组合。
- 针对账号密码、银行卡、OTP、恢复码等类型建立小型可复用 section，而不是大型通用表单。

禁止：

- 新增老式“万能表单组件”。
- 在 TextField 内部直接执行保存、认证、数据库操作。

## BottomSheet、Dialog 和菜单

必须：

- 文件名体现形态：`XxxSheet.kt`、`XxxBottomSheet.kt`、`XxxDialog.kt`。
- BottomSheet 只负责展示和收集输入；业务提交交给 ViewModel。
- 弹出层状态由 `UiState` 控制，关闭事件走 Intent。

推荐：

- 已存在的设置密码、备份导出等组件优先复用。
- 如果弹窗类型变多，使用 typed sheet state，而不是多个 Boolean 长期堆叠。

禁止：

- 文件名叫 `Dialog` 但实际是 BottomSheet。
- 弹窗组件直接依赖具体页面 ViewModel。

## 资源和本地化

必须：

- UI 文案放在 `strings.xml` 或对应资源文件。
- ViewModel 不硬编码界面文案。
- Effect 不传裸中文字符串表达固定事件，应传 typed effect，UI 层 `stringResource` 或 `context.getString`。
- 语言选择使用 Android locale / app locale 机制，不在 Kotlin 文件里硬编码语言映射表。

允许：

- ViewModel 返回来自后端、文件或用户输入的动态错误详情，但必须确认不含敏感数据。

禁止：

- 在 Kotlin 中维护“中文 -> 英文”语言映射。
- 在日志或错误提示中暴露恢复码、密码、密钥材料。

## 测试和提交

代码改动推荐按风险选择门禁：

- 普通 Kotlin/UI 逻辑：`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
- 资源、Manifest、导航：增加 `:app:assembleDebug`
- 安全、数据库、备份、认证：必须跑相关单测；必要时补架构边界测试。
- 文档-only：检查链接和术语即可，不要求编译。

提交要求：

- 功能、格式化、重命名、删除旧代码分开提交。
- 安全边界变更必须在提交信息或文档中说明边界变化。
- 大规模重构前先收敛目标，不在同一提交里同时改 UI、数据库迁移和认证策略。

## 代码审查检查表

提交前至少检查：

- ViewModel 是否还有硬编码 UI 文案。
- Composable 是否直接依赖 repository/DAO/security executor。
- 敏感字段是否通过明确命名的读取路径。
- 恢复模式是否仍被限制在恢复页、恢复导出和重建认证方式。
- 主题圆角、间距、颜色是否来自 token。
- 新组件是否足够小，是否可被其他页面复用。
- 是否删除了已经替代的旧组件，避免双轨维护。
- 是否补充或更新了对应边界测试。
