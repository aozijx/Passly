# Credential Manager Provider

状态：当前实现（API 34+，代码核对：2026-10-18）。

Passly 当前发布 `android.credentials.TYPE_PASSWORD_CREDENTIAL`，支持 password begin-get/get、create 和
clear-state。暂不支持 privileged browser origin、Passkey、远程条目、sticky session 或按用户名 upsert。

## Get

`ModernCredentialService` 与 `CredentialBeginGetHandler` 只处理 Credential Manager 请求。调用方身份来自系统注入的
`callingAppInfo`；自定义 extras 只携带 interaction/entry 指针。锁定时返回一个 `AuthenticationAction`，成功后
用同一 interaction 重新发现；候选状态下不查询秘密。

最终选择通过 `PendingIntentHandler.retrieveProviderGetCredentialRequest()` 取得系统请求，重新验证调用方、
interaction scope、已展示 Entry 和 `allowedUserIds`，再读取单条密码。Activity 先发布
`GetCredentialResponse`，之后才封闭 lease 并完成 interaction。它不创建 Legacy Dataset。

## Create

`CredentialBeginCreateHandler` 只接受 password create。Credential Manager 总开关与 Legacy “保存新登录”开关
彼此独立；两者只复用平台无关草稿校验和 `SaveAutofillCredential`。

Vault 已有完整安全会话时直接写入；否则将草稿密封写入 Autofill Inbox，不弹出认证。完整 Vault 解锁后由 Inbox drainer 导入。保存始终新增登录条目，不按用户名 upsert。

## PendingIntent 和清理

PendingIntent 是显式 mutable Activity intent，identity 包含 interaction ID。有效 response 和 Credential exception
使用 `RESULT_OK`；只有无法构造协议响应才取消。解锁返回候选是中间态，选择/create 是终态。

interaction 为 process-local、60 秒超时；认证或秘密解析中的操作会延后回收。`onClearCredentialStateRequest()`
只过期 Credential Manager interactions，不删除条目，也不锁定用户原本拥有的完整 Vault 会话。

相关实现：

- [`ModernCredentialService`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/ModernCredentialService.kt)
- [`CredentialManagerFlow`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt)
- [`CredentialResponseViewModel`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseViewModel.kt)

平台依据：[Credential provider integration](https://developer.android.com/identity/sign-in/credential-provider)。
