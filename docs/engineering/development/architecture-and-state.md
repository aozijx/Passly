# Presentation 状态管理

状态：当前开发规范；当前结构示例和迁移状态以 Task 80 全量复核为准。

Passly 使用分层 MVVM 与单向数据流（UDF）：ViewModel 持有页面业务状态，Route 收集状态并连接宿主能力，Content 渲染状态并把用户操作向上发送。Repository Flow、异步操作结果和用户操作是状态生产的不同输入，不构成不同架构。不要把“Flow 页面”与“Action 页面”分别当作 MVVM/MVI 模式。

### Android 官方建议与 Passly 约定

[Android 架构建议](https://developer.android.com/topic/architecture/recommendations)将这些内容标为建议，需按应用场景采用；本节将官方建议与 Passly 的具体合同分开说明。

**Android 官方建议：**

- UI 遵循 UDF：ViewModel 向 UI 暴露可观察状态，UI 通过方法调用把用户动作送回 ViewModel。状态向下流，动作向状态 owner 向上流。
- ViewModel 负责获取应用数据并生成 UI state。通过 Repository 暴露数据，不让 ViewModel/Composable 直接访问 DAO、DataStore 等数据源；跨层观察使用 Flow，执行命令使用挂起函数和 `viewModelScope`。
- UI state 建议由一个 `uiState` 属性暴露为 `StateFlow`。同一页面的相关数据通常合并为一个状态；彼此无关的状态可以分别暴露。Repository stream 可通过 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)` 转为 StateFlow；没有上游 stream 的简单页面也可用 `MutableStateFlow` 实现，再只暴露只读 `StateFlow`。
- UI state 应是不可变快照，包含完整渲染所需的数据与加载/错误语义。UI 只消费并渲染状态，不直接修改 ViewModel 所拥有的数据。
- Compose 使用 `collectAsStateWithLifecycle()` 等生命周期感知方式收集 UI state。ViewModel 保持 screen/destination 级，不传给可复用 UI 小组件；可复用组件用普通 state holder。
- ViewModel 不持有 `Activity`、`Context`、`Resources` 等生命周期或平台引用。Android 建议优先使用普通 `ViewModel`，不使用 `AndroidViewModel`/`Application` 来绕过依赖边界。
- Android 官方强烈建议避免 ViewModel 向 UI 单向发送一次性事件；优先在 ViewModel 处理输入，并把处理结果表达为 UI state。

**Passly 的落地合同：**

- ViewModel 是 screen/destination 级状态 holder，产生并暴露 UI state，接收用户动作，协调 Repository/UseCase 与页面异步工作。它不持有 Activity、Context、NavController 或资源引用。业务数据真相仍归 Repository/Domain；可编辑草稿要定义 owner、提交/丢弃及撤权清理。
- Route/Activity 连接 UI state 与宿主：使用生命周期感知收集，处理导航、权限、Activity Result、系统服务和资源文案。Screen/Content 通常只消费不可变状态并上送用户意图，不接收 ViewModel。
- 成功、失败、空结果、加载和可重试条件若影响用户下一步，应尽量表示为 UI state。Android 官方建议不把 ViewModel 到 UI 的一次性消息作为默认通道；Route 根据状态变化决定何时显示和清除消息。
- 当前项目仍有 ViewModel `Effect`/`PageEffects` 通道，其中既有宿主平台命令，也有部分提示反馈；这是现存实现，与 Android 的状态优先建议存在差异。新代码优先将用户可见结果建模为 UI state；Effect 只保留给确需 Route/Activity 执行的宿主命令。迁移旧提示前先核对消息去重、页面 owner、生命周期暂停和重试合同，不要把迁移描述成已完成。
- `uiState` 通常是单一只读 `StateFlow`；Compose 也允许用可观察的 Compose State。多个互不相关的状态可以分别暴露，但每个状态都须定义初始值、失败和重订阅行为。Repository/Domain 的持久数据不可由 UI 的长期副本争夺 owner。

Android 参考：[架构建议：UI 层与 ViewModel](https://developer.android.com/topic/architecture/recommendations#ui-layer)、[UI 层状态与 UDF](https://developer.android.com/topic/architecture/ui-layer)、[生命周期感知状态收集](https://developer.android.com/topic/architecture/recommendations#ui-layer)。

| 状态生产需要 | 同一 UDF 架构中的实现 |
| --- | --- |
| 持久数据变化 | ViewModel 观察 Repository Flow，映射为屏幕状态 |
| 简单用户命令 | 调用语义明确的方法；不必为每个操作定义 Action sealed class |
| 多类或需穷尽表达的页面输入 | 用类型化 `UiAction` 进入同一个 ViewModel 状态 owner |
| 状态转换复杂、异步结果汇合或非法组合需禁止 | 可选纯 Reducer；不负责 I/O、认证、导航或异步编排 |
| 纯静态内容或短暂视觉状态 | 无需强行创建 ViewModel；由 Compose/组件 state holder 管理 |

## 页面状态与动作

- `UiState` 表达 UI 完整渲染所需的不可变快照，不包含 ViewModel、NavController、DAO 或长期敏感明文。相关阶段用密封状态表达；可并存的独立属性继续使用普通字段。
- UI 事件由 Screen/Content 通过 ViewModel 方法上送；简单交互无需统一包装成 `UiAction`。动作多且需要穷尽分支时可用类型化 Action。不要增加字段一一对应、只由 Route 转发的 Event 层。
- Route 负责导航、平台 callback、权限、资源文案和需要 Android 生命周期的 UI 行为。可复用 UI 组件通过参数和普通 state holder 接收状态/动作，不从组合树任意位置获取 ViewModel。
- Reducer 是可选的纯转换；Mapper 生成 UI 展示投影。两者边界、采用条件及测试责任见[ViewModel 所有权规范](../../platform/architecture/viewmodel-ownership.md)。

## 状态 owner 与业务逻辑

ViewModel 负责页面状态生产与协调；Reducer 与状态携带纯行为的选择、Kotlin 密封状态和空值建模细则见[ViewModel 状态类型与转换逻辑](../../platform/architecture/viewmodel-ownership.md#状态类型与转换逻辑)。

屏幕 `UiState` 通常是内存中的展示投影，不是持久数据源。Repository/Domain 是持久业务数据的事实来源；Compose `remember` 只拥有适合组件生命周期的视觉状态。可编辑草稿可独立存在，但需明确它与已保存数据的关系、owner、提交/丢弃及敏感值清理语义。不得让 Repository Flow 与另一份长期可变副本同时声称拥有同一业务字段。

ViewModel 负责屏幕协调，不应因此成为所有业务规则和工作流的收纳处。简单的单一 Repository 操作可直接调用；当一个有名称的业务动作包含验证、不变量、跨端口/仓库编排、可复用策略或需要独立测试的完整流程时，放入职责匹配的 UseCase/应用服务。纯领域判断放入 Domain Policy/规则。UseCase 即使只调用一个 Repository 也可以有价值；若只是逐参数转发一个简单 CRUD 调用，则不增加它。提取后必须实际减少 ViewModel 的依赖、分支或异步职责，不能把 God ViewModel 原样搬成 God UseCase。

Reducer 是可选的。多个输入汇合、相关字段必须原子转换、需要排除非法组合，或纯转换规则集中后明显更易检查时才提取；事件数量本身不构成采用理由。Reducer 消费
Mutation/Result，不直接执行数据库、认证或导航，也不直接消费需要异步工作的 UiAction。状态是否可序列化、能否回放以及异步工作是否可测，都需要各自的合同和 owner 级验证，不能仅由 Reducer 保证。

```text
UI event -> Route/Screen -> ViewModel -> Repository / UseCase
                                      -> observable UiState -> Route/Screen
