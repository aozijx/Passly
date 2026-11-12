# 全项目测试逻辑清理审查

## 审查范围

本记录归档主维护计划中的跨模块测试逻辑审查与已完成修正。判断测试是否重复时，以生产 owner、输入、失败来源和最终副作用为准；相似名称不代表重复，不能用更短的文件作为删除理由。本记录不替代开放项：全仓逐例审查与 Detail 测试拆分评估留在[主维护 TODO Task 74](../work/plans/2026-10-30-passly-maintainability-todo.md#task-74)；Data/Room 的真实运行时验证留在[Task 36](../work/plans/2026-10-30-passly-maintainability-todo.md#task-36)。

## 已审查与修正

- **App 与 Feature：** Detail/Backup mapper 的状态断言迁到当前 UI 投影；Unlock 回归覆盖错误凭据失败后清除敏感输入，并在 attempt-status 读取失败时保留错误凭据反馈。Bitwarden 自动格式识别路径改由 registry/dispatcher 的测试覆盖，样本断言真实 folder 映射；Backup sheet metadata 和 OTP 列表复制的 owner 行为各由对应测试验证。`DetailUiMapperTest` 的 section payload 测试改用 `DetailSectionResolver` 为每种凭据构造有效输入，避免把互斥凭据 section 塞进 NOTE 夹具；`DefaultAppNoticeDispatcherTest` 的同步闩锁改为响应任何设置快照读取，避免只在瞬时 `Loading` 才唤醒造成超时。当前 `DetailRoute` 仅在 route entry 与已加载 content entry 相同且存在 OTP section 时申请图级 lease，并在 effect 键变化或 Route 离开组合时释放；对应生产入口已列在 OTP Runtime 专题。当前仓库没有 `DetailOtpRetentionEffectTest` 源文件，故移除无法由当前测试源码支持的“该测试已编译通过”记录；Task 32 仍跟踪 Detail/List/Trash 转场的宿主级验收。
- **备份事务与解析：** `BackupImportPlannerTest` 区分可忽略输入、普通 IO 异常和 fatal `Error`；`DefaultBackupOperationGatewayTest` 按授权、导入/导出提交阶段和资源清理分别覆盖取消、失败、fatal 与密码擦除。跨阶段失败合同各自保留，没有把同名风格的异常测试合并。
- **Security 与 Runtime：** 会话 lock 与 database reset 的 fatal 清理入口分别保留；取消测试补调用 Job 的取消态断言。DEK 清理测试验证异常类型/message 与敏感状态终态，不假定跨协程 throwable 对象 identity 稳定。Runtime lease 的普通退出、超时、seal 和并发状态转换继续由不同场景覆盖。
- **Security DEK 测试复核（2026-11-11）：** `SensitiveDataKeyManagerTest`、`AttachmentContentCryptoTest`、`DekManagerTest` 的密钥擦除、包装持久化和锁定断言归属不同生产 owner，保留各自行为测试；三个文件此前各自重复实现同一 `VaultBootstrapStore` 内存假对象，已收敛为 `InMemoryDekBootstrapStore`，并保留敏感密钥/附件密钥 envelope 的独立存储及失败注入。`AuthorizationPermitRegistryTest` 的精确 scope、错误 scope 一次性消费、伪造 permit、超时/撤权及 revision action 断言对应不同授权边界，未机械合并。`:security:testDebugUnitTest` 本次复跑通过。
- **认证可用性测试范围校正：** `AuthenticationAvailabilityResolverTest` 实际只测 credential snapshot 到 `AuthenticationMethods` 的纯映射，不构造 Resolver、持久 Store 或 Manager。该测试先更名为 `AuthenticationCredentialSnapshotMappingTest`；全仓同名扫描再发现 Data 的同名测试实际测 `BootstrapData` envelope/binding→credential snapshot，与 Security 的 snapshot→methods 映射责任不同。保留两段映射断言，并将 Security 类进一步更名为 `AuthenticationMethodsMappingTest` 以区分 owner。Task 46 仍明确缺少 Resolver/Manager/Store/Provisioner 联动；state generation 与 retry 调度测试保留各自边界。
- **App Password 设置 owner（2026-11-11）：** `AppPasswordSettingsViewModelTest` 新增操作挂起时 ViewModel 清理取消、Provisioner 提交前取消与持久 availability 已更新后的提交后取消回归。各路径断言请求 CharArray 清零、操作态复位且不误发成功/失败；提交后取消还断言页面最终以 `isEnabled` Flow 反映已持久化状态。定向类测试通过；与真实 Provisioner/持久 Store 的 owner 联动仍在 Task 59 跟踪。
- **Data 与 Room：** `DatabaseReadAccess` 将授权、数据库 lock state 与单调 lock generation 组合成可读状态；`AppDatabaseSessionObservations` 只把 `SessionLockedException` 转为空占位，其他查询异常仍传播。Room observation 测试分别验证授权复核、已准入事务、reset 后分页、seal 等待/超时、锁定时取消旧 emission 与重订阅。`DatabaseObservationRecoveryTest` 覆盖 lock race 占位后重订阅、阻塞查询取消时不泄漏旧数据。修订历史 DAO tie-break 测试把同时间戳数据放到保留边界并反序插入，以断言稳定删除顺序。`EntryRevisionHistoryInitializationFlowTest` 覆盖已完成代次跳过、重建失败与标记失败后的重试、标记先于 post-commit cleanup，以及并发调用只重建一次；当前待补的是受影响 Data/Room instrumentation 的设备运行。
- **Diagnostics：** JSONL/TSV manifest、service 输出与正式格式页统一为 schema 1，payload 版本拒绝测试继续保留为独立负向合同。单格式 JSONL 本地输出断言并入全格式循环；factory/encoder 与 service 端到端合同仍由不同测试覆盖。
- **buildSrc：** `SourceBoundaryViewModelTest`、`PresentationUiApiVerifierTest` 与 DI/module boundary 测试针对不同 verifier 和输入边界；补齐 supertype 实际来源、Compose `Scope` 与嵌套泛型参数断言。不同 rule ID 与模块限制行为没有合并。

OTP generation、Runtime lease/失效和旧 Detail 刷新测试迁移的细节集中在[OTP Runtime 专项审查](2026-11-11-otp-runtime-owner-and-invalidation.md)，此处不重复。

## Activity 与 Revision DAO 收敛证据

主维护 TODO Task 34/36 的生产接口收敛和实现验证已完成；活动计划只保留尚未完成的设备 instrumentation 运行。

- **Task 34，Activity 查询：** `ActivityQueryRepository` 的唯一生产消费者是 `DetailViewModel`，只订阅 `observeByEntryId()`。移除没有生产消费者的 `observeAll()`、`observeByType()` 及对应 Repository/Room adapter 路径；保留 Room session gate 测试需要的查询和修订恢复事务测试使用的 `getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()`。`EntryActivityAnalyticsDao`、`AppDatabase` accessor 和聚合 DTO 无生产调用者；列表 usageCount/lastUsedAt 由 `EntryPagingQueryFactory` 提供。完整备份不含修订历史，因此没有备份合同依赖被删接口。
- **Task 36，Revision DAO：** 删除没有生产消费者的 `getByEntryId()`、`getLatest()`、`countAll()`、批量 `insertAllStrict()`、Import upsert 和 `deleteBefore()`；测试改用生产观察 Flow 与逐条严格插入，保留历史上限、恢复后最新快照、清空回收站与附件/图标引用保护断言。未改数据库实体或 Schema。
- **初始化编排：** `EntryRevisionHistoryInitializationFlowTest` 覆盖已完成 generation 短路、事务成功后的单次标记与先标记后清理、事务失败重试、generation 标记失败重试及并发调用串行复核。实现与调用位置见下方表格。

历史执行记录：Task 34/36 的 `:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin`、`verifyArchitecture` 与目标文件 `git diff --check` 曾通过；初始化编排定向 JVM 5/5、`:data:testDebugUnitTest` 177/177 与 `:data:compileDebugAndroidTestKotlin` 曾通过。上述均为计划中记录的 2026-11-08/11-11 结果，本次未重跑；Android instrumentation 未在设备执行，仍由主计划 Task 34/36 跟踪。

<a id="task-34-36-device-install-attempt"></a>

2026-11-11 随后连接 Huawei OXF-AN00 尝试 App instrumentation 时，设备 `/data/user/0` 仅余 616 MB，安装因 `Requested internal only, but not enough space` 失败；因此 Room/Revision instrumentation 当次没有执行。Data AndroidTest 源集编译已通过。此为当次设备环境记录，不代表当前可用空间或后续运行结果。

## 历史验证边界

主计划迁移前的 2026-11-11 记录报告了全 JVM 测试通过及 App/Core/Data/Security/Autofill AndroidTest 源集编译通过；同一记录说明没有连接设备，Android instrumentation 未执行。此处保留为当日历史执行记录，本次归档没有重跑测试；不能据此判定全仓测试逻辑审查完成或设备行为通过。

**本轮复跑（2026-11-11，按当前 XML 报告汇总）：** `:app:testDebugUnitTest` 1191 项、`:core:testDebugUnitTest` 73 项、`:data:testDebugUnitTest` 176 项、`:security:testDebugUnitTest` 180 项、`:feature:autofill:testDebugUnitTest` 140 项、`:domain:test` 208 项、`:runtime:session:test` 34 项与 `buildSrc:test` 112 项，共 **2114 项，0 failures、0 errors、0 skipped**。App/Data/Security/Autofill AndroidTest 源集编译通过，Core AndroidTest 无源；`adb devices -l` 无设备，instrumentation runtime 未执行。该结果验证当前 JVM 报告及 AndroidTest 源码编译，不代表设备行为覆盖，也不代表全仓测试逻辑审查已结束。

## 代码与测试位置

| 职责 | 类/函数与测试 | 源码 |
| --- | --- | --- |
| 当前 Vault 页面投影与 Detail 行为 | `VaultViewModelTest`、`DetailViewModelTest` | [`VaultViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModelTest.kt)、[`DetailViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModelTest.kt) |
| Backup 导入计划与事务清理 | `BackupImportPlanner`、`DefaultBackupOperationGateway` | [`BackupImportPlanner.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupImportPlanner.kt)、[`DefaultBackupOperationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt) |
| Security 会话与密钥清理 | `VaultSessionController`、`DekManager` | [`VaultSessionController.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt)、[`DekManager.kt`](../../../security/src/main/java/com/aozijx/passly/security/dek/DekManager.kt) |
| 修订历史 Room 查询和初始化测试 owner | `EntryRevisionHistoryRebuilder`、`InitializeEntryRevisionHistory` | [`EntryRevisionHistoryRebuilder.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionHistoryRebuilder.kt)、[`InitializeEntryRevisionHistory.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/InitializeEntryRevisionHistory.kt) |
| 初始化代次和失败重试编排回归 | `EntryRevisionHistoryInitializationFlowTest` | [`EntryRevisionHistoryInitializationFlowTest.kt`](../../../data/src/test/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionHistoryInitializationFlowTest.kt) |
| Activity 与 Revision DAO 的保留查询和写入 | `EntryActivityQueryDao`、`EntryRevisionQueryDao`、`EntryRevisionCommandDao` | [`EntryActivityQueryDao.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/dao/activity/EntryActivityQueryDao.kt)、[`EntryRevisionQueryDao.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/dao/revision/EntryRevisionQueryDao.kt)、[`EntryRevisionCommandDao.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/dao/revision/EntryRevisionCommandDao.kt)、[`RoomObservationSessionTest.kt`](../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/session/RoomObservationSessionTest.kt)、[`EntryUpdateAndRevisionRestoreTransactionTest.kt`](../../../data/src/androidTest/java/com/aozijx/passly/data/repository/entry/EntryUpdateAndRevisionRestoreTransactionTest.kt) |
| Room 观察在授权撤销、锁定和快速重开后的恢复 | `observeDatabaseReadAccess()`、`AppDatabaseSessionObservations`、`DatabaseObservationRecoveryTest` | [`DatabaseReadAccess.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/DatabaseReadAccess.kt)、[`AppDatabaseSessionObservations.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionObservations.kt)、[`DatabaseObservationRecoveryTest.kt`](../../../data/src/test/java/com/aozijx/passly/data/local/database/session/DatabaseObservationRecoveryTest.kt)、[`RoomObservationSessionTest.kt`](../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/session/RoomObservationSessionTest.kt)、[数据库会话合同](../../platform/data/database.md#session-生命周期) |
| 诊断导出 manifest 与 service | `DiagnosticsExportEncoders`、`ObservabilityExportService` | [`DiagnosticsExportEncoders.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt)、[`ObservabilityExportService.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |

## 本轮 Data/Room 测试逻辑复核

- `RoomObservationSessionTest` 的八个场景分别覆盖授权二次核验、已准入事务排空、新命令撤权拒绝、reset 后 Paging 重启、seal 等待/超时，以及普通锁/数据库锁导致旧 DAO emission 取消或占位后恢复订阅；这些场景验证的授权时点和 Room 生命周期不同，保留独立测试。
- `EntryRevisionLifecycleDaoTest` 分别验证单条目保留上限、跨条目全局清理稳定 tie-break、软删除与永久删除历史差异、清空回收站仅清删除项历史；没有同一结果的重复断言。
- `EntryLinkConstraintDaoTest.endpointTripleIsUnique_andDeletingEndpointCascadesLink` 已覆盖 DAO 唯一约束、同 ID 更新和级联删除；缺少的是 Repository `upsert()` 与端点删除的事务交错，所以 Task 35 只追 Repository 竞态，不再添加相同 DAO 约束测试。
- 当前 Data 有 176 项 JVM 测试，AndroidTest 源集编译通过。全仓未发现 `src/testFixtures`/`src/androidTestFixtures` 目录或 Gradle fixture 配置，也未发现 disabled/ignored 测试、明显恒真断言或重复的反引号测试名；这些命名/静态筛查不能代替剩余 Feature owner 的逐例审查。

## DetailViewModelTest 拆分评估

当前 `DetailViewModelTest` 覆盖 favicon/敏感字段交互、完整编辑准备、package picker、activity/revision history、detail snapshot 与 revision restore。相似的“失败后可重试”场景由不同 owner 和状态源驱动；取消、fatal error、session lock 与 entry replacement 断言也对应不同恢复/迟到结果合同，目前没有可安全删除的整组重复行为。该测试正在随 Detail owner 的其他改动一起变化，因此本轮不做机械搬文件；待 production owner 稳定后再按工作流拆分，避免搬动尚未稳定的宽 fixture。当前证据见主维护 TODO Task 74。

## Security DEK 测试结构收敛

`SensitiveDataKeyManagerTest`、`AttachmentContentCryptoTest` 和 `DekManagerTest` 复用 `InMemoryDekBootstrapStore`，移除三份重复的完整 `VaultBootstrapStore` fake 实现，同时保留每个测试各自的密钥行为断言及对应 envelope 保存失败注入。`:security:testDebugUnitTest` 于 2026-11-11 复跑通过，覆盖该共享 fake 改动。

## OTP Runtime 测试逻辑抽查

`OtpCodeRuntimeTest` 中易被误判为重复的 HOTP copy 异常测试分别走已激活 schedule 的 `generateHotpCode()` 分支和无 lease 状态下的 on-demand material 分支；前者校验 activation token，后者在 consume 后复核 copy token/session epoch，因此保留。其余定时复制、下一窗口预生成、隐藏条目按需复制、HOTP 计数提交、session lock、lease 最终释放、并发配置读取及旧 generation 丢弃也属于独立状态/副作用边界。当前 `OtpCodeRuntime` 与测试文件都仍有工作区修改，本次只做逻辑复核，不拆分或迁移其仍在变化的 fixture；Task 27 的真实 Route/NavHost 行为验收仍未完成。

## Vault OTP 映射断言去重（2026-11-12）

`VaultListUiMapperTest` 原本还包含 timed 与 HOTP runtime 状态到 `OtpUiState` 的投影断言，但这两条只调用 OTP state mapper，并未覆盖 `VaultListUiMapper`；同一映射 owner 已由专属 `OtpUiStateMapperTest` 覆盖，包含 timed current/next code 与进度、HOTP 不生成 next code、pending/failed、hidden/not-applicable/not-requested。已从 Vault list mapper 测试移除这两条重复场景与相应 imports，保留该文件真正验证的 list projection 合同。2026-11-12 的 `:app:compileDebugUnitTestKotlin` 尝试未能验证本改动：当次失败发生于 `:app:compileDebugKotlin`，当时工作区的 OTP UI 合并存在 `OtpPresentation`/`OtpType` 调用合同、`OtpUiState` 派生属性、Detail 映射及 card style 不一致，Backup 编译也有未解析错误类型/常量；测试源码尚未到达编译阶段。随后当前工作树的 `:app:compileDebugKotlin verifyArchitecture --no-parallel --max-workers=2` 已通过，但没有编译或运行测试源集；仍需重新执行相关测试验证。本批改动未提交。

## Backup Archive Service 与 Operation Gateway 测试 owner 复核（2026-11-12）

复核 `BackupArchiveServiceImplTest` 与 `DefaultBackupOperationGatewayTest` 的相似异常用例后未合并：前者验证 Archive Service 的文件流 close、解码 bundle 资源清零、restore commit 后日志失败/取消/fatal 的结果合同，以及 encrypted terminal 缺失时不 restore；后者验证 authorization/storage 异常映射、SAF 导出目标创建和失败文件删除、调用方所有权开关、操作密码副本清零及清理失败如何保留/附加原异常。两处对取消和 fatal 的覆盖分别约束 Archive 内部事务与 Gateway 外部文件/授权副作用，删除任一组都会丢失不同 owner 的边界断言。静态阅读未发现可安全删并的测试；App 生产编译当前失败，未执行这两个测试类。

## RuntimeSessionManager 大测试类逻辑复核（2026-11-12）

`RuntimeSessionManagerTest` 虽有 900 行左右，测试按 session lifecycle 与 lease drain owner 覆盖 open/resume/soft-lock/seal、key 获取/open/close 错误分类、key 清零、telemetry 普通错误/取消/fatal 对每类转场结果的影响、活动 lease 正常排空、超时后取消写入，以及非协作写入仍存活时资源延迟关闭。表面相似的 `DRAIN_TIMEOUT` telemetry 场景分别约束普通异常继续收尾，和取消/fatal 仍执行 lease cancellation/resource close；转场矩阵不覆盖 lease drain，二者也不重复。没有因行数拆分或删除场景。`:runtime:session:test --rerun-tasks --no-parallel --max-workers=2` 于 2026-11-12 实际执行通过；XML 汇总 34 tests、0 failures、0 errors、0 skipped。`UP-TO-DATE` 首次调用不计作执行证据。

## Security session lifecycle 测试与 Runtime session 去重检查（2026-11-12）

`VaultSessionControllerLifecycleTest` 与 `RuntimeSessionManagerTest` 的 close/cancel/fatal 用例分别验证安全编排和底层资源 lease：Security owner 还需完成认证状态撤权、permit revoke、DEK/sensitive key 清除、reset/recovery 交接及失败后重试。其同名附近的“调用者取消 lock”与“sessionManager.seal 自身抛 CancellationException”区分取消来源；soft-lock cancellation 也保留 SOFT_LOCKED 终态，不能与 seal 合并。没有发现可安全删除的跨类重复测试。`:security:testDebugUnitTest --rerun-tasks --no-parallel --max-workers=2` 于 2026-11-12 实际执行通过；XML 汇总 180 tests、0 failures、0 errors、0 skipped。

## More 菜单 UI 测试复核

按 `moreMenu`/More 菜单语义检索全项目 `*Test.kt`，只有一个 `VaultTopBarMenuTest`，没有重复的 More 菜单测试文件或测试方法。该测试经同一顶栏弹出菜单检查 Scanner/Settings 路由、排序动作和动画首帧 popup 可测量性，属于一个真实 UI owner 的不同可观察结果；目前证据不支持删除或合并其他测试。

## Vault 列表 ViewModel 测试复核

`VaultViewModelTest` 的类别流“首个 emission 前失败”与“先发值再失败”分别验证空初态错误和已发状态后错误；字段复制与 OTP 复制在页面离开时分别保护敏感字段写入与 OTP lease 状态；删除后清 OTP 状态与清理 graph lease 验证的是运行时最终态与 lease 副作用；设置读取恢复、排序 Paging generation、排序写入失败反馈也属于不同数据源/状态 owner。未发现可删除的重复场景。删除旧 `VaultDisplayViewModelTest` 后曾只保留失败重试与 hierarchy 行为，本轮补回了新的 owner 下“等所有设置源发值再离开 Loading”及完整 Ready 字段投影断言。`retainOtpIsIdempotentAndGraphViewModelOwnsLeaseUntilCleared` 原来只持有一个 entry，本轮扩展为两个 entry，重复 retain 第一个，并验证 ViewModel 清理时两个 lease 都释放。`:app:testDebugUnitTest --tests com.aozijx.passly.feature.vault.presentation.list.VaultViewModelTest` 通过。

同名源集测试核对发现 JVM 与 AndroidTest 各有 `AppBackupSnapshotGatewayTest`，但并非重复：JVM 测试覆盖 restore 的映射、overwrite 保护和 OTP invalidation；AndroidTest 覆盖实际 Android `Context` 下的 `read()`、资源 ID 和读取选项上限。保留两个 source set 的测试 owner。

## Autofill 提交与结果测试复核

`AutofillActivityResultClaimTest` 验证单个 Activity 的平台结果 claim 在提交标记前可恢复、标记后不可撤销；`AutofillResultDeliveryTest` 则验证 Autofill interaction 完成、selection 与临时解锁 lease 的释放。两者 owner 和提交范围不同。`AutofillResultDeliveryTest` 中普通 trace 异常被吸收、fatal `Error` 在已送达结果完成收尾后重抛，尽管重复了部分 setup，但断言的异常契约不同，继续保留为分开的场景。该模块当前 test/AndroidTest 共 32 个测试源码文件，没有重复 basename；其中 Android PendingIntent 与 origin attack instrumentation 验证真实平台行为，不由纯 JVM policy/parser 测试替代。

## Core 测试逻辑抽查

Core 中易混淆的场景分别绑定到不同边界：clipboard scheduler 的取消、替换、交错复制和旧 timer owner 检查覆盖不同时间竞争；permission dialog arbiter 的互斥持有与重复释放验证不同 lease 不变量；trace token 的单次 resume 与 continuation callback key 的单次消费不是同一种凭据；`FieldEncryptorTest` 的 fresh nonce/AAD 认证与短 payload 拒绝也对应加密正确性和输入格式校验。按这些生产 owner 复核没有发现可安全删除的重复断言，未改动 Core 测试源码。

## 当前删除测试文件复核

- `VaultAddTypeUiModelTest` 对应的旧 UI model/转换已从生产代码移除；`AddTypePresentationTest` 改验 `AddType` 直接元数据与 FAB 支持，`VaultListUiMapperTest` 仍校验可创建类型和顺序。
- `CardStyleSettingsProjectionTest` 的 persisted selection 与不支持样式回退断言迁入 `InterfaceSettingsUiMapperTest`；新的 `CardStyleSettingsSectionTest` 另验真实设置页展开、预览尺寸和选择 action。
- `RevisionHistorySheetTest` 替换为当前同时呈现 activity/revision 的 `DetailHistorySheetTest`，保留长列表滚动、revision 比较与恢复、仅对可 reveal 历史敏感行提供 reveal，以及恢复中禁止离开。
- `VaultOtpSubscriptionEffectTest` 的组合生命周期 unsubscribe 合同随 UI 改为 request-only 已过时。`VaultOtpRequestEffectTest` 后续也已删除：它依赖已删除的 `onAcquireOtp` 参数，并重复验证 `VaultViewModelTest` 已覆盖的图级 lease；当前没有 Compose 测试覆盖可见时的请求及真实导航交接，Route/NavHost 验收仍由主维护计划 Task 32 跟踪。
- `VaultDisplayViewModelTest` 的 loading/字段投影缺口已迁入当前 `VaultViewModelTest` 并补回；失败重试和 hierarchy 无关行为也迁到同一 owner。
- `EntryVisualCategoryClassifierTest` 移到新的 `entryvisual` owner 包并增加所有固定 EntryType 的分类覆盖；Domain OTP 测试移到 `otp.generation` 包，SHA256 弱“成功即可”检查替换为 SHA1/SHA256/SHA512 RFC 6238 全向量校验并补 byte material 清理覆盖。Security mapping 测试按实际 owner 更名，测试内容保留。

上述测试删除均已对照当前生产 owner 与替代测试审阅；`:domain:test --rerun-tasks --no-parallel --max-workers=2` 本次实际执行通过。AndroidTest 替代文件仍需源集编译验证，设备运行不由 JVM 测试结果代替。

## Task 73 历史质量修正

2026-11-07：`DetailViewModelTest` 中旧 CREATE/Lifecycle fixture 改为 AUTOFILL，重试回归确认订阅两次后进入 Ready；`ScannerResultCard` 的 Modifier 位置调整后，Entry 拆分阶段的 lint 任务成功退出。旧的 2026-11-02 失败统计不代表当前状态。本记录归档此前计划中的执行结果，本轮未重跑该定向测试或 lint。

## HOTP 事务 AndroidTest 逻辑复核（2026-11-11）

`:data:compileDebugAndroidTestKotlin` 首次重跑发现 `HotpCodeConsumptionTransactionTest` 有 5 处直接调用未传入生产接口新增的 `expectedCounter`。按 `HotpCodeConsumptionTransaction.consume()` 的合同，现有成功、失败、缺失/非 OTP 与无关敏感字段用例均显式提供期望计数；新增陈旧计数场景断言返回 `CounterChanged`、不调用 generator、持久 counter 不变。`:data:compileDebugAndroidTestKotlin` 使用 fresh single-use Gradle daemon 后通过。第一次重跑中的 KSP cache corruption 与复用 daemon 后的 `Storage already registered` 属于构建缓存/进程状态错误，不是测试断言失败。连接的 Huawei OXF-AN00 `/data/user/0` 只余 616 MB，之前 App instrumentation 安装失败；本测试只验证源码编译，未在设备执行。
## OTP Runtime 测试 API 与耗尽态复核（2026-11-11）

App 单测源集编译先揭示 `OtpCodeRuntimeTest` 中 3 处过时使用：HOTP consume 测试端口已改为 `(entryId, expectedCounter)`，测试 helper 仍声明单参数回调；耗尽态用例还直接调用了私有 `generateHotpCode()`。已统一 helper/fake 签名，让计数消费用例断言 Runtime 传入的期望计数，并将耗尽态用例改为通过公开 `codeForCopy()` 验证“无可复制码且 state 为 `CounterExhausted`”。`:app:compileDebugUnitTestKotlin` 通过；定向 `OtpCodeRuntimeTest` XML 报告为 42 tests、0 failures、0 errors、0 skipped。该结果只覆盖此 Runtime 单测类，不代表 Detail 全类或全仓逻辑审查完成。
