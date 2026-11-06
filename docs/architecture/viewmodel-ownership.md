# ViewModel 依赖与 owner 边界

ViewModel 拥有单个页面或明确页面流程的状态、动作和生命周期任务。它可以依赖 Domain 规则、Repository、UseCase、Interactor 和窄的应用端口；不得注入、保存、调用或在签名中接收另一个 ViewModel。共享业务行为放入有明确职责的 UseCase/Interactor 或端口，不通过 ViewModel 相互调用传递。

这条边界覆盖主构造器、属性、函数 receiver/参数/返回类型、嵌套泛型和可执行表达式中的类型引用，也包括 import alias 与完全限定名。普通字符串文本、注释与转义美元符号不构成类型依赖。架构门禁应基于实际 ViewModel 声明识别引用目标，不能只因某个普通类型的名字包含 `ViewModel` 而拒绝它。

## UI owner

Route 或 Activity 可以作为 composition/lifecycle owner 协调多个独立 ViewModel。它们可以收集各自状态、路由动作和 effect，但不得把 ViewModel 本身继续传给纯 UI Content 或普通业务 helper。下层接收不可变 UI state、稳定数据值和窄 action/effect 回调；同一状态保持单一发布者，不复制状态或建立只转发调用的桥接对象。

Autofill Activity 通过 Activity ViewModel owner 管理平台授权、发现/选择、取消和最终结果提交，因此对应 Route 可以接收宿主创建的 ViewModel 作为生命周期连接点。`PasslyAppNavigation` 也在 Activity owner 下创建 `ScannedOtpImportViewModel`，经 `registerVaultGraph` 传给 `VaultRoute`；Vault Route 同时协调它与 graph owner 的 `VaultViewModel` 及 destination-scoped 的显示设置 ViewModel。该导入 ViewModel 承载跨 Vault destination 的导入流程；列表 Content 仍只接收合成后的 UI model 和事件回调。改变这类宿主参数前，必须验证配置变更、取消与结果 owner 是否仍正确。

当功能被多个页面共享时，将业务能力移入独立的应用服务或端口；共享 UI 状态由单个 owner 发布。不要以某个 ViewModel 暴露另一个 ViewModel，或以 Route 直接调用 ViewModel 私有策略，来替代明确的业务边界。

## 架构门禁

`verifyArchitecture` 检查 ViewModel 声明到另一个 ViewModel 的依赖边，并允许 Route/Activity composition owner 协调多个状态 owner。对应实现和正反例位于 [`ViewModelDependencyVerifier.kt`](../../buildSrc/src/main/kotlin/passly/ViewModelDependencyVerifier.kt) 与 [`SourceBoundaryViewModelTest.kt`](../../buildSrc/src/test/kotlin/passly/SourceBoundaryViewModelTest.kt)。这是轻量源码检查，不做完整 Kotlin 语义解析：目前识别普通函数参数、局部 `val`/`var`，以及同一逻辑行的表达式函数参数遮蔽；复杂 lambda、多行表达式、destructuring 和扫描输入外部的符号解析仍需人工审查。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 识别 ViewModel 声明并查找跨 owner 类型引用 | `ViewModelDependencyVerifier.verify(sources)` | [`ViewModelDependencyVerifier.kt`](../../buildSrc/src/main/kotlin/passly/ViewModelDependencyVerifier.kt) |
| 注册架构门禁聚合任务 | `ModuleBoundariesPlugin.apply(target)` 中的 `verifyArchitecture` 任务注册 | [`ModuleBoundariesPlugin.kt`](../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt) |
| ViewModel 依赖声明边界回归 | `SourceBoundaryViewModelTest` | [`SourceBoundaryViewModelTest.kt`](../../buildSrc/src/test/kotlin/passly/SourceBoundaryViewModelTest.kt) |
