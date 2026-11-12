# 认证提交后维护与取消恢复审查

## 当前合同

应用密码与生物识别策略在持久提交后，普通的 session reconciliation、availability refresh 或候选 cleanup 失败不能把已提交结果伪装成未提交。派生维护以 `pendingMaintenance` 表达并可重试；取消继续传播，fatal `Error` 不映射成普通失败。正式调用顺序与页面结果合同见[认证机制与会话](../../platform/security/authentication.md)。

## 已有底层覆盖

- `AppPasswordProvisioningCommitTest` 覆盖 durable save 前失败可重试、写后普通失败保持 committed Success、availability maintenance 标记、持久化后取消继续传播及 fatal `Error` 继续传播。
- `BiometricRotationCandidateTest` 覆盖未提交 candidate 的取消清理、原子提交后的迟到取消不删除 active candidate、提交后状态读取失败保留成功并请求 cleanup retry、删除失败保留 journal、旧 candidate 不清除新 journal，以及取消/fatal 的清理优先级。
- `BiometricRotationReconcilerTest` 覆盖 committed alias/journal 清理重试、PREPARED candidate 删除失败、已激活 candidate 保护、普通 store failure、取消/fatal 传播和 metadata 更新失败后的幂等重试。
- `AuthenticationAvailabilityRetryTest` 覆盖 transient refresh failure 的有界退避；`AppPasswordSettingsViewModelTest` 和 `SecuritySettingsViewModelTest` 覆盖 pending maintenance 到页面反馈的映射。

这些测试验证组件与 helper 的合同，不证明真实 `RotateBiometricKeyUseCase`、`DefaultAuthenticationMethodProvisioner`、durable Store、Authentication Host 和设置页在提交竞态下的联合结果。[`AppPasswordSettingsViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordSettingsViewModelTest.kt) 已覆盖请求缓冲清理、页面 owner/生命周期取消，以及提交前后取消时的页面状态和反馈；它使用 fake Provisioner 与 availability Flow，不能证明真实 Interactor/Provisioner/Store 的联合结果。本审查没有执行测试。

## 未关闭的 owner 验收

Task 59 保留三项：实际 provisioner/store 组合在绑定提交后失败时返回成功及 pending 状态；真实 Host/启动 owner 的轮换错误、取消、fatal、journal 与 lease 终态；真实 App Password Interactor/Provisioner/Store 在提交前后取消时的持久状态、页面结果与维护重试一致。输入缓冲清理和 ViewModel 本地取消边界已由现有设置页测试覆盖。

## 代码与测试位置

| 职责 | 类/函数与测试 | 源码 |
| --- | --- | --- |
| 处理应用密码持久提交结果和取消 | `runAppPasswordProvisioning()`、`DefaultAuthenticationMethodProvisioner.setAppPassword()` | [`AppPasswordProvisioningCommit.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/method/credential/AppPasswordProvisioningCommit.kt)、[`DefaultAuthenticationMethodProvisioner.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/method/credential/DefaultAuthenticationMethodProvisioner.kt) |
| 应用密码 durable commit 边界回归 | `AppPasswordProvisioningCommitTest` | [`AppPasswordProvisioningCommitTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/authentication/method/credential/AppPasswordProvisioningCommitTest.kt) |
| 处理轮换 candidate 和 journal | `withBiometricRotationCandidate()`、`BiometricRotationReconciler.reconcile()` | [`BiometricRotationCandidate.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/method/biometric/BiometricRotationCandidate.kt)、[`BiometricRotationReconciler.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/method/biometric/BiometricRotationReconciler.kt) |
| candidate 与 journal 清理恢复回归 | `BiometricRotationCandidateTest`、`BiometricRotationReconcilerTest` | [`BiometricRotationCandidateTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/authentication/method/biometric/BiometricRotationCandidateTest.kt)、[`BiometricRotationReconcilerTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/authentication/method/biometric/BiometricRotationReconcilerTest.kt) |
| 可用性刷新退避及页面维护提示 | `AuthenticationAvailabilityRetryTest`、`AppPasswordSettingsViewModelTest` | [`AuthenticationAvailabilityRetryTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/authentication/flow/AuthenticationAvailabilityRetryTest.kt)、[`AppPasswordSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordSettingsViewModelTest.kt) |
