# 认证 KDF 参数首次接入审查

## 当前合同

认证 Argon2id 参数首次按设备基线选择并持久化；主密码与恢复码复用该参数，解锁读取既有配置，加密备份使用独立参数。具体配置选择、DataStore first-writer-wins、AAD 绑定及参数公开性以[密钥管理专题](../../platform/security/key-management.md#参数与清理的已知边界)为当前实现合同，本记录只归档本次扫描计划中的验证结果与缺口。

## 已有验证记录

下列结果来自安全计划记录的历史验证，不表示本轮重新执行：

- `:domain:test` 187 项、`:core:testDebugUnitTest` 80 项、`:security:testDebugUnitTest` 149 项和 `:data:testDebugUnitTest` 曾通过。
- `:core:ktlintCheck`、`:security:ktlintCheck`、`:data:ktlintCheck`、`verifyArchitecture`、`:app:processDebugResources` 和 `:app:compileDebugKotlin` 曾通过。
- App 单测编译当时被并行 Autofill 迁移期间 `RecoveryModeBoundaryTest` 访问 `internal StartAutofillInteraction` 阻断；全仓 KtLint 也被其他工作流文件阻断。上述失败不证明当前主线仍有相同问题。

## 未完成验收

真实低内存设备的首次校准耗时和 128 MiB 资源压力未验证。现有证据也未覆盖 Proto DataStore 字段 10 的真实写入、进程重启读取和并发首次写入；参数缺失、篡改、超限在生产派生入口前的完整拒绝，以及 durable commit 前后失败与敏感缓冲清理仍需补齐。未完成范围由[安全计划 Task 9](../work/plans/2026-11-06-passly-security-scan-followups.md#task-9)维护。

本次只迁移文档证据，没有运行构建、测试或设备验收。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 选择并缓存本设备认证 KDF 参数 | `DeviceKdfConfiguration.selected()`、`calibrate()`、`DeviceKdfPolicy.select()` | [`DeviceKdfConfiguration.kt`](../../../security/src/main/java/com/aozijx/passly/security/authentication/method/credential/DeviceKdfConfiguration.kt) |
| 认证参数端口及稳定值域 | `AuthenticationKdfParameters`、`AuthenticationKdfConfigurationStore` | [`AuthenticationKdfParameters.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/access/model/AuthenticationKdfParameters.kt)、[`AuthenticationKdfConfigurationStore.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/access/port/AuthenticationKdfConfigurationStore.kt) |
| Proto DataStore 持久化 | `ProtoVaultBootstrapStore.toProto()` / `toDomain()` | [`ProtoVaultBootstrapStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/datastore/ProtoVaultBootstrapStore.kt)、[`bootstrap.proto`](../../../data/src/main/proto/bootstrap.proto) |
