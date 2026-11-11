# OTP Runtime owner 与缓存失效审查

## 当前实现

`OtpRuntimeModule` 在 `SingletonComponent` 提供一个共享 `OtpCodeRuntime`。Vault 图级 `VaultViewModel` 为列表与 Detail 持有按条目 ID 管理的 lease；页面只请求激活，不各自关闭共享 Runtime 的条目状态。Runtime 使用 entry/session epoch 丢弃过期读取与验证码结果。OTP 编辑成功后调用 `entryChanged(entryId)`；覆盖导入在数据库提交后调用 `vaultReplaced()`。锁定时清理验证码和种子缓存，保留仍有效的 lease，解锁后再加载。当前合同详见[OTP 运行时与复制](../../product/vault/otp-runtime.md)。

## 历史行为验收

2026-11-07 的主维护记录显示，`VaultViewModelTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` 增加了对生产 `WhileSubscribed` OTP 状态流的收集，以确保测试先观察到 Ready 再验证离页行为；历史记录注明修正后强制全量 App JVM 单测通过。当前测试仍保留状态 collector，并断言离页后不写剪贴板，同时 Vault 图 owner 保留 OTP 状态。

同一轮的 `VaultViewModelTest` 3 项与 `CopyEntryFieldUseCaseTest` 7 项曾通过，覆盖授权等待期间离页不交付，以及活动 owner 下字段复制成功。该证据针对通用复制交付 owner，不证明 OTP 编辑返回、列表与 Detail 导航、锁定恢复或 Route 生命周期组合。

## 仍待验证

Task 27 只保留真实 Compose Route/NavHost/宿主回归：编辑成功后 Detail 显示与复制取新配置，取消/失败不触发保存成功刷新；owner 替换、锁定、离页与恢复时不激活无主条目，也不接受迟到结果。现有 Runtime 与 ViewModel 测试及 `VaultOtpRequestEffectTest` 源码不足以证明这些真实导航交互；该 AndroidTest 尚未由本审查执行。

## 相关测试维护记录

当前 Domain OTP 测试位于 `entry/otp/generation`，覆盖 RFC 6238 SHA1/SHA256/SHA512 向量、HOTP counter 边界、Steam 样例、Base32/Base64 编解码与 typed failure；URI 的 Base64 往返语义由 `OtpAuthUriCodecTest` 验证。运行时测试分别保留按需复制与活动 lease、会话/条目 epoch、瞬态读取退避、HOTP 事务及时间窗边界等不同合同。重复的单次读取恢复与多 lease 清理断言曾合并进覆盖面更完整的用例；自定义周期复制测试现用生成时间戳区分当前码和下一窗码。

旧 Detail 专属 OTP 刷新测试曾依赖已删除的 `refreshOtpConfiguration` 页面路径，现由共享 Vault graph Runtime 状态和 `entryChanged` 配置失效路径取代；Detail 的保留工作流测试继续覆盖修订恢复、条目替换及敏感状态清理。以上为此前测试维护记录的归档；本审查仅复核当前代码位置与用例仍存在，未重新执行这些测试。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 创建单例 Runtime 并绑定应用级信号 | `OtpRuntimeModule.provideOtpCodeRuntime()` | [`OtpRuntimeModule.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpRuntimeModule.kt) |
| 持有列表与 Detail 共用的条目 lease | `VaultViewModel.retainOtp(entryId)` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) |
| OTP 保存成功后失效旧配置 | `AddOtpViewModel` 保存成功分支、`OtpCodeRuntime.entryChanged(entryId)` | [`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt)、[`OtpCodeRuntime.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt) |
| 覆盖导入提交后失效 Vault 快照 | `AppBackupSnapshotGateway` 的 `onCommitted` | [`AppBackupSnapshotGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt) |
| 离页复制请求的交付 owner 回归 | `VaultViewModelTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` | [`VaultViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModelTest.kt) |
| OTP 算法向量、编码和计数器回归 | `OtpGeneratorTest`、`OtpSecretCodecTest` | [`OtpGeneratorTest.kt`](../../../domain/src/test/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpGeneratorTest.kt)、[`OtpSecretCodecTest.kt`](../../../domain/src/test/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpSecretCodecTest.kt) |
| Runtime lease、失效、复制和重试回归 | `OtpCodeRuntimeTest` | [`OtpCodeRuntimeTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntimeTest.kt) |
