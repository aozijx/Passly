# 全项目测试逻辑清理审查

## 审查范围

本记录归档主维护计划中的跨模块测试逻辑审查与已完成修正。判断测试是否重复时，以生产 owner、输入、失败来源和最终副作用为准；相似名称不代表重复，不能用更短的文件作为删除理由。本记录不替代开放项：全仓逐例审查与 Detail 测试拆分评估留在[主维护 TODO Task 74](../work/plans/2026-10-30-passly-maintainability-todo.md#task-74)；Data/Room 的真实运行时验证留在[Task 36](../work/plans/2026-10-30-passly-maintainability-todo.md#task-36)。

## 已审查与修正

- **App 与 Feature：** Detail/Backup mapper 的状态断言迁到当前 UI 投影；Unlock 回归覆盖错误凭据失败后清除敏感输入，并在 attempt-status 读取失败时保留错误凭据反馈。Bitwarden 自动格式识别路径改由 registry/dispatcher 的测试覆盖，样本断言真实 folder 映射；Backup sheet metadata 和 OTP 列表复制的 owner 行为各由对应测试验证。
- **备份事务与解析：** `BackupImportPlannerTest` 区分可忽略输入、普通 IO 异常和 fatal `Error`；`DefaultBackupOperationGatewayTest` 按授权、导入/导出提交阶段和资源清理分别覆盖取消、失败、fatal 与密码擦除。跨阶段失败合同各自保留，没有把同名风格的异常测试合并。
- **Security 与 Runtime：** 会话 lock 与 database reset 的 fatal 清理入口分别保留；取消测试补调用 Job 的取消态断言。DEK 清理测试验证异常类型/message 与敏感状态终态，不假定跨协程 throwable 对象 identity 稳定。Runtime lease 的普通退出、超时、seal 和并发状态转换继续由不同场景覆盖。
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

## 历史验证边界

主计划迁移前的 2026-11-11 记录报告了全 JVM 测试通过及 App/Core/Data/Security/Autofill AndroidTest 源集编译通过；同一记录说明没有连接设备，Android instrumentation 未执行。此处保留为当日历史执行记录，本次归档没有重跑测试；不能据此判定全仓测试逻辑审查完成或设备行为通过。

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

## Task 73 历史质量修正

2026-11-07：`DetailViewModelTest` 中旧 CREATE/Lifecycle fixture 改为 AUTOFILL，重试回归确认订阅两次后进入 Ready；`ScannerResultCard` 的 Modifier 位置调整后，Entry 拆分阶段的 lint 任务成功退出。旧的 2026-11-02 失败统计不代表当前状态。本记录归档此前计划中的执行结果，本轮未重跑该定向测试或 lint。
