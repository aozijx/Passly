# Autofill 宿主与字段交付验收

**范围：** 归档 Autofill 统一宿主、结果 claim 与 API 36 字段交付的已完成证据。未完成范围由[活动后续清单 Task 21](../work/plans/2026-11-07-passly-autofill-followups.md#task-21)跟踪；产品设置快照策略由 [Task 82](../work/plans/2026-11-07-passly-autofill-followups.md#task-82)跟踪。

| 验收证据 | 结果与边界 |
| --- | --- |
| `AutofillResultDeliveryTest` | 注入平台交付后的 fatal trace capture `AssertionError`；验证平台 callback 恰好一次、原错误传播、interaction 完成和 Autofill lease 释放。Feature Autofill 单测 151 项通过。 |
| `AutofillActivityResultClaimTest` | 显式 platform commit marker 执行后，即使 callback 抛 fatal `Error`，claim 仍保持终态，取消结果不能重新认领。之后 `:feature:autofill:testDebugUnitTest` 152 项通过，`verifyArchitecture` 与 Feature AndroidTest 源集编译通过。此单测不证明真实 Activity 的部分副作用恢复。 |
| `AndroidAutofillLaunchTargetTest`（Pixel_8_Pro API 36） | Accessibility entry 仅创建对应模式 ViewModel；无 capture 时 Activity + Feature entry 完成后宿主销毁一次；该仪器类 3/3 通过。未覆盖其他 mode 的 teardown/lock 互斥。 |
| API 36 Legacy 实机交付 | 锁定态显示认证入口；认证后出现已匹配候选，选择后目标页面收到 username/password 字段值。系统设置项在验收后恢复，临时 Debug Vault 数据已清理。请求没有 InlinePresentation support，Android 使用系统下拉候选，因此该次验收不证明 Inline 专属 UI。 |
| 构建/架构 | `:app:compileDebugKotlin`、`:feature:autofill:compileDebugKotlin` 和 `verifyArchitecture` 通过；当时没有运行额外单测任务。 |

## 2026-11-12 AccessibilityFillSessionStore owner review

`AccessibilityFillSessionStore` 是 `:feature:autofill` 进程内单例，独占 `Empty / Captured / Pending` 单槽状态与 `AccessibilityNodeInfo` 引用。宿主 Service 绑定/解绑弱引用；用户显式触发 capture 后保存受限节点元数据和 30 秒有效期；选择后的角色映射、结果 payload 与 receipt 最多暂存 10 秒。token、package/window 与时限校验阻止旧请求认领，claim 在交付前清空槽位，并在填充时重新核对目标与节点能力。

本轮未发现可安全合并的重复状态 owner；现有测试未直接覆盖 Store 的完整状态合同，因此不把代码审查当作测试证据。capture 超时仅清除捕获槽，pending 交付超时通过服务回调取消 interaction。Accessibility finish/destroy 与 session lock 互斥阻塞交错仍由[Task 21](../work/plans/2026-11-07-passly-autofill-followups.md#task-21)验收。

仍需验收 Legacy Activity 配置重建、callback 部分提交后的失败/重试、Accessibility destroy 与 session lock 交错、Credential Manager 的取消/交付/宿主替换，以及 BottomSheet 和 InlinePresentation 专属字段写入。`result.delivered` 仅表示平台收到认证结果，不能代替目标控件值检查。当前生产合同见 [Autofill 共享流程](../../product/autofill/fill-flow.md)、[Legacy Service](../../product/autofill/legacy-service.md)、[Credential Manager](../../product/autofill/credential-manager.md)和[验证矩阵](../../product/autofill/verification.md)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 解析 Autofill 入口并分派到模式专属宿主 | `AutofillFeatureEntry`、`LegacyAutofillEntry`、`AccessibilityAutofillEntry`、`CredentialAutofillEntry` | [`AutofillFeatureEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/AutofillFeatureEntry.kt)、[`LegacyAutofillEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/LegacyAutofillEntry.kt)、[`AccessibilityAutofillEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityAutofillEntry.kt)、[`CredentialAutofillEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialAutofillEntry.kt) |
| 认证后完成 Legacy/Accessibility 平台结果交付 | `AutofillResultDelivery.commitResult(...)` | [`AutofillResultDelivery.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
| 在 Activity result callback 执行处提交 claim | `AutofillActivityResultClaim.commit(deliverToPlatform)` | [`AutofillActivityResultClaim.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/result/AutofillActivityResultClaim.kt) |
| 构造 Legacy 候选及锁定响应 | `LegacyResponseFactory.buildUnlockResponse(...)`、`buildDataset(...)` | [`LegacyResponseFactory.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt) |
