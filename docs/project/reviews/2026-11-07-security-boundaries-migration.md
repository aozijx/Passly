# Security 模块边界迁移审查记录

**日期：** 2026-11-07

**范围：** 将受 Vault 会话控制的密钥、Envelope 与认证实现归到 `:security`，收窄 Core/Data 加密能力边界，并按运行职责整理认证包。

**状态：** 源码迁移与记录内的构建/单测门禁完成；没有运行设备级 instrumentation。

## 最终边界

- Core 保留通用 AES/KDF/内存原语及 `SensitiveFieldCryptor`、`AttachmentContentProtector` 等能力合同，不含
  `com.aozijx.passly.security.*` 实现。
- Security 实现并在 `SensitiveKeyModule` 绑定 Core 合同：`SecuritySensitiveFieldCryptor`、
  `AttachmentContentCrypto`、DEK/字段密钥生命周期及 `EnvelopeManager`。不再暴露旧 `decryptProvisioned` 路径。
- Data 生产源码只依赖 Core 加密合同；Data AndroidTest 使用 `TestCryptoCapabilities` 本地 fake，不依赖 Security 实现。
- 认证代码按 `authentication.flow`、`authentication.method.credential`、`authentication.method.biometric`、
  `authentication.host` 和 `session` 组织。App 跨模块仅使用 `AuthenticationHost`；认证、DEK 和会话实现类在 Security
  内部可见。
- Keystore 的日志加密与遥测散列仍归各自 Data/App owner，不随 Vault 密钥生命周期搬迁。

## 静态边界复核

- Core main 未发现 Security package/reference。
- Data main 未发现 `com.aozijx.passly.security.*` import。
- App production 对 Security 的 import 仅为 `AuthenticationHost`。
- `decryptProvisioned` 在 Core、Security、Data、App 与 Feature 生产/测试源中无剩余引用。
- `docs/platform/architecture/package-boundaries.md` 与 Security/认证文档已记录新 owner 和源码路径。

## 验收记录

原实施计划记录以下结果：`:buildSrc:test` 通过；`:security:testDebugUnitTest` 174 项/33 类、0 失败；
`:core:testDebugUnitTest` 71 项/18 类、0 失败；Data unit-test 与 AndroidTest 源码编译、App 主源码编译、
`verifyArchitecture` 通过（部分 compile task 为 UP-TO-DATE）；`git diff --check` 通过。没有设备级 instrumentation。
以上是实施计划中的执行记录，本次归档未重跑 Gradle。

更广的数据库授权线性化、已进入事务后的并发终态、Recovery/Autofill/reset 交错仍由[主维护 TODO Task 37](../work/plans/2026-10-30-passly-maintainability-todo.md#task-37)跟踪；本次模块搬迁测试不能替代这些行为验收。

## 代码位置

| 职责 | 当前 owner | 源码 |
| --- | --- | --- |
| 敏感字段 Core 合同 | `SensitiveFieldCryptor` | [`SensitiveFieldCryptor.kt`](../../../core/src/main/kotlin/com/aozijx/passly/core/crypto/SensitiveFieldCryptor.kt) |
| Security 加密实现及绑定 | `SecuritySensitiveFieldCryptor`、`SensitiveKeyModule` | [`SecuritySensitiveFieldCryptor.kt`](../../../security/src/main/java/com/aozijx/passly/security/dek/SecuritySensitiveFieldCryptor.kt)、[`SensitiveKeyModule.kt`](../../../security/src/main/java/com/aozijx/passly/security/di/SensitiveKeyModule.kt) |
| 认证流程与认证方式 | `DefaultAuthenticationManager`、credential/biometric executors | [`DefaultAuthenticationManager.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/flow/DefaultAuthenticationManager.kt) |
| Vault 会话状态转换 | `VaultSessionController` | [`VaultSessionController.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt) |
| App 公共认证宿主 | `AuthenticationHost` | [`AuthenticationHost.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/host/AuthenticationHost.kt) |
