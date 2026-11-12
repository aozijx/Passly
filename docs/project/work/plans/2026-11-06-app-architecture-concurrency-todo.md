# Passly 应用架构与并发设计 TODO

> **状态：** 调查与提案阶段。此文件是新建的独立待办；不替代 [统一 Presentation 架构与 ViewModel 审计](2026-10-30-passly-maintainability-todo.md) 中的既有条目。当前工作假设是保留模块化分层 MVVM + UDF，按证据整理并发 owner 和局部模式，不先做全局架构迁移。

## 目标

形成适合 Passly 的架构约定，说明：

- GoF 模式何时有用、何时会成为额外层；
- ViewModel、Route/Activity、Feature 工作流、Repository、Security、Runtime 和系统 Service 各自拥有哪组协程；
- IO/CPU 执行上下文、取消、并发限制、队列、资源释放和后台持久任务的处理规则；
- 如何分批改造，并用行为测试验证生命周期、安全和并发合同。

不以线程数、GoF 模式数量、UseCase 数量或新 Gradle 模块数量衡量架构质量。不建立全局 EventBus、万能 Coordinator/Manager 或第二份业务状态真相源。

## 创建时架构快照（2026-11-06）

以下是计划建立时对源码、模块图和依赖的检索摘要，用来说明提案背景，不是当前工作树清单。继续本计划时应以生产实现、Gradle 配置和正式架构文档重新核对具体候选。

- 当时的 Gradle 模块包括 `:app`、`:core`、`:core:common`、`:data`、`:domain`、`:runtime:session`、`:security`、`:feature:autofill`。Autofill 已抽为独立 Android library；该模块直接依赖 `:core`、`:domain`、`:runtime:session`，`:app` 显式依赖它。App 保留共享认证 Activity 宿主与窄平台适配，其余业务 Feature 主要仍在 App 包内。依赖白名单见 [`build.gradle.kts`](../../../../build.gradle.kts) 中的 `verifyModuleBoundaries` 配置。
- 当时的主结构是分层 MVVM + 单向数据流：UI → Route/ViewModel → Feature UseCase/应用流程（按需要）→ Domain port → Data Repository；Security 拥有认证和安全会话编排，Runtime Session 拥有资源无关的会话状态机。
- 当时版本目录记录 Kotlin `2.3.20`、kotlinx.coroutines `1.11.0`。线程切换并不统一：部分操作接收注入的 `CoroutineDispatcher`（例如 Backup dispatcher、`RuntimeSessionManager`），多处实现直接引用 `Dispatchers.IO/Default`。
- 当时页面任务主要由 `viewModelScope` 管理；Application、Security singleton 与 Android Service 有多个独立 `SupervisorJob` Scope。KDF、加密日志写入和 CameraX 分析分别使用专用单线程 Executor/线程池。
- 当时生产代码存在 `runBlocking` 桥接：`ManualAccessibilityFillService.onDestroy()`、`AutofillResultDelivery` 的同步平台提交与 lease 收尾，以及 `ObservabilityRuntimeController` 生命周期/崩溃记录路径。它们各有同步顺序和资源合同，审计结论应逐处形成，不做机械删除。
- `:runtime:session` 是纯 Kotlin/JVM Library，`api(:domain)` 并实现协程 session gate/lease 状态机；它不是 Android Runtime（ART）、Android Service 或 OS process。`Security` 与 `Data` 都依赖该资源无关合同，以避免双方相互依赖。
- 当时检索未发现 Manifest `android:process` 声明；Legacy Autofill、Accessibility 和 Credential Provider 服务默认与应用其他组件在同一进程。`:data` 使用 AndroidX `dataStore` 单进程 delegate，`DatabaseProvider` 默认 Room builder 未启用 multi-instance invalidation。贸然把服务改到独立进程会要求显式处理 Application/Hilt 重初始化、数据库/DataStore 多进程合同、session/lease 状态和 IPC。
- 日志按层分工：`core` 定义记录/Store 接口，`data` 持有加密日志存储，`app` 持有 logger 组合、crash capture 和生命周期协调。`EncryptedLogRecordStore` 使用有界单线程 `ThreadPoolExecutor`，append 后调用 `fd.sync()`；uncaught crash handler 会限时 flush，失败后使用独立 emergency-write 路径。独立进程只能提供一定故障隔离，不能保证进程存活，也不能替代持久队列、同步写入和进程死亡验证。
- 当时目录与依赖检索未发现 WorkManager 依赖或调用；是否需要持久后台任务应由具体业务的进程死亡合同决定，不作为全局并发改造的前置条件。

