# 兼容与冗余清理审计

本文只跟踪尚未完成的全仓声明与重复规则审计。已完成清理及留存合同见[审计记录](../../reviews/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)；当前 Backup 边界见[工作流总览](../../../product/backup/backup-workflows.md)和[验证要求](../../../product/backup/verification.md)。

## Task 1：完成全仓未引用声明与重复规则审计

- [ ] 按生产 owner 检索各模块 Kotlin/Java 未引用声明、重复策略及测试专用生产接口；对每个候选闭合生产调用、DI/注册、持久化或协议、测试和正式文档证据。只将已证明冗余的候选移除；有效合同或缺少证据的候选记为具体后续项，不按搜索命中直接删除。
- [ ] 更新对应正式主题文档和本审查记录，移除已完成事项；将本任务改成仅包含剩余候选，或在审计闭合后删除本计划并修复索引。

范围包括 `app`、`domain`、`data`、`security`、`runtime`、`core` 和 `buildSrc` 当前生产源集。既有工作区改动需先排除；本计划不包含构建或测试验收，除非用户另行要求。

## 已审计模块

### `runtime/session`（2026-11-12）

模块内七个生产 Kotlin 文件及仓库调用点已逐项核对，未发现有证据支持删除的声明或重复实现。`SessionStateProvider` 提供锁状态和 generation 读取，供 Data 清理队列与 App OTP Runtime 使用；`DatabaseSessionLifecycle` 提供 unlock、soft-lock、seal 命令，供 Security 锁定/重试和 Data reset 使用。两者由 [`AppDatabaseSession`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) 分别实现，DI 绑定见 [`VaultStorageModule`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/VaultStorageModule.kt)。`SessionResource` 由 Data 的 [`AppDatabaseSessionResource`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionResource.kt) 实现，`SessionKeySource` 由 Security 的 [`SecureSessionKeySource`](../../../../security/src/main/java/com/aozijx/passly/security/session/SecureSessionKeySource.kt) 通过 [`SensitiveKeyModule`](../../../../security/src/main/java/com/aozijx/passly/security/di/SensitiveKeyModule.kt) 绑定；并发 lease 和状态转场由 [`RuntimeSessionManagerTest`](../../../../runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManagerTest.kt)、[`RuntimeSessionLifecycleConcurrencyTest`](../../../../runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionLifecycleConcurrencyTest.kt) 覆盖，当前行为见[数据库会话合同](../../../platform/data/database.md#session-生命周期)和[运行时流程](../../../platform/architecture/runtime-flows.md)。此次为源码、注册、调用方与文档静态审查，没有修改实现，也没有运行测试；其余模块仍未完成审计。

### `buildSrc`（2026-11-12）

13 个生产 Kotlin 文件都接入了构建逻辑：根插件注册架构依赖/源码边界门禁、错误目录生成与校验及显式架构指标报告；根构建应用插件，App/Core 模块消费生成的目录源码，错误码验证由各子项目 `check` 共享。[`VerifyModuleBoundariesTask`](../../../../buildSrc/src/main/kotlin/passly/VerifyModuleBoundariesTask.kt) 组合调用源码边界与 Presentation API 检查，错误目录任务分别生成 Kotlin 源码/Markdown 并验证资源覆盖。Gradle 注册、生成输出和测试引用均已检索；没有发现可确认删除的旧任务、未调用校验器或重复生成路径。`reportArchitectureMetrics` 是显式调用时产出报告的任务，不因没有其他任务依赖它而视为死代码。此次为源码和 Gradle 配置静态审查，没有修改实现，也没有运行 buildSrc 测试或架构门禁；其余模块仍未完成审计。相关入口见[根构建](../../../../build.gradle.kts)、[模块插件](../../../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt)和[buildSrc 测试](../../../../buildSrc/src/test/kotlin/passly)。

### `core/platform/packageinfo`（2026-11-12）

已核对包名目录、图标加载器、Compose 状态帮助函数、Hilt 绑定和仓库调用方。`InstalledAppCatalog` 同时实现 Domain 的 [`ApplicationLabelResolver`](../../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/ApplicationLabelResolver.kt) 与 Core 的 [`InstalledAppDirectory`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppDirectory.kt)：前者供 Autofill 标签解析，后者供新增密码和详情关联应用的元数据/可启动列表；两者语义不同，不能合并成重复端口。Core 的 [`InstalledAppCatalogModule`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppCatalogModule.kt) 分别绑定两个端口。图标路径由[`InstalledAppIconLoader`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppIconLoader.kt)、[`InstalledAppServicesProvider`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppServicesProvider.kt) 和[`rememberInstalledAppIconBitmap`](../../../../core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppIconState.kt)组成；Vault 图标组件和详情/新增密码页面仍有生产调用，缓存、Android PackageManager 访问、图像转换及 Compose 生命周期边界均有实际职责。App 单测通过目录端口替身覆盖相应页面行为；Core 中没有该目录的独立测试文件。没有发现可确认删除的声明或重复路径。此次为静态源码、调用方和 DI 审查，没有改实现或运行测试；其余 Core 包及其他模块仍待审计。

## 本轮已完成的局部调用闭包（2026-11-12）

以下结果仅覆盖列出的 Vault/App/Domain/Data 路径；不代表这些模块或全仓审计结束。

- Vault 字段读取的固定纯映射不再通过 App Hilt 注入：所有 Detail、Copy、Reveal 与 revision compare 调用直接使用 Domain `EntryFieldReader`；原映射测试和各消费方测试已迁移。提交 `881d5f322`。`:app:compileDebugUnitTestKotlin :domain:compileTestKotlin` 通过；只编译测试源码，未执行测试。
- `EntryListItemMapper` 现一次完成 Room entry row 到轻量 Domain `EntryListItem` 的身份、Profile、能力、OTP 类型及可选 usage/account projection 组装；分页、回收站和 Autofill 调用迁移，旧 `assemble(entity, summary)` 删除。提交 `95f43d712`。`:data:compileDebugUnitTestKotlin :app:compileDebugUnitTestKotlin` 通过；首次编译发现 mapper 测试残留旧调用，迁移后重编译通过，未执行测试。
- 无向 `EntryLink` 的另一端判断集中于 Domain `EntryLink.otherEndpoint()`，Details 与 Related Password operations 不再各自实现端点方向判断。提交 `dca99dd65`。`:app:compileDebugUnitTestKotlin :domain:compileTestKotlin` 通过，原有 link canonicalization/self-link 测试保留并增加端点查询断言；未执行测试。
- 修订历史列表 affected-fields 与差异行共用 `DetailRevisionFieldLabelUiState.resolveText()`。提交 `d6ae88c00`。`:app:compileDebugUnitTestKotlin` 通过；未执行测试。该构建提示既有 `FaviconImageStore.kt:145` nullable receiver 警告，与本修改无关。
- 针对 Entry、Link 与 Attachment DAO 按声明检索当前生产和 instrumentation 调用方：批量入口仍分别服务重建、备份、关系解析和事务；删除无源码/测试调用的 `AttachmentResourceDao.updateState(List, state)`、`updateState(id, state)` 与 `AttachmentRefQueryDao.countByEntryId()`。前两者由带完整状态语义的 `markPendingGc()` / `reactivate()` 保留唯一写入路径。保留仅被 instrumentation 使用的 `EntryActivityQueryDao` 计数查询与 `RevisionAttachmentRefDao.countByResourceId()`，它们分别断言事务的活动数量和修订附件清理结果。`:data:compileDebugUnitTestKotlin` 用于确认 Room/KSP 源集，未执行测试。其他 DAO 和 Data 契约仍待审计。
- Data 的 Entry/Activity mapper 只被 Data repository、codec 和事务实现调用，没有跨 `:data` 的生产消费者；将 assembler、profile/secret mapper、capability write mapping 与 activity mapper 收窄为 `internal`，映射逻辑不变。`:app:compileDebugUnitTestKotlin` 通过，覆盖 Data 与 App 生产/测试源码编译；未执行测试。
- Security authentication 的顶层 owner/helper 声明定向检索中，低调用数项仍分别被当前生产流程和行为测试使用：会话复用策略、认证方式映射、Android prompt spec/error classification、secret input ownership 和 biometric alias cleanup 均不是旧路径；本轮未发现可删除项。该结果不是 Security 全模块审计结论。
- Autofill `AutofillInteractionSource.LEGACY` 与 `AutofillInboxSource.LEGACY_SERVICE` 仍由 Android Autofill Service/Legacy entry、interaction store 和观测调用，受 Android 平台交互和 Inbox 来源合同保护；只移除已经没有生产状态的旧 `LEGACY_SAVE` 结果类型。Autofill 其余实现仍待审计。
- Core 的 observability/UI 声明定向复核中，`ObservabilityIdGenerator` 有 App DI 与 logger/trace tests 消费；`FatalLogStatement` 保留 `unhandledException()` 的 fatal 分类返回合同；`PasslySheetTitle` 有 Autofill 与 Vault 两个调用点，样式函数在更多页面复用。未发现本轮可删除项；Core 其余包仍待审计。
- App 与 Domain `internal` 顶层函数的低引用候选定向复核中，settings navigation transition helpers、theme interpolation、device-lock result 和 Backup write cleanup helper 都有同模块生产路径及对应行为测试；没有孤立声明可移除。此为符号级局部检查，不代表 App/Domain 模块审计完成。
- `:feature:autofill` 低引用声明复核中，Legacy Autofill、Manual Accessibility Fill 与 Modern Credential Provider service 都由 Feature Manifest 注册；save rejection/result/title helpers 有当前调用或供对应行为测试使用，未发现孤立旧路径。模块其他声明仍待审计。
- Domain access 中 `AuthenticationPolicy`、`AuthenticationRequirement` 与 `AuthenticationFreshness` 没有实现、DI 绑定、生产/测试消费者，只在相互 KDoc 中出现；当前策略由 `AuthenticationMethodPolicy` 和 `AuthenticationManager` 实际执行。删除未接入的旧 API，并把 `AuthorizationGate` KDoc 同步到当前链路。`:domain:compileTestKotlin :security:compileDebugUnitTestKotlin :app:compileDebugUnitTestKotlin` 通过；仅编译源码，未执行测试。
- Domain ports 定向扫描未发现只有声明、没有生产实现/消费者的接口；所有低引用接口至少有所属模块绑定与生产使用。这个结果与本轮移除的非-port `AuthenticationPolicy` 分开记录，其他模块端口仍待查。
- Vault 搜索状态持有器的 `queryChanged()` 只转发同一个 `SearchQueryChanged` action，且没有独立状态行为；由 `VaultSearchHeader` 直接发出 action，清除查询仍由持有器负责保持其退出搜索状态的行为。删除转发入口，保留管理焦点、搜索生命周期和返回键状态的持有器。核对范围为 Vault 搜索持有器及调用点；未运行测试或构建。
- Detail ViewModel 私有 `openPasswordBinding()` 是单调用点的一行 `passwordRelationsWorkflow.open()` 转发，没有额外检查或状态处理；在两个打开/重试 action 分支直接调用 workflow 并删除 helper。`DetailPasswordRelationsWorkflow.open()` 的任务和会话校验行为保持不变；未运行测试或构建。
- `VaultSearchStateHolder` 仅由 `:app` 的 Vault Screen/TopBar 生产代码和同模块测试使用；没有跨 Gradle 模块调用或平台反射/注册合同，构造与 remember 工厂原已为 `internal`。将类收窄为 `internal`，使其状态 API 不再泄漏为 App 模块公共类型；未运行构建或测试。
- Hilt `@Module` census 覆盖 `app/src/main` 全部模块声明：现有多数模块已为 `internal`；余下 App `DatabaseResetAdapterModule`、`EntryCreationModule`、`EntryPolicyModule`、`FaviconImageProcessorModule` 与 `NoticeModule` 均只由 Hilt `@InstallIn` 注册，仓库无手动安装调用。结合 Dagger 官方 Hilt module visibility best practice，将五者收窄为 `internal`，并将同一 DI binding 中仅用于绑定/测试的 `DatabaseResetGatewayAdapter` 一并收窄。`:app:compileDebugKotlin --no-parallel --max-workers=2` 成功，执行 App KSP 和 Kotlin production compile；存在既有 `FaviconImageStore.kt:147` nullable `File?` 警告，未执行测试。
- Data 修订/密钥字段/维护 DAO 方法级调用核对中，`SecretFieldQueryDao.exists(entryId)` 没有任何生产、单测或 instrumentation 调用；同名但独立的 `EntryQueryDao.exists()` 仍由备份与恢复路径使用。删除未使用的 secret-field query。`:data:compileDebugKotlin --rerun-tasks --no-parallel --max-workers=2` 成功并执行 KSP 与生产 Kotlin 编译；未执行测试。其余修订查询、附件引用、清理写入方法均找到具体事务或 Room 行为测试消费者。
- `EntryCommandDao` 全部写入方法沿 Create/Update/Trash/Restore/永久删除/修订版本与 Room instrumentation 调用逐项闭合；未发现无调用的写查询。本记录仅为静态调用审查，未运行测试。
- 补查 `EntryActivityCommandDao`、`EntryLinkCommandDao` 与 `AttachmentRefCommandDao` 的所有方法：活动清理、幂等记录、链接 upsert/恢复事务及附件备份/恢复事务均有生产消费者；严格 insert 等入口也有对应 Room 测试。没有可确认删除项；静态审查未执行测试。
- `AppDatabase`、`DatabaseProvider`、15 个 Room DAO、8 个 Entity、`EntryPagingRow`、`AttachmentResourceState` 与 `DatabaseSchema` 的使用都留在 `:data`；App、Security、Core、Domain、Autofill 不直接导入 Room 实现，跨层调用经 Repository/Domain 端口或 Runtime session。统一收窄这些持久化实现类型，并将暴露 `AppDatabase` 的 `SecretFieldStore` 与 `AttachmentResourceGarbageCollector` 也设为模块内部。首次编译指出两个 helper 仍以 public 方法暴露该类型，已按真实 owner 收窄。共享构建输出因同时运行的 App 测试任务发生缓存冲突后，使用独立 build 输出重编成功：`:data:compileDebugUnitTestKotlin :data:compileDebugAndroidTestKotlin` 均通过，Room/KSP 与 Data 生产编译也通过；未执行测试。
- Data 的 secret field/bundle、revision snapshot、encrypted log codecs，三项 Proto `Serializer` 和 Room `TypeConverter` 均只由 `:data` 实现/测试源码使用，不是跨模块格式 API。按 Data artifact 边界将这些实现类型收窄为 `internal`，编码内容与字段键不变；待 Room/KSP 与两类测试源码编译核验。
