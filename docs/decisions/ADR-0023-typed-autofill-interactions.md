# ADR-0023: 类型化 Autofill Interaction

- 状态：Accepted
- 日期：2026-10-13
- 替代：ADR-0005、ADR-0006

## 背景

旧 Autofill 链路以 `FillRequestDispatcher`、`CandidateRetriever`、`ResolvedCandidate` 和平台各自的认证 session 为中心。同一个候选类型既可能只有展示元数据，也可能已经携带秘密；认证、短期 grant、Vault 解锁回收和最终作用域检查分散在多个入口中。

这使“候选阶段绝不加载秘密”“一次系统交互只认证一次”“最终选择只能消费实际展示过的条目”等安全性质只能依赖调用约定，无法由类型和状态转换直接表达。

## 决策

采用 process-local、显式 ID 的 Autofill interaction：

1. `AutofillCandidateSummary` 只包含列表元数据和匹配原因；不得依赖 `Entry` 聚合或 secret-bearing 类型。
2. `AutofillCredentialPayload` 只由最终选择解析产生，并且只包含本次填充需要的字段。
3. `AutofillInteractionStore` 保存来源、规范化作用域、状态和已展示 entry ID；PendingIntent 只传 interaction/entry ID。
4. `DiscoverAutofillCandidates`、`AuthorizeAutofillInteraction`、`ResolveAutofillSelection`、保存操作和 `FinishAutofillInteraction` 是两种 Android 入口共享的唯一业务链路。
5. Autofill 自己触发的 Vault 解锁由 `AutofillUnlockLease` 按活跃 interaction 借用计数；最后一个借用者结束后回收。
6. interaction 创建后 60 秒自动过期；正在认证、解析秘密或保存时延后回收。进程死亡时不从 Intent extras 恢复授权，而是拒绝旧选择并要求系统重新查询。
7. 平台适配器不得直接依赖 Entry repository 或认证端口；构建期边界规则负责阻止回归。

## 后果

- 候选展示与秘密解析在类型和数据访问上分离。
- Legacy Autofill 与 Credential Manager 共享认证、作用域、取消、超时和临时解锁语义。
- 解锁重发现与最终选择可以消费同一个 interaction，避免重复认证。
- Android 选择器跨进程存活时，旧选择需要重试；这是避免 extras 成为授权凭据的明确取舍。
- process-local runtime 需要统一处理完成、取消、clear-state 与超时，不能由 Activity 各自维护 session。

## 备选方案

- 未保留 `ResolvedCandidate(includeSecrets)` 或空密码 sentinel，因为调用方无法从类型确认秘密是否可用。
- 未保留按包名/域名的短期全局 grant，因为它会把一次交互授权扩大为时间窗口授权。
- 未把认证与 repository 注入 Android service/handler，因为这会重新产生平台双轨业务逻辑。

## 关联资料

- [自动填充安全与架构](../security/autofill.md)
- [Credential Manager Provider](../features/credential-manager.md)
