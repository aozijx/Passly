# Autofill Feature 模块实施记录

**状态：模块切换已完成；不是待执行的抽取计划。** 当前边界与源码已复核；剩余交付/宿主验收统一见[Autofill 后续 Task 21](2026-11-07-passly-autofill-followups.md#task-21)。

## 已完成边界

- `:feature:autofill` 拥有 Legacy/Accessibility/Credential 服务、请求解析、候选/授权用例、Feature ViewModel/Route、结果构造、交付及 Feature bindings。
- App 拥有 AutofillAuthActivity、平台 launch/status 适配和最终 Manifest/Hilt 组合；Activity 只接收 Feature 的 completion 合同并执行系统回调。
- Security 在共享 AuthenticationHost 内拥有认证 UI、host registry 与认证生命周期；Feature 不反向依赖 App、Data 或 Security。
- App 的 Feature 导入限于 launch contract/target、host entry/presentation/completion API；解析结果 DTO、匹配 qualifier 与实现适配器为模块内部。
- Autofill 策略由 Domain 专属 Source/Repository 提供并绑定同一 Proto store；系统选中服务的观察留在 App。旧 OTP、保存提示、未匹配回退和可配候选数设置已删除，Proto 旧编号/名称保留 reserved。

## 已有验证记录的范围

原实施阶段记录了 Feature 单测/AndroidTest 编译、App 编译/Manifest merge 和架构门禁通过。2026-11-07 边界复核移除了 App 对 `androidx.autofill` 的重复依赖：App Activity 只使用 Android SDK 的 Autofill exclusion flag，该 AndroidX 库仅由 Feature 源码使用。`:feature:autofill:testDebugUnitTest` 的 dry-run task graph 不包含 App 或 Security。

本轮 `:feature:autofill:testDebugUnitTest :app:compileDebugKotlin verifyArchitecture` 与 `:app:assembleDebug` 均通过；合并 Manifest 含一个 App-owned `AutofillAuthActivity` 和 Feature 的 Legacy、Accessibility、Credential 三个服务。Android 12 上的 Legacy response-factory 仪器套件历史记录为 9 项/0 失败；它验证协议构造，不证明真实目标字段填值。

**2026-11-07 独立边界复验：** 单独运行 `:feature:autofill:testDebugUnitTest`，任务图只包含 Core、Domain、Runtime Session 与 Autofill Feature，没有 App 或 Security 任务；148 项测试通过。`:app:compileDebugKotlin`、`verifyArchitecture`、`:feature:autofill:compileDebugAndroidTestKotlin`、`:app:compileDebugAndroidTestKotlin` 和 `:app:processDebugManifest` 通过。AndroidTest 编译发现旧用例读取 `FillResponse.datasets`（当前 SDK 不公开该属性）；断言收敛为当前结果合同中的 `summary.datasetCount` 与 response 非空。该编译结果不代表仪器测试已在设备执行。

不再保留重复的 AVD 凭据填充任务、固定设备/测试条目名称、数据清除后的 setup 前提或临时工作区指令。宿主重建、停止/销毁、回调异常、真实字段填值和 lease 收尾统一由后续 Task 21 验收。

原[模块设计](../specs/2026-11-06-autofill-feature-module-design.md)保留决策依据；当前实现合同以[包边界](../../architecture/package-boundaries.md)、[Legacy 流程](../../features/autofill/legacy-service.md)及源码为准。
