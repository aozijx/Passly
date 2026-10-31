# Credential Manager Provider

状态：当前实现（API 34+，代码核对：2026-10-31）。

Passly 当前发布 `android.credentials.TYPE_PASSWORD_CREDENTIAL`，支持 password begin-get/get、create 和
clear-state。暂不支持 privileged browser origin、Passkey、远程条目、sticky session 或按用户名 upsert。

## Get

`ModernCredentialService` 与 `CredentialBeginGetHandler` 只处理 Credential Manager 请求。调用方身份来自系统注入的
`callingAppInfo`；自定义 extras 只携带 interaction/entry 指针。锁定时返回一个 `AuthenticationAction`，成功后
用同一 interaction 重新发现；候选状态下不查询秘密。

最终选择通过 `PendingIntentHandler.retrieveProviderGetCredentialRequest()` 取得系统请求，重新验证调用方、
interaction scope、已展示 Entry 和 `allowedUserIds`，再读取单条密码。ViewModel 的可观察 UI state 只表示
`Loading` 或不可恢复状态；含系统返回 `Intent` 的完成结果经进程内缓冲的一次性事件交给 Activity，Route 只在
Activity 为 `STARTED` 时消费。该 Intent 仅作为 Android Credential Manager 必需的平台响应载体，不存入 StateFlow
或 SavedState。Activity 以单次 claim 交付结果，并在提交 `GetCredentialResponse` 的 callback 内连续调用
`setResult()` 与 `finish()`；成功后才清除 ViewModel 的 active interaction。随后 Autofill 交付器关闭 interaction
并释放 lease。它不创建 Legacy Dataset。

若平台 callback 抛错，Activity 释放本次 claim，取消 interaction 并返回 `RESULT_CANCELED`；销毁取消路径不会被失败的
claim 阻塞。平台 callback 已成功后，日志/trace 记录异常不会将已交付的凭据改报为失败，interaction 完成与 lease
收尾仍继续。配置重建期间尚未消费的事件留在 ViewModel 的进程内 Channel，重新进入 `STARTED` 后最多消费一次。

## Create

`CredentialBeginCreateHandler` 只接受 password create。Credential Manager 总开关与 Legacy “保存新登录”开关
彼此独立；两者只复用平台无关草稿校验和 `SaveAutofillCredential`。

Vault 已有完整安全会话时直接写入；否则将草稿密封写入 Autofill Inbox，不弹出认证。完整 Vault 解锁后由 Inbox drainer 导入。保存始终新增登录条目，不按用户名 upsert。

## PendingIntent 和清理

PendingIntent 是显式 mutable Activity intent，identity 包含 interaction ID。有效 response 和 Credential exception
使用 `RESULT_OK`；只有无法构造协议响应才取消。解锁返回候选是中间态，选择/create 是终态。

interaction 为 process-local、60 秒超时；认证或秘密解析中的操作会延后回收。`onClearCredentialStateRequest()`
只过期 Credential Manager interactions，不删除条目，也不锁定用户原本拥有的完整 Vault 会话。清理一旦启动，
即使 Service scope 或请求随后取消也会完成；取消时不再调用系统成功回调。普通 Service 销毁不会立即过期
这些 interaction，因为系统可能在 Service 重建后才触发已返回的 PendingIntent；无人继续的 interaction 由进程级
60 秒过期器回收。

平台依据：[Credential provider integration](https://developer.android.com/identity/sign-in/credential-provider)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Credential Manager 平台请求入口 | `ModernCredentialService.onBeginGetCredentialRequest()`、`onBeginCreateCredentialRequest()`、`onClearCredentialStateRequest()` | [`ModernCredentialService.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/ModernCredentialService.kt) |
| 凭据发现、选择、创建与取消编排 | `CredentialManagerFlow.beginGet()`、`selectPassword()`、`beginCreate()`、`createPassword()`、`cancel()` | [`CredentialManagerFlow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt) |
| 响应 UI state、一次性完成事件与活动交互清理 | `CredentialResponseViewModel.onAction()`、`cancelActiveInteraction()`、`CredentialResponseResultEvents` | [`CredentialResponseViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseViewModel.kt)、[`CredentialResponseResultEvents.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseResultEvents.kt) |
| Credential Manager 结果交付与 claim | `CredentialResponseActivity.finishWithResult()`、`ActivityResultClaim.commit()` | [`CredentialResponseActivity.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseActivity.kt)、[`ActivityResultClaim.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/ActivityResultClaim.kt) |