## 执行入口与去重（2026-11-07）

先按[主维护待办](2026-10-30-passly-maintainability-todo.md)处理仍开放的 Task 37、59 等具体合同及[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)，本页广域提案不排在已确认问题之前。Task 1 中 Autofill 模块比较/抽取已被当前迁移替代，剩余为边界与宿主验收；诊断独立进程仍待决策。Task 6 持久设置已有 RecoverableRead，不重新实现；Task 7 引用实际 owner 清单，不复制主待办 80/83/84。

## 待办

### Task 1：确定目标架构与 GoF 使用边界

目标结构和层职责已在[架构概览](../../../platform/architecture/overview.md)、[包边界](../../../platform/architecture/package-boundaries.md)与[状态管理规范](../../../engineering/development/architecture-and-state.md)中明确；Domain/模块依赖审查记录见[边界扫描](../../reviews/2026-11-07-domain-boundary-scan.md)和[职责凝聚迁移](../../reviews/2026-11-07-domain-cohesion-migration.md)。本 Task 只保留尚未完成的方案比较与模式使用规则。

- [ ] 在架构决策记录中比较至少三条路线：现有模块内分层加固；按已验证的依赖闭包/构建隔离证据拆分其他 Feature Gradle 模块；全局事件/Actor 或总线驱动。`:feature:autofill` 已完成切换，不再作为未来拆分候选；本项只评估是否需要更多模块。记录复杂度、边界收益、迁移成本及状态/取消风险。
- [ ] 单独比较 Gradle 模块拆分、Android 独立进程和独立持久化/服务三个不同决策。新增 Gradle module 只改变编译依赖/API 边界，不形成 crash/process isolation；`android:process` 则引入独立 Application/Hilt 单例、IPC 和跨进程数据/状态合同。不得用“加模块”满足进程隔离需求，也不得用“独立进程”代替 durable log write。
- [ ] 日志若考虑独立进程，先定义故障类型、事件交付、最大丢失窗口、IPC 失败回退、Room/DataStore/会话共享和强杀验证；以现有队列、`fd.sync()`、crash flush/emergency-write 机制为对照。
- [ ] 编写 GoF 模式使用表并用现有代码示例校验：State 用于有穷状态机；Adapter 隔离 Android/持久化协议；Strategy 仅用于确有多实现的行为；Command/UseCase 表达有意义的业务动作；Flow/StateFlow 承担观察数据流；Factory 由 DI 或真实构造变化驱动。明确简单一对一包装、无实际替换的 Strategy、全局 Observer/EventBus 和只转发调用的 Coordinator 不作为默认做法。
- [ ] 与 `docs/platform/architecture/overview.md`、`docs/platform/architecture/package-boundaries.md`、`docs/platform/architecture/viewmodel-ownership.md`、`docs/engineering/development/architecture-and-state.md` 和 `docs/engineering/development/maintenance-boundaries.md` 对照，找出重叠、冲突或缺失后再更新长期规则；本计划不是正式架构合同。

### Task 2：建立并发 owner 与任务类别目录

