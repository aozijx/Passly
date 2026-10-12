# 自动填充安全与架构

状态：当前实现。

代码核对日期：2026-10-13。

Passly 同时支持传统 Autofill Service 与 Android 14+ Credential Manager。两套 Android 入口只负责解析系统请求和构造平台响应；候选发现、交互授权、单条秘密解析、保存与结束由同一组共享操作负责。

## 当前架构

```mermaid
flowchart TD
    Legacy[LegacyAutofillService] --> Parse[AssistStructure parser]
    Credential[ModernCredentialService] --> CredentialAdapter[Credential handlers]
    Parse --> Discover[DiscoverAutofillCandidates]
    CredentialAdapter --> Discover
    Discover --> CandidateRepository[AutofillCandidateRepository]
    CandidateRepository --> Metadata[(entries metadata)]
    Discover --> Store[AutofillInteractionStore]
    Store --> Authorize[AuthorizeAutofillInteraction]
    Authorize --> Auth[AuthenticationRequester]
    Store --> Resolve[ResolveAutofillSelection]
    Resolve --> Resolver[AutofillCredentialResolver]
    Resolver --> Secret[(selected secret field)]
    Store --> Finish[FinishAutofillInteraction]
    Finish --> Lease[AutofillUnlockLease]
```

共享操作的职责如下：

- `DiscoverAutofillCandidates`：读取当前设置、识别字段、建立或恢复 interaction，并只查询候选元数据。
- `AuthorizeAutofillInteraction`：以 `AuthenticationPurpose.AUTOFILL` 完成一次交互授权；并发授权会合并为同一次认证。
- `ResolveAutofillSelection`：验证 interaction、规范化作用域和“确实展示过”的 entry ID 后，才读取该条目的秘密。
- `SaveAutofillCredential` / `SaveAutofillInteraction`：使用正式 Entry 写事务保存捕获到的密码。
- `FinishAutofillInteraction`：统一完成、取消或过期 interaction，并释放 Autofill 持有的临时解锁。

`HeuristicMatchStrategy` 仅供传统 Autofill 解析非标准表单；Credential Manager 使用严格字段策略。平台适配器不得直接依赖 Entry repository 或认证端口，这一依赖方向由 `SourceBoundaryPolicy` 验证。

## 信任边界

Autofill 跨越目标应用、Android 系统、Passly Service、认证 Activity 和 Vault：

- Credential Manager 的调用方身份只能来自系统注入的 `ProviderGetCredentialRequest`、`ProviderCreateCredentialRequest` 或原始 `BeginGetCredentialRequest`。
- PendingIntent 自定义 extras 只携带 Passly 生成的 `interactionId` 与不透明 `entryId`，不能携带或重建调用方授权。
- 传统 Autofill 的包名、域名和字段来自当前 `AssistStructure`；返回 Dataset 前仍需通过 interaction 中记录的规范化作用域。
- 进程死亡会丢失 process-local interaction。旧选择因此被拒绝并由 Android 重新发起查询；Passly 不从 extras 恢复授权。

返回秘密前必须同时满足：

1. 当前入口与设置仍然启用；
2. interaction 已授权且未结束；
3. 最终系统请求的包名/域名与 interaction 作用域一致；
4. entry ID 属于本次实际展示的候选集合；
5. Credential Manager 的 `allowedUserIds` 在展示与最终选择阶段均通过；
6. 解析结果包含可填充的非空密码。

## 一次认证与临时解锁

认证绑定 `AutofillInteractionId`，不是按包名授予的时间窗口，也不能跨 interaction 复用。

- Vault 锁定或设置要求填充前验证时，interaction 进入 `RequiresUnlock`。
- 成功认证后，同一个 interaction 进入 `Authorized`；解锁后重新发现和最终选择不会再次认证。
- 如果 Autofill 的认证动作实际解锁了 Vault，`AutofillUnlockLease` 记录所有借用该解锁的活跃 interaction。最后一个借用者结束后才执行 `AUTOFILL_REQUEST_FINISHED` 锁定。
- 每个 interaction 创建时安排 60 秒 process-local 超时。Android 选择器被放弃、Activity 未返回或调用方未清理状态时，超时会把 interaction 标记为 `Expired` 并释放临时解锁。
- 应用原本已经解锁时，Autofill 结束不会额外锁定用户会话。

## 数据最小化

- 候选阶段只使用 `AutofillCandidateSummary`，其中只有 `EntryListItem` 元数据和匹配原因，不允许出现 `Entry` 聚合、`EntrySecret` 或 credential secret 类型。
- `AutofillCredentialResolver` 只在最终选择后读取单个条目的密码字段。
- 密码不会进入候选列表、PendingIntent extras、SavedStateHandle 或 interaction snapshot。
- 传统 Autofill 的认证结果使用临时 Dataset 标志，减少系统长期缓存。

## 取消与清理

- 成功、失败、用户取消和协程取消都必须进入统一终态，并在不可取消的收尾区释放 lease。
- Credential Manager 的 `onClearCredentialStateRequest()` 会过期所有该来源的活跃 interaction，但不会删除密码或锁定原本由用户打开的 Vault。
- 对已经完成的 interaction 重复结束是幂等操作。

## 相关实现

- [AutofillInteractionStore](../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt)
- [AutofillInteractionTimeout](../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionTimeout.kt)
- [DiscoverAutofillCandidates](../../app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt)
- [AuthorizeAutofillInteraction](../../app/src/main/java/com/aozijx/passly/feature/autofill/core/AuthorizeAutofillInteraction.kt)
- [ResolveAutofillSelection](../../app/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt)
- [LegacyAutofillService](../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt)
- [ModernCredentialService](../../app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/ModernCredentialService.kt)

## 官方文档

- [Autofill framework](https://developer.android.com/identity/autofill)
- [Integrate Credential Manager with a credential provider](https://developer.android.com/identity/sign-in/credential-provider)
