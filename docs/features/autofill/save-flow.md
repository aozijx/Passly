# Autofill 保存语义

状态：当前实现；保存入口分别处理平台结果，已解锁时直接写入，锁定时密封排队并在完整解锁后导入。

## 当前行为

Legacy 与 Credential Manager 分别检查自己的 feature switch 和平台请求，再把类型化草稿交给共享
`SaveAutofillCredential`。它复用正式 `EntryDraft` 校验、物化和 Entry 写事务，不读取 Android 类型或平台开关。

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
