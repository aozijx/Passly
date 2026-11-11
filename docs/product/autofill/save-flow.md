# Autofill 保存语义

状态：当前实现；保存入口分别处理平台结果，已解锁时直接写入，锁定时密封排队并在完整解锁后导入。

## 当前行为

Legacy 与 Credential Manager 分别检查自己的 feature switch 和平台请求，再把类型化草稿交给共享
`SaveAutofillCredential`。它复用正式 `EntryDraft` 校验、物化和 Entry 写事务，不读取 Android 类型或平台开关。

Legacy `LegacyAutofillService.onSaveRequest(request, callback)` 以 `request.fillContexts` 调用
`AutofillStructureParser.parse(...)`，再用 `adapter.buildRequest(parsed)` 构造领域 `AutofillRequest`，并把它传给
`LegacySaveFlow.prepare(request)`。该流程先读取 `AutofillSettingsSource.autofillSettings.first().enabled`，拒绝
`ambiguousWebOrigin`，再调用 `AutofillSaveDraftExtractor.invoke(request)`。提取器按 `FieldMatchStrategy.match(request).roleMap`
选择密码和用户名：密码必须唯一（优先唯一聚焦值，否则唯一非空值）；用户名缺失或歧义时置空。有效草稿含规范前的
`packageName`、`domain`、`username` 和密码，`pageTitle` 固定为 `null`。

完整安全会话由 `LegacySaveFlow.persistImmediately(draft)` 调 `SaveAutofillCredential.invoke(draft)`；否则调用
`AutofillInboxQueue.enqueue(AutofillInboxSource.LEGACY_SERVICE, draft)`。保存/排队结果分别映射为
`SaveCallback.onSuccess()`；`Disabled`、`InvalidDraft`、`Failed` 映射为一次 `onFailure(message)`，由
`AtomicBoolean.compareAndSet(false, true)` 防止回调重复。取消先回调失败，再继续传播 `CancellationException`。

Credential Manager 的 `CredentialManagerFlow.createPassword(interactionId, scope, username, password)` 重新读取设置，要求
`enabled && credentialManagerEnabled`，并验证 interaction source 与规范化 scope。它以这些入参构造 `AutofillSaveDraft`
（`pageTitle = null`）。没有完整安全会话时，直接将草稿以 `AutofillInboxSource.CREDENTIAL_MANAGER` 入队并完成 interaction；
已有完整会话时走 `authorizeInteraction(interactionId)` 后调用同一 `SaveAutofillCredential.invoke(draft)`。持久化成功完成
interaction；拒绝、无效草稿、失败或取消走取消收尾。系统返回分别使用 Credential Manager 自己的 create 成功/失败合同，
不调用 Legacy `SaveCallback`。

已解锁会话直接写 Entry。锁定会话将草稿密封写入独立 Inbox，不要求 Autofill session 或弹出认证；完整 Vault
解锁后由 single-flight drainer 解密并导入。Android 的保存确认只表示 durable queue 已接收，不表示 Entry 已落库。

## 结果与持久化边界

```text
LegacySavePreparation
  -> Saved | Queued | InvalidDraft | Disabled | Failed

CredentialManagerCreateResult
  -> Saved | Queued | NotSaved | NotAuthorized
```

两条入口各自校验 feature switch 与平台请求。完整安全会话直接保存；否则将完整草稿密封落盘。`Queued` 表示 durable
accepted，不是已经出现在 Vault；只有解锁导入事务提交才是 `Imported/Saved`。稳定 Entry ID 由 draft ID 派生，
重复导入在 Entry 写入事务内识别，不增加 Room 表或改动 Schema 版本。

Legacy 对 Saved/Queued 调一次 `SaveCallback.onSuccess()`；失败调一次非敏感 `onFailure()`。Credential Manager
使用自己的 create success/failure，不复用 SaveCallback。两条路径都不得静默丢弃旧 pending、虚报成功或为了保存
弹认证。

加密、容量、重启恢复和幂等导入见 [Sealed Save Inbox](sealed-inbox.md)。

`SaveAutofillCredential.invoke(draft, inboxDraftId)` 先拒绝空密码，再由
`buildAutofillCredentialDraft(draft, applicationLabelResolver)` 验证并规范化网页域/包名，构造 `EntryType.LOGIN` 的
`EntryDraft`，写入标题、密码、可选用户名，以及精确域名或 application ID。它调用
`AutofillEntryCreation.createFromAutofill(entryDraft, inboxDraftId)`；成功映射为 `Saved`，Domain `AppResult.Failure`
映射为 `Failed(PERSISTENCE_FAILED)`。Inbox drain 时传入 `inboxDraftId`，普通直接保存时为 null。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 提取并校验平台保存草稿 | `AutofillSaveDraftExtractor.invoke()` | [`AutofillSaveDraftExtractor.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/AutofillSaveDraftExtractor.kt) |
| 写入或排队保存凭据 | `SaveAutofillCredential.invoke()`、`AutofillInboxQueue.enqueue()` | [`SaveAutofillCredential.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt)、[`AutofillInboxQueue.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillInboxQueue.kt) |
| 构造 Legacy 保存草稿并映射平台回调 | `LegacyAutofillService.onSaveRequest(request, callback)`、`LegacySaveFlow.prepare(request)`、`persistImmediately(draft)` | [`LegacyAutofillService.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt)、[`LegacySaveFlow.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlow.kt) |
| 创建 Credential Manager 草稿并完成 interaction | `CredentialManagerFlow.createPassword(interactionId, scope, username, password)` | [`CredentialManagerFlow.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt) |
| 将平台无关草稿转换为 Login `EntryDraft` 并持久化 | `SaveAutofillCredential.invoke(draft, inboxDraftId)`、`buildAutofillCredentialDraft(draft, applicationLabelResolver)`、`AutofillEntryCreation.createFromAutofill(entryDraft, inboxDraftId)` | [`SaveAutofillCredential.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt)、[`AutofillEntryCreation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillEntryCreation.kt) |