- [ ] 对生产协程、Flow、Executor 和线程池画出 owner 图，至少覆盖 ViewModel 页面任务、Route/Activity 系统 effect、Feature 工作流、Application 级任务、Service callback、Runtime Session、安全资源任务、持久化/文件任务及测试 scope。每个自建 `CoroutineScope`/Executor 标明创建者、有效期、关闭点、失败策略、队列容量与调用方。
- [ ] 把任务分为 UI-oriented（离页可取消）、App-oriented（进程仍活动时继续）和 Business-oriented（需在进程死亡后恢复）三类，检查调用方生命周期是否与类型一致。仅 Business-oriented 且确实需要跨重启持续执行的作业才评估 WorkManager；不得把密码、Recovery Code、解密数据或 lease 句柄序列化进持久任务输入。
- [ ] 检查每个 `SupervisorJob` Scope 是否有明确的子任务失败处理和关闭责任。彼此独立的任务可隔离失败；必须共同成功/失败的阶段使用结构化父子关系，并明确普通失败、`CancellationException` 与 fatal `Error` 的处理。
- [ ] 明确数据共享准则：优先传不可变快照/值对象；并发写入由单一 owner、事务、Mutex 或原子状态转换保护。StateFlow 是可观察状态而非事件队列；Channel 只用于明确的一次性交付/有界队列合同；不以增加并行度绕开 Repository、Room transaction 或 Session lease 顺序。

### Task 3：统一线程切换和 Dispatcher 规则

- [ ] 全量搜索生产代码中的 `Dispatchers.*`、`withContext`、`flowOn`、`CoroutineDispatcher` 注入和 Dispatcher qualifier，按 CPU 密集、阻塞 IO、UI/主线程、串行专用执行器归类；检查调用方是否在错误层切线程，以及测试能否注入/替换执行上下文。
- [ ] 定义 main-safe 合同：Repository、数据源、Security 和平台能力负责让自身阻塞工作不占调用者主线程；ViewModel/UseCase 不猜测下层线程。Room、DataStore 等已提供挂起/异步 API 的调用不额外包装 `Dispatchers.IO`，纯轻量映射也不无差别切线程。
- [ ] 决定是否提供少量有语义的 `@IoDispatcher` / `@DefaultDispatcher` 等 qualifier，以及 qualifier 所属模块和注入范围。复用 Backup 现有 dispatcher 经验，但不建立无边界的全局线程配置库；对依赖阻塞专用线程或严格单线程语义的能力单独说明。
- [ ] 在有代表性的数据库规模和目标设备上测量 `RoomEntryListQueryRepository.getActiveByType()` 的列表映射耗时，再决定是否将 CPU 映射移出主线程；不可仅凭其他 Flow 使用 `flowOn` 就机械切换。

  源码核查已完成：唯一生产消费者 [`RelatedPasswordOperationsImpl.activeLogins()`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/RelatedPasswordOperationsImpl.kt) 经 [`DetailPasswordRelationsWorkflow`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPasswordRelationsWorkflow.kt) 使用 [`DetailViewModel.viewModelScope`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)；DAO 挂起查询完成后，列表映射在调用者上下文继续。当前 mapper 仅组装列表项、身份和 Profile，并复用 Entity 已有的集合字段。查询没有条数上限，是否值得切换仍需真实数据量和主线程耗时证据。

已排除的线程候选：

