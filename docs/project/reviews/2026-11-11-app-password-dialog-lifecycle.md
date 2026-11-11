# App Password 对话框生命周期审查

## 当前合同

`SecurityRoute` 在组合中通过 `rememberAppPasswordDialogCoordinator()` 持有对话框协调器，并注册 `AppPasswordDialogCoordinatorOwner`；owner 的 `DisposableEffect` 在 Route 离开组合时调用 `clear()`，关闭活动对话框并清理 Coordinator 持有的草稿数组。新 Route 建立新的空 Coordinator。配置重建按同一 Route 销毁/重建策略清空草稿，不恢复密码输入。认证合同见[认证与恢复码](../../platform/security/authentication.md)。

Coordinator/ViewModel JVM 测试分别覆盖关闭后迟到输入不能恢复草稿、已退出页面不接收迟到授权/操作结果及请求缓冲清理。`AppPasswordDialogCoordinatorLifecycleTest` 的 Compose 用例只切换 Route 组合开关，断言旧 Coordinator 草稿清空、重新组合使用新 Coordinator 且从空状态开始；它没有触发真实 Activity recreation。

## 尚未验收

当前无连接的 Android 设备/AVD，仪器测试尚未运行。仍需执行该 Compose 测试并补真实配置重建验收，记录设备/API。该缺口由主维护 TODO Task 29 跟踪；现有 JVM owner 测试不能代替 Android 宿主生命周期验证。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| Security 页面持有对话框 owner | `SecurityRoute` | [`SecurityRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/core/SecurityRoute.kt) |
| 记住 Coordinator 并在 dispose 时清除 | `rememberAppPasswordDialogCoordinator()`、`AppPasswordDialogCoordinatorOwner()` | [`AppPasswordDialogCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinator.kt) |
| Route 移除后清空并重新组合为空的 Compose 回归 | `route disposal clears old coordinator and recreated route starts empty` | [`AppPasswordDialogCoordinatorLifecycleTest.kt`](../../../app/src/androidTest/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinatorLifecycleTest.kt) |
| 迟到密码输入/结果 owner 回归 | `AppPasswordDialogCoordinatorTest`、`AppPasswordSettingsViewModelTest` | [`AppPasswordDialogCoordinatorTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinatorTest.kt)、[`AppPasswordSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordSettingsViewModelTest.kt) |
