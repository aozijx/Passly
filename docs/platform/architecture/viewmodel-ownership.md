# ViewModel 依赖与 owner 边界

## 官方建议与 Passly owner 合同

[Android UI layer 指南](https://developer.android.com/topic/architecture/ui-layer)推荐以 ViewModel 管理需要访问数据层的 screen-level UI state：ViewModel 接收 UI 动作、协调数据层并暴露可观察状态；UI 观察状态并上送用户意图。[架构建议](https://developer.android.com/topic/architecture/recommendations#ui-layer)建议使用 StateFlow/Flow、挂起函数、`viewModelScope` 和生命周期感知收集，并避免向 UI 发送一次性消息；官方页面将这些列为建议，允许按应用场景适配。

Passly 的 ViewModel 拥有单个页面或明确页面流程的 UI state 和页面异步工作，依赖 Domain 规则、Repository、UseCase、Interactor 或窄应用端口。它不持有 Activity、Context、Resources、NavController 或另一个 ViewModel。Route/Activity 拥有宿主生命周期、导航、权限与 Activity Result；Screen/Content 消费不可变 UI state 并上送动作。一次性 host effect 是当前项目的受限实现合同，不代表 Android 官方推荐模式；优先把用户可处理的结果建模为 UI state。详细的状态收集和状态优先规则见[Presentation 状态管理](../../engineering/development/architecture-and-state.md#android-官方建议与-passly-约定)。

## 状态类型与转换逻辑

状态建模回答“当前处于什么状态”，转换逻辑回答“给定输入后如何变化”。选择实现方式时先辨认状态的事实来源、转换是否纯粹，以及异步工作的 owner；事件和状态的数量只是检索复杂度的线索。不要因为使用 `sealed` 就把状态类做成只转发事件的包装层，也不要把所有状态变化都塞进一个巨大 Reducer。页面状态优先保持为不可变数据。

| 方式 | 适用信号 | 约束 |
| --- | --- | --- |
| ViewModel 直接更新 | 页面简单，输入只改变一个字段或调用一个清楚的业务动作 | 不为每个 setter 建 Action/Mutation/Reducer 三层；保留单一状态 owner |
| 纯 Reducer | 多个输入汇合到同一页面状态，或相关字段必须一次转换并排除非法组合；纯规则集中后更易检查 | 形式为 `(State, Mutation) -> State`；不执行 I/O、认证、导航、日志、时钟读取或协程编排 |
| 状态携带纯行为 | 少数互斥状态对同一输入有显著不同的纯计算，且计算依赖该状态独有的数据；按状态放置后更易读 | 状态对象不得持有 Repository、Scope、平台对象或可变外部资源；进入/退出状态的副作用仍归 ViewModel、Route 或专门 owner |

先用密封分支表达真正互斥的状态，再按转换复杂度选择 ViewModel 直接更新、纯 Reducer 或状态上的纯行为。密封状态与 Reducer 可以组合；状态上的纯行为也不要求把所有事件分发到各个状态类。若转换需要挂起、授权、读写 Repository、发出平台 effect 或清理资源，这些步骤由对应 owner 编排，并把可归约的结果作为显式输入。不要为了缩短 `when` 把副作用和失败合同藏进多态方法或通用 handler 表。

### Kotlin 状态建模约定

- 用 `sealed interface`/`sealed class` 表达有限且互斥的状态，用 `data class` 携带该分支必需的数据，用 `data object` 表达无数据分支。转换与投影尽量通过穷尽 `when` 处理，让新增分支触发编译期检查；不要用 `else` 隐藏未处理状态。
- 状态字段应非空且语义明确。若 `null` 只是表示 Loading/Unavailable/None 等状态，改成显式分支；若值在业务语义上确实可选，nullable 可以保留，并在边界集中处理，不要求为消除问号而制造一层镜像类型。
- 不用多个布尔值、nullable 错误和 nullable payload 拼接互相矛盾的状态；把合法组合放进同一密封分支。独立且可同时成立的页面属性继续作为普通字段，不要把整页机械密封化。
- `data class.copy` 适用于不可变状态的局部更新。保持单次转换内相关字段一致；不要为了调用 `copy` 而复制 Repository/Domain 已拥有的持久业务字段。
- 屏幕状态默认是内存展示投影；Reducer 的纯度、`data class` 或密封类型都不保证状态可序列化或适合持久化。需要持久化、回放或对外传输时，先限定可保存的字段与输入，定义独立 DTO、稳定键、版本及显式映射，不依赖 Kotlin 类型名或枚举序号。

Kotlin 官方保证：密封类型配合 `when` 可由编译器检查分支完整性；Kotlin 类型系统将 nullable 和 non-nullable 明确区分。以上何时使用 Reducer、状态对象是否携带行为、UI 字段的非空要求，是 Passly 的架构约定，而不是语言强制规则。参考 [密封类与接口](https://kotlinlang.org/docs/sealed-classes.html)、[空安全](https://kotlinlang.org/docs/null-safety.html) 和 [数据类](https://kotlinlang.org/docs/data-classes.html)。

### 状态转换的分层与测试

- ViewModel 接收用户输入，执行异步工作并发布新状态；纯展示投影留在 Presentation。Route 管理生命周期和平台 effect，Content 只读取状态并发出动作。
- 纯业务规则及跨页面复用的不变量归 Domain Policy/规则；完整业务动作归 UseCase/应用服务。Presentation 依赖 Domain 能力或应用端口，不直接依赖 Data/Security 等实现包。Reducer 也只处理它拥有的 Presentation 状态和输入。
- 状态转换需要测试时，Reducer 或无副作用状态行为用纯单元测试覆盖关键输入、穷尽状态和非法组合；异步任务、取消、失败重试、迟到结果和平台回调还需在 ViewModel/Route owner 边界验证，纯 Reducer 测试不能替代这些行为测试。
- 状态要做操作回放时，先明确哪些输入是可重放的纯数据，以及敏感值是否允许进入记录。时间、随机数、I/O 结果和平台回调须作为显式输入/结果进入状态转换；仅有纯 Reducer 并不能自动获得可靠回放或快照能力。

Vault 列表的搜索、分类和类型筛选分别更新单一页面字段，因此由 `VaultViewModel` 直接发布状态；查询 Flow 汇合搜索、防抖后的输入、分类与设置。排序真相来自设置仓库，查询可用性是独立状态，不在 `VaultUiState` 里复制排序值，也不让 nullable 查询状态兼任多个语义。简单设置页面则直接投影 Repository Flow，不需为了统一风格补齐 Action/Reducer 套件。

## 避免上帝 ViewModel 与业务逻辑膨胀

ViewModel 的职责是屏幕状态生产与页面级协调，不是容纳所有 feature 逻辑。评估是否过载时，检查它是否同时拥有多个可独立变化的业务流程、无关状态机、过宽依赖闭包、重复分支/映射，或多个有不同取消与重试合同的异步任务组；行数和函数数量只用于发现候选，不单独决定拆分。

- 屏幕展示映射和短生命周期页面状态留在 Presentation/ViewModel；持久数据仍由 Repository/Domain 提供，避免复制一份可独立写入的业务真相。
- 可复用的纯业务判断放进所属 Domain Policy/规则；完整业务动作及其验证、不变量、幂等/事务或跨端口编排放入职责明确的 UseCase/应用服务。
- UseCase 可以只依赖一个 Repository，只要它封装了有意义的业务动作、规则或独立测试边界；单纯一对一转发简单读写仍由 ViewModel 直接调用 Repository。
- Activity Result、导航和 Android 平台生命周期留在 Route/Activity；只有独立状态、任务生命周期和取消/清理合同完整的流程才建立独立 owner。
- 提取后的 owner 必须让原 ViewModel 实际减少状态、依赖、分支或异步职责；不把整段实现搬进万能 UseCase、Coordinator、Manager，也不新增纯转发桥。

异步编排或状态 owner 迁移需要 ViewModel/Route/宿主行为测试，覆盖取消、失败重试、迟到结果和平台交付；纯函数测试不能代替 owner 行为覆盖。

这条边界覆盖主构造器、属性、函数 receiver/参数/返回类型、嵌套泛型和可执行表达式中的类型引用，也包括 import alias 与完全限定名。普通字符串文本、注释与转义美元符号不构成类型依赖。架构门禁应基于实际 ViewModel 声明识别引用目标，不能只因某个普通类型的名字包含 `ViewModel` 而拒绝它。

## Route、ViewModel 与 UI owner

Route 是页面和宿主能力的连接点：用生命周期感知 API 收集 ViewModel 状态；负责导航、Activity Result、系统 API、权限和 Android 资源；将状态与回调传给被动 UI。组合根或 Route 可协调多个独立状态 owner，但不得把 ViewModel 继续传给纯 UI Content 或普通业务 helper。Screen/Content 接收不可变 UI state、稳定数据值和窄回调；同一状态保持单一发布者，不复制状态或建立只转发调用的桥接对象。

Android 官方建议把 Snackbar/message 等一次性 UI 消息作为状态交给 UI 消费。Passly 当前 `Effect`/Channel 既交付 Route/平台命令，也承载部分提示反馈；提示反馈属于现存差异，不应据此指导新代码。新提示优先进入 UI state。遗留 effect 须绑定页面 owner，明示 `STOPPED`、重建、关闭及重新订阅时的投递合同；平台动作也不得依赖 `LaunchedEffect` 自动随 Activity 暂停。

Autofill Activity 通过 Activity ViewModel owner 管理平台授权、发现/选择、取消和最终结果提交，因此对应 Route 可以接收宿主创建的 ViewModel 作为生命周期连接点。`PasslyAppNavigation` 也在 Activity owner 下创建 `ScannedOtpImportViewModel`，经 `registerVaultGraph` 传给 `VaultRoute`；Vault Route 同时协调它与 graph owner 的 `VaultViewModel` 及 destination-scoped 的显示设置 ViewModel。该导入 ViewModel 承载跨 Vault destination 的导入流程；列表 Content 仍只接收合成后的 UI model 和事件回调。改变这类宿主参数前，必须验证配置变更、取消与结果 owner 是否仍正确。

当功能被多个页面共享时，将业务能力移入独立的应用服务或端口；共享 UI state 由单个 owner 发布。不要以某个 ViewModel 暴露另一个 ViewModel，或以 Route 直接调用 ViewModel 私有策略，来替代明确的业务边界。

## 架构门禁

`verifyArchitecture` 检查 ViewModel 声明到另一个 ViewModel 的依赖边，并允许 Route/Activity composition owner 协调多个状态 owner。对应实现和正反例位于 [`ViewModelDependencyVerifier.kt`](../../../buildSrc/src/main/kotlin/passly/ViewModelDependencyVerifier.kt) 与 [`SourceBoundaryViewModelTest.kt`](../../../buildSrc/src/test/kotlin/passly/SourceBoundaryViewModelTest.kt)。这是轻量源码检查，不做完整 Kotlin 语义解析：目前识别普通函数参数、局部 `val`/`var`，以及同一逻辑行的表达式函数参数遮蔽；复杂 lambda、多行表达式、destructuring 和扫描输入外部的符号解析仍需人工审查。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 识别 ViewModel 声明并查找跨 owner 类型引用 | `ViewModelDependencyVerifier.verify(sources)` | [`ViewModelDependencyVerifier.kt`](../../../buildSrc/src/main/kotlin/passly/ViewModelDependencyVerifier.kt) |
| 注册架构门禁聚合任务 | `ModuleBoundariesPlugin.apply(target)` 中的 `verifyArchitecture` 任务注册 | [`ModuleBoundariesPlugin.kt`](../../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt) |
| ViewModel 依赖声明边界回归 | `SourceBoundaryViewModelTest` | [`SourceBoundaryViewModelTest.kt`](../../../buildSrc/src/test/kotlin/passly/SourceBoundaryViewModelTest.kt) |
