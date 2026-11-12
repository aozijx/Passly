# AppThemeSettingsViewModel owner 审查

**范围：** 追踪 `AppThemeSettingsViewModel` 的创建者、Theme consumer、设置源、错误重试与测试，判定它与 `AppShellViewModel` 的主题投影是否重复。

## Owner 与调用链

主 Activity 由 `PasslyApp()` 创建 `AppShellViewModel`；它聚合 Appearance、Interface 和 Security 设置，为 Shell 页面提供根状态，并把 `shellState.settings` 直接传给 `AppTheme`。Autofill 系统认证 Activity 则在 `AutofillAuthActivity.onCreate()` 组合 `PersistedAppTheme`，后者创建 Activity-scoped `AppThemeSettingsViewModel`，只组合 Appearance 与 Interface 设置并将其投影到同一个 `AppTheme` API。

两个 ViewModel 读取有重叠的 Repository，但归不同 Activity 与 `ViewModelStoreOwner`。主 App 根没有同时创建 `AppThemeSettingsViewModel`；Autofill 认证 Activity 不能依赖主 Activity ViewModel。移除该 VM 将使系统认证 Activity 失去持久主题读取/错误重试 owner；将两个 Activity 状态强行合并又会跨 owner 依赖。因此保留独立小投影，不把主题读取迁回 Shell 或新增主题转发层。正式边界见[包边界与主题入口](../../platform/architecture/package-boundaries.md)。

该 VM 使用共享 `RecoverableRead` 区分 `Loading / Ready / Failed`，只提供显式 Retry，无写入和其他业务工作流。`AppThemeSettingsViewModelTest` 覆盖损坏配置先显示失败并在 retry 后重新收集；没有 Autofill Activity/Compose 级主题失败交互测试。本次完成的是 VM 调用闭包分类，不宣称 Activity recreation 或真实 UI 已验证。

## 代码位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 为 Autofill Activity 提供 Activity-scoped 设置 projection | `PersistedAppTheme()`、`AppThemeSettingsViewModel` | [`PersistedAppTheme.kt`](../../../app/src/main/java/com/aozijx/passly/app/theme/PersistedAppTheme.kt)、[`AppThemeSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/app/theme/AppThemeSettingsViewModel.kt) |
| 安装持久主题于 Android 系统认证宿主 | `AutofillAuthActivity.onCreate()` | [`AutofillAuthActivity.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt) |
| 为主 Activity 提供根主题 settings | `PasslyApp()`、`AppShellViewModel.uiState.settings` | [`PasslyApp.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/PasslyApp.kt)、[`AppShellViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt) |
| 验证损坏读取失败态与显式重试 | `AppThemeSettingsViewModelTest` | [`AppThemeSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/app/theme/AppThemeSettingsViewModelTest.kt) |
