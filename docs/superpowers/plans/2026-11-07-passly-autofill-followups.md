# Autofill 重构后待办与验收

**复核：2026-11-08。** 本页是 Autofill 维护后续的唯一执行清单；保留仍有效的原 Task ID。模块切换、统一宿主、framework result 迁出 ViewModel、direct-entry resume 归用例、设置端口拆分均已接入，不再作为待实现项。生产合同见[共享填充](../../features/autofill/fill-flow.md)、[Legacy 平台](../../features/autofill/legacy-service.md)与[验证矩阵](../../features/autofill/verification.md)；已完成的宿主/AVD 验收证据见[审查记录](../../reviews/2026-11-08-autofill-host-validation.md)。

| 顺序 | 剩余事项 | 性质 |
| --- | --- | --- |
| 1 | [安全 Task 6](2026-11-06-passly-security-scan-followups.md#task-6)：Legacy 网页接收方身份 | 已确认安全问题；在候选展示与凭据交付前闭合接收方信任边界 |
| 2 | [Task 21](#task-21)：统一宿主结果交付与销毁验收 | 已实现合同的宿主/设备验证，包含原 Task 9 |
| 3 | [Task 82](#task-82)：设置变更后现存交互有效期 | 待裁定策略，尚未认定为运行缺陷 |

网页接收方身份由[安全 TODO Task 6](2026-11-06-passly-security-scan-followups.md#task-6)持有并优先处理；质量门禁由[主待办 Task 73](2026-10-30-passly-maintainability-todo.md#task-73)持有。这里不复制安全问题、Lint 报告或整个 ViewModel 审计。

<a id="task-21"></a>

## Task 21：验证统一宿主的交付、重建与收尾

- [ ] 以真实 App Activity + Feature entry 覆盖 Legacy discovery/selection 的配置重建、回调失败及重试、成功后终态释放。验证 Feature delivery claim 与 Activity result claim 只在各自显式 commit marker 后终结；当前 App completion callback 返回后才调用 marker，因此还需覆盖 callback 在 `setResult` 等部分副作用后抛错的恢复语义。断言平台提交先于 interaction/lease 收尾、callback 恰好一次且记录不重复。SelectedCredential 是为重建/重试暂存的语义结果；不能把“VM 不持有 Dataset”改写成“VM 不得暂存任何结果”。
- [ ] 覆盖 Accessibility finish/destroy 与 session lock 互斥阻塞交错，断言主线程 teardown 返回且最终清理完成；覆盖 Credential Manager 支持 API 上的取消、交付及宿主替换。复用已有 lease/claim 单测，不重新实现其已具备的取消等待和重试机制。
- [ ] 以真实宿主覆盖认证、候选查询、选择和 Route 尚未启动时的非配置 `ON_STOP`；确认旧结果不再交付、interaction/lease 终结且下一次填充立即可用。本轮源码审查、模块编译及 `:feature:autofill:testDebugUnitTest` 已通过；两项 fatal 路径测试改为比较异常类型与消息，避免协程边界复制堆栈时的错误身份断言。`adb devices` 未发现设备，宿主整链路仍待设备验收。
- [ ] 按[验证矩阵](../../features/autofill/verification.md)补齐尚缺的真实字段验收：BottomSheet 选择后直接填入目标控件；提供 InlinePresentation 的请求下验证 Inline 专属候选与认证结果。SYSTEM_INLINE 下系统下拉候选认证后真实填值已在 Pixel_8_Pro API 36 验证；不能把该结果扩大为 Inline 专属界面证明，也不能将 result.delivered 单独当成成功填值。设备、测试应用与测试凭据按执行时现状记录，不沿用历史 AVD/reset 条件。
Task 21 的已完成单测、Activity entry 检查和 API 36 AVD 验收证据集中保存在上述审查记录；本 Task 只保留未执行的整链路与设备验收，不重复列实现 owner 或历史环境清理步骤。

### 2026-11-10 从头复核与日志进度

- 已沿 Legacy、Credential Manager、Accessibility 入口复核发现、认证、选择、结果交付和宿主退出路径；给三类宿主的未提交退出、Legacy/Credential Manager 系统请求取消、Legacy/Credential Manager 平台结果提交异常增加结构化事件。候选查询异常保留异常类型和来源，不记录凭据或原始请求值。
- 定向测试复现查询正常返回后协程已取消但 interaction 仍活动：无竞争的状态写入不会必然检查取消。`DiscoverAutofillCandidates` 现于查询返回和发布结果前显式检查取消，并在创建 interaction 后的取消路径终结它；新增回归测试通过。
- Credential Manager begin-get 现在把构造出的响应与 interaction ID 一并返回 Service。系统取消信号在回调前到达、响应构造中取消或抛普通异常时，立即结束该 interaction；Legacy 与 Credential Manager 复用同一发现结果 ID 映射。
- `:core:test`、`:feature:autofill:testDebugUnitTest` 与 AndroidTest Kotlin 编译通过。`:app:compileDebugKotlin` 被 Vault `BankCardSection.kt:95` 的既有 `String?`/`String` 类型不匹配阻断，无法据此证明宿主模块编译通过。仍需覆盖真实 Activity/Service 取消交错、配置重建与设备交付。

<a id="task-82"></a>

## Task 82：裁定已展示交互的设置快照合同

- [ ] 明确总开关或 Credential Manager 开关关闭后，既有 interaction/系统 PendingIntent 是可在原有效期内完成，还是立即撤销；再按所选合同覆盖候选展示、选择/交付、来源隔离、过期与 lease 收尾，不预设必须增加设置重读。

**当前证据：** 设置项及其现有生效阶段见[共享填充设置表](../../features/autofill/fill-flow.md#设置项与生效阶段)。`DiscoverAutofillCandidates` 每次发现读取最新设置，候选展示后的 `ResolveAutofillSelection` 不重读；Credential Manager create 最终保存时会重读开关。缺少“关闭即撤销”或“原请求继续”的明确产品合同，不能仅凭某一阶段不重读设置认定漏洞。

## 本次清理决定

- **合并：** 原 Task 9 → 21；原 Task 52 的系统状态观察验收已完成并从计划清除。主清单移除重复段落；本页当前未完成项为 Task 21、82。
- **移除：** 原 Task 85 的强制 entries.firstOrNull → when 形式改造。当前协议已用显式 wireKey/wireValue，未知值仍拒绝；没有依据说明查找形式本身造成行为缺陷。保留稳定键和未知值拒绝合同，不改生产实现。
- **归档：** 模块抽取与 BottomSheet 认证计划转为实施记录，去掉过期任务、临时工作区指令、固定 AVD 名称与 reset/setup 操作前提；剩余验收只由本页 Task 21 持有。
