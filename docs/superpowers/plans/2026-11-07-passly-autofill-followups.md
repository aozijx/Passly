# Autofill 重构后待办与验收

**复核：2026-11-07。** 本页是 Autofill 维护后续的唯一执行清单；保留仍有效的原 Task ID。模块切换、统一宿主、framework result 迁出 ViewModel、direct-entry resume 归用例、设置端口拆分均已接入，不再作为待实现项。生产合同见[共享填充](../../features/autofill/fill-flow.md)、[Legacy 平台](../../features/autofill/legacy-service.md)与[验证矩阵](../../features/autofill/verification.md)。

| 顺序 | 剩余事项 | 性质 |
| --- | --- | --- |
| 1 | [Task 21](#task-21)：统一宿主结果交付与销毁验收 | 已实现合同的宿主/设备验证，包含原 Task 9 |
| 2 | [Task 82](#task-82)：设置变更后现存交互有效期 | 待裁定策略，尚未认定为运行缺陷 |

已完成：Task 81（共享授权异常终态）、Task 33（系统状态单次初读与 observer 生命周期）。

网页接收方身份由[安全 TODO Task 6](2026-11-06-passly-security-scan-followups.md#task-6)持有；质量门禁由[主待办 Task 73/86](2026-10-30-passly-maintainability-todo.md#task-73)持有。这里不复制安全问题、Lint 报告或整个 ViewModel 审计。

<a id="task-81"></a>

## Task 81：异常终结共享授权 attempt

- [x] 在 prepare 成为授权 owner 后，为 requester、lease 获取及授权收尾的普通异常与 fatal Error 定义清理终态：移除对应 attempts 项并结束共享 deferred，避免后续 caller 永久 await；保留取消/fatal 传播及已有完整 Vault 会话所有权。
- [x] 用两个 caller 共享同一 interaction 的行为测试覆盖普通失败、取消、fatal、会话已解锁后的 lease 清理、重试和迟到结果；验证 waiter 结束、interaction/lease 清理及错误类型，不用 timeout 清表掩盖异常 owner。

**2026-11-07 完成：** owner 现在对认证、lease 和 finish 阶段的任何 Throwable 都在 NonCancellable 收尾中写入 Failed、释放可能已获取的 Autofill lease、移除当前 attempt 并终结共享 deferred。普通异常由 owner 传播，普通清理异常作为 suppressed 保留；清理若抛出 fatal `Error`，则传播 fatal `Error`，避免把它压成普通失败。取消仍写入 Cancelled 并传播取消。`AutofillInteractionLifecycleTest` 覆盖共享 caller 的普通异常、fatal、取消、认证状态已解锁后的 lease 清理、不同 interaction 重试和 waiter 终结；同套用例已有迟到认证成功被已取消 interaction 收尾的回归。`:feature:autofill:testDebugUnitTest` 全量通过。

<a id="task-21"></a>

## Task 21：验证统一宿主的交付、重建与收尾

- [ ] 以真实 App Activity + Feature entry 覆盖 Legacy discovery/selection 的配置重建、回调失败及重试、成功后终态释放，验证 callback 恰好一次、记录不重复、平台提交先于 interaction/lease 收尾。SelectedCredential 是为重建/重试暂存的语义结果；不能把“VM 不持有 Dataset”改写成“VM 不得暂存任何结果”。
- [ ] 覆盖 Accessibility finish/destroy 与 session lock 互斥阻塞交错，断言主线程 teardown 返回且最终清理完成；覆盖 Credential Manager 支持 API 上的取消、交付及宿主替换。复用已有 lease/claim 单测，不重新实现其已具备的取消等待和重试机制。
- [ ] 按[验证矩阵](../../features/autofill/verification.md)完成真实字段验收：BottomSheet 选择后直接填入目标控件；系统/Inline 保持各自认证结果合同；实际观察目标字段，不能将 result.delivered 当成成功填值。设备、测试应用与测试凭据按执行时现状记录，不沿用历史 AVD/reset 条件。

**当前 owner：** App `AutofillAuthActivity.completeAutofill()` 只负责 setResult/hide IME/finish；Feature `AutofillFeatureEntry` 解析并分派 launch；独立的 Legacy、Accessibility、Credential mode host 各自获取对应 Activity-scoped VM、处理生命周期取消和平台结果交付；Security 提供 AuthenticationHost。Legacy VM 不注入 Context/factory/store，direct-entry resume 在 DiscoverAutofillCandidates。原 Task 9 的宿主收尾验证合入此处，旧三 Activity、Activity 内建 Dataset、VM 直接读 store 的描述删除。

**2026-11-07 部分验证：** Feature entry 按 Legacy、Accessibility、Credential 分派到独立的 mode host；每个 host 只创建对应 ViewModel，并在同一边界管理该模式的销毁/取消与结果交付。App Activity 仍只组合 Security 的 `AuthenticationHost` 与 Feature 入口。`AndroidAutofillLaunchTargetTest.accessibilityHostCreatesOnlyItsModeViewModel` 在 Pixel_8_Pro API 36 仪器运行通过，确认 Accessibility 宿主创建自己的 ViewModel，且不创建 Legacy/Credential ViewModel。此项不覆盖 Task 21 的结果交付、配置重建、并发收尾或真实字段填充验收。

**2026-11-07 当前 AVD 现场（不作为新源码验收）：** 设备原有 `com.aozijx.passly` 是 versionCode 8 / versionName 0.3.3 的旧安装包，Manifest 仍注册已从当前源码移除的 `AutofillFillActivity`；因此从它打开旧认证框不能验证本次统一宿主。当前源码构建的 Debug APK 使用独立 applicationId `com.aozijx.passly.debug`，其 Manifest 只有 `AutofillAuthActivity` 和三个 Feature Autofill 服务；该包已安装，但新 Vault 停在 `Set App Password` 初始设置页。设备上 `com.example.autofilltarget` 有 Username、Password、Confirm password 三个字段；系统 Autofill 选择已恢复为原始空值。未设置或输入密码，真实填值验收仍待完成。

**已有证据的边界：** AutofillActivityResultClaimTest 与 AutofillUnlockLeaseTest 已覆盖基础 claim/lease 行为；原模块实施记录中的 response-factory 仪器结果只证明协议构造，不能证明目标字段填值。这里不新增“再次抽取模块/移出 framework 对象”的实现任务。

<a id="task-33"></a>

## Task 33：系统状态初读与 observer 生命周期

- [x] 在 App 的 AndroidAutofillServiceStatusSource 中收敛重复初读：保留先注册 observer、再发已注册快照，消除注册前的 onStart 额外读取；不能改成先读后注册而引入丢失变化窗口。
- [x] 用 observer flow 生命周期测试覆盖首次快照、变更、注册/读取/回调失败、取消注销及 fatal Error 传播；现有 ViewModel 测试覆盖失败 source 重试时旧 collector 先取消。

**2026-11-07 完成：** App `AndroidAutofillServiceStatusSource` 将平台读取映射与 `observeBooleanStatus` observer 生命周期拆开；Flow 只在注册 observer 后读取一次初始快照，移除注册前的 `onStart` 查询。`AutofillServiceStatusObservationTest` 覆盖注册顺序、初始值、变更、注册/读取/observer 回调失败、取消注销和 fatal Error；Flow 异常校验具体类型和 message，不要求 coroutine stacktrace recovery 保留异常对象 identity。`AutofillSettingsViewModelTest.retry restarts only failed system source` 覆盖重试替换时取消旧 collector。2026-11-07 全量复跑 App 单测 991 项、Feature Autofill 单测 135 项均通过；`verifyArchitecture` 通过。

<a id="task-82"></a>

## Task 82：裁定已展示交互的设置快照合同

- [ ] 明确总开关或 Credential Manager 开关关闭后，既有 interaction/系统 PendingIntent 是可在原有效期内完成，还是立即撤销；再按所选合同覆盖候选展示、选择/交付、来源隔离、过期与 lease 收尾，不预设必须增加设置重读。
- [ ] 记录当前仍存在的 enabled/credentialManagerEnabled、requireAuthentication、strictDomainMatching、presentation 分别在何阶段生效；已删除的 OTP-inclusion、save-prompt、unmatched fallback、候选数量选项不进入后续任务。

**当前证据：** DiscoverAutofillCandidates 从专属 AutofillSettingsSource 读取策略并检查 enabled/supports；ResolveAutofillSelection 只验证既有 scope、state、已展示候选及读取结果。缺少“关闭即撤销”或“原请求继续”的明确产品合同，不能仅凭不重读设置认定漏洞。

## 本次清理决定

- **合并：** 原 Task 9 → 21，原 Task 52 → 33；主清单移除这些重复段落。本页当前未完成项为 Task 21、82；Task 81、33 已完成并保留验收记录。
- **移除：** 原 Task 85 的强制 entries.firstOrNull → when 形式改造。当前协议已用显式 wireKey/wireValue，未知值仍拒绝；没有依据说明查找形式本身造成行为缺陷。保留稳定键和未知值拒绝合同，不改生产实现。
- **归档：** 模块抽取与 BottomSheet 认证计划转为实施记录，去掉过期任务、临时工作区指令、固定 AVD 名称与 reset/setup 操作前提；剩余验收只由本页 Task 21 持有。
