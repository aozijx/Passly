# Autofill 网页来源 claim 攻击与防护验收

**范围：** 归档 Legacy Autofill 网页来源 claim 的测试与设备验收证据。当前安全合同见[Autofill 安全](../../platform/security/autofill.md)；仍未闭合的 Passly provider 端到端验收由[安全扫描遗留问题 Task 6](../work/plans/2026-11-06-passly-security-scan-followups.md#task-6)跟踪。

## 已确认边界

Legacy Autofill 的 `AssistStructure.ViewNode.webDomain` 是平台请求中的来源 claim。系统权限、严格 host 解析与普通 Vault 解锁都不能证明调用方拥有该域名。当前实现拒绝歧义结构；对网页请求先显示不含候选的通用确认操作，再在确认后查询候选，并在用户选定条目后再次确认。交互绑定规范化 host、条目 ID 和认证 session generation；读取凭据前及平台交付前重新检查授权与交互 owner。该流程降低未经确认披露凭据的风险，但不等同于 DAL 或签名验证，也不证明任意宿主交错下均无法绕过。

## 验收证据

| 检查范围 | 证据 | 结论与限制 |
| --- | --- | --- |
| 结构 claim 解析 | `AutofillWebOriginResolverTest.originClaimsAndTruncationResolveAsExpected` | 覆盖同 host 规范化、多 host、有效与无效 claim 混合及遍历截断；没有网页 claim 的原生请求不因此拒绝。 |
| 候选发现 | `DiscoverAutofillCandidatesTest.websiteCandidateSearchUsesDomainWithoutBrowserPackageAssociation` | 无关联包声明可提供可命中网站候选；未确认前不查询，确认后仅按域名查询。 |
| 凭据选择与确认 | `ResolveAutofillSelectionTest.unverifiedLegacyWebSelectionCannotReadCredentialBeforeExplicitEntryConfirmation`、`webConsentFromAnEarlierVaultSessionCannotReadCredential`、`AutofillWebConfirmationTest` | 未确认条目或旧 session 确认不能触发 secret 读取；确认受同一 live interaction、scope 和候选集合约束。 |
| Legacy 首屏响应 | `LegacyResponseFactoryInstrumentedTest.unconfirmedWebRecipientReceivesOnlyGenericConfirmationAction` | 首屏只含通用确认操作，不公开候选标题或账户信息。 |
| 系统结构注入 | `AutofillOriginAttackInstrumentedTest.unassociatedTestPackageCanSupplyAnotherWebDomainInTheSystemStructure`（Pixel_8_Pro API 36） | 无 `asset_statements` 的测试 APK 使用自定义 `EditText.onProvideAutofillStructure()` 声明 `login.example.com`；系统将该 claim 交给捕获 Service，生产 `AutofillStructureParser` 解析后保留该 host，且字段结构无多 origin 歧义。测试在 `finally` 中恢复 `secure/autofill_service`。这证明来源 claim 可伪造，不证明 Passly provider 已完成端到端拦截验收。 |

以上单测当时随 `:feature:autofill:testDebugUnitTest` 152 项通过；`verifyArchitecture` 与 AndroidTest Kotlin 源集编译通过。`AutofillOriginAttackInstrumentedTest` 单独 1/1 通过；随后该设备上的完整 `:feature:autofill:connectedDebugAndroidTest` 为 13/13。此处为 2026-11-07 的历史结果，本次文档复核未重跑 Gradle。

## 未完成验收

- 使用同一无关联攻击方经 Passly provider 完成真实认证、候选展示与选择，验证来源确认、取消和 scope/owner 替换无法越过控制；同时覆盖合法关联 App 与可信浏览器路径。
- 在 API 31 与当前 Android API 复核平台 claim 及 provider 行为。2026-11-12 检查时 `adb devices -l` 无设备；`D:/AndroidSDK/system-images/android-31` 下只有空的 `default` 目录，当前 PATH 找不到 `avdmanager` 或 `sdkmanager`，因此本次无法启动 API 31 验收。

## 代码与测试位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 解析并拒绝歧义网页来源 claim | `AutofillStructureParser.parse()`、`AutofillWebOriginResolver` | [`AutofillStructureParser.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/parser/AutofillStructureParser.kt)、[`AutofillWebOriginResolver.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/parser/AutofillWebOriginResolver.kt)、[`AutofillWebOriginResolverTest.kt`](../../../feature/autofill/src/test/java/com/aozijx/passly/feature/autofill/legacy/service/parser/AutofillWebOriginResolverTest.kt) |
| 通过用户确认后发现网页候选 | `DiscoverAutofillCandidates.invoke()` | [`DiscoverAutofillCandidates.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt)、[`DiscoverAutofillCandidatesTest.kt`](../../../feature/autofill/src/test/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidatesTest.kt) |
| 校验确认与当前 session 后读取凭据 | `ResolveAutofillSelection.invoke()` | [`ResolveAutofillSelection.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt)、[`ResolveAutofillSelectionTest.kt`](../../../feature/autofill/src/test/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelectionTest.kt) |
| 通过系统 ViewStructure 注入伪造 host | `AutofillOriginAttackActivity`、`AutofillOriginAttackInstrumentedTest` | [`AutofillOriginAttackActivity.kt`](../../../feature/autofill/src/androidTest/java/com/aozijx/passly/feature/autofill/attack/AutofillOriginAttackActivity.kt)、[`AutofillOriginAttackInstrumentedTest.kt`](../../../feature/autofill/src/androidTest/java/com/aozijx/passly/feature/autofill/attack/AutofillOriginAttackInstrumentedTest.kt) |
