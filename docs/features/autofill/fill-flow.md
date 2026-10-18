# Autofill 共享填充流程

状态：当前实现（代码核对：2026-10-18）。

## 发现与授权

`DiscoverAutofillCandidates` 原子读取当前设置、规范化目标作用域、创建/恢复 interaction，并只查询
`AutofillCandidateSummary` 元数据。候选阶段不读取 Entry 聚合、密码或 OTP secret。

Legacy 可用 `HeuristicMatchStrategy` 识别非标准表单；Credential Manager 使用自己的严格 option/request 解析。
平台 adapter 不直接依赖 Entry repository 或认证实现。

需要解锁或设置要求每次验证时，`AuthorizeAutofillInteraction` 以 `AuthenticationPurpose.AUTOFILL` 授权同一个
interaction。并发授权合并为一次认证；成功后重新发现或最终选择不再次认证。interaction 绑定来源、规范化作用域和
实际展示过的 Entry IDs，不是按包名授予的时间窗口。

## 解析秘密

`ResolveAutofillSelection` 依次验证：入口仍启用、interaction 存在且已授权、最终调用方作用域相同、Entry ID
确实展示过、Credential Manager allowed user 仍通过。之后 `AutofillCredentialResolver` 只读取选中条目的密码。
请求需要 OTP 时，才沿 `OTP_FOR` 关系读取独立加密 secret 并生成验证码。

## 结果与关闭顺序

平台 Activity/Service 先发布自己的成功结果；`AutofillResultDelivery` 确认成功后立即阻止本轮 lease 的新借用，
再在不可取消收尾中调用 `CompleteAutofillSelection`。首次终态转换记录使用活动并释放 lease；重复回调幂等。

这保证 Android 收到结果以前不会关库，同时填充完成后再次点击输入框也不能复用上一轮临时授权。Android 没有确认
字段实际写入的回调时，日志不能伪造 `field_filled`。

## 临时会话

`AutofillAuthenticated` 只允许候选、单条秘密解析、使用记录和已确认凭据保存，不挂载普通 Vault。多个已授权
interaction 可以借用同一个临时数据库会话，但不共享候选或选择权限。

每个 interaction 有 60 秒 process-local 超时。进程死亡后 extras 不能恢复授权；系统必须重新发起请求。主 App
完成普通解锁后会话提升为 `Authenticated`，迟到的 Autofill 释放不得锁掉它。

相关实现：

- [`DiscoverAutofillCandidates`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt)
- [`ResolveAutofillSelection`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt)
- [`AutofillResultDelivery`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt)
- [`AutofillInteractionStore`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt)
