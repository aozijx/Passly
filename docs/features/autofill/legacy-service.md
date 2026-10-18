# Legacy Autofill Service

状态：当前实现（API 31+，代码核对：2026-10-18）。

`LegacyAutofillService` 解析 `AssistStructure`、处理取消信号并构造 Legacy `FillResponse`/`Dataset`。它不构造
Credential Manager response。

## Fill 和 SaveInfo

`LegacyResponseFactory` 从已分类角色创建 `SaveInfo`：密码 IDs 为 required，用户名 IDs 为 optional；没有密码
字段时不广告保存。SaveInfo 使用 password data type，有用户名时增加 username type，并设置
`FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE`。即使没有候选，也可以返回只含 SaveInfo 的 FillResponse。

Android 的该 flag 表示所有 required views 不可见时可触发保存，不等同于 Passly 自己持久化或排队凭据。
`FLAG_DELAY_SAVE` 只把 FillContext 传递到同一 task 的后续 Autofill 请求，不能让 Passly 在未来解锁时主动取回
一份挂起凭据，因此不是 sealed Inbox 的替代实现。

## 当前 onSaveRequest

服务从最新 `FillContext` 重新解析并分类，不查找或重开已结束的 fill interaction。最低草稿要求是非空包名和至少
一个已分类非空密码；用户名可空，page title 当前固定为 null。

- 已有完整 `Authenticated` 会话：验证草稿后直接写入。
- 否则：`LegacySaveDraftStore` 在内存暂存最多 8 条、60 秒、单次消费的草稿，返回随机 token；
  `SaveCallback.onSuccess(IntentSender)` 启动内部 `LegacySaveActivity`，后者建立独立 `LEGACY_SAVE` interaction，
  认证一次并写入。

token Intent 不包含用户名、密码、包名或域名。消费后的草稿只由 Activity-retained ViewModel 持有，进程死亡即
失败且不能误报已保存。配置变化可复用当前 ViewModel。成功、取消、失败、返回或协程取消只结束该 save interaction。

目标 Inbox 实现后，本节将改为 locked save 直接密封，并删除 `LegacySaveActivity`、plaintext store 和
`LEGACY_SAVE` interaction；这些组件当前仍在用，不能提前删除。

相关实现：

- [`LegacyAutofillService`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt)
- [`LegacySaveFlow`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlow.kt)
- [`LegacySaveDraftStore`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/runtime/LegacySaveDraftStore.kt)

平台依据：[Build autofill services](https://developer.android.com/identity/autofill/autofill-services)、
[SaveInfo](https://developer.android.com/reference/android/service/autofill/SaveInfo)。