- [`rememberInstalledAppIconBitmap`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppIconState.kt) 在 Compose `LaunchedEffect` 中通过 `withContext(Dispatchers.IO)` 调用图标 loader。
- `AndroidAppCacheStore`、[`AndroidBackupDirectoryAccess`](../../../../app/src/main/java/com/aozijx/passly/app/backup/AndroidBackupDirectoryAccess.kt)、[`DatabaseProvider`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/DatabaseProvider.kt)、[`VaultDatabaseResetController`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/port/VaultDatabaseResetController.kt)、[`RoomActivityQueryRepository`](../../../../data/src/main/java/com/aozijx/passly/data/repository/activity/RoomActivityQueryRepository.kt)、[`DefaultFaviconImageProcessor`](../../../../app/src/main/java/com/aozijx/passly/app/entry/favicon/DefaultFaviconImageProcessor.kt)、[`FaviconImageDownloader`](../../../../app/src/main/java/com/aozijx/passly/app/entry/favicon/FaviconImageDownloader.kt)、[`BarcodeImageDecoder`](../../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/image/BarcodeImageDecoder.kt) 与 [`PermissionManagementViewModel.inspectDirectory()`](../../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/permissions/PermissionManagementViewModel.kt) 的阻塞工作已在所属边界或所有生产调用方切到 IO。
- Security Autofill inbox seal/key provisioning、设备 KDF 校准和认证可用性解析分别在各自加密或计算 owner 使用 `Dispatchers.Default`；交互式 KDF 由 [`SingleThreadKdfRunner`](../../../../security/src/main/java/com/aozijx/passly/security/authentication/method/credential/SingleThreadKdfRunner.kt) 的专用串行 executor 执行。没有证据支持统一迁移这些执行上下文。
- `SessionLeaseGate.observe()` 已从硬编码 `Dispatchers.IO` 改为 Runtime 注入的 `ioDispatcher`。行为覆盖见 [`RuntimeSessionManagerTest`](../../../../runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManagerTest.kt)，正式合同见[运行时流程](../../../platform/architecture/runtime-flows.md)。
- Domain [`ApplicationLabelResolver.labelFor()`](../../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/ApplicationLabelResolver.kt) 与 Core [`InstalledAppCatalog.labelFor()`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppCatalog.kt) 已迁移为 main-safe 挂起合同；调用和测试见[Autofill 保存流程](../../../product/autofill/save-flow.md)与[`AutofillCredentialTitleResolverTest`](../../../../feature/autofill/src/test/java/com/aozijx/passly/feature/autofill/core/AutofillCredentialTitleResolverTest.kt)。
- [ ] 对需大量并行/扇出工作的流程评估有界并发、背压和去重：按实际工作量选择结构化 `coroutineScope`/`supervisorScope`、Semaphore、有限 Dispatcher、Flow buffer 或已有串行队列；不得为低频独立页面任务引入通用调度框架。

### Task 4：逐处审查同步桥接和生命周期终止