Route handles navigation, platform APIs, lifecycle and UI behavior
```

## 生命周期与可复用 UI

Route 通过 `collectAsStateWithLifecycle()` 或相应 lifecycle API 收集 ViewModel 状态。`LaunchedEffect` 的生命周期是组合生命周期，不代表宿主处于 `STARTED`；任何需要可见期间执行的平台行为须由 Lifecycle API 或显式 lifecycle observer 限定。

### 页面一次性事件的可见性与 owner

对使用 `PageEffects` 的页面，`collectPageEffects()` 通过 `repeatOnLifecycle(STARTED)` 在宿主可见时收集。`PageEffects` 将事件绑定到页面访问 owner：同一 owner 在宿主 STOPPED 时发出的事件可缓冲到恢复后交付；owner 退出后，其待处理事件会被丢弃。该通用收集器只定义传递时机和 owner 隔离，不替各类事件决定是否应重放。Toast/Snackbar、导航、SAF 选择器、权限请求及平台结果各自仍须明确离页、重建和取消语义。

[`PageEffectLifecycleTest`](../../../app/src/test/java/com/aozijx/passly/presentation/shared/event/PageEffectLifecycleTest.kt) 覆盖同一页面恢复后交付缓冲事件，以及旧 owner 退出后丢弃停止期间的事件；[`PageEffectsTest`](../../../app/src/test/java/com/aozijx/passly/presentation/shared/event/PageEffectsTest.kt) 覆盖 owner 归属。该 helper 测试不代替各 Route、Activity 或真实导航宿主的行为验收。

焦点、Popup 展开、动画进度等纯视觉状态可由 Compose 或普通 state holder 管理。页面级状态由 screen/destination 的 ViewModel 持有；不要为了小组件复用而把 ViewModel 下传，也不要仅为隐藏一个函数而创建第二个 ViewModel。

## 当前应用

- Vault 列表由 ViewModel 持有页面状态并直接更新独立筛选字段；搜索焦点和下拉位移由 Compose state holder 管理。排序设置由 Repository 提供，查询生成由 Flow 汇合。
- Backup 使用封闭工作流状态，导入与导出参数只存在于相应分支。
- Diagnostics 是独立二级页面，拥有 `DiagnosticsUiState`、Reducer/ViewModel 和 Route；数据管理页只持有容量和
  清理状态，不持有查看或导出状态。
- About 是无 ViewModel 的静态页面。
- 简单 Settings 页面直接投影 Proto-backed repository Flow，不为每个开关创建完整 MVI 套件。

## 持久设置读取失败与恢复

Proto-backed Settings Repository Flow 始终是持久化真相源；读取失败不能映射成默认偏好，也不能触发静默重写。DataStore
磁盘读取 `IOException`、协议损坏和意外普通异常分别形成明确的 `Failed` 原因；`CancellationException` 原样传播，fatal
`Error` 不捕获。损坏数据必须先由用户可见，只有显式 Retry 才重新订阅冷源；普通 I/O 失败显示失败状态并按有界退避自动重试，页面 Retry 会提前触发下一次订阅。正常 Flow 完成保留最后一个状态。

读取所有者通过共享 `RecoverableRead` 管理一次冷源观察和重新订阅；页面只投影 `Loading`、`Ready`、`Failed` 并调用 Retry，不能各自复制 `catch`/fallback 策略。显式重试同时广播给关联的进程级长读者，让消息快照和诊断记录策略也能从损坏状态重新订阅。设置读取失败时隐藏依赖该设置的可编辑控件，ViewModel 在异步写入前再次确认读取为 Ready，避免直接 Action 或迟到操作写入基于未知状态的变更。短期操作使用 `.first()` 读取仍把异常交给操作调用方处理，不伪造设置默认值。

```text
Repository cold Flow -> RecoverableRead -> Loading / Ready / Failed
                                               |              |
                                               |              +-> Route shows error + Retry
                                               +-> projection  +-> source is collected again
```

UI 与 Domain 语义相同的设置选项直接使用 Domain 类型；需要不同展示语义时才建立 UI model，并使用穷尽映射，
不按枚举名称反射转换。简单设置 Content 直接发送页面 Action，不增加字段一一对应的 Event 转发层。
页面私有 Sheet、Dialog 和 UiModel 放在拥有其状态的 feature，而不是入口页面目录。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Route 收集状态并处理生命周期事件 | `VaultRoute()` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt) |
| 维护列表界面状态与业务动作 | `VaultViewModel.onAction()` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) |
| 将筛选和页面输入发布为状态 | `VaultViewModel.onAction()` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) |
| 重订阅失败的设置冷源并发布读取状态 | `RecoverableRead<T>` | [`RecoverableRead.kt`](../../../app/src/main/java/com/aozijx/passly/app/settings/RecoverableRead.kt) |
| 显示统一的设置读取失败与重试入口 | `SettingsReadFailureContent()` | [`SettingsReadFailureContent.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/shared/state/SettingsReadFailureContent.kt) |
