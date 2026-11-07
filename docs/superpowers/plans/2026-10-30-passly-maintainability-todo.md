# Passly 深度维护审查待办

**最近复核：2026-11-07。** 全模块 Kotlin 静态盘点，并沿生产调用闭包重点检查会话/认证、敏感清理、Flow 恢复、页面与平台宿主、备份、诊断及既有待办；不等同于全仓安全扫描或逐行证明。

## 执行顺序

保留原 Task ID，正文按批次重排。P0 表示工程门禁阻断，P1 为敏感/资源安全或具体运行流程缺口，P2 为恢复/交付合同，P3 为结构维护。**retained** 表示仍有证据支持的工作；**deferred/待验收** 表示已有机制但指定验证未完成；**removed** 仅移除过时缺陷，不代表整项已通过。编号不代表优先级。

| 顺序 | 执行入口 | 依赖与原因 |
| --- | --- | --- |
| 1 / P1 | [安全 Tasks 6、2](2026-11-06-passly-security-scan-followups.md)，本页 26、37、43、59 | 接收方身份、lease 关闭、撤权与提交事实；26/37 先统一生命周期/线性化定义；安全 Task 8 已完成 |
| 2 / P1 | 本页 38、42、40、41、32；[Backup Tasks 1、5](2026-11-06-passly-backup-review-followups.md)；[诊断 Tasks 1–3](2026-11-06-passly-diagnostics-reliability-followup.md) | 保存期输入、seed/流资源、操作终态、分类查询、flush；先固定失败结果再改 owner |
| 3 / P2 | 本页 46、51；[安全 Tasks 3、4、7、9](2026-11-06-passly-security-scan-followups.md)、Backup Tasks 2、3、OTP/搜索专题；Autofill Task 82（设置策略） | 46 接 59 的提交后恢复；资源预算先于最低设备验收；Autofill 设置策略待办由专题清单维护 |
| 4 / 定向验收 | 本页 22、23、27、29、31、44、45、64；[Autofill 宿主验收](2026-11-07-passly-autofill-followups.md#task-21)、Scanner 验收 | 已有机制，不重复实现；JVM、Route/宿主和设备验证分别标记 |
| 5 / P3 | 本页 83 → 84/80，39、35、34、36、78、74、75、77 | 83 管 Detail 具体流程，84 管 owner 清单，80 管状态模式；39/77 先稳定诊断可靠性 |

安全 Task 1/5/10、Autofill 设置快照、OTP HOTP 和非 ASCII 搜索仍有策略合同待裁定；本轮不臆定产品行为，不把候选升级为已验证漏洞。允许独立项并行，表中箭头表示必须先满足的前置条件。

## 专题 owner 索引

| 主题 | 唯一详细待办/验收来源 | 本页关联 |
| --- | --- | --- |
| 架构与并发 | [架构与并发 TODO](2026-11-06-app-architecture-concurrency-todo.md) | 26、37、51、80、83、84；具体缺陷先处理，广域提案随后 |
| Autofill 后续 | [唯一后续清单](2026-11-07-passly-autofill-followups.md)；[模块实施记录](2026-11-06-autofill-feature-module.md)、[BottomSheet 实施记录](2026-11-06-autofill-bottom-sheet-auth-contract.md) | 21 宿主验收、82 设置策略；81 授权异常与 33 系统观察已完成，不再作为当前待办 |
| Scanner | [审查 TODO](2026-11-06-passly-scanner-review-followups.md)、[取景框计划](2026-11-06-scanner-entry-and-framing.md) | 42、51、86；显式关闭/宿主暂停区分在专题 Task 4 |
| Vault / OTP | [搜索 TODO](2026-11-06-passly-vault-search-review-followups.md)、[OTP TODO](2026-11-06-passly-otp-review-followups.md) | 27、32、34–36、42、43 |
| Backup / 诊断 | [Backup TODO](2026-11-06-passly-backup-review-followups.md)、[数据端口迁移计划](2026-11-07-backup-data-port.md)、[诊断可靠性 TODO](2026-11-06-passly-diagnostics-reliability-followup.md) | 39–41、73、74、77、86 |
| 安全与资源 | [安全遗留 TODO](2026-11-06-passly-security-scan-followups.md) | 26、37、43–46、59、64；安全 Task 2 统一持有非合作 lease 关闭清单 |

## Autofill 重构后的清理

Autofill 的 7 个旧段落移出本页：9 合入 21、52 合入 33、85 移除；21/33/81/82 由[专题清单](2026-11-07-passly-autofill-followups.md)唯一维护。当前 Autofill 清单只把 21、82 列为未完成；81、33 保留为已完成证据。下方扫描数量与失败统计是历史采样记录。

## 此前扫描与验证记录

- 扫描开始时清点 8 个 Gradle 业务/能力模块及 buildSrc：生产 main 1,072 个 Kotlin 文件（含 buildSrc 13 个），测试源 377 个。尺寸只用于筛查，不按行数拆分。39 个既有主任务逐项复核，新增验证基线 Task 86。
- 修正已迁移的 Autofill、Room 观察、认证输入、通知快照、GC 和正式 UDF 规范等过时描述；补分类查询恢复、导出流所有权及 Scanner 输入资格，保留各专题 owner，不复制第二份修复清单。
- 串行运行 `gradlew.bat -p buildSrc test --no-parallel --max-workers=2`：通过，实际执行 121 项；随后 `verifyArchitecture`：通过，含 module/error-code 门禁。
- 运行 `gradlew.bat :app:testDebugUnitTest :feature:autofill:testDebugUnitTest --continue --no-parallel --max-workers=2`：首轮 App 主/测试源码编译通过，987 项中 9 失败；Autofill test 任务 UP-TO-DATE。详见 Task 86。
- 收尾运行 `gradlew.bat :app:testDebugUnitTest :feature:autofill:testDebugUnitTest :app:lintDebug :feature:autofill:lintDebug --continue --no-parallel --max-workers=2 --quiet`：App **984 项/9 失败**，仍为诊断 8 项与 Scanner 1 项；Lint 为 app **2 errors/27 warnings**、Autofill **5 errors/1 warning**，构建退出 1。Autofill 单测 XML 131/0，任务复用缓存。
- 执行期间有其他工作推进 Autofill/Scanner/Backup 和测试；本页保留这些并发改动及 Task 21 的同期纠正。统计与验证绑定各次工作区采样，不保证后续变化；新备份端口计划由其原 owner 继续维护。
- 2026-11-07 首轮仅更新待办与本地维护规则，未修改生产/测试实现；后续安全 Task 8 批次完成敏感数组清理和状态一致性修复并单独记录验证。保留既有和并发改动；设备仪器、APK Analyzer、KDF/低内存基准、强杀持久性未执行，不沿用历史通过记录代替。

## P0：当前无工程门禁阻断

<a id="task-86"></a>

### Task 86：恢复本轮扫描发现的验证基线（已完成）

- [x] 修正 `AutofillServiceStatusObservationTest` 5 个过强 identity 断言为异常类型和 message 断言；source 异常合同保持不变。
- [x] 重跑 App 与 Autofill 单测：App 991 项、Feature Autofill 135 项均通过。App/Autofill Lint 已在 Autofill entry 拆分后的工作区运行成功。

**2026-11-07 完成：** 旧 984 项/9 失败和 7 个 lint errors 是历史快照。诊断异常断言、Scanner 停止输入分支与相关 Lint 项已由 owner 修正。状态观察异常在 `callbackFlow` 的 coroutine stacktrace recovery 边界被复制，5 处 `assertSame` 失败实际/期望类型和 message 相同；收窄为稳定语义断言后，过滤用例 7/7 通过，随后 App 991/991、Feature Autofill 135/135 通过。Lint 使用此前同一 entry 拆分源码状态的成功结果，不把当前全量单测命令描述成运行过 Lint。


## P1：敏感数据、并发与用户流程缺口

<a id="task-26"></a>

### Task 26：明确 Runtime Session 生命周期转换的并发所有者

- [x] 定义 open/resume/soft-lock/seal/close 的统一生命周期 owner：RuntimeSessionManager 串行化转换；保留 VaultSessionController 会话级串行化并明确锁顺序。
- [x] 屏障测试覆盖两个首次 unlock 并发 open、open 与 seal、seal drain 中 unlock 不能提前 resume、soft-lock/resume；不得覆盖未关闭句柄，终态与生命周期 Mutex 顺序一致。
- [x] 定义 seal 等待 drain 时取消的清理 owner、结果与恢复：一旦发布 SEALED，Runtime 捕获调用方取消、请求 lease 取消并等待实际 drain/close，再传播取消，不留下无人回收的句柄。
- [x] 非合作 lease 超时关闭的详细清单由[安全 Task 2](2026-11-06-passly-security-scan-followups.md#task-2)持有；Task 2 已实现实际 drain 后关闭。
- [ ] 与 Task 37 完成 soft-lock、认证撤权和 Data Reset 的跨层终态验收：覆盖已有 lease 的读写/观察在排空与取消后的真实退出、迟到结果处理及 reset 与并发 lock 的交错。Data 命令 admission 已在 Task 37 统一；此项只保留仍缺的跨层终态测试。运行 Runtime、Security Session、Data Reset 定向测试。

**2026-11-07 Runtime lifecycle 串行化：** `RuntimeSessionManager.lifecycleMutex` 保护完整 `unlock`（close-failure 检查、resume、key copy、resource open）、`softLock` 与 `seal`；`VaultSessionController` 的会话互斥继续作为外层 owner，`SecureDatabaseResetCoordinator` 也使用该外层互斥。锁顺序为 Security/reset session mutex → Runtime lifecycle mutex → SessionLeaseGate 的 resource/lease mutex；请求 lease 与执行资源操作分阶段取 gate 锁，`resumeIfOpen` 唯一的嵌套顺序为 resource mutex → lease mutex。五项屏障用例先在旧实现失败，再在新 owner 下通过，覆盖两个首次 unlock、open/seal、seal drain/unlock、soft-lock/resume 和 seal 调用方取消。

**2026-11-07 soft-lock 排空批次：** Runtime 在 `softLock()` 发布 `SOFT_LOCKED` 后等待活动 lease；5 秒超时或调用方取消会请求登记 owner 取消，再在不可取消清理区等待实际退出，保留已打开的 handle。新增两项屏障回归，分别验证超时取消与调用方取消后仍等待、不关闭资源，且新 lease 被拒绝。Runtime 用例通过。Security 当前已先发布认证撤权、等待 Runtime soft-lock/seal 排空，再清敏感会话 key；Task 26 仅保留与 Task 37 共用的 Reset/跨层终态验收。

<a id="task-37"></a>

### Task 37：为认证撤权与数据库操作定义同一线性化边界

- [x] 明确锁定从请求发起到 Data 禁止新命令进入 DAO 的线性化点。`VaultSessionController.lock()` 先发布 `AuthenticationState.Locking`；`AppDatabaseSession` 直接查询 Security-owned `SecureSessionAccessState`，在 Runtime lease 内、DAO block 前重新核验授权。lock 请求前通过 admission 的操作持有 lease，由 Runtime drain 等待真实退出。
- [x] 审计并收口数据库命令入口。`DatabaseTransactionRunner`、Entry/Link/Sensitive/OTP Repository、Autofill Candidate/Credential Resolver、Backup Store、Activity Recorder 与附件 GC 均迁移到 Full Vault 或 Autofill 专用 session API；删除 Repository/runner 自己的授权检查与重复注入。GC 仍在文件副作用前核验 Full Vault。数据库清理使用单独命名的 maintenance API，仍受 Runtime lease/lock gate 保护。没有无授权的通用 `query/transaction` 重载。
- [x] 统一 `DatabaseTransactionRunner.read()`、`write()` 与 `writeForAutofill()` 的 lease 内授权复核；直接 Full Vault 与 Autofill 命令使用同一个 `AppDatabaseSession` admission 边界。新增边界回归分别让 read/write 的授权第一次为真、lease 内复核时为假，并断言 DAO block 不运行。
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [ ] 核对锁定、恢复模式切换、Autofill 会话结束、idle timeout 和 reset 是否共享该次序；更新定向测试并运行 Security、Runtime Session、Data 事务/会话、`verifyArchitecture` 与 `git diff --check`。本批次的 Android instrumentation 是否可运行待确认。

**审查证据：** 原调用闭包中 Runner 与多个 Repository 各自只在获取 lease 前检查授权。当前 `AppDatabaseSession` 通过 Hilt `Provider<SecureSessionAccessState>` 获取当前 Security owner，并按 Full Vault/Autofill 语义提供 protected query/transaction；旧的无授权通用 command API 与逐调用谓词均已删除。普通命令入口均已迁移；maintenance API 只给数据库清理与测试 fixture 使用，GC 不使用旁路。Room observations 也由 `AppDatabaseSession.observeFullVaultWhenReadable()` 内部读取授权 Flow，再组合 Runtime lock/generation 重新订阅，Repository 不再注入或传递授权状态。当前边界测试用可控 access-state 验证第二次授权拒绝时 read/write DAO block 不运行；它还未验证真实锁定与 Repository/Autofill 并发入口的提交/取消终态，因此 Task 37 仍未完成。

**2026-11-07 Security 锁定顺序批次：** `VaultSessionController.lock()` 现在在进入可挂起清理前发布 `AuthenticationState.Locking`，保留变更前状态以判断 RecoveryMode；随后撤销 permit、调用 Runtime soft-lock/seal 排空 lease，再清敏感会话 key，sealed/fatal 路径在 key 清理后锁 DEK。新增可控 soft-lock 与 key-clear 屏障，验证 Locking 已使 full access 失效，且 key clear 要等 Runtime soft-lock 返回；Security 150 项与 `verifyArchitecture` 通过。Task 37 仍未完成：AppDatabaseSession/Runner/直接 Repository 的 lease 后授权复核、Autofill 与内部维护入口分类、已进入事务的提交/取消结果，以及 reset/恢复模式的同序测试仍待单独批次。

**2026-11-07 Data admission 批次：** `AppDatabaseSession` 通过惰性 Security provider 直接读取认证权限，并在 lease 内再次核验；Repository 与 Runner 不再传递授权谓词或持有重复访问状态，Room observation 也由 session 集中读取认证 Flow。Full Vault、Autofill 与 maintenance 入口语义显式区分。新增 Room observation instrumentation 回归，分别验证 read/write 在第二次授权核验拒绝且 DAO block 不执行；`:app:kspDebugKotlin`、Data 主代码与 AndroidTest 源集编译通过。`:security:testDebugUnitTest` 与 `:runtime:session:test` 强制重跑通过；`adb devices` 无可用设备，instrumentation 未运行。同步修正数据库映射和认证文档。Task 37 仍待真实 Security/Data 并发终态、Autofill/Recovery/idle/reset 次序验收和定向验证。

**2026-11-07 生命周期顺序审查：** idle/background 与 Autofill 结果完成后的释放都调用 `VaultSessionController.lock()`；RecoveryMode 锁定升级为 SEALED。Autofill 最后一个 borrower 释放受结果完成屏障保护。Reset 则由 `ResetDatabaseUseCase` 通过 Full Vault 状态与 `AuthorizationGate` 后调用 Data `DatabaseSessionLifecycle.seal()/unlock()`，未经过同一 Security lock owner；它与并发 lock 的交错尚无终态测试，因此保留 Task 37 未完成。
<a id="task-43"></a>

### Task 43：保证条目与修订敏感揭示异常时擦除部分结果

- [ ] 为 `RevealEntryFieldsUseCase.reveal()` 建立局部敏感值所有权交接：`OwnedChars` 在返回前由 UseCase 持有；任何后续字段读取、授权、Repository reveal 或活动记录 suspend 调用异常/取消时都必须擦除已累计值与未交接的 reveal 结果，然后原样重抛取消/错误。成功返回时仅将明确纳入结果的值转交调用方，不能双重 wipe 或遗留未采纳的敏感结果。
- [ ] 同时闭合 Data producer 的部分返回清理：`RoomSensitiveFieldRepository.revealMany()` 与 `RoomEntryRevisionRepository.reveal()` 都在查询/解密迭代中逐个创建 `OwnedChars`；若后续字段 decrypt 或整体查询抛错，已创建容器不可从未返回的 List 交给上层清理。令 Repository 在构造列表期间拥有已揭示值，失败时 wipe 全部后重抛，再仅在完整结果返回时转移所有权。
- [ ] 增加可控失败与取消测试：Data 侧普通字段及 revision 第二个字段 decrypt/查询失败，UseCase 侧低敏字段读取后授权失败抛错、高敏 reveal 后 `ActivityRecorder.recordUsage` 抛错/取消、字段读取中途抛错；断言每个已创建 `OwnedChars` 已 wipe，取消仍传播且成功/拒绝路径行为不变。再核实 Detail 与 History ViewModel 对返回值的发布/离页清理合同，运行 Data Repository、Reveal UseCase/Detail/History 生命周期定向测试、`verifyArchitecture` 与 `git diff --check`。

**证据：** `RevealEntryFieldsUseCase.reveal()` 先将低敏感字段用 `OwnedChars.fromNullableString()` 放进局部 `values`，之后可能继续读取多个字段、调用 `AuthorizationGate.authorize()` 和 `SensitiveFieldRepository.revealMany()`，最后还可能挂起调用 `ActivityRecorder.recordUsage()`；整个过程没有 `try/finally`。上述任一步骤异常或取消都会让方法在返回 map 前丢弃局部引用且不 wipe 已建容器；尤其在高敏授权/揭示成功后 activity recording 抛错时，全部解密值都已进入 `values`。调用方 `DetailViewModel.revealFields()` 只有收到 map 后才把它交给 `DetailWorkSession.publishReveals()`，后者能清理迟到或发布失败的值，却无法接管 UseCase 异常退出时从未返回的 map。Data 侧 `RoomSensitiveFieldRepository.revealMany()` 在 `keys.mapNotNull`、`RoomEntryRevisionRepository.reveal()` 在 `keys.map` 中也逐项创建 `OwnedChars`；若第二个及后续 `codec.decrypt()` 或 DAO 查询抛错，前面生成的结果列表不会返回给上层，当前实现同样没有局部失败清理。现有 `RevealEntryFieldsUseCaseTest` 仅覆盖低敏/高敏成功与授权拒绝，并在成功后由测试手动 wipe；仓库精确搜索未发现 `RoomSensitiveFieldRepository`/`RoomEntryRevisionRepository` 的 reveal 异常清理行为测试，也没有 UseCase 读取/授权/activity recording 抛错、取消用例或断言生产边界清理责任。该问题是静态敏感资源所有权缺口，尚未由运行时测试复现。

<a id="task-59"></a>

### Task 59：闭合生物识别轮换的提交后恢复与启动对账

- [ ] 统一轮换 durable commit 后结果：binding 已切换后，候选清理状态读取或 availability refresh 普通失败不得显示未提交/允许盲重试；按持久事实保留 pending 派生恢复。
- [ ] 覆盖 withBiometricRotationCandidate 正常返回后 loadBiometricState 失败、provisioner 在 rotate Success 后 refreshAvailability 失败；验证提交前/后异常、取消、fatal、alias/journal、启动恢复、敏感缓冲、host lease。
- [ ] 保留 reconciler 可重试清理及 fatal 传播，以真实 rotate/provisioner/启动 owner 测试闭合并同步认证文档。

**2026-11-07：retained，提交后窄残余。** RotateBiometricKeyUseCase 已以 Success + RETRY_REQUIRED 表达清理未完，reconciler 已降级普通异常、deleteAlias 仅捕获 Exception，旧缺陷描述移除。BiometricRotationCandidate:40、49 正常返回后的持久状态重读、DefaultAuthenticationMethodProvisioner:238 成功后的 refresh 仍可抛错；SecuritySettingsViewModel:163–167 会显示操作失败。


<a id="task-38"></a>

### Task 38：让密码配置状态机在保存期间拒绝迟到输入

- [ ] 在 `UnlockViewModel` 与 `RecoveryModeViewModel` 的状态所有者/Reducer 闭合 `Saving` 状态：提交启动后到达的新密码、确认密码、重新打开或关闭对话框命令不能结束/替换当前请求，也不能重新开放第二次提交。不能只依靠 Compose `enabled = false`，因为 ViewModel 是状态和命令合同的最终所有者。
- [ ] 为每次密码配置操作绑定请求代际/owner，或以等价的单飞状态保证旧结果不能改写用户后来重新打开的密码对话框。分别覆盖 `Started → Changed/Reopened/Dismissed → Started` 与 Success/Failure/Cancelled/exception 次序；断言 provisioner 调用数量、当前对话框状态和结果反馈归属正确，所有替换/丢弃的 `OwnedChars` 与局部 `CharArray` 都被清零。
- [ ] 检查 App Password Settings 和恢复码设置中同类 reducer/handler 是否已有等价 guard；只归并确有相同状态漏洞的路径，不引入跨功能通用状态框架。运行 Unlock/Recovery ViewModel 与 reducer 定向测试、`verifyArchitecture` 和 `git diff --check`。

**证据：** `UnlockViewModel.updateNewAppPassword()`/`updateConfirmAppPassword()` 只要求 `passwordSetup is Editing`，未拒绝 `phase == Saving`；两者调用 `UnlockReducer` 后会把 phase 设置为 Idle。`SetPasswordClicked` 也无条件发送 `PasswordSetupOpened`，Reducer 会把现有 Saving 替换成新的空 `Editing`。恢复模式的 `RecoveryModeViewModel.updateNewPassword()`/`updateConfirmPassword()` 同样不检查 Saving，`RecoveryModeReducer` 将输入变化设回 Idle；`showSetPasswordDialog()` 也会无条件发送 `PasswordDialogOpened`，替换正在保存的 draft。两条 ViewModel 的 dismiss action 也没有自己的 Saving guard，虽然 UI 组件禁用了忙碌时的字段、确认和取消控件；这项约束未在状态所有者中执行。Provisioner 返回后 Unlock 无 request owner 检查，Recovery Mode 只检查页面 owner，不检查 dialog/request 代际，因此旧请求完成可能修改重开后的新状态。`AuthenticationReducerTest` 只覆盖静态 reducer 转换；未找到 UnlockViewModel 或 RecoveryModeViewModel 的操作并发/迟到输入行为测试。现有 `AppPasswordSettingsViewModel`、`RecoveryCodeSettingsViewModel` 需在实施时按实际状态合同一并复核，不能只靠输入控件禁用证明安全。

<a id="task-42"></a>

### Task 42：让共享条目编辑能力由条目编辑层持有

- [ ] 按实际调用闭包重新裁定条目编辑应用能力 owner：`EditDetailEntryUseCase` 被 Detail 与四类编辑 ViewModel 共同调用，当前编辑器依赖 `feature.vault.detail` 包下的 detail 命名实现；评估将通用 `EntryEdit` 写入能力放到真实共享 owner，并在迁移调用方后删除旧包名/过时测试引用。`LoadEditableEntryUseCase` 与 `EditableEntryLoadResult` 当前只由 `DetailViewModel.prepareEntryForEdit()` 消费，先核实它们是否仍属 Detail 专属读取/导航准备能力，不因四编辑器现已消费 seed 而机械搬入编辑层。
- [ ] 按新 owner 收敛包依赖与架构门禁：编辑能力只依赖 Domain/Core 端口与规则，禁止 `presentation/editor/*` 反向依赖 Detail presentation 或仅为搬包新增同签名转发层。迁移保留认证代际检查、敏感字段揭示与 wipe、编辑版本/冲突合同、统一日志及成功/失败行为；若发现该能力确实拥有 Detail 专属宿主状态，先拆清该状态 owner，不把页面导航/状态搬入 UseCase。
- [ ] 为 Detail 预加载补齐可恢复异常反馈与重试合同：`LoadEditableEntryUseCase.invoke(entryId)` 的 Repository、授权及 activity-recording suspend 调用若抛普通异常，会沿 `DetailViewModel.prepareEntryForEdit()` 经 `DetailRoute` 的 `editScope.launch` 逸出；当前没有 owner 可见失败 state/effect。明确由 Detail 页面 owner 映射普通 `Exception` 并提供重试/重新发起入口，同时继续传播 `CancellationException` 与 fatal `Error`；不得把这些错误伪装成 `NotFound`、`AccessDenied` 或无 seed 的静默结果。
- [ ] 评估完整表单 reveal 的 JVM 敏感值驻留：当前 `SensitiveValue.toTransientString()` 通过 `String(chars)` 创建不可变明文副本，再将其放进 `Entry`/表单和一次性 seed；`finally` 只覆盖临时 `CharArray`，关闭 seed/Form 只能释放可控引用，不能保证擦零 String 副本。评估是否能在不泄漏到导航/SavedState/日志且不复制一套表单模型的前提下缩短或改用可控缓冲生命周期；若 Kotlin `String` 仍是表单/领域必需载体，记录残留边界并避免声称可靠内存擦除。覆盖授权拒绝、异常、锁定、编辑取消和保存完成的引用释放路径。
- [ ] 分开定义 Detail 预加载失败与编辑器 seed 失败的重试路径：Detail 的读取异常应由当前详情页 owner 显示失败并允许重新加载；编辑器 `loadExistingEntry(result)` 只消费一次性 seed，不重新查询条目，Missing/无效 seed 的 `failEditLoad()` 反馈需引导回详情重新 stage 新 token。不要在编辑 ViewModel 增加对已消费 token 的虚假重试，覆盖条目/认证会话替换与迟到结果过滤。
- [ ] 闭合 seed 已暂存但导航抛错时的释放：编辑目的地注册后的 `DisposableEffect(seedToken, seedCoordinator)` 已在离开时按 token 调用 `discard()`，只覆盖目的地完成组合后的退出；`context.navigateToRoute(route)` 若在目的地组合前抛异常，stage+navigation 之间仍无补偿。`VaultNavigation.navigate(EditScannedWifi(payload))` 也先 `seedStager.stage(EntryEditorSeed.Wifi(payload))` 再调用 `navigateToSingleTopRoute(...)`，而 `EntryEditorSeedStager` 只有 `stage()`、无按 token discard 能力。定义两条导航边界上的失败和取消合同，确保补偿只清本次 token、不误删后续 owner seed，普通异常有可见失败反馈，取消与 fatal `Error` 清理后仍传播。关闭单槽只丢弃受控引用，不声称擦除 seed 中的不可变 String；保留已消费 seed、会话撤销和 graph 销毁时的现有语义。
- [ ] 闭合共享 `EditorWriteSession` 的 fatal `Error` 路径：当前保存协程对 `CancellationException` 和普通 `Exception` 有显式转换，但 fatal `Error` 会原样逃逸且不会调用 `onIdle()`/`onClear()`；若宿主在错误传播后仍存活，session 可能卡在 `Saving` 且敏感表单仍保留。定义窄清理路径擦除表单并释放忙碌状态，同时继续传播原始 `Error`，不能将未知提交结果映射为可重试 `Failed`，避免可能的重复写入。
- [ ] 按当前 owner 补齐行为测试：`LoadEditableEntryUseCase` 与 `DetailViewModel.prepareEntryForEdit()` 覆盖 Repository/授权/activity-recording 普通异常、取消、认证撤销及条目版本变化；Detail Route 覆盖失败可见性和重新发起。共享 `EditDetailEntryUseCase` 迁移时更新 Detail 与四类编辑 ViewModel 调用方测试，覆盖版本冲突、取消与迟到结果；seed 消费测试覆盖无效/已消费 token 后需返回 Detail 重新 stage。另为 `EditorWriteSession` 覆盖 fatal Error 清理并继续重抛。运行相关 UseCase/ViewModel/Route 测试、`verifyArchitecture` 与 `git diff --check`。

**当前调用闭包复核（2026-11-06）：** `rg` 生产引用显示 `LoadEditableEntryUseCase` / `EditableEntryLoadResult` 只由 `DetailViewModel` 使用：`prepareEntryForEdit()` 以当前 `uiState.entry.id` 调 `loadEditableEntry(entryId)`，收到 `Loaded(entry, authentication)` 后核对 ID、类型、认证对象身份、完整访问权和当前详情条目，再交给 `VaultGraphRegistration.onEditFullForm` stage。四个 Route 从 token 调 `seedCoordinator.consume(token)`，随后同步调用各自 `loadExistingEntry(result)`；这四个函数不依赖或调用读取用例，只校验 seed 访问并把 `seed.entry` 映射成表单。相反，`EditDetailEntryUseCase.edit(entryId, edit)` 由 DetailViewModel 和 Password/OTP/BankCard/Wi-Fi 四个 ViewModel 共用，维护互斥、读取最新条目、`EntryEdit.applyTo()`、预期版本更新及一次冲突重试；其 `feature.vault.detail` 包路径是已证实的共享能力命名/依赖泄漏候选。重归属范围应优先核对此保存能力，loader 是否保留 Detail owner 需单独裁定；没有证据表明当前更新行为已经失败。

**新增 Detail 预加载异常证据（复核并替换过期描述）：** `LoadEditableEntryUseCase.invoke(entryId)` 依序调用 `EntryQueryRepository.getById(entryId)`、`SensitiveFieldRepository.getPresence(entryId)` / `revealMany(entryId, requestedKeys, REVEAL, permit)`、`AuthorizationGate.authorize(scope) { permit -> ... }`、再次 `getById(entryId)` 与 `ActivityRecorder.recordUsage(entryId.value, VIEW)`；这些调用抛出的普通异常不在用例中映射。唯一生产调用是 `DetailViewModel.prepareEntryForEdit()`，`DetailRoute` 在 `editScope.launch` 中直接 await，并未捕获或发出加载失败 effect。故异常会从详情预加载协程逸出，而不是将四个编辑器留在 Loading；编辑器只处理 seed 的预期失败。需要在 Detail owner 明确失败/重试状态，取消与 fatal `Error` 继续传播。旧记录“四个编辑 ViewModel 调用 loader”的结论已经因预加载改为 Detail → seed → Editor 的链路而失效。

**新增暂存 seed 后导航失败证据（2026-11-06）：** 完整编辑的 `VaultGraphRegistration.onEditFullForm(editableEntry)` 先调用 `seedCoordinator.stage(editableEntry)`；只有 route 为 null 时调用 `discard(staged.token)`，成功构造 route 后的 `context.navigateToRoute(route)` 抛错没有局部 cleanup。`VaultNavigation.navigate(EditScannedWifi(payload))` 同样先执行 `seedStager.stage(EntryEditorSeed.Wifi(payload))`，再调用 `navigateToSingleTopRoute(AppRoute.AddWifiFromSeed.createRoute(staged.token.value))`；`EntryEditorSeedStager` 接口只有 `stage(seed)`，此 caller 无法 token-aware discard。两者共用图级单槽 `EntryEditorSeedStore`，seed 未消费时会保留 Entry/Wi-Fi payload 的可控引用，直到新 seed 替换、认证撤销或 coordinator ViewModel 清理；关闭槽位不能保证擦除 String。导航调用抛普通异常、取消或 fatal `Error` 后的显式 token 清理及 owner 可见失败合同仍待设计；不得为方便丢弃任意当前槽位，以免清错后续 owner 的 seed。

**新增内存所有权证据（2026-11-06）：** `LoadEditableEntryUseCase.invoke()` 在授权闭包内取得 `revealed` 后，对每个值调用私有扩展 `SensitiveValue.toTransientString()`；该函数先 `toCharArray()`，再执行 `String(chars)` 并在 `finally` 将原数组填零。返回的不可变 String 随后进入 `Entry.withRevealedField(field, value)`、`EditableEntryLoadResult.Loaded.entry`、`EntryEditorSeed.Editable`、图级 `EntryEditorSeedStore` 与类型表单 String。seed Store 消费/清空和 ViewModel `onClear()` 会失去持有引用，但不会原位覆盖 String。当前没有证据表明其越过内存/导航边界；记录为敏感明文对象驻留和擦除能力的设计评估，不据此宣称已有凭据泄漏，也不建议在没有全链路类型所有权方案前只替换局部表单字段类型。

**新增重试与 fatal 清理证据（2026-11-06）：** 四类 Route 在 seed 失败状态展示 Snackbar，没有当前页面内的重试 action；其 `loadExistingEntry(result)` 是同步 seed 校验/表单映射，不是异步 Repository load。一次性 seed 已消费后，重试完整编辑需返回 Detail 再 stage 新 token，不能重复消费原 token。`EditorWriteSession.save()` 将 `CancellationException` 恢复为编辑/清理状态、将普通 `Exception` 转成 `EditorSaveEffect.Failed`，然后通过认证/attempt 检查投递结果；若 writer 抛出 fatal `Error`，控制流绕过其后的状态检查和 `onClear()`，attempt 留在 `Phase.Saving`，受控表单不会被会话清理。现有 `EditorWriteSessionTest.unexpectedFailureUsesTheLocalizedFallbackWithoutExposingTheException` 使用 `error(...)`，实际抛出的是 `IllegalStateException`，只覆盖普通 Exception；没有验证 fatal Error 清理和重抛。未知提交结果不能直接映射成可重试保存失败，需定义窄清理后的终态并继续传播原 Error。以上为静态源码与现有测试覆盖审查，未运行测试。



**2026-11-07 目的地释放复核：** token discard 位于完整 Edit 目的地 VaultGraphRegistration:260–267；AddWifiFromSeed 注册（:157–204）无同等退出清理。正常 Route 初始化会消费 seed，不能据此声称正常路径必然遗留；需覆盖消费前离开、stage 后导航异常/取消，只补偿本次 token。四编辑器测试已适配三参构造与 editableSeed，当前编译不属于 API 失配。


<a id="task-40"></a>

### Task 40：让备份目录设置命令失败可见并保留授权状态

- [ ] 在 `DataManagementSettingsViewModel` 为目录选择与清除命令建立完整失败反馈：协程取消继续传播；DataStore 读取/写入和目录授权操作的普通异常转为页面 owner 所有的失败 Effect，不得从 `viewModelScope.launch` 无处理逸出，也不得把未完成操作呈现为成功。
- [ ] 收窄 `BackupDirectorySettingsInteractor` 的 `Throwable`/`runCatching` 补偿边界：`persistGrantedDirectory()` 在 `setBackupDirectoryUri()` 抛 fatal `Error` 后会读取持久 URI；若 URI 已提交则继续返回 Saved，若未提交则可能清理 grant 后返回 Unavailable。补偿读取与 NonCancellable grant release 的 `runCatching` 也会吞掉 fatal cleanup 错误。普通可恢复异常可按持久事实补偿/映射；完成必要 grant 清理后，fatal Error 必须传播且不能伪装成 Saved/Unavailable。
- [ ] 目录清除失败时保留持久 URI 与仍被配置引用的 Android grant，向用户呈现明确失败；目录选择失败时按 interactor 已有补偿合同释放新 grant，仅当设置没有提交引用该 URI 时释放。`BackupRoute` 现在在确认回调中启动异步清除后立即关闭确认对话框，需让成功/失败结果拥有清晰的 UI 反馈，不要求重新选择仍有效的目录。
- [ ] 增加 ViewModel/Route/Interactor 行为覆盖：验证 `clear()` 失败且设置仍指向旧 URI 时不释放 grant、页面报告清除失败；选择前读取失败或 grant/持久化异常时不崩溃、不误报 Saved；再分别注入普通 Exception、CancellationException 与 fatal Error 到写入、补偿读取和 grant release，断言持久事实、异常优先级与 grant 状态；页面退出后迟到结果不能投递给新 owner。保留“先持久化新 URI、再释放旧 URI”顺序；运行备份目录 interactor/VM/Route 定向测试、`verifyArchitecture` 和 `git diff --check`。

**证据：** `BackupDirectorySettingsInteractorTest.failed clear keeps grant when settings still reference it` 已证明 `clear()` 在持久化清除失败且 URI 仍被引用时抛出异常并保留 grant；相邻用例证明新目录持久化后才释放旧授权。生产 `DataManagementSettingsViewModel.onAction()` 在 `viewModelScope.launch` 内直接调用 `directorySettings.clear()`，无 try/catch；`handleSelection()` 也直接调用可抛异常的 `select()`，仅对正常枚举结果发 Effect。`BackupRoute` 的 `BackupDirectoryClearDialog.onConfirm` 先启动清除 action 再立即将 dialog state 设为 false，当前 Effect 枚举只有取消、拒绝、Unavailable，没有清除失败反馈。故储存层已有保留授权的正确补偿，但 Presentation 命令边界未传递失败结果；这会让清除失败对用户不可见，异常也可能由 ViewModel 协程传播至未处理异常处理器。尚未运行 ViewModel/Route 行为测试。

<a id="task-41"></a>

### Task 41：保证 Backup Gateway 异常能结束操作状态

- [ ] 让 `DefaultBackupOperationGateway` 的公开操作遵守统一终态合同：`AuthorizationGate`、备份目录设置读取、SAF storage port 或 archive service 抛出的非取消异常都必须成为 `BackupExecutionResult.Failure`，不能越过 Gateway 边界；`CancellationException` 继续传播，提交/清理与敏感密码清零顺序保持不变。不要在 Gateway 与 ViewModel 两层重复捕获和映射同一异常。
- [ ] 覆盖 `BackupViewModel.execute()` 的异常终态：操作开始后任何失败都应复位忙碌/Executing 状态、发布当前 page owner 的失败 effect 并保留可重试工作流；不得把原始异常直接抛进 `viewModelScope`，也不能把取消伪装成普通失败。Gateway 已返回 `Failure` 时继续沿现有 reducer/notice 映射处理。
- [ ] 扩展 `DefaultBackupOperationGatewayTest`、`BackupViewModelLifecycleTest` 与 `BackupViewModelAccessTest`，至少注入配置目录 `Flow.first()` 失败、AuthorizationGate 抛异常、storage/check 或 archive service 抛异常及协程取消；断言 Result、页面终态、失败反馈、密码擦除和可重试性。将“设置流异常”与[DataStore 长期订阅恢复合同](../../data/settings-storage.md)分开，将“页面退出/执行取消”与页面工作所有权及退出取消合同分开；运行 Backup 定向测试、`verifyArchitecture` 和 `git diff --check`。

**证据：** `BackupViewModel.execute()` 先同步调用 `BackupMutation.OperationStarted`，协程体只在 `try/finally` 中调用 Gateway、映射正常返回结果和擦除密码；没有 catch。Gateway 抛出异常时不会调用 `applyResult(Failure)`，Reducer 会留在执行阶段，异常交给 `viewModelScope` 的未处理异常路径。`DefaultBackupOperationGateway.authorizeOperation()` 未映射 `AuthorizationGate.authorize()` 的异常；`exportToConfiguredDirectory()` 的 `settingsSource.backupDirectoryUri.first()` 位于捕获范围外；`checkDirectoryWritable()` 只处理 storage port 返回的 `Result`。Export/Import 映射 archive service 的普通异常，但不覆盖其之前的授权、配置读取或 target 创建步骤。已有 `DefaultBackupOperationGatewayTest` 覆盖部分返回失败与取消路径，`BackupViewModelLifecycleTest` 覆盖返回 `Failure`、销毁取消和密码清理；尚未覆盖上述端口直接抛出异常后的页面终态。该异常状态卡住由当前控制流直接推导，尚未通过新行为测试复现。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖

- [ ] 先为当前 `VaultViewModel` 增加 ViewModel 层行为测试，覆盖页面 owner 前后提交 action、Settings Flow 的 sort 更新、`EntryListQueryRepository.availableCategories` 更新、搜索与筛选组合产生的 Paging query generation、删除单飞及迟到完成、字段/OTP copy effect、OTP 订阅与删除失效、离页后 effect 丢弃和 lock/access 撤销。纯 `VaultReducer`/`VaultQueryState` 单测不能代替这些由 ViewModel 负责的协程、依赖编排和 PageEffects 所有权合同。
- [ ] 基于上述依赖闭包裁定是否拆成“列表查询/筛选状态 owner”与“行操作/一次性 effect owner”：当前查询侧由 `LibraryViewSettingsRepository`、`EntryListQueryRepository`、`VaultEntryPageSource` 和 `buildVaultQueryStates()` 组成；行操作侧编排 trash、字段 copy、OTP copy、OTP runtime 与页面 effect。数据库 reset 后的 Paging 重订阅由 `AppDatabaseSession.observeFullVaultWhenReadable()` 管理；分析 ViewModel 所有权时不另造 reload 桥。只有拆后每个 owner 有明确状态、任务取消和 Route 生命周期，并使原 owner 实际减少职责与依赖时才迁移；若仍需同一 `VaultUiState`/page owner 协调，则保留一个 VM 并提取纯 query/state 规则，不添一一转发类。
- [ ] 让 Route 消费与各 owner 对齐：`VaultRoute` 负责 Compose/导航/system effect，Content 只接收 immutable state/actions；确保图级 `VaultViewModel` 在 Trash/Detail 转场或目的地保留期间不会重复创建 effect collector 或重复订阅 Paging/OTP。给选定的 owners 增加 Route/NavHost 行为回归后同步状态管理文档，运行 Vault Presentation 定向测试、`verifyArchitecture` 和 `git diff --check`。

**当前证据：** 当前 `VaultViewModel` 注入 `EntryListQueryRepository`、`VaultEntryPageSource`、`LibraryViewSettingsRepository`、`TrashCommandUseCase`、`CopyEntryFieldUseCase`、`CopyOtpCodeUseCase`、`SecureSessionAccessState` 与 `OtpCodeRuntimeFactory`。它启动 settings sort 与 available categories 两个常驻 collector，构造带 250ms 搜索 debounce 的 Paging query/generation 与 `cachedIn` Flow，另持有 OTP Runtime/可见行订阅，并编排 delete 单飞、trash 完成后的 OTP invalidation、字段/OTP copy 与 page-owned errors/effects。数据库 reset 后，Room/Paging 查询经 `AppDatabaseSession.observeFullVaultWhenReadable()` 随 session 可读状态变化重新订阅；`VaultRoute` 同时从该对象收集 UiState、Paging、OTP 和 effects，并连接 Scanner、导航、Toast 与目的地 lifecycle；其自身还组装 swipe command 映射和 display settings 投影。状态里 `pendingDelete` 与筛选/搜索共居于一个 `VaultUiState`，OTP 删除失效与 row command 有真实联系，因此不能简单按方法组拆成两个 ViewModel。全仓 `app/src/test`/`app/src/androidTest` 搜索没有 `VaultViewModel(...)` 构造或 VaultViewModel 行为测试；目前只有 `VaultReducerTest`、`VaultQueryStateTest`、`VaultOtpStateProviderTest` 等分离的纯规则/适配测试，不能证明 VM effect owner、Flow 编排或异步结果行为。`VaultGraphRegistration.vaultGraphViewModel()` 将 VM scope 设为 Vault graph owner，高于单个 destination；Route 生命周期需按实际 destination 与图级 ViewModel owner 核对。Vault list 与 Detail 等 destination 的转场、活跃行订阅和 page owner 生命周期仍需宿主级测试；此项以行为测试先固定剩余合同，再决定实际所有权迁移，避免按文件长度或依赖个数先拆后引入协调桥。

**2026-11-07 新增具体缺口：** VaultViewModel:278–282 直接 collect availableCategories，UiState 用初始空列表表示分类；只有 library settings 有 RecoverableRead/Retry。RoomEntryListQueryRepository:37–42 经 observeWhenReadable 提供 DAO categories Flow，普通异常不属于 session-lock 恢复分支。异常会结束分类子 collector，页面无分类失败态或重订阅入口，空/旧分类与失败无法区分；这是源码确认的 owner/状态缺口，尚未注入复现。

- [ ] 先为分类查询定义 Loading/Ready/Failed 与重订阅/重试 owner，覆盖首次/发值后普通异常、恢复、锁定取消、迟到结果和正常空列表；以 VaultViewModel 行为测试固定合同，再裁定职责拆分。当前 Paging 查询由 RecoverableRead + buildRecoverableVaultQueryStates 组合，不另造全局刷新桥。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补真实 manager/store/provisioner 集成覆盖：同一持久快照解析、并发刷新、提交后刷新、调用方取消、恢复事件；状态对象单测不代替 manager 合同。
- [ ] 定义 DefaultAuthenticationManager:69–70 首次后台 refreshAvailability 的普通失败 owner、可见状态与重试；不让子任务结束后永久保持未就绪/旧状态，与 Task 59 共用提交后派生恢复设计。

**2026-11-07：retained，收窄剩余合同。** resolver 已读统一 credentials snapshot，AuthenticationMethodsState 已有 generation、串行解析、recovery revision，刷新由 manager 长期 scope 托管；测试覆盖状态对象竞争/取消/失败 observer。移除旧“分别解析过时快照”的描述。

<a id="task-51"></a>

### Task 51：明确非 Scanner Route 一次性 effect 的前后台交付合同

- [ ] 逐项定义 Toast/Snackbar、导航、SAF picker、权限和平台交付在 STOPPED、恢复、离页、owner 替换后的有效期；短命反馈明确是否丢弃，待完成请求明确如何恢复。
- [ ] 补 PageEffects 及真实 Route/宿主测试，覆盖同页 STOPPED→STARTED、转场、请求代际、锁定/关闭及 callback 异常；不把不同 effect 塞入统一 handler 表。

**2026-11-07：retained，机制已接入，剩语义/宿主验收。** Detail、Vault、BackupOperation 等已用 collectPageEffects，PageEffects:61–69 已 repeatOnLifecycle(STARTED)。PageEffectLifecycleTest:21–59 明确同页恢复补发缓冲事件，剩余是区分 effect 有效期，不是笼统添加 wrapper；旧直接后台 collector 清单移除。


## P3：按 owner 分批整理

<a id="task-73"></a>

### Task 73：把官方 Android 检查纳入维护复核并处理已发现的质量项

- [x] Autofill entry API guard/context、Scanner camera opt-in 与 SecurityRoute 配置感知资源访问已按当前实现修正。
- [x] App 与 Autofill Lint 在 entry 拆分后的工作区复跑退出 0；历史 warning/error 计数不再作为当前清理目标。

- [ ] 按本轮 app/Autofill Lint 新报告裁定剩余项，不沿用旧数量；清理 EmptySuperCall、冗余 API、UseKtx、资源时保留清理/持久时序及中英文合同。
- [ ] 依赖升级独立验证：catalog 当前 palette 1.1.0、Material3 1.5.0-beta02、DataStore 1.3.0、SQLCipher 4.20.0 已更新；旧“14 条升级提示”不再整体列为未实施。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

**2026-11-07 更正：** DetailViewModelTest 旧 CREATE/Lifecycle 夹具已改 AUTOFILL，实际 retry 并验证订阅两次及 Ready，移出待修。ScannerResultCard 的 Modifier 位置已调整，Lint 任务已在 entry 拆分后成功退出；当前测试失败由 Task 86 收敛并已清零，不沿用 2026-11-02 统计。

<a id="task-83"></a>

### Task 83：复核 Repository 覆盖并把业务工作流移出大 ViewModel

- [ ] 以当前工作区版本审查 `DetailViewModel`（约 1208 行）中的独立工作流，按“状态 owner/任务组/依赖闭包是否能一起迁走并实际减轻原 VM”决定拆分，不能只把函数搬到新文件或新建一一转发 UseCase。已确认的业务编排候选是修订历史打开流程：`observeRevisionHistory()` 在 ViewModel 中直接调用 `EntryRevisionRepository.initializeHistory()`，处理初始化结果后再订阅 metadata；Data 实现的初始化会执行一次性历史重建、数据库事务和附件/图标清理。将“历史就绪后观察”的业务动作归入有明确 owner 的 UseCase/应用服务或启动维护流程；ViewModel 保留页面订阅、重试与 UI 失败投影。选定触发 owner 时保持现有懒触发语义或明确迁移依据，并覆盖初始化失败重试、取消、会话锁定、metadata Flow 后续失败/重订阅及初始化重复调用的幂等性。
- [ ] 评估 Detail favicon 编辑是否应从 `DetailViewModel` 提取为独立工作流 owner：当前 VM 同时发起 URI/URL staging、裁剪后图片处理、staged image promote、取消/过期结果回收及 `EntryEdit.SetIcon` 持久化；`DetailFaviconSession` 已拥有准备/保存 Job 与资源释放的一部分合同。按职责将图像处理/条目命令放在合适 UseCase，将裁剪输入、临时路径和页面生命周期放在 Feature 工作流 owner；不得把 Android/UI 会话状态塞进通用 UseCase，也不得拆出不能减少 ViewModel 依赖、分支或异步 owner 的薄包装。必须保持过期会话不回写、promote 后失败回收、取消/关闭时敏感与文件资源清理及提交结果一致性。
- [ ] 同一轮核对 Detail 的其余候选及最大 ViewModel：保留 `ActivityQueryRepository` 等简单只读 Flow 的页面订阅在 VM（包括异常态、重试 owner），已有 `EditDetailEntryUseCase`、revision compare/reveal/restore、copy、QR 等完整动作不要再包一层；对 Backup、Credential Response、Diagnostics、Unlock 等大文件检查是否有跨端口业务决策或事务流程仍留在 VM，再依据证据决定提取或保留。只将有业务意图/不变量、复用价值、跨端口编排或独立取消/重试合同的流程归入专属 UseCase/工作流；UI 草稿、屏幕投影、Action 分发和生命周期任务仍由对应 Presentation owner 管理，防止造出上帝 UseCase。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

**初始审计证据（2026-11-06）：** 静态盘点到 17 个 Repository 契约及对应 Hilt 绑定；暂未发现需要增补接口的证据。ViewModel 文件行数快照中 `DetailViewModel.kt` 为 1208 行，依赖同时包括两个 Repository、`EntryTagQuery`、`FaviconImageProcessor` 及多个既有 UseCase。已验证 `observeRevisionHistory()` 先直接调用 `entryRevisionRepository.initializeHistory()`，其 Data 实现会在事务中重建 revision history、安排 attachment GC，并进行 post-commit 图标/附件清理，属于应从大 ViewModel 归位的明确业务维护流程。Activity history 的 Repository Flow 收集是页面加载/错误投影，不据此要求抽 UseCase。Detail favicon 路径存在多个资源与生命周期阶段，已部分封装于 `DetailFaviconSession`，将其列为工作流边界审查而非预断必须全部迁入 UseCase。Backup（385 行）、CredentialResponse（364 行）、Diagnostics（350 行）、Unlock（344 行）只按快照列为扫描范围；尚未完成其业务职责判断。该段为 2026-11-06 静态审计历史；本轮构建/测试结果统一见扫描验证记录。

**修订历史调用闭包复核（2026-11-06）：** `openRevisionHistory()` 从 `_uiState.value.entry.id` 取 `entryId`，检查同条目历史已打开后取得 `workSession.current(entryId)`；随后取消旧的 `revisionObservationJob` / `revisionOperationJob`、清 reveal 并发布 `Revision.Opened(entryId)`，调用 `observeRevisionHistory(entryId, token)`。重试入口只接受当前 `DetailRevisionState.Open` 中 `page == List` 且 `revisions is Failed`，用同一 `state.entryId` 取当前 token、清 job、重发 Opened 后再次调用 `observeRevisionHistory(entryId, token)`。该函数增加 `revisionObservationGeneration`，在 `viewModelScope.launch` 中调用无参 `entryRevisionRepository.initializeHistory()`；`AppResult.Failure` 只有在 `workSession.isCurrent(token)` 且 generation 未变时才以 `mutateRevisionFailure(entryId, revisionId = null, Failed(error))` 投影，否则丢弃迟到结果；Success 才开始 `entryRevisionRepository.observeMetadata(entryId).collect`，每个列表仅在 token/generation 仍有效时转为 `Revision.MetadataChanged(entryId.value, revisions)`。取消重抛，`SessionLockedException` 由会话撤权路径清理，其他普通 `Exception` 在 owner 仍有效时映射为 `DetailRevisionFailure.UNEXPECTED`。Data `RoomEntryRevisionRepository.initializeHistory()` 无参数地委托 `historyInitializer.execute()`；该 initializer 先在 Mutex 内读取 `maintenanceStore.completedGeneration()`，若低于 `CURRENT_GENERATION = 1`，以 `databaseTransactions.write("entry_revision_history_initialize")` 执行 `rebuilder.rebuildInTransaction(db, clock.now())`；该重建器内部清空旧修订，再遍历未按 `deletedAt` 过滤的 `entryQueryDao().getAll()`（含回收站），并为每项复用同一个 `now` 调用 `EntryRevisionWriter.snapshotChanges(db, entryId, entryVersion, summary, secret, entryType, now)`；缺少 `STRUCT_BUNDLE` 时使用空 `EntrySecret``，对旧图标引用调用 `entryResourceCleaner.discardUnreferencedIcon()`，并 `attachmentGarbageCollector.scheduleInTransaction(db)`；transaction 成功后写 `maintenanceStore.markCompleted(1)`，再分别执行 `postCommitCleanup.run { attachmentGarbageCollector.drain() }` 与 `postCommitCleanup.run { entryResourceCleaner.drainPendingIcons() }`。因此跨端口的一次性初始化、数据库写事务和提交后资源维护不是简单读取，应评估移到具备启动/首次打开 owner 的应用流程；ViewModel 对 `entryId + token + generation` 的页面订阅、失败投影、重试和旧结果丢弃继续由页面 owner 持有。迁移需保留懒触发语义，并验证重建幂等、提交后 cleanup、锁定/取消及 metadata Flow 异常后的重订阅。

<a id="task-84"></a>

### Task 84：全量审计 ViewModel 与 Coordinator 的职责归属并整理分层规范

- [ ] 对当前 `:app`、`:security`、`:feature:autofill` 中 34 个生产 ViewModel 逐个完成调用闭包审计，输出保留/迁移/合并/继续调查及其证据，不按行数或类名自动判断。清点范围：Shell/宿主（`AppShellViewModel`、`AppThemeSettingsViewModel`、`AppNoticeHostViewModel` 属于 App，`AuthenticationHostOwnerViewModel` 属于 Security）；Vault（`VaultViewModel`、`VaultDisplayViewModel`、`TrashViewModel`、`DetailViewModel`、`ScannedOtpImportViewModel`、四种 `Add*ViewModel`）；Settings/Backup（`AutofillSettingsViewModel`、`SecuritySettingsViewModel`、`RecoveryCodeSettingsViewModel`、`PrivacySettingsViewModel`、`AppPasswordSettingsViewModel`、`InterfaceSettingsViewModel`、`AppearanceSettingsViewModel`、`PermissionManagementViewModel`、`InteractionSettingsViewModel`、`NotificationSettingsViewModel`、`DiagnosticsViewModel`、`DataManagementSettingsViewModel`、`StorageSettingsViewModel`、`BackupViewModel`）；Autofill 工作流（`AutofillFillViewModel`、`CredentialResponseViewModel`、`AccessibilityFillViewModel`，均属于 `:feature:autofill`）；Scanner/Unlock/Recovery/Database（`ScannerViewModel`、`UnlockViewModel`、`RecoveryModeViewModel`、`DatabaseResetViewModel`）。同时覆盖每页 Route/Activity/Content 的实际消费者、DI 绑定、相关测试及下面被调用的 UseCase/Interactor/Repository/Coordinator/Store，而非只看 ViewModel 文件。
- [ ] 将执行职责从页面状态协调中归位：已确认的 Detail 修订历史初始化候选引用 Task 83，避免重复建项；重点审查 `RecoveryCodeSettingsViewModel.confirmAndEnable()` 与 `reconcileInterruptedCommit()` 中的提交后事实核对、`NonCancellable` 恢复、取消与 fatal `Error` 分支。这些负责 durable commit 协调的流程应由 `RecoveryCodeDraftInteractor` 或 Security 应用操作 owner 统一定义结果；ViewModel 保留页面状态、披露草稿和 owner 有效性投影。任何迁移都须保持 commit 前失败、持久化后异常、取消、fatal `Error`、离页与敏感值擦除合同，并通过 ViewModel/Interactor 行为测试证明状态不会错误回滚或允许重复提交。
- [ ] 审查 `StorageSettingsViewModel` 同时直接协调 cache 清理/大小读取、诊断日志清理/大小读取和 log-recording 偏好写入的边界：分别核对 `AppCacheStore`、`DiagnosticsLogStore`、`DiagnosticsLogSettingsRepository` 的所属层和所有消费者，区分简单单仓库读写、跨能力业务动作、格式化展示和页面错误状态；只有行为契约/事务/复用确有 owner 时才提取 UseCase 或应用服务，并让刷新/清理任务的取消、锁定、失败反馈仍由一个清楚的页面或工作流 owner 管理。
- [ ] 审查 `AppShellViewModel` 对外观/界面/安全设置流、认证状态、session lock 及 database failure/retry 的组合是否仍是 Shell 页面所需的屏幕投影和生命周期 owner。沿 `DatabaseSessionRecovery`、`DatabaseSessionRetryCoordinator`、`RecoverableRead` 到具体执行端检查是否有重复 retry 状态机、重复事实副本或可独立取消的子 owner；保留合法的应用 Shell 聚合，不为了减少构造器参数把状态拆进一对一转发 Coordinator。
- [ ] 对其余大/多流程候选逐项判断：`VaultViewModel` 的 Paging 查询、设置读取、OTP runtime 和条目命令；Backup 文档选择与备份操作；Credential Manager 的系统请求/结果交付与业务解析；Legacy/Accessibility Autofill 的授权、续接、选择和平台结果；Diagnostics 导出、清理与权限；Unlock 的认证和 App Password provision；Scanner 的相机/图片输入与解码；Recovery Mode 的凭据设置；通知运行时权限的请求 owner。[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)、Task 80 的统一架构审计和 Task 83 的 Detail 项应引用并更新状态，不重复复制缺陷清单；分支映射仅在同一 Presentation owner 确有重复、纯显示元数据时考虑合并。
- [ ] 审查分散在层外/文件外的 Coordinator、Session、Store 和 helper：至少覆盖 `DatabaseSessionRetryCoordinator`、`SecureDatabaseResetCoordinator`、`EntryEditorSeedCoordinator`、`AppPasswordDialogCoordinator`、`PermissionRequestSession`、`AccessibilityFillSessionStore`、`DetailWorkSession`、`DetailQrSession`、`DetailFaviconSession`、`DetailRevealStore`、`DetailRevisionRevealStore`、`EditorWriteSession`、`ScannerCameraHost`、`ScannerResultSession`、`RecoverableRead` 与 `PageEffects`。为每项记录真实创建者、生命周期、共享者、状态真相源及所处 Gradle module；合并重复机制或移除纯桥接前，证明其职责未被配置重建、Route、系统 callback、认证/锁定 owner 或其他调用方需要。已确认四种编辑器共用 `EditorWriteSession`，七处 Settings `saveSetting` 共用 `writePreferenceWithFeedback`：审计为保留/扩展现有共享逻辑，不复制到新 Coordinator；`AppShell` 等用例须按调用图核实。
- [ ] 对每个 ViewModel 依赖分类并验证层边界：可观察的 Domain/Repository 数据、完整业务动作 UseCase/Interactor、窄应用端口、Presentation 状态 owner、平台/持久化实现。继续禁止 ViewModel 间依赖及直接 DAO/Room/Data Entity/具体文件实现；明确哪些 `feature.*.data` 或 `feature.*.internal` 类型实际是跨层实现泄漏，避免仅按 `internal` 包名搬移。核查 `verifyArchitecture` 当前 ViewModel 门禁与 `ViewModelDependencyVerifier` 的覆盖范围；若新增静态边界，只针对已证实错误依赖设计白名单/反例测试，不把合法 Repository Flow 或窄业务端口误禁。
- [ ] 将本轮的稳定规则同步到 `docs/architecture/viewmodel-ownership.md`、`docs/architecture/package-boundaries.md`、`docs/development/maintenance-boundaries.md` 及必要功能文档，并使本 TODO 保留未迁移清单与决策证据。迁移需要分批按 Feature 执行；每次必须减少原 ViewModel 的独立业务职责、状态、异步任务组或实现依赖，而不是只变短文件。覆盖取消/重试/离页迟到结果/配置重建/会话锁定/持久提交点/敏感清理；运行对应 ViewModel/Route/宿主测试、相关模块门禁、`verifyArchitecture` 和 `git diff --check`。

**全量清点证据（2026-11-06，工作区快照）：** 按生产 `src/main` 清点到 34 个 ViewModel；`buildSrc/ViewModelDependencyVerifier.kt` 是架构检查器，不计作生产 ViewModel。文件长度范围为 8–1208 行；仅行数用于筛查，不作为拆分依据。代码摘要覆盖了每个 ViewModel 的构造依赖、公开/私有方法、`launch`/`collect` 与分支密度；并对 Shell、Storage Settings、Recovery Code、Unlock、Detail 及共享 Editor/Settings helper 做了方法级复核。初步确认 Recovery Code ViewModel 仍直接执行 durable commit 异常/取消/fatal 恢复协调；Storage Settings 同时操作 cache、diagnostics log 与记录模式；Shell 聚合设置、session 和 database retry 流；这些是优先复核项，是否迁层按实际 owner 合同完成闭包审计后决定。多个简单 Settings ViewModel、只投影配置的 Theme/Vault Display ViewModel、扫描导入 ViewModel 使用窄数据源/UseCase；四类编辑器已共享 `EditorWriteSession`，七个设置保存入口共用 `writePreferenceWithFeedback`，这些共享职责暂列保留核实，不提出机械新增层。当前 `viewmodel-ownership.md` 规定不得 ViewModel 相互依赖及不得直接依赖 DAO/Room/文件实现，`verifyArchitecture` 的 ViewModel 检查定位于跨 ViewModel 依赖；层间实现依赖和具体工作流归属仍需本任务逐页核对。此为全量生产文件清点与候选筛查，尚未逐项完成所有 Route/DI/测试的调用闭包验证，也未运行构建或测试。

<a id="task-80"></a>

### Task 80：统一 Presentation 的 MVVM 与单向数据流契约

- [ ] 全量盘点 `:app` 的 Route、ViewModel、UI state/action/effect、Reducer、Repository Flow 与 Compose state holder，按真实状态 owner 和任务生命周期分类；至少覆盖设置、列表/分页、详情、编辑器、备份、扫描、认证/恢复、权限与平台宿主。记录每页当前模式、依赖闭包、异步任务组、取消/重试与 effect 交付合同，标明保留、迁移或需进一步调查及证据；不要按文件名、分支数或是否存在 `Reducer` 推断架构。
- [x] 正式规范已统一为一套架构契约：分层 MVVM + 单向数据流（UDF）。ViewModel 是屏幕业务状态 owner，暴露不可变、可观察的屏幕 `UiState` 并接收 UI 操作；Repository Flow、异步操作结果和离散用户输入都是状态生产过程的输入，不是两种不同架构。语义方法或类型化 `UiAction` 按表达清晰度选择；Reducer 仅是复杂转换时可选的纯实现，Effect 也只是受生命周期/owner 约束的宿主交互出口。静态 Content 与短暂视觉状态无需 ViewModel，不构成另一套架构。
- [ ] 明确单一数据真相源合同并审查潜在双份状态：持久业务数据以 Repository/Domain 为真相源，ViewModel 的屏幕状态是其可丢弃投影；用户编辑草稿、筛选输入、展开状态等可以是独立的屏幕/交互状态，但须有明确 owner、生命周期和提交/丢弃语义。排查从 Repository 读取后另存一份可独立修改的长期副本、Flow 与手动赋值竞争同一字段、多个 ViewModel/Compose state 同时声称拥有同一状态等情形；区分有意未提交草稿与意外镜像，禁止把 Action 另做一条并行业务状态真相。保留 Paging 等确有独立语义的输出，不为形式统一强塞到一个 State，也不引入全局 EventBus。
- [ ] 按统一契约审查所有页面并制定分批迁移清单：合并重复的 Action/状态/事件转发与纯桥接；移除没有带来明确状态转换保证或可测试性的 MVI ceremony；让真实多输入/异步工作流仍按 UDF 汇入屏幕状态，必要时用纯 Reducer。只在能明确减少职责、重复或依赖时改代码；不得以统一为由制造上帝 ViewModel、万能 Reducer/Effect handler、全局状态容器或新的转发层。复杂状态机、认证提交顺序、协议分支、敏感数据清理与平台生命周期合同不得表驱动化抹平。
- [ ] 对所有 ViewModel 做职责密度与依赖闭包审计，标出同时拥有多个独立业务流程/状态机、宽依赖集合、重复决策映射或不同取消/重试任务组的候选；按[ViewModel owner 边界](../../architecture/viewmodel-ownership.md)和[维护边界](../../development/maintenance-boundaries.md)，结合调用方、状态 owner、事务/安全边界决定保留或拆分，不能仅按行数拆。
- [ ] 为每个拆分候选确定逻辑 owner：纯且可复用的业务判断归 Domain Policy/规则；完整业务动作及验证、不变量、可复用流程或事务/跨端口编排归职责匹配的 UseCase/应用服务；简单单一 Repository CRUD 保持直接调用，不造一对一转发 UseCase；平台生命周期和系统 API 留在 Route/Activity。UseCase 也须单一职责，提取后验证原 ViewModel 的依赖、分支或异步职责实际减少。
- [ ] 对跨层流程追踪 `Presentation → Application/UseCase → Domain Port/Policy → Repository → Data` 的真实依赖方向，检查错误/取消/重试、durable commit、认证、敏感数据清理和调用方闭包；不要把规则搬进 ViewModel，也不要把大 ViewModel 原样搬进 God UseCase。拆分异步 owner 时增加 VM/Route/宿主行为覆盖，不以 Reducer 或纯 UseCase 单测代替。
- [ ] 逐页迁移确认实际合同变化后，按需更新正式文档代码示例与例外；统一术语和基础规则已完成，不再重复立项。
- [ ] 按 feature/owner 分阶段实施迁移；异步编排或状态 owner 变化须有 ViewModel/Route/宿主行为覆盖，验证 Flow 失败与重订阅、CancellationException/fatal Error、离页迟到结果、配置重建、Effect 前后台交付、Repository 提交后状态与敏感值擦除。保留无需迁移且符合模板的页面，并记录理由；每阶段运行受影响模块测试、`verifyArchitecture`、相关 lint/编译门禁与 `git diff --check`。当前 Detail/完整表单编辑器存在未提交 owner 重构，先以稳定后的代码为基线，保留并适配工作区改动。

**2026-11-07：retained，仅剩逐页审计/迁移。** architecture-and-state.md 已明确分层 MVVM + UDF、Repository 真相源、可选 Reducer 和 UseCase owner；viewmodel-ownership.md 已有对应边界。移除“正式规范仍分四套架构”的过期事实。Task 84 持有 ViewModel/Coordinator 调用闭包清单，Task 83 持有具体 Detail 候选，本 Task 只管状态生产、真相源及 action/effect 模式，不复制 owner 清单。

<a id="task-39"></a>

### Task 39：拆开诊断运行时生命周期与设置日志读取适配

- [ ] 收窄 `ObservabilityRuntimeController` 的所有权：保留进程级 crash handler 安装/恢复、日志策略监听、上一进程退出记录、生命周期 flush 与 shutdown；让独立的 app 层适配器直接实现 `DiagnosticsLogStore`，负责 `LogRecordStore` 的行读取/编码、清理和存储大小查询。适配器应直接持有必要端口，不要再经控制器加一层纯转发。
- [ ] 让 `DiagnosticsLogStore` 的 feature-facing `DiagnosticsLogLine` 转换留在该端口的适配边界，不再让进程运行时控制器同时成为 Settings 功能存储实现。DI 继续共享同一个受策略包装的 `LogRecordStore`；验证功能依赖不会绕过记录策略或获得未经脱敏的原始记录。
- [ ] 新增只读 `DiagnosticsLogSettingsSource`，由 `DiagnosticsLogSettingsRepository` 扩展，并在同一 `ProtoAppPreferencesStore` 上绑定；将 runtime controller 改为依赖只读 source，保留 `StorageSettingsViewModel` 通过 repository 写模式。当前它是设置 Source/Repository 分离族中唯一缺少 read-only port 的能力，运行时不应获得未使用的设置写权限。
- [ ] 将覆盖分成真实职责：运行时控制器测试继续验证 crash handler、启动监听、flush/shutdown 次序；新增 store adapter 测试验证限量分页、TSV 编码字段、清理结果和 storageBytes 直传；保留 Storage/Diagnostics ViewModel 对 `DiagnosticsLogStore` 的替身测试，并运行 `verifyArchitecture`、相关定向测试与 `git diff --check`。若拆分后新增的对象只是把方法逐个委托给控制器、未减少双方依赖或职责，则不采用该拆分。

**证据：** `ObservabilityRuntimeController` 同时实现 `DiagnosticsLogStore`，并由 `PasslyApplication` 调用 `start()`、由 `AppLifecycleObserver` 调用 `flush()`/`shutdown()`；同一实例因此跨进程生命周期与 Settings 读取/清理/容量查询两类调用方。它唯一读取 `DiagnosticsLogSettingsRepository.diagnosticsLogRecordingMode`，不调用 setter；`StorageSettingsViewModel` 则同时读写该 Repository。与 Interaction、Message、Security、Library View、Backup Directory 已有 `*Source` + Repository 的端口分离对照，Diagnostics 缺少只读 source 使 runtime 注入了未用写能力。该控制器还把 `SanitizedLogRecord` 转换成 `feature.settings.diagnostics.DiagnosticsLogLine`，承担 feature 合同的展示 DTO 编码，同时持有 `Logger`、crash capture、previous-exit source、crash handler registry、process terminator、event aggregator、设置 repository 和 recording policy。Hilt 将其直接绑定为 `DiagnosticsLogStore`。现有 `ObservabilityRuntimeControllerTest` 主要覆盖 handler 安装、上一进程记录、crash、flush 与 shutdown；Settings ViewModel 测试使用独立 store 替身，没有独立验证生产 adapter 的分页/编码/清理实现。该耦合没有证据表明造成运行时错误；本项是按实际端口调用闭包收窄职责并固定 adapter 行为，实施时必须保持同一个策略包装 store 和 crash capture 顺序，不按类长度机械拆分。

<a id="task-35"></a>

### Task 35：按真实消费收窄 EntryLinkRepository 与详情关系查询

- [ ] 将 `EntryLinkRepository` 按生产调用闭包收窄：目前唯一业务 consumer 是 `DetailSessionQuery` 的 `getAll()`；核实并删除未消费的 `observeAll()`、`observeLinks()`、`getLinks()`、`upsert()` 与 `delete()` 接口/实现及只服务于它们的查询 DAO 方法。若将来没有正式关系编辑功能合同，不保留完整 CRUD 作为预留 API。
- [ ] `RoomEntryLinkRepository.upsert/delete` 内含 endpoint 授权/类型校验、修订快照、事务和附件 GC；删除前核对提交后图标资源回收路径，只从该待办移除确实随死 API 删除的调用路径。`EntryLinkCommandDao` 仍被 `EntryRevisionRestoreTransaction.restore(...)` 与 Data `RoomVaultBackupStore.restore(snapshot, mode)` 用于历史/备份恢复，必须保留这些 Data 命令和 Room 存储格式。2026-11-07 复核：旧 App `RoomBackupSnapshotRestorer` 已由 Data port 迁移替代；DAO 的备份消费者现在位于 Data `RoomVaultBackupStore`。
- [ ] 评估 `DetailSessionQuery.relatedEntries()` 对 `getAll()` 的全表读取：基于 `EntryAccountGraph.accountFor()/membersOf()` 所需的 MEMBER_OF_ACCOUNT、OTP_FOR、RECOVERY_FOR 关系闭包设计范围查询，并证明 account、login、OTP、recovery 详情结果等价；不得只按 entryId 查询导致遗漏同账户其他成员或 OTP。若没有可证明更窄且合理的方案，可保留 `getAll()` 作为唯一读取 API，并在文档说明其数据量与生命周期边界。
- [ ] 收敛关联条目加载的 N+1 查询：`DetailSessionQuery.relatedEntries()` 在关系图得到 N 个关联 ID 后逐一调用 `EntryQueryRepository.getById()`，每项各自获取一次数据库会话。为 Domain query port/Room adapter 提供批量读取并复用 `EntryQueryDao.getByIds()`，在一次可读 session/query 内装配条目与 secret bundle；恢复原关系 ID 顺序、跳过并发删除后不存在的条目，保持当前详情展示集合语义。验证空集合不执行数据库查询，并测定批量路径调用次数与单项路径等价。
- [ ] 清理对应 Hilt bind、Room query 方法与测试替身，保留 Domain link model/policy、历史快照、Backup 稳定关系键和 `EntryLinkCommandDao` 的真实读写消费者；Room schema/internal format 维持版本 1。运行 EntryLink/revision/backup/Detail 定向测试、`verifyArchitecture` 与 `git diff --check`。

**证据：** 全仓生产调用中 `EntryLinkRepository` 只有 `DetailSessionQuery` 调用 `getAll()`；`observeAll()`、`observeLinks()`、`getLinks()`、`upsert()`、`delete()` 没有业务调用者，也没有 `RoomEntryLinkRepository` 专用行为测试。`DetailViewModelTest`/`DetailPresentationLoaderTest` 的 fake 使用 `observeAll()` 仅为满足接口，没有调用行为。`RoomObservationSessionTest` 直接用 `EntryLinkQueryDao.observeAll()` 作为通用 session gate 测试入口，是唯一测试 consumer，删除 DAO 方法前要改为仍活跃的 query 并保留 gate 覆盖。当前 `getAll()` 将全库 links 装入 `EntryAccountGraph` 以计算 ACCOUNT 分组内关联条目；局部闭包不宜简单替换为单条 `getByEntryId()`，因为 OTP_FOR 需要经过 login 到 account，并需列出 account 下其他 login/recovery/OTP 成员。图构建后，`DetailSessionQuery.relatedEntries()` 对 `relatedIds.forEach` 逐条调用 `EntryQueryRepository.getById()`；这会把 `getByIds()` DAO 能力闲置在更低层，并为 N 个关联分别进入 `AppDatabaseSession.query`、重复获取 lease 且允许读取快照跨多个数据库时点。批量端口应重用现有 DAO query，避免再复制 query 或把 DAO 暴露至 App。死 CRUD 方法构造了 Repository 对 `DatabaseTransactionRunner`、`EntryRevisionWriter`、`SecretFieldStore`、`DatabaseClock`、`AttachmentResourceGarbageCollector` 的依赖；而 DAO 命令仍被修订恢复及 Backup 恢复直接使用，所以只能裁掉 Repository 的无消费者表面，不能删除持久化写能力或关系数据合同。

<a id="task-34"></a>

### Task 34：收窄未消费的 Activity 查询流

- [ ] 从 `ActivityQueryRepository` 移除没有生产消费者的 `observeAll()` 与 `observeByType(activityType)`。对 `EntryActivityQueryDao` 逐方法收敛：目前仅 `observeByEntryId()` 有生产消费者；`observeAll()` 只被 Room session 集成测试使用；`getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()` 由 revision restore 集成测试验证；其余 `observeUsage*`/`observeMutation*`、所有 `paging*`、`getById()`、`exists()`、`count()` 与 usage/mutation counts 在仓库范围内仅有声明。移除闲置方法时保留/改写锁切换、revision restore 的行为测试，不删掉这些真实验证。
- [ ] 核对并移除没有任何调用者的 `EntryActivityAnalyticsDao`、`AppDatabase.entryActivityAnalyticsDao()` accessor 及局部 DTO：当前列表 usage stats 由 `EntryPagingQueryFactory` 的实际 Paging 查询计算。`RoomObservationSessionTest` 直接调用 Activity/Link DAO 的 `observeAll()` 以测试通用 gate；迁移为按条目观察测试或证明此专用查询仍有不可替代验证价值，不能为了 test-only API 永久保留生产 DAO 方法。
- [ ] 清理测试替身与文档；先确认无反射、Hilt 多绑定或构建生成消费者。保留/改写针对 Detail 按条目读取、数据库锁切换取消/重建的行为覆盖，不为删掉未消费方法制造只断言符号不存在的测试。运行 Activity/Detail 定向测试、`verifyArchitecture` 与 `git diff --check`。

**证据：** `ActivityQueryRepository.observeByEntryId()` 唯一生产调用位于 `DetailViewModel`，支持详情活动记录；`observeAll()` 与 `observeByType()` 没有生产 consumer。全仓搜索 `EntryActivityQueryDao` 的方法后，确认 usage/mutation 四类观察、四个 paging、`getById()`、`exists()`、`count()` 和两个 usage/mutation counts 只在 DAO 声明。`observeAll()` 由 `RoomObservationSessionTest` 用作 session gate 集成测试入口；`getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()` 在 `EntryRevisionRestoreTransactionTest` 中验证恢复行为，需要保留覆盖。`EntryActivityAnalyticsDao` 的四个聚合查询只在 DAO 声明及 `AppDatabase` accessor 出现，没有生产调用者；列表 usageCount/lastUsedAt 实际由 `EntryPagingQueryFactory` 内联 SQL 生成。现有 `DetailViewModelTest` fake 实现全量/类型方法仅为满足接口，没有行为断言；未发现正式文档声明这些额外 API 合同。

<a id="task-36"></a>

### Task 36：按生产消费闭包收敛修订 Room DAO

- [ ] 从 `EntryRevisionQueryDao` 核实并移除从未调用的 `getByVersion(entryId, version)`；`getByEntryId()` 当前只供仪器测试读取完整历史，应改由可观察的生产路径或保留为明确测试查询合同，不能让测试便利 API 无说明地扩张生产 DAO。`getLatest()` 当前只有 `EntryRevisionRestoreTransactionTest` 两处调用；改写/保留其真实恢复断言时覆盖“创建恢复版本后最新快照正确”，不可因测试专用就删掉行为验证。
- [ ] 从 `EntryRevisionCommandDao` 核实并收敛未接入生产流程的 `upsertForImport()`、`upsertAllForImport()` 与 `deleteBefore(timestamp)`。当前备份合同明确排除修订历史；须确认不存在导入旧版、迁移/维护或其他间接入口，再删除这些生产 DAO 表面。`insertAllStrict()` 目前只在仪器测试使用，评估改由逐条严格插入或实际生产批量路径覆盖后再裁剪。
- [ ] 保留有实际事务消费者的方法：普通修订写入、恢复、删除、按条目/全局裁剪、清空回收站修订和旧图标引用回收；保留 `countByIconCustomReference()` 等图标引用保护查询。变更只收窄 DAO API，不改 Room Schema/internal format 版本 1，也不把修订持久化职责搬到 App/Presentation。
- [ ] 迁移或保留 `RoomObservationSessionTest` 中 `observeByEntryId()` 的 session gate 覆盖，以及 EntryRevision restore/history rebuilder、历史裁剪和图标引用回收的数据库行为断言；更新受影响测试与正式修订历史文档（若行为合同有变化），运行 Data 仪器定向测试、`verifyArchitecture` 与 `git diff --check`。

**证据：** 精确搜索 `EntryRevisionQueryDao` 方法后，`getByVersion()` 只在 DAO 声明；`getByEntryId()` 目前由 HOTP 消费事务、修订恢复/历史重建和生命周期仪器测试读取；`getLatest()` 仅由 `EntryRevisionRestoreTransactionTest` 两处断言调用；`countAll()` 为历史重建和生命周期测试提供存量断言。生产 Repository 通过 `observeByEntryId()` 发布修订元数据，并通过 `getById()` 加载、揭示和恢复目标修订；`EntryRevisionWriter`、永久删除、清空回收站、图标回收路径消费其余裁剪与图标引用查询。Command DAO 的 `insertStrict()`、删除/裁剪方法有生产事务消费者；`upsertForImport()` 与 `upsertAllForImport()` 只有声明，且 [`docs/features/backup/backup-workflows.md`](../../features/backup/backup-workflows.md) 明确完整备份不含修订历史；`deleteBefore()` 也只有 DAO 声明。`insertAllStrict()` 当前仅由修订历史仪器测试直接调用，恢复生产路径没有批量插入 EntryRevision 的消费者。该收敛不得误删针对历史上限、恢复后新版本、清空回收站及附件/图标引用保护的行为验证。

<a id="task-78"></a>

### Task 78：让完整表单可编辑能力查询不伪造 seed token 参数

- [ ] 将 `DetailUiMapper.detailHeaderUiModel()` 对可编辑类型的探测从 `editRouteFor(entry.type, entry.id.value) != null` 改为只接受 `EntryType` 的能力查询，或采用同样不会要求虚构路由参数的类型注册表接口。确认详情展示与实际导航仍由同一组支持类型决定，且不生成/消费 seed、不改变路由字符串。
- [ ] 核对 `VaultEditorDestinationRegistry` 是否还存在其他只为探测 capability 而传入占位 `seedToken` 的调用；保留真正创建路由时的 `editRouteFor(type, token)`。增加针对四种受支持类型和所有不支持类型的映射覆盖后运行相关 Detail UI Mapper 测试、`verifyArchitecture` 与 `git diff --check`。

**证据（2026-11-06，当前工作区源码复核）：** `toDetailPresentationModel(state, otp, revisionReveals)` 调用 `detailHeaderUiModel(entry, state)`；后者只需要 Boolean `canEditFullForm`，却以 `VaultEditorDestinationRegistry.editRouteFor(entry.type, entry.id.value) != null` 探测能力并丢弃返回的 route。注册表签名 `editRouteFor(type: EntryType, seedToken: String): String?` 对 LOGIN/OTP/BANK_CARD/WIFI 分别调用 `AppRoute.EditPassword/EditOtp/EditBankCard/EditWifi.createRoute(seedToken)`，其余 EntryType 返回 `null`，因此 `entry.id.value` 被放在 route 的 seedToken 槽位；当前调用不导航、不创建或消费 seed，能力结果实际只由 `type` 的穷尽分支决定。唯一另一处生产消费者在 `VaultGraphRegistration` 的真实导航闭包：先以 `seedCoordinator.stage(editableEntry)` 获取 `staged.token.value`，再调用 `editRouteFor(editableEntry.entry.type, staged.token.value)`，route 为 null 时执行 `seedCoordinator.discard(staged.token)`，否则 `navigateToRoute(route)`。全仓检索未发现其它把 `entry.id.value` 或固定占位值传入注册表的能力探测。现有 `VaultEditorDestinationRegistryTest.editRoutesExistOnlyForTheFourSupportedCreationTypes()` 以字符串 `"id"` 断言四种 createRoute 并检查所有其他类型为 null；`DetailUiMapperTest` 的 `headerMapperKeepsOnlyHeaderState()` 使用 NOTE，只断言标题/favorite/edit state，没有断言可编辑类型投影，因此目前没有覆盖详情能力与真实导航映射一致性的测试。此处属于路由构造的多余依赖与参数语义误用，不是已证实的用户可见故障。后续应让详情能力查询只接受 `EntryType`，同时确保能力判定与实际 route 支持类型来自同一 owner，并为 4 种可编辑类型和全部不支持类型加投影覆盖；保留真实导航的 `editRouteFor(type, token)`，不得复制第二份类型列表。

<a id="task-79"></a>

### Task 79：移除二维码会话仅供旧调用签名使用的授权回调桥

- [x] 移除仅由测试调用的次级构造器 `(Entry) -> EntryQrExportResult`；生产 `DetailViewModel` 原本已使用带 `onAuthorized` 的主构造器，没有需要兼容的生产 owner。
- [x] 将 `DetailQrSessionTest` 全部迁到主构造器并显式控制授权回调，覆盖授权前 Authorizing、授权后 Loading、拒绝/取消隐藏、generation 过期 payload 擦除及锁定/关闭清理；不更改 `ExportEntryQrUseCase.invoke(entry, onAuthorized)` 的授权边界。

**2026-11-07 完成：** 生产调用唯一一处 `DetailViewModel` 已使用显式授权回调；全仓生产引用没有单参数 consumer。新增状态回归用例在旧构造器下先实际失败（授权尚未回调就进入 Loading），再将 10 个 session 用例统一迁到双参数主构造器并删除次级构造器。定向 `DetailQrSessionTest` 10 项通过；完整 App 测试显式按 `com.aozijx.passly.*` 执行，XML 汇总 992 项、0 失败、0 error。第一次 full-task 汇总曾被并行定向测试的 11 项报告覆盖，因此不作为全套证据；最终验证使用显式包过滤并在同一命令结束时汇总 XML。


<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配

- [ ] 拆分 `buildSrc/src/test/kotlin/passly/SourceBoundaryPolicyTest.kt`（1,705 行、66 个 `@Test`）：当前一个测试类覆盖 package/layer matrix、feature ownership、presentation API、settings/command bus、runtime/data/security/nav host 等不同规则组。按 policy family 分成多个窄测试类，继续直接验证 `SourceBoundaryPolicy`/`SourceBoundaryVerifier` 的真实边界；保留有区分度的负例、FQN/模板/注释 decoy 与诊断证据，不把多个条件压成一个不可定位的参数化断言。
- [ ] 重组 `security/src/test/java/com/aozijx/passly/security/authentication/VaultSessionControllerLifecycleTest.kt`（1,187 行、23 个 `@Test`）：已按实际生产 owner 将旧的 IdleTimeout 文件重命名；主体仍混合 lock/reset、取消和 fatal 清理、retry/reopen、soft-lock、后台超时与 Autofill Inbox key provisioning。按会话锁/重置合同、后台超时合同、Inbox provisioning 真实 owner 拆分；23 组 dispatcher/Main 成对装配已合并到文件内 `runMainTest` helper，并保持 `StandardTestDispatcher(testScheduler)` 的 scheduler 绑定和 `finally` 复位；后续拆类需保留共享调度语义，不必再抽 JUnit rule。不要为一个模块的重复 setup 新建跨模块 test-fixtures 模块。
- [ ] 拆分 `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt`（815 行、21 个 `@Test`）：同一文件混合 ZIP 输入边界、AES-GCM/Argon2 容器与头部认证、加密 archive、`BackupFormatRegistry`/Bitwarden 检测、Passly JSON/wire format 与 bundle/resource 完整性。按实际 codec/adapter/validator/registry owner 分组；先与既有 `BackupArchiveKeyRegistryTest`、`BackupBundleValidatorKeyedTest`、`BitwardenCardImportTest`、`BitwardenOtpImportTest` 核对覆盖；稳定 wire keys 断言已合并到 `BackupCodecTest.fieldArchiveV1_wireFieldNamesAreStableAndIndependentFromDatabasePayloads`。保留 keyed golden、密文篡改、KDF 前长度检查及资源校验边界。
- [ ] 在 Detail 生产 owner 重构完成后重新评估 `DetailViewModelTest.kt`（当前工作区 1,413 行、24 个 `@Test`）：其用例组覆盖 package picker 失败/取消/迟到结果、Activity/Revision Repository Flow 异常与取消、restore 提交后离页/换条目的迟到结果，单文件私有 fixture 同时手写 `MutableEntries`、`BlockingRevisionRepository`、敏感字段库和 session/access fake。可按真实工作流拆测试类并只抽共享构造器；先按[ViewModel owner 边界](../../architecture/viewmodel-ownership.md)与[修订历史生命周期合同](../../features/vault/entry-revision-history.md)裁定职责，避免测试拆分把仍在演进的生产边界固化成重复 fixture。当前源文件有用户未提交改动，后续迁移须保留并适配。
- [ ] 对全仓 63 个恰含一个 `@Test` 的文件和 136 个“含 1–3 个 tests 且不超过 100 行”的文件做 owner 对照后再决定是否合并；Android 平台/生命周期合同、不同映射文件、稳定协议键与安全策略即使只有一个测试也保持独立。特别保留 `LibraryProtoMappingsTest`、`InteractionProtoMappingsTest`、`DiagnosticsLogRecordingProtoMappingsTest`、`ClipboardPolicyProtoMappingsTest` 的分组，它们分别对应不同生产 Proto mapper 文件和不同持久化默认/拒绝语义，不因同 package 或文件小而并到单个 settings mega-suite。

**2026-11-07 扫描开始时盘点：** 377 个 Kotlin test/androidTest 源文件（含 helper）、2,030 个 @Test 声明，均为静态注解数而非执行数；14 个文件超过 500 行。除上述候选外，RuntimeSessionManagerTest 920/26、SourceBoundaryViewModelTest 874/27、OtpCodeRuntimeTest 824/26、ObservabilityExportServiceTest 728/21、RoomObservationSessionTest 699/6、DiagnosticsViewModelTest 688/15、EncryptedLogRecordStoreTest 612/21、DefaultAppNoticeDispatcherTest 610/12（行/注解）。平台/资源/会话场景先保留完整合同，Detail owner 稳定后再拆。旧 351 文件/1,669 声明及 5 个超 500 行统计移除；执行期间测试文件有并发调整，以上尺寸/注解数绑定扫描开始快照；收尾执行数见 Task 86。失败先按 Task 86 处理，结构整理随后。

**2026-11-07 续扫：** 当前源码树为 372 个 Kotlin test/androidTest 文件、1,941 个 @Test 声明、15 个 500 行以上测试文件；静态计数包含独立声明，不代表执行通过。复核 DefaultAppNoticeDispatcherTest 的 12 个用例：settings 加载/恢复、mandatory 路由、系统投递失败 fallback、eventId 去重、sink 返回失败与抛异常分别走不同生产分支，未发现可确认重复或过时用例。相似的失败重试断言保留，因为返回 SinkResult.Failed 与 sink 抛异常分别覆盖正常失败分支和异常释放 claim 分支。单用例文件的 owner 对照还核实了 Detail favicon 异常映射与 state-to-UI 投影、Security 与 Privacy 两个 settings mapper、Vault OTP provider 与 Compose 可见性/组合生命周期各自验证不同生产边界，保留独立。未运行测试。

继续复核 `data/src/test/java/com/aozijx/passly/data/observability/EncryptedLogRecordStoreTest.kt`（21 个用例）：过期清理、按日/数量限制、按大小/总字节限制对应 EncryptedLogFileManager.cleanup 的不同条件；队列优先级两例验证不同被淘汰等级；普通 key unwrap 异常在 nextFileCursor 被跳过，fatal Error 继续传播；返回失败和抛异常分别验证 dedup/异常收尾。各组契约不同，未发现可确认冗余。

**2026-11-07 会话层重复性复核：** `RuntimeSessionManagerTest` 直接验证 `SessionLeaseGate` / `SessionResource` 的 unlock key ownership、lease drain/timeout、观测取消、资源关闭与 runtime transition telemetry；`VaultSessionControllerLifecycleTest` 通过控制器组合验证 AuthenticationState、DEK/permit 清理、数据库 reset/reopen、后台计时及 inbox key provisioning。两层都出现 seal/取消/fatal 字样，但断言 owner 和失败后的责任不同，不合并。

**2026-11-07 导出测试逻辑复核：** `ObservabilityExportServiceTest` 的 21 个用例分别覆盖授权前置、读取分页/排序去重、URI 与 share 文件生命周期、普通失败/取消/fatal 的传播及 cleanup failure 优先级；与 `EncryptedLogRecordStoreTest` 的加密存储、队列、retention 合同分层。share 的返回失败与抛错、设置删除失败与取消/Fatal 保留共享文件，分别走不同异常边界；未发现确认冗余。

**2026-11-07 More 菜单 UI 测试复核：** 全部 test/androidTest 仅有一个 more menu 用例 `VaultTopBarMenuTest.moreMenuKeepsPopupsMeasurableAndDispatchesRoutes`；它覆盖 More popup 尺寸、Scanner/Settings route dispatch、单帧动画可测性及 Sort 子菜单，不存在重复的 More 测试文件或同名行为用例。不同 popup/帧断言分别保护不同 UI 阶段，保留。

**2026-11-07 Wi-Fi 测试 owner 对照：** `WifiQrPayloadParserTest` / `WifiQrPayloadFormatterTest` 验证线格式、转义和协议拒绝；`WifiFormMapperTest` 验证 draft/UI 字段与扫码应用；`AddWifiViewModelTest` 验证初始化 owner、锁/保存竞争和用户可见状态。相同凭据/空密码字样来自 parser、formatter、mapper、ViewModel 不同层次合同，不合并。

**2026-11-07 全量续扫：** 当前树有 368 个包含 `@Test` 的 Kotlin test/androidTest 文件、1,953 个静态测试声明；模块分布为 app 197/1,022、buildSrc 8/124、core 22/80、data 47/213、domain 35/192、feature:autofill 29/146、runtime:session 1/26、security 29/150（文件/声明）。`OtpCodeRuntimeTest` 对照 `OtpCodeRuntime` 的订阅、授权暂停/恢复、ticker、HOTP 消耗、session lock 与 epoch 过期路径后，删除仅断言 fresh-auth 失败时状态为空的用例；该状态由 fresh-auth 恢复用例断言，且 ticker 不重试用例覆盖同一失败路径，删除后保留的断言更完整。另将两处 moving-factor 用例改用注入的固定时钟，消除墙钟周期边界依赖；未运行测试。`SensitiveRevisionFingerprintTest` 的稳定/变化断言已迁入同一 `SensitiveRevisionSnapshotCodecTest`，codec 生产 owner 未改变。备份 wire-model 序列化断言合入 `BackupCodecTest.fieldArchiveV1_wireFieldNamesAreStableAndIndependentFromDatabasePayloads`；银行卡 mapper 的 round-trip 与稳定键断言合并后仍保留完整 `assertEquals(entry, mapper.toEntry(decoded))` 和关键字段核对。Detail favicon/tag editor 删除的 StateModel/Presentation 测试断言分别在 Reducer/DetailReducer 与 `DetailUiMapperTest` 中找到对应覆盖；迁移保留了脏状态确认、保存失败草稿保留、关闭后忽略输入及 UI 字段映射。这些删除/合并未发现已丢失的独立断言。以上为当前未提交改动的静态逻辑复核。

**2026-11-07 OTP 导入用例分层复核：** `ImportScannedOtpUseCaseTest` 检查配置到完整 EntryDraft 的持久化、返回值等待存储确认、失败/异常、锁定/无密钥和取消传播；`ScannedOtpImportViewModelTest` 检查 ViewModel task owner 越过 Scanner caller 退出、认证会话在任务启动前变化与写入开始后撤权、重建 Scanner 后并发确认只写一次，以及 ViewModel 对成功/失败结果的等待与传递。两个文件都有 deferred 写入断言，但分别约束 UseCase 的存储事实和 ViewModel 的任务所有权/返回合同，保留。

**2026-11-07 备份资源缓冲测试复核：** 新增的 `VaultBackupResourceAccumulatorTest` 覆盖超出累计资源上限时擦除被拒内容，以及后续失败时擦除已累计内容并清零计数；`RoomVaultBackupStoreTest` 只验证真实 Room/文件资源的读写回环和回滚，不重复缓冲对象的敏感字节清理合同。保留该单测。

**2026-11-07 buildSrc 边界测试重叠复核：** `SourceBoundaryPolicyTest` 对 `SourceBoundaryVerifier` 的通用规则断言 ruleId 和精确 evidence；`EditorSourceBoundaryVerifierTest` 走路径归属包装器，检查文件归入 `presentation.ui` 后聚合报告的 owner 与规则标签；`SourceBoundaryViewModelTest` 则覆盖 ViewModel 声明范围、类型别名及可执行模板引用。虽有相同禁用 import/ViewModel 情景，调用入口和负责规则不同，当前没有可删的重复逻辑用例；大文件按 Task 74 另做 policy-family 拆分评估。

**2026-11-07 全量续扫（Scanner 生命周期）：** 当前静态盘点为 369 个含 `@Test` 的 Kotlin/Java 测试文件、1,956 个 `@Test` 声明；按模块 app 197/1,022、buildSrc 8/124、core 22/80、data 48/216、domain 35/192、feature 29/146、runtime 1/26、security 29/150（文件/声明）。复核 `ScannerReducerTest`、`ScannerViewModelTest`、`ScannerRouteEffectsTest` 及 Route/VM owner：Reducer 测状态转移，ViewModel 测取消失效/显式停止/宿主暂停与照片 picker 结果，RouteEffects 测 Lifecycle STARTED 收集、背景反馈丢弃与恢复后新反馈；跨层相似断言保护不同 owner/边界，没有可确认重复项。`StopScanning`/Route dispose 终止 owner，`HostStopped` 保留可恢复页面状态并让 picker 结果接管；当前用例覆盖旧 decode 的非协作取消迟到结果。未运行测试；此前 Task 74 的 368/1,953 是旧快照，以上为本次刷新值。

**2026-11-07 全量续扫（UI/仪器测试）：** 刷新后共 369 个含 `@Test` 文件、1,958 个声明；app 197/1,023、buildSrc 8/124、core 22/80、data 48/217、domain 35/192、feature 29/146、runtime 1/26、security 29/150。`app/src/androidTest` 有 23 个文件、43 个声明。复核 `InternalInputAutofillTest` 与 `SensitiveFieldVisibilityTest`、`TagEditorSheetTest` 与 reducer 测试、`RevisionHistorySheetTest` 的 UI 行为/状态边界后，当前相似覆盖分别属于 autofill 排除、无障碍语义、真实 Compose 控件禁用态、纯状态转移或滚动/尺寸合同，没有确认重复。另发现工作区正在修改的 `AndroidAutofillLaunchTargetTest` 新增 `accessibilityHostCreatesOnlyItsModeViewModel`，却断言 Activity ViewModelStore 恰有 1 项；当前 `AuthenticationHost` 与 `AutofillFeatureEntry` 分别请求 owner、legacy、accessibility、credential 四个 Activity 级 ViewModel，故该断言与现有生产路径不符。之前读取时的重复 import 在随后重新读取时已消失，未触碰该并行编辑文件。保留为 Autofill 变更中的待核实测试/生产合同冲突，不擅自删除或改写；未运行测试。

**2026-11-07 备份清理测试交叉复核：** `BackupWriteTestCleanupTest` 直接验证 `enforceWriteTestCleanup()` 对目录探测临时文件的删除失败及异常优先级；`BackupCreatedDocumentCleanupTest` 验证新建 SAF 文档在后续操作失败时的 owner 清理；`DefaultBackupOperationGatewayTest` 验证通过 `BackupStorageSupport.deleteDocument()` 回收导出目标并保留 cancellation/fatal 语义。生产 owner 分别是目录写入探测、文档创建到交接之间的生命周期、备份操作网关；异常类型虽相似，但调用边界不同，不能合并成一个 helper 测试，也未发现可删除断言。仅静态对照生产实现，未运行测试。

**2026-11-07 Revision History UI 测试逻辑整理：** `RevisionHistorySheetTest.captureSheet()` 原本在 `waitForIdle()` 和 500ms Compose 动画等待后，额外按屏幕顶部/底部 RGB 差值判断空历史截图有效，并最多 `Thread.sleep(100)` 轮询 20 次；长列表截图则直接接受首帧。该启发式只对其中一个状态生效，节点可见性与 sheet 尺寸已有 Compose 断言负责行为验证。删除像素探测/墙钟轮询，保留 Compose 等待、截图文件输出和 Bitmap 回收；未运行仪器测试，`git diff --check` 通过。

**2026-11-07 编辑器保存测试重叠复核：** `EditorWriteSessionTest.duplicateSaveAndEditsAreRejectedWhileTheAttemptIsRunning` 验证共享 owner 在 saving phase 拒绝第二次保存/编辑；`AddWifiViewModelTest.duplicateSaveIsSuppressedWhilePersistenceIsInFlight` 经过实际 Wi-Fi `Save` action 和仓储调用，验证该表单接线不重复提交并保持 Saving。一个是共享 session 单元合同，一个是唯一代表性的 ViewModel 集成合同，不能互相替代；Bank Card 的相邻用例测 CVV 校验/保存门禁，没有第二份相同 in-flight 重复保存断言。保留，无需合并。

**2026-11-07 合并 App Backup Snapshot Gateway 重复覆盖：** 新增的 unit `AppBackupSnapshotGatewayTest` 已覆盖 restore 时稳定 icon key 转为 Domain resource key、字节数组 owner 转移，以及兼容过滤后空内容的非空来源禁止 OVERWRITE 且不调用 store。删除 `app/src/androidTest/.../AppBackupSnapshotGatewayTest.overwriteRejectsCompatibilityFilteredNonemptySourceBeforeCallingStore` 的同义断言；该 `restore` 路径不访问 Android `Context`，unit 中 `ContextWrapper(null)` 已足够。保留 androidTest 的真实 `PackageManager` 读取映射/资源限制覆盖。刷新静态清点为 370 个含 `@Test` 文件、1,959 个声明；app 198/1,024、buildSrc 8/124、core 22/80、data 48/217、domain 35/192、feature 29/146、runtime 1/26、security 29/150。未运行测试。

**2026-11-07 跨全仓测试名扫描：** 解析到 1,953 个 `@Test` 方法，没有重复的方法名；该结果只作为检索线索，不能替代按行为与生产 owner 的覆盖比较。备份数据层 `VaultBackupResourceAccumulatorTest` 与应用层 `BackupSnapshotArchiveMapperTest` 都断言失败时擦除缓冲区，但前者覆盖累计上限/接收失败，后者覆盖快照到 archive 的重复资源 ID 校验失败；清理边界各自属于不同 owner，保留。

**2026-11-07 Autofill scope 拒绝复核：** `ResolveAutofillSelectionTest` 检查核心解析器在 scope mismatch 时不调用 credential resolver；`CredentialManagerFlowTest` 通过 API 流程检查映射成 `NotAvailable` 并在失败分支取消 interaction。相同拒绝输入保护不同边界和交互清理，不合并。

**2026-11-07 通知分发链测试复核：** `DefaultNoticeRouterTest` 单测路由计划，`DefaultNoticeDeduplicatorTest` 单测 claim/TTL，`DefaultInAppNoticeSinkTest` 验证容量/队列保持；`DefaultAppNoticeDispatcherTest` 验证它们在 direct dispatch/有界 channel 下的端到端组合。虽然都覆盖 mandatory、重复事件和失败重试，相应断言分别保护策略纯逻辑、claim 状态机、sink buffer 与收据/实际交付，不是可删重复。Sink 返回 `Failed` 与抛 `Exception` 的重试合同也保留为不同分支。当前工作区对恢复场景的 `firstSettingsRead` latch 改成不论 Loading/Failed 都释放，能避免首读直接 Failed 时测试卡在等待；未运行测试。

**2026-11-07 异步测试等待复核：** `SensitiveDataKeyManagerTest` 的 100ms 等待对应真实 monotonic TTL（配置 20ms），`AuthenticationAttemptSupportTest` 的 1ms delay 运行在 `runTest` 虚拟时钟中，用来让 NonCancellable 恢复跨过挂起点；两者不是墙钟轮询重复。`RoomObservationSessionTest` 的 250ms 是 Room/Paging 负向观察窗口，用于检查 reset 后无额外 emission。`AttachmentResourceGarbageCollectorTest` 的 100ms 则在 SEALED 初始状态后给后台 wakeup worker 调度机会，再验证资源文件仍在；当前类没有可观察的“已处理 SEALED wakeup”同步点，属于较弱但有明确目的的负向边界等待，暂未机械替换。未运行仪器测试。

<a id="task-75"></a>

### Task 75：统一固定常量的文件位置与 owner，收敛重复合同值

- [ ] 把文件私有/单文件常量从类型尾部移到 Kotlin 文件顶部声明区（imports 后、主要类型/函数前），保留 `private` 等最小可见性与原始值；优先复核 `SessionLeaseGate.kt`、`DefaultAuthorizationGate.kt`、`CredentialAttemptLimiter.kt`、`AutofillInboxKeyMaterialCodec.kt`、`AutofillInboxRecordCodec.kt`、`ObservabilityRuntimeController.kt`、`ObservabilityExportService.kt` 等常量集中在类尾部 companion 的文件。仅位置迁移不改算法、超时、上限或协议行为。
- [ ] 按真实语义核对并集中跨文件重复合同：`ObservabilityRuntimeController` 与 `ObservabilityExportService` 都以 `PAGE_SIZE = 500` 分页读同一日志 store；`MainActivity` 与 `AppShell` 都以 `APP_CLOSE_MESSAGE_DELAY_MS = 1_000L` 延迟结束 task；`FaviconImageDownloader` 与 `FaviconImageStore` 都限制 `MAX_INPUT_BYTES = 10 MiB`；`AutofillInboxKeyMaterialCodec` 与 `AutofillInboxRecordCodec` 重复声明 RSA 与 SHA-256 算法标识。若调用语义、失败边界或 owner 确实相同，放入同层/同 feature 专用常量文件；若限制的是不同资源、协议字段或操作阶段，保留各自命名与 owner，不按数值相等合并。
- [ ] 继续检查跨文件协议/算法值，例如 `AES/GCM/NoPadding` 在 `CryptoConfig`、`EnvelopeManager`、`VerificationTag` 与日志加密实现中的定义，以及 Favicon 裁剪上下限在 transformer/Compose crop transform 的边界；先判断是否是同一协议合同，再决定复用 core 配置或维持模块私有值，避免为共享字符串引入反向依赖或把不同尺寸/时长误当同一策略。
- [ ] 验证新增/迁移后的可见范围、所有调用点和单一真相源；持久化 key、wire tag、格式版本不得改变，安全限制不得被放宽。补充/迁移对应常量与协议测试；运行受影响模块单测、`verifyArchitecture`、lint 和 `git diff --check`。同步更新 Kotlin 规范中的位置规则；不要建立无 owner 的全局 `Constants.kt`，也不要把资源文本、Gradle 构建值或运行时可调配置复制成 Kotlin 常量。

**约定完成与扫描证据（2026-11-02）：** `docs/development/kotlin-style.md` 新增“常量与固定值归属”规则，`AGENTS.md` 加入简短入口约束；常量按文件私有实现、跨文件同 owner、协议/算法语义、资源/Gradle/运行期配置分别归属。生产源码静态扫描命中 360 个 `const val` 声明，多个声明位于文件尾部 companion；精确字面量聚合发现一批待语义核验的重复（例：observability page size、应用关闭延迟、Favicon 输入上限、Autofill Inbox RSA/SHA 标识）。该扫描同时发现相同数字在不同文件中表达不同策略，故本任务要求核对消费者和行为合同后再集中；本轮只更新规则与 TODO，未移动常量或更改生产代码。


<a id="task-77"></a>

### Task 77：核实诊断日志退出时的持久性与周期写入策略

- [ ] 稳定 flush 合同后调查离开/后台/移除 task/force-stop/强杀的日志损失窗口，记录是否等待 flush、最后持久批次及设备/API；任意终止不保证回调。
- [ ] 先完成[诊断 Tasks 1、2](2026-11-06-passly-diagnostics-reliability-followup.md)，再裁定周期/退出策略，不因独立模块或进程就宣称 durable。

**2026-11-07：deferred，设备持久性调查。** store 已有容量 256 单线程有界队列、50ms/16 条微批，每批 flush + fd.sync；onStop 仅异步排队 flush，不保证完成。移除旧“尚未批量写入/落盘”前提。

## 已实现机制：保留定向验收


<a id="task-22"></a>

### Task 22：保留通知权限请求在 Activity 重建后的用户意图

- [ ] 设备验收 `AppCompatDelegate.setApplicationLocales()` 配置重建与系统授权/拒绝弹窗组合，并确认 PermissionRequestHost arbiter lease 最终释放。现有 `PermissionRequestHostTest` 只覆盖宿主替换和旧结果拒绝，不能代替真实 Activity recreation。

**实现证据（2026-11-01，提交 `ca0e02602`）：** Route 在 Activity 配置变化销毁时通过 `isChangingConfigurations` 保留 ViewModel 中相同的 `NotificationPermissionRequest` 与开通意图；新页面 owner 进入后重绑并重发请求。真正离页会清除其 owner 的待处理请求，迟到的旧 owner dispose/result 不能影响新 owner。`NotificationSettingsViewModelTest` 已覆盖重建后同一 token 恢复并只写一次、真实离页后旧结果作废，以及新 owner 请求不被旧 owner dispose 清理。通知设置及权限中心文档已描述这些合同。实际 locale recreation 与系统弹窗仍缺设备级组合验证。

**旧证据更正：** 早期记录的无 owner `CancelRuntimePermissionRequest` 分支已删除。当前 Route 只调用 `onPageExited(owner, preservePendingPermissionRequest=...)`；ViewModel 先验证 owner 仍 active，再只修改该 owner 持有的 pending request。`AnimatedContent` 交错目前有 ViewModel owner 回归，但动画实际 dispose 顺序仍未由 Compose 导航测试验证。

<a id="task-23"></a>

### Task 23：闭合通知分发在设置快照未就绪时的事件所有权

- [ ] 给 AndroidSystemNotificationGateway 增加平台注入测试，覆盖 channel 创建、文案解析和 notify() 的普通异常、取消及 fatal Error；验证失败结果、异常类型和 dispatcher dedup claim 释放。

**2026-11-07：retained，仅平台测试缺口。** 快照 provider 已使用 RecoverableRead 发布 Loading/Ready/Failed 并重订阅；dispatcher 对 mandatory 在 Loading/Failed 时继续路由。provider/dispatcher 测试已有首次失败、旧快照失效、publisher 启动竞态、恢复和取消覆盖。等级比较已用显式 severity/meetsMinimum，Snackbar 时长由 longToast 表达。移除旧“永久 Loading/Ready、mandatory 被抑制、ordinal 决策”的实现待办。

<a id="task-27"></a>

### Task 27：闭合 OTP Runtime 缓存失效与刷新故障恢复合同

- [x] 成功保存 `EntryType.OTP` 的 `FieldKey.OTP_SECRET` 或成功恢复包含不同 OTP 配置的历史修订后，立即使当前 Detail 工作流中的 OTP schedule/code 失效，并基于已持久化的新配置重新生成；在 OTP 周期边界到达前，展示和复制都不得继续使用旧配置对应的 TOTP。保存失败、无效编辑、同值编辑和页面/认证 owner 替换分别保留正确的缓存与取消行为。
- [ ] 为 Route 生命周期补宿主行为验证：OTP 编辑成功返回时 Detail 刷新当前条目，编辑取消/失败不会触发保存成功路径；Vault 列表恢复时只刷新仍可见订阅，离开后不重新激活无 owner 条目。当前无跨 ViewModel 全局广播桥。
- [ ] 增加 Route/编辑器宿主行为回归：OTP secret 保存成功后返回 Detail 时展示与 copy 使用新配置；离页/锁定期间迟到结果不得覆盖当前状态。运行 OtpCodeRuntime、DetailViewModel 与 Vault OTP 订阅相关测试、`verifyArchitecture` 和 `git diff --check`。

**实现边界：** Detail 与 Vault List 通过独立 Runtime 持有各自 OTP state。OTP 编辑页保存成功返回前台时，Route 刷新本 Runtime 当前仍请求的条目；Detail 的成功历史修订恢复分支也立即刷新当前条目。`entryChanged()` 保留 active/subscription owner，只撤销旧 epoch、schedule 与验证码，再对仍有 owner 的条目异步重载；不会把临时列表订阅提升成永久 active entry。激活读取由 entry epoch 去重，但显式 owner/config 变化可以替换未完成读取的 epoch，旧结果及其认证/锁定异常不能覆盖新配置。Detail ViewModel 现在覆盖前台重载与 restore 成功/失败；未覆盖真实 Route lifecycle、编辑成功返回、锁定或离页期间晚到结果。
**ticker 验证：** `start()` 现使用可替换的毫秒时钟（生产默认 `System.currentTimeMillis`），测试通过受控时钟与 coroutine test scheduler 驱动周期边界。回归验证一个条目普通读取异常清除旧码、其他条目同轮继续刷新、失败条目下一轮恢复；显式 `refreshStates(nowSeconds)` 测试仍保留，用来验证具体移动因子和 copy 状态。

<a id="task-29"></a>

### Task 29：在 Security 页面退出时清除 App Password 对话框草稿

- [ ] 增加 Coordinator/Compose Route 生命周期回归，验证输入后真实离开 Security 页面会清空对话框与草稿、迟到输入/授权或操作结果不能在重进后重新写入旧状态或打开旧对话框、配置重建遵守选定的恢复策略；保留现有提交期间页面 owner 和 `CharArray` `finally` 清理合同。同步认证/安全文档，运行 App Password Coordinator/ViewModel 与 Route 行为测试、`verifyArchitecture` 和 `git diff --check`。（Compose 生命周期仪器测试已编写；待设备连接后执行。）

<a id="task-31"></a>

### Task 31：让 Room 长期观察在会话锁切换时可取消并可重建

- [ ] 在真实 SQLCipher/设备执行已有 RoomObservationSessionTest 的 soft-lock、seal/reopen、DAO emission race、旧 lease 释放用例；验证普通错误可见、撤权清敏感状态、快速恢复不读旧 handle。

**2026-11-07：deferred，机制/竞态测试已接入，待仪器执行。** AppDatabaseSessionObservations 已用 lock generation 取消旧流并处理 SessionLockedException；DatabaseObservationRecoveryTest 覆盖恢复、旧 emission 取消和普通错误不吞空。RoomObservationSessionTest:402、470 已有屏障测试，移除“无恢复机制/无屏障测试”的旧描述。

<a id="task-44"></a>

### Task 44：迟到的 AuthenticationHost 密码提交必须擦除输入缓冲

- [ ] 为真实认证宿主补取消/替换/重建后的迟到密码 callback 测试，保留实际 CharArray 引用，验证错误 request、stale identity、CAS 失配和未接管路径擦除，且不提交给新请求。

**2026-11-07：deferred，防护已实现待宿主验收。** ActivityAuthUiHost:270–283 已擦除请求身份/类型不符及 CAS 失败输入；SecretDialogSubmissionTest 已覆盖空请求、错误请求、inactive、接管及 stale identity，不再列为当前缺少基础 wipe。

<a id="task-45"></a>

### Task 45：统一认证请求与生物识别策略轮换的 Host 并发所有权

- [ ] 以真实 Host/Provisioner 覆盖普通认证与轮换争用、等待取消、pause/destroy、迟到 callback、journal 收尾和重试；同一 UI lease 只服务一个请求，旧 owner 不影响新 owner。

**2026-11-07：deferred，机制已实现待整链路验收。** AuthenticationHostRegistry:73–92 已有可取消 UI mutex；provisioner 与普通认证共用 lease/finally release。ActivityAuthUiHost 使用 request-owned prompt/identity/CAS，HostRegistry/RequestOwnedSlot 已有竞争/取消单测。旧 requestMutex.tryLock 描述移除，当前为 AuthenticationRequestSlot。


<a id="task-64"></a>

### Task 64：让附件 GC 解锁后完整排空并在失败后恢复

- [ ] 在真实 SQLCipher/文件系统执行 AttachmentResourceGarbageCollectorTest 多批排空、失败文件重试、worker 重建唤醒，验证撤权、当前/历史引用保护及最终回收。

**2026-11-07：deferred，实现/测试已接入待仪器执行。** worker 已跨页、越过失败行并有界退避；单测有跨批/锁定/退避/fatal/取消，仪器源码有跨批/文件重试/重建唤醒。移除旧“仅一批、无重试 owner”实现待办。

**2026-11-07 全量续扫（设置/备份/会话锁）：** 当前静态清点为 370 个含 `@Test` 文件、1,959 个声明。Interface settings mapper 直接投影六个 UI 字段与半径范围，ViewModel 用例另测恢复读取、写失败、仓储发射与 page owner；映射与异步写入职责不同，保留。备份 wire model 的单独测试在当前工作区删除，完整 format/version/revision、keyed value、旧字段排除断言已并入 `BackupCodecTest` 的稳定 wire test；银行卡序列化测试把原 round-trip 与 stable-key 用例合为一例，仍检查全部 Entry round-trip、重要 wire keys 与业务字段，合并后无已识别断言缺口。`VaultSessionControllerLifecycleTest` 中同样的 fatal permit revoke 场景分别从 `lock()` 和 exclusive reset 收尾，分别约束两个公开生产入口，保留。SourceBoundaryPolicy 的 66 例逐条保护规则、归属或证据；与 ViewModel declaration scanner 有相似输入，但执行的 verifier/规则不同，暂无可删重复，Task 74 的按 policy family 拆分仍待做。上一批 Revision History 截图等待清理已由 `894703e73` 提交；本轮没有提交、没有运行测试。
**2026-11-07 Autofill Activity ViewModel 断言复核更正：** 先前把“只创建所选模式的 Autofill ViewModel”误读为 Activity `ViewModelStore` 总数必须等于 1。当前 `AutofillFeatureEntry` 用 `when (launch)` 进入独立 Legacy/Accessibility/Credential composable，各分支仅调用本模式的 `hiltViewModel(activity)`；`AuthenticationHost` 自己创建 `AuthenticationHostOwnerViewModel`，`PersistedAppTheme` 也有其主题 ViewModel，但测试只按三个 Autofill ViewModel 的类名检查所选存在、其余两种不存在。因此 `accessibilityHostCreatesOnlyItsModeViewModel` 与当前生产结构相符，旧的“计数冲突”结论撤销。尚未执行仪器测试；测试只覆盖 Accessibility 的分支，若要覆盖三类互斥创建，可后续为 Legacy/Credential 补对称用例。
**2026-11-07 停用测试全仓静态筛查：** 对 `app/buildSrc/core/data/domain/feature/runtime/security` 的 Kotlin 测试源码搜索 `@Ignore`、`@Disabled`、测试 TODO/FIXME 与 `skip()`，当前均无匹配；该筛查只排除显式停用标记，不能替代逐用例逻辑复核。
**2026-11-07 小型测试文件 owner 对照（App/Shell）：** `SessionActivityTouchDispatchTest` 两例分别验证 ACTION_DOWN 在内容收到事件前报告 session activity，以及 MOVE 只委派且保持内容处理结果，属于同一策略的正反分支，作为同一测试类已合并表达，未见重复。`AppShellDestinationTest` 覆盖失败优先级、session mode 和 AuthenticationState 到 typed destination 的状态映射；Android `SecureShellDestinationTest` 验证 Compose 切换目的地时 Vault 敏感内容无需等待动画即 dispose。二者分别约束纯状态映射和真实 Compose 生命周期，不合并。当前只完成该候选子集，Task 74 其余单/小测试文件仍待 owner 对照。
**2026-11-07 认证策略小型测试交叉复核：** `AuthenticationAvailabilityResolverTest` 验证设备可用性只门控已配置的 Biometric，`AppPasswordProvisioningCommitTest` 另验证持久化后即使刷新报告异常，已写凭据仍被解析为可用；与 `AuthenticationSessionReusePolicyTest` 的会话复用判断不是同一规则。后者分别覆盖 Locking 拒绝、Authenticated 允许、AutofillAuthenticated 按 purpose 隔离；但 `canReuseSession()` 还有 `storageUnlocked=false` 与 `requiresFreshAuthentication=true` 两个显式拒绝门，当前直接单测未分别断言。记录为逻辑覆盖待补，现有用例保留；本轮未新增测试或运行测试。
**2026-11-07 Detail 银行卡 Reveal All 过时分支清理：** `DetailEntryTypeUiMapper` 是 `DetailBankCardUiModel` 的唯一生产构造点，并始终设置 `cardholderRevealed=true`；`BankCardSection` 将 cardholder 作为普通可见字段渲染，敏感 reveal selection 只由 card number/CVV/payment PIN 的存在与 revealed 状态决定。旧 `fieldsToReveal()` 仍将 cardholder 加入 Reveal All，只有测试构造的不可达状态能触发。删除该选择分支，并把测试 fixture 改为生产状态、预期只选择已存在且未揭示的 card number；未改测试数量，未运行测试。
**2026-11-07 Autofill 设置页不可达失败态清理：** `RecoverableReadState.Loading/Failed` 不携带旧 preferences；`AutofillRoute` 仅在 `availableValue != null` 时调用 settings mapper/Section，加载或读取失败走独立不可用 UI。因此 Section 内 `preferencesRefreshing`、`preferencesFailed`、`canEditPreferences` 的只读/重试分支无法由生产路径构造；Compose 用例 `failedPreferencesRemainVisibleButReadOnlyUntilRetry` 与 ViewModel“失败不暴露旧设置”合同冲突。删除该仪器测试，移除 UiModel/mapper/Route/Section 中不可达状态字段及对应刷新提示资源，保留 Route 的 retry/load failure UI 和 system-service failure 分支；Section 测试从 4 例减为 3 例。当前全仓静态清点 370 个测试文件、1,958 个声明；静态引用搜索与 `git diff --check` 通过，未运行测试。
**2026-11-07 消息等级小型测试对照：** `NoticeLevelTest` 只覆盖 App 通知的 Toast 长短显示策略（ERROR/CRITICAL 长显示），`MessageLevelTest` 穷举 domain `MessageLevel` 的所有实际/最低等级组合，验证 `severity >= severity` 的门槛函数；前者不涉及分级排序，后者不涉及 Toast 时长，生产 owner 与合同不同。当前实现均显式使用 `longToast`/`severity`，测试中未发现旧 `ordinal` 比较，保留两例。
**2026-11-07 认证复用拒绝门覆盖已补：** 针对上条复核发现的 `canReuseSession()` 缺口，在已有 Authenticated 正向测试中加入 `storageUnlocked=false` 和 `requiresFreshAuthentication=true` 两个负例，分别验证生产 helper 的两个全局早退条件；没有新增 `@Test` 或改生产代码。该待补项现已闭合；静态 `git diff --check` 通过，测试仍未运行。

**2026-11-07 KDF runner 测试失败路径清理：** `SingleThreadKdfRunnerTest` 的 worker 由 `CountDownLatch` 阻塞；旧清理只在全部断言通过后释放 worker 并关闭 runner，早期 assertion failure 会遗留已启动的 runner/secret。将 worker 释放、job join、secret close 与 runner close 移入 `finally`。同时删除 `resultDiscarded.await(50ms)`：result 只在 release latch 后才创建，release 前该负断言无法区分任何可行生产行为；保留 cancel 后 callback 未继续、释放 worker 后迟到 discard 必须到达的有效合同。主路径和 finally 都 `countDown`，保证失败时 worker 可退出。静态检查待本轮结束复核，未运行测试。

**2026-11-07 测试方法重名与 Runtime session 逻辑复核：** 全仓 test/androidTest 源码清点为 378 个 Kotlin/Java 源文件、370 个含 `@Test` 文件、1,959 个声明；64 个单测试文件、139 个不超过 100 行且含 1–3 个测试的文件。注解方法名扫描到 1,959 个方法名且无重名；仅能排除字面重名，不能证明行为不重复。`RuntimeSessionManagerTest` 的 26 例按 key 获取/擦除、资源 open 与失败回滚、事件失败语义、soft lock/resume、lease drain/timeout、dispatcher 与终态重新打开分别核对；取消/fatal 组合测试覆盖转换时机和结果，不与普通异常 telemetry 用例重复，现有 lease 关闭和 cleanup 阶段断言亦有独立生产分支。保留该类用例，Task 74 仍需对剩余单/小测试文件按 owner 继续筛查。当前静态 diff 检查通过，无测试执行、无提交。

**2026-11-07 Revision history rebuilder 单例用例逻辑核实：** `EntryRevisionHistoryRebuilderTest` 的单个 Room transaction 合同一次构造 active 与 soft-deleted entry，并覆盖删除两者的旧 revision、当前 entry version 不被修改、同一时间的当前内容基线解密和最终 revision 总数；步骤共同验证一次性重建后的数据库事实，不拆成互不独立的断言用例。旧名称 `VersionOneBaselines` 容易被读作把 entry revision 改成 1，实际断言保留 entry version 8/3，改名为 `rebuild_discardsObsoleteHistoryAndCreatesCurrentBaselinesForActiveAndDeletedEntries`。全仓测试引用只找到 Rebuilder 的这一个直接 Room 用例；`InitializeEntryRevisionHistory.execute()` 的 generation marker、transaction 失败重试和 post-commit cleanup 尚无直接测试覆盖，记为待补逻辑覆盖；本轮未新增测试。

**2026-11-07 数据库 schema 不匹配测试逻辑修正：** `DatabaseSchemaCompatibilityTest` 旧名声称“不删数据”，但 fixture 未插入数据，且用 `assertNotNull` 接受任何打开异常。现插入哨兵 `EntryEntity`，制造 attachment_refs 旧列结构后验证 Room 以 `IllegalStateException` 拒绝打开、DB 文件保留、旧列结构仍在且哨兵标题仍可从原表读回；因此实际覆盖 fail closed 与数据保留，而不只是文件存在。单例仍验证同一故障事务前后的完整数据库合同，不拆分。未运行仪器测试。

**2026-11-07 Flow 异常传播测试捕获范围收紧：** 全仓 test Kotlin 检查到 `SettingsDataStoreReadTest` 用 `catch(Throwable)` 暂存 protobuf corruption、IOException 和 CancellationException，再断言结果。生产 `tagCorruptSettingsData()` 只转换 `CorruptionException`、其他错误原样重抛；按预期类型分别捕获 `CorruptSettingsDataException`、`IOException`、`CancellationException` 并继续检查原始对象，unexpected `Error` 不再被测试吞成断言结果。没有新增测试；静态检查待本轮结束复核，未运行测试。

**2026-11-07 全仓宽异常断言复扫：** 全仓仍有 5 个 `catch(Throwable)` 测试位置：`RuntimeSessionManagerTest` 两处遍历 Cancellation/Error 矩阵并分别核对身份/清理状态；`ObservabilityExportServiceTest` 一处使用 Cancellation/Error 的计划删除异常矩阵；两个 Favicon 测试位置是泛型 `assertSuspendThrows<T>` helper，捕获后检查 `T` 并返回原实例。都对应 fatal/cancellation 传播合同或类型化 helper，不收紧。JUnit/Kotlin 对 `Exception`、`Throwable`、`RuntimeException` 作宽期望类型的全仓正则扫描无匹配。`SettingsDataStoreReadTest` 的三处宽捕获已换为具体 expected type。当前四个测试文件的 `git diff --check` 通过，未运行测试或暂存。
**2026-11-07 Settings 读失败状态二次交叉核对：** 复查 Autofil 失败态清理边界后，未发现同类不可达失败 UI 测试遗漏。`PermissionManagementScreen` 直接接收 VM 的 `RecoverableReadState`，Retry action 回到同一 VM；`DataManagementSettingsViewModel` 仍由独立 `BackupRoute` 使用，Failed/Retry 用例覆盖该 backup-directory owner；主 Data Management 页面使用 `StorageSettingsViewModel`，其 `logRecordingMode` Failed/Retry 由对应 VM 测试覆盖且 Route 分支可达。相似“settings read failed”文案属于多个真实状态 owner，没有可合并或删除的同义用例。
**2026-11-07 Notification Settings ViewModel 逻辑复核：** 17 个用例逐项对照 `NotificationSettingsViewModel` 状态机。三个相似的 permission owner 测试分别覆盖正常离页后的旧 grant/deny 拒绝、配置重建复用同一 request identity 并提交，以及旧 owner 的迟到 dispose 不得撤销新 owner request；另有用户禁用时拒绝晚到 grant、重复 enable 共用 pending request、撤销后 denial 忽略并可重试，输入时序不同且状态责任独立。读取失败/失败态拒绝写入/Loading、平台权限申请与平台开关复核、消息和 topic 写入、普通保存失败与 grant 后保存失败各守不同路径。暂未发现重复或过时用例；本轮只静态复核，未运行测试。
**2026-11-07 Security/Privacy settings 逻辑复核：** Security 与 Privacy 用例共享底层 `SecuritySettingsRepository`，但分别验证认证策略操作（Biometric 轮换、last-method、operation owner）与隐私/剪贴板设置；读失败/重试也各验证独立 ViewModel 接线，没有合并候选。发现 `PrivacySettingsAction.ClearClipboardNow` 只有生产分支、没有测试调用；新增一个参数化结果顺序用例，在同一 Page owner 内验证清理器返回 owns=true/false 分别映射到 `ClipboardCleared`/`ClipboardNotCleared`，同时确认各调用一次。静态声明数因此增加 1；`git diff --check` 通过，未运行测试。
**2026-11-07 Permission Screen 测试 fixture 状态修正：** `PermissionManagementScreen` 只有 `backupDirectoryReadState=Ready` 才渲染备份目录管理 row；Compose action 测试原先只设置 `backupDirectory` 展示数据，read state 沿用 `Loading` 默认值，却点击 Ready 分支才有的“管理” action。将该测试状态设为 `RecoverableReadState.Ready`，使 action 断言对应真实可达 UI；用例数不变。静态 diff 检查通过，未运行仪器测试。
**2026-11-07 Compose settings group/editor focus 复核：** `SegmentedSettingsGroupTest` 的 anchored selection 用例验证用户选择更新外部状态；slider 居中用例覆盖 `sliderSettingsGroupItem` 的几何/语义边界，而该组件仍由 `InterfaceScreen` 调用，Autofill 页面移除 slider 不影响它，两例保留。`EntryFocusBehaviorTest` 检查密码编辑器向下传递 screen/save modifiers，并通过 IME 验证标题→账号→密码焦点顺序；`NextFocusTextField` 也用于银行卡、Wi-Fi、OTP 编辑器，目前没有重复焦点测试，保留该代表性 UI 合同。本轮静态查看，未运行仪器测试。
**2026-11-07 Revision History Compose 测试副作用清理：** `RevisionHistorySheetTest` 已通过 semantics 断言空态/restore 可用性、50 条历史与 30 项差异的滚动选择、敏感字段 reveal gating，以及恢复中禁止返回/下滑关闭；另有两处 `captureSheet()` 只在测试结束时把屏幕截图写入外部文件，没有 golden 比较或后续读取，且重复等待/推进时钟。删除截图写入 helper 与调用，保留布局尺寸和交互断言。`git diff --check` 通过，未运行仪器测试。
**2026-11-07 动画测试容差逻辑修正：** `AnimatedColorSchemeComposeTest.assertColorNear` 原先将 expected 与 actual 的中点和 expected 比较，实际容差因此被放宽约一倍；改为直接比较 RGBA 四通道并移除无用 `lerp` 导入。保留同一重定向/平滑过渡用例；`git diff --check` 通过，未运行仪器测试。

**2026-11-07 More 菜单与墙钟等待交叉搜索：** 全仓只发现一条 Compose `More` 菜单交互测试；`VaultNavigationTest` 的 `OpenSettings` 是纯导航状态映射，与 UI action 不重复。测试中的扫描/设置路由、popup 尺寸和 sort 子菜单断言属于不同职责但同一 top bar owner，暂留在单例用例。全仓匹配到 20ms TTL 后等待 100ms、sealed GC worker 启动观察 100ms、Room observer reset 后等待 250ms 的无额外 emission 窗口；它们分别约束时钟过期、启动恢复与异步 observer 静默，没有将其当作可删除的相同 sleep。后续若要消除墙钟等待，需要相应 owner 暴露可控时钟/启动屏障或可判定的观察窗口；未改这些合同。

**2026-11-07 失败类型断言逻辑复核：** `EntryRevisionRestoreTransactionTest.staleVersion_rollsBackWithoutChangingCurrentEntry` 用 expectedVersion=1 对 version=2 的记录调用 restore；生产首个版本检查明确抛 `Conflict`，旧断言只确认任意 failure 非空，允许错误异常类型通过。改为断言 `Conflict` 并附实际异常信息，同时保留回滚后条目/修订/活动记录断言。静态 `git diff --check` 通过，未运行仪器测试。

**2026-11-07 KeyStore fatal 错误传播断言加强：** `BiometricAliasDeletionTest` 旧 fatal 用例比较 message，无法证明 `Error` 原实例穿过仅捕获 `Exception` 的 helper 传播；改用 `assertSame(fatal, actual)`，保留普通异常返回 retryable false 与成功 true 两个相邻分支。静态 `git diff --check` 通过，未运行测试。

**2026-11-07 Autofill launch UI 采样时序修正：** `AndroidAutofillLaunchTargetTest` 原先在 ActivityScenario 达到 RESUMED 后立刻 close，并只在 `onActivityPaused` 读取 ViewModelStore；Compose 首次组合可能仍待主线程/首帧调度，导致把尚未创建 VM 当成路由失败。close 前加入 `InstrumentationRegistry.waitForIdleSync()`，再由 pause callback 取样；只检查三类 Autofill workflow VM 的存在/互斥，不限制 AuthenticationHost 或主题 owner。静态 `git diff --check` 通过，未运行仪器测试。

**2026-11-07 Autofill launch target 参数合同补齐：** `AndroidAutofillLaunchTargetTest` 为 Legacy 与 Credential 各传入了 request ID，但未读取返回 Intent 的 `REQUEST_ID` extra；测试此前只证明路由目标/action。新增两条 extra 值断言，覆盖 target 将调用参数写入平台 Intent 的合同。`git diff --check` 通过，未运行仪器测试。

**2026-11-07 Backup JSON 格式错误断言收紧：** `BackupCodecTest.documentValidator_acceptsAccountHierarchy_andRejectsMixedPayload` 把合法导出 JSON 的唯一 `format` 值改成旧格式后导入；执行路径到 `BackupBundleValidator` 的 format `require`，合同是 `IllegalArgumentException`。旧 `assertThrows(Exception::class.java)` 会让解码/IO 等无关异常也通过，已收紧到该类型。容器密码/密文认证测试仍保留宽异常断言，因为 AES-GCM provider 认证失败的具体异常类不属于稳定跨平台合同。静态 `git diff --check` 通过，未运行测试。

**2026-11-07 异常类型断言二次核实更正：** 初审认为 AES-GCM provider 认证失败应保持宽异常；沿调用链确认相关测试直接返回 `Cipher.doFinal`，JCA 对 AEAD tag 校验失败合同为 `AEADBadTagException`。因此收紧 `BackupCodecTest` 三个错误密码/密文/header 认证断言、`EncryptedLogRecordStoreTest` 文件篡改、`AutofillInboxKeyMaterialCodecTest` 错误 DEK 与 `VerificationTagTest` 错误 DEK；非法 UTF-8 则收紧为 `CharacterCodingException`。先前“容器认证保留宽异常”的记录由本条更正。`git diff --check` 通过，未运行测试。

**2026-11-07 全仓宽异常期望类型扫描：** Kotlin `*Test.kt` 对 JUnit `assertThrows(Exception|Throwable|RuntimeException)` 与 Kotlin `assertThrows/assertFailsWith` 的全仓搜索当前无匹配；本轮已将可由本地调用链证明的格式拒绝、GCM tag 失败和严格 UTF-8 解码改为具体异常。此静态规则不覆盖 catch 后手写判定或自定义 expectFailure helper，后续逻辑复核仍需查看调用点。修改路径 `git diff --check` 通过，未运行测试。

**2026-11-07 会话测试过时名称清理：** 将文件与类从 `VaultSessionIdleTimeoutTest` 重命名为 `VaultSessionControllerLifecycleTest`，准确反映其主要生产 owner；未移动/合并用例或改断言，23 个 `@Test` 保持。对照 `RuntimeSessionManagerTest` 后确认相似 seal/cancel/fatal 场景分属资源运行时与安全控制器责任，保留两组。旧名称全仓搜索无匹配；未运行测试。

**2026-11-07 Domain Wi-Fi/card validation 测试 owner 对齐：** `EntryValidationTest` 中 open 密码拒绝、WEP 长度、WPA 长度/hex 与空 SSID 四例直接调用 `WifiCredentialValidation`，其余覆盖 `EntryValidation` 投影与委托；全仓测试引用未找到这些规则的其他副本。将四例移动到新增 `WifiCredentialValidationTest`，`EntryValidationTest` 保留真正通过 `Entry` 构造调用的集成验证；不删除断言、不增加 `@Test`。相邻 `WifiSecurityTypeTest` 检查的是持久化字符串解析/稳定值，不是同一规则。CardNumberValidator 直接测试算法结果；EntryValidation 的同组卡号 fixture 则验证把 checksum/characters 结果映射到稳定 `EntryViolation` 且接受 editor 格式输入，保持独立，并将方法名改为说明其 mapping 合同。静态 diff 与未跟踪新文件尾随空格检查通过，未运行测试。

**2026-11-07 Autofill 状态观察异常断言更正：** 首次把 7 个 observer 生命周期用例中的 5 处断言强化为异常对象 identity；执行后确认 `callbackFlow` 的 coroutine stacktrace recovery 会复制 throwable，实际/期望的具体类型和 message 一致。恢复为类型与 message 语义断言；7 个用例及 App/Autofill 全量单测现均通过。之前记录的“改 assertSame、未运行”已由本条实测更正。

**2026-11-07 测试源码计数刷新：** 当前工作树含 380 个 test/androidTest Kotlin/Java 源文件、372 个含 `@Test` 的文件、1,966 个声明；其中 `domain` 仍为 192。相较上一快照，四个 Wi-Fi 用例仅移动到 owner 文件、声明数不变；并行新增的 `AutofillServiceStatusObservationTest` 贡献 1 个测试文件和 7 个声明。后续按当前工作树继续刷新，避免把并行功能测试归入本次移动。
