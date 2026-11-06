# 安全凭据提交点与 fatal 错误传播交付记录

审查日期：2026-11-06。

状态：已完成。本文保留安全凭据 provision/Recovery Code commit 的实现与验证证据；当前行为以[统一认证与恢复码](../security/authentication.md)为准。

## 提交点与结果映射

`DefaultAuthenticationMethodProvisioner.setAppPassword(password)` 校验密码策略后接管 `CharArray` 为 `SecretChars`、生成本次 `salt`，再调用 `runAppPasswordProvisioning(store, salt, requestId, persist, reconcileSession)`。`persist` 闭包在 `kdfRunner.execute(secret)` 中派生 Argon2id key，经 `dekManager.rekeyWithKey(...)` 或 `initializeWithKey(...)` 写入 envelope；`reconcileSession` 在 Recovery Mode 下调用 `finishRecoveryPasswordProvisioning(seal, refreshAvailability)`，普通模式则刷新 availability 并发布认证状态。`rawKey`、`secret` 与 `salt` 分别在内层 `finally`、`SecretChars.close()` 和外层 `finally` 擦除。

`runAppPasswordProvisioning()` 将 envelope 写入作为 durable commit point：正常 persist/reconcile 返回 APP_PASSWORD Success；`CancellationException` 原样传播；普通 `Exception` 后在 `NonCancellable` 中以本次随机 salt 读取并核实 envelope，已提交仍返回 Success，未提交才返回 `SESSION_TRANSITION_FAILED`。它不捕获 fatal `Error`。`finishRecoveryPasswordProvisioning()` 先在 `NonCancellable` 完成 Recovery session seal，再刷新派生 availability；刷新普通异常不覆盖 envelope 已提交事实，取消传播，fatal `Error` 未被降级。

Recovery Code 生成路径由 `DefaultRecoveryCredentialGenerator.generate()` 持有 code、salt、派生 key 与 envelope 的临时资源；仅将完整草稿所有权转交 `SecureRecoveryCodeDraft` 后返回 `Ready`。普通 `Exception` 映射为生成失败，取消与 fatal `Error` 传播，未转交资源及 code 在 `finally` 清理。`SecureRecoveryCodeDraft.commit()` 以 `vaultBootstrapStore.save(copy)` 为提交点；save 抛出后在 `NonCancellable` 中读回并比较完整 envelope，`isCommitted` 暴露持久事实。提交后 availability refresh 的普通失败仍返回 RECOVERY_CODE Success；取消或 fatal 错误先关闭敏感草稿再传播原异常。

`RecoveryCodeSettingsViewModel.generate()` 和 `confirmAndEnable()` 只把普通异常转为页面失败；取消/fatal 路径先清理或按 `draft.isCommitted` 收敛 disclosure 与可用状态，再传播原异常。因此持久化已提交而派生状态刷新失败时，不会把已生效凭据呈现为允许盲目重试的未提交失败。

## 根因与验收证据

修复前，`DefaultAuthenticationMethodProvisioner`、Recovery draft/generator 和 `RecoveryCodeSettingsViewModel` 的若干边界把普通异常与 fatal `Throwable` 混在同一映射中。envelope 保存后若会话协调/availability refresh 抛错，调用方可能观察到普通失败或取消而无法得知凭据已提交；生成器还可能把 fatal 错误伪装成可恢复的 Recovery Code 失败。风险同时涉及 durable commit、页面状态、取消和敏感缓冲清理，不能只靠单层 `Error` 传播断言覆盖。

定向行为测试覆盖 app-password 提交前普通失败与保存后的协调失败、Recovery envelope save 后抛错与完整读回确认、availability refresh 的普通失败/取消/fatal、生成与 commit 的 `AssertionError` 传播、草稿明文关闭，以及 ViewModel 按 `isCommitted` 收敛页面状态。实现阶段的 RED/GREEN 与全量 gate 结果记载于原 Task 53；最终 App/Security/Domain 定向 JVM 测试、`verifyArchitecture`、格式门禁与 `git diff --check` 均通过。未进行实体设备 Keystore/数据库重开验收；本记录不将 JVM 测试视作设备级验证。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 协调应用密码 envelope 持久提交与 session 发布 | `DefaultAuthenticationMethodProvisioner.setAppPassword()`、`runAppPasswordProvisioning()` | [`DefaultAuthenticationMethodProvisioner.kt`](../../security/src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationMethodProvisioner.kt)、[`AppPasswordProvisioningCommit.kt`](../../security/src/main/java/com/aozijx/passly/security/authentication/AppPasswordProvisioningCommit.kt) |
| 完成 Recovery Mode 退出后的 session seal 与 availability 刷新 | `finishRecoveryPasswordProvisioning(seal, refreshAvailability)` | [`RecoverySecurityLifecycle.kt`](../../security/src/main/java/com/aozijx/passly/security/authentication/RecoverySecurityLifecycle.kt) |
| 生成敏感草稿并按 envelope 保存事实提交 | `DefaultRecoveryCredentialGenerator.generate()`、`SecureRecoveryCodeDraft.commit()` | [`DefaultRecoveryCodeDraftFactory.kt`](../../security/src/main/java/com/aozijx/passly/security/authentication/DefaultRecoveryCodeDraftFactory.kt) |
| 映射草稿生成/确认的页面终态并处理提交后错误 | `RecoveryCodeSettingsViewModel.generate()`、`confirmAndEnable()` | [`RecoveryCodeSettingsViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt) |
