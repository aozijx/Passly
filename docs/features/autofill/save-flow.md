# Autofill 保存语义

状态：过渡中；当前保存已实现，direct-or-queued 目标尚未实现。

## 当前行为

Legacy 与 Credential Manager 分别检查自己的 feature switch 和平台请求，再把类型化草稿交给共享
`SaveAutofillCredential`。它复用正式 `EntryDraft` 校验、物化和 Entry 写事务，不读取 Android 类型或平台开关。

当前已解锁会话直接保存；锁定保存需要相应平台 interaction 先认证。Android 的保存确认不是 Passly Vault 认证，
认证来自 `AuthenticationManager`。

## 已批准目标

```text
AcceptAutofillSave(draft, source)
  -> Saved(entryId)
  -> Queued(pendingId)
  -> InvalidDraft
  -> Disabled
  -> InboxUnavailable(reason)
  -> Failed(errorCode)
```

顺序固定为：校验来源开关和草稿；有 Autofill session write access 时直接保存；否则密封写入 Inbox；只有 Entry
事务提交或 Inbox 原子落盘成功后才完成平台结果。`Queued` 表示 durable accepted，不是已经出现在 Vault；只有解锁
导入事务提交才是 `Imported/Saved`。

Legacy 对 Saved/Queued 调一次 `SaveCallback.onSuccess()`；失败调一次非敏感 `onFailure()`。Credential Manager
使用自己的 create success/failure，不复用 SaveCallback。两条路径都不得静默丢弃旧 pending、虚报成功或为了保存
弹认证。

加密、容量、重启恢复和幂等导入见 [Sealed Save Inbox](sealed-inbox.md)。