- [ ] 验收 [`ManualAccessibilityFillService.onDestroy()`](../../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/accessibility/ManualAccessibilityFillService.kt) 的同步 teardown：确认两处 `runBlocking` 所在回调线程、等待上限与返回保证；覆盖失败日志或一个 interaction 的锁失败是否会中断后续清理并跳过 `scope.cancel()` / `super.onDestroy()`。先验证系统回调与资源后果，再决定是否调整失败隔离或 teardown 收尾。源码调用链及待验证失败路径见[Service teardown 审查](../../reviews/2026-11-12-accessibility-service-destroy.md)。
- [ ] 验收 `AutofillResultDelivery.commitResult()` 尚未覆盖的同步边界：核实 `runBlocking` 调用线程与最长等待，并验证 completion scope 取消时 completion barrier 和 lease 如何终止。`AutofillResultDeliveryTest` 已覆盖 callback 异常后的 barrier 释放与重试、普通 trace 失败后的已提交收尾，以及 fatal trace `Error` 收尾后原样传播；宿主 callback 部分副作用失败仍由[Autofill Task 21](2026-11-07-passly-autofill-followups.md#task-21)唯一维护。实现合同见[Autofill 填充流程](../../../product/autofill/fill-flow.md)，测试边界见[测试逻辑维护审查](../../reviews/2026-11-11-test-logic-maintenance-review.md)，运行证据见[Autofill 宿主验收记录](../../reviews/2026-11-08-autofill-host-validation.md)。
- [ ] 审计 `ObservabilityRuntimeController` 中 `runBlocking` 的位置和调用线程（生命周期 flush、前次退出摘要、崩溃捕获）。保留 crash/lifecycle 尾声所需的同步可观测性，同时避免普通页面路径无界等待；fatal 错误不可被普通诊断失败吞掉。
- [ ] 对其余生产 `runBlocking` 做全量闭包搜索；按“不可避免的同步边界”“能改为 suspend/异步 owner”“测试专用”分类，检查主线程、锁重入、dispatcher starvation、超时、取消及调用结束后的资源状态。不是所有 `runBlocking` 都属于缺陷。

### Task 5：保留并验证有特定合同的专用执行器

- [ ] 审查 `SingleThreadKdfRunner`：单线程排队是否由 KDF 资源/成本策略要求；验证取消后 worker secret 擦除、迟到结果丢弃、executor shutdown 与排队任务结束行为。评估 `CoroutineDispatcher.limitedParallelism(1)` 或 channel worker 时必须证明等价的串行和秘密清理语义，禁止只为统一技术栈改写。
- [ ] 审查 `EncryptedLogRecordStore` 的有界 `ThreadPoolExecutor`、溢出/紧急线程、优先级驱逐、`flush(timeout)`、shutdown 与日志丢失窗口。当前专用写队列承担串行文件写和同步 flush；若考虑 Channel/协程 actor，需比较队列背压、fatal logging、线程中断及 shutdown 语义并复用已有并发测试。
- [ ] 补相机 host 生命周期行为验证：覆盖权限变化/组合重建使旧 provider listener 失效、无重复绑定，以及 analyzer 正在处理时停止 session 并关闭 scanner/executor 的清理顺序。owner 分类与源码合同见[Scanner 相机与结果确认 owner 审查](../../reviews/2026-11-12-scanner-camera-and-result-owners.md)；当前源码审查没有专用 Host/Session 测试，不能代替实际异步与设备行为验证。
- [ ] 审查 Runtime Session、Autofill lease/timeout、Clipboard 清理、Favicon 与附件资源回收等自建 scope/dispatcher。`Job.cancel()` 仅表示取消请求；对仍在使用安全资源的任务，须等 lease/事务退出或依赖可验证 abort 合同后才可关闭资源。

### Task 6：制定状态、Flow 与并发失败合同

- [ ] 对长期 Repository/服务 Flow 记录收集 owner、初始/空/失败状态、上游异常后的重订阅或 retry 信号，以及 session lock 撤权的取消语义。避免 Flow 异常终止后 UI 默认为它自动恢复。
- [ ] 对并发子任务区分“彼此独立”和“全有或全无”：选择 `coroutineScope` 或 `supervisorScope` 时写明兄弟任务失败的预期；所有异常映射先传播 `CancellationException`，只将可恢复普通 `Exception` 转成业务失败；窄清理边界可以捕获 `Throwable`，清理后仍须原样传播 fatal `Error`。
- [ ] 对状态机和共享缓存标明单一写 owner、互斥区、generation/token、完成/失败终态和幂等键；验证老请求在页面替换、锁定、Service 销毁和配置重建后不能回写或执行已撤销副作用。
- [ ] 对 CPU/IO 高负载与队列容量收集真实指标（耗时、排队长度、丢弃数、超时、资源使用），有测量结果后再调度优先级、并发数和 buffer；不从源码命中数推断性能瓶颈。

### Task 7：选择小批次试点并迁移

- [ ] 先以 Task 4 的同步桥接或 Task 3 中证明缺乏可替换执行上下文的一个叶子能力做试点，形成前后职责图、明确合同、定向行为测试与性能/延迟基线；不得同时重写 KDF、日志 writer、Runtime Session 和 Autofill lease。
- [ ] 每个试点先更新设计/调用者图，再实现并验证。异步 owner 改变时覆盖正常成功、普通异常、取消、fatal `Error`、超时、scope 销毁、晚到完成及重复请求；涉及认证/持久提交/lease 时额外覆盖 commit 前后、清理和重试。
- [ ] 以真实构建架构门禁和定向测试验证，必要时加 `verifyArchitecture` 规则与正反例测试。任何新规则应约束已证明会造成层泄漏的具体依赖，不误禁简单 Repository Flow、窄 Domain port 或 Android callback Adapter。
- [ ] 记录试点结果和保留/迁移/移除决定；只有跨 Feature 稳定复用的规则才同步进长期文档和 `AGENTS.md`。不将一次性并发审核细节变成根规则。

## 成功标准

- 有一份以当前模块边界为基础、明确 GoF 模式选择条件的正式架构决策；没有为了模式名而增加空壳层或复制业务状态。
- 每个生产后台 Scope、Dispatcher、Executor、Channel 和同步桥接都有可追踪的创建者、线程/队列合同、生命周期与失败/清理路径。
- ViewModel/UseCase/Repository 不负责猜测下层 Dispatcher；阻塞工作不会无意运行在主线程，CPU/IO 调整有具体调用链和测试/测量依据。
- 安全会话、Autofill 结果提交、密钥与日志专用队列保留其串行、提交顺序、取消和擦除合同。
- 试点实际减少了不清晰的 owner 或验证了现状合理；未验证的猜测保留为调查项，不作为重构结论。

## 初始源码与规范依据

- `docs/platform/architecture/overview.md`：模块、依赖、数据流与 UseCase 可选条件。
- `docs/platform/architecture/viewmodel-ownership.md`、`docs/engineering/development/maintenance-boundaries.md`：ViewModel 所有权、任务生命周期、取消与拆分合同。
- `runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt`、`RuntimeSessionManager.kt`：会话状态、lease 与资源退出并发。
- `security/src/main/java/com/aozijx/passly/security/authentication/method/credential/SingleThreadKdfRunner.kt`：KDF 单线程执行、取消和 worker secret 清理。
- `data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt`、`app/src/main/java/com/aozijx/passly/app/observability/ObservabilityRuntimeController.kt`：有界日志队列、flush 与生命周期/崩溃桥接。
- `feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt`、`feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/accessibility/ManualAccessibilityFillService.kt`、`feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/FinishAutofillInteraction.kt`、`feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillUnlockLease.kt`：系统 callback、逐 interaction 终结与 lease 释放调用链。
- `app/src/main/AndroidManifest.xml`、`data/src/main/java/com/aozijx/passly/data/local/datastore/AppPreferencesDataStore.kt`、`SecurityPreferencesDataStore.kt`、`data/src/main/java/com/aozijx/passly/data/local/database/DatabaseProvider.kt`：组件进程声明与当前 DataStore/Room 创建方式。

## 外部技术依据

- [Android 架构建议](https://developer.android.com/topic/architecture/recommendations)
- [Android Coroutines 最佳实践](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Android Data Layer：线程安全与操作生命周期](https://developer.android.com/topic/architecture/data-layer)
- [Kotlin Coroutines：结构化并发与 Dispatcher](https://kotlinlang.org/docs/coroutines-basics.html)
- [Android WorkManager：持久后台任务](https://developer.android.com/develop/background-work/background-tasks/persistent)

## 代码位置

| 职责 | 主要位置 |
| --- | --- |
| 模块与业务流程边界 | [`docs/platform/architecture/overview.md`](../../../platform/architecture/overview.md) |
| ViewModel/Route owner | [`docs/platform/architecture/viewmodel-ownership.md`](../../../platform/architecture/viewmodel-ownership.md) |
| Runtime Session lease 状态机 | [`SessionLeaseGate.kt`](../../../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt) |
| KDF 串行执行器 | [`SingleThreadKdfRunner.kt`](../../../../security/src/main/java/com/aozijx/passly/security/authentication/method/credential/SingleThreadKdfRunner.kt) |
| 加密日志有界写队列 | [`EncryptedLogRecordStore.kt`](../../../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt) |
| 平台结果与 lease 交付 | [`AutofillResultDelivery.kt`](../../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
