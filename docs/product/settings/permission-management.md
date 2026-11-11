# 权限管理

状态：当前实现（代码核对：2026-11-06）。

权限管理页是系统访问能力的统一查看入口，不建立新的权限真值存储。Runtime permission 通过共享 `PermissionRequestHost` 查询；SAF 目录能力读取持久化 URI grant；Manifest capability 只展示说明，不伪装成可切换权限。

## 页面状态与刷新

`PermissionsRoute()` 收集 `PermissionManagementViewModel.uiState`。初次组成、权限请求结果返回和每次恢复前台时都调用 Route 内 `refreshRuntimeAccess()`，它以 `RuntimePermission.entries.associateWith(permissionHost::access)` 重读每项状态，再将 Map 传给 `viewModel.runtimeAccessUpdated(access)`；ViewModel 复制 Map 后调用 `publishState()` 和 `PermissionManagementUiMapper.map(runtimeAccess, directoryStatus)`。页面始终显示清单中的每个 Runtime permission，不按授权状态过滤条目。

Route 的 `LifecycleResumeEffect(permissionHost, viewModel)` 在恢复时同时调用 `refreshRuntimeAccess()` 与 `viewModel.refreshDirectoryAccess()`。后者若目录 Flow 尚未 Ready 就触发 `directoryRead.retry()`；已 Ready 时在 `viewModelScope` 的 IO 检查中捕获当前 URI，调用 `directoryStatusReader.inspect(uri)`，只有检查结束时 URI 仍相同才发布状态，避免旧目录检查覆盖刚更新的 URI。

Camera 与 Notifications 始终显示。`REQUESTABLE` 项点击后请求权限；已授权、需转到系统设置或当前版本不适用的项点击后打开应用系统设置。系统总开关、通知渠道需在通知设置页单独检查。备份文件夹行只显示授权状态，不再拼接所选目录名；点击只导航到唯一负责选择/更换/清除的备份设置页。Network、Vibration、Biometric 和 Package visibility 只显示 Manifest capability。

## 请求与设置导航

所有 Runtime permission 行都显示当前状态。未授权且可请求时点击“授权”发起系统请求；已授权时点击“管理”打开应用详情设置。永久拒绝和 API 不适用的项同样保留，并通过“管理”进入系统设置。返回页面后会重新读取状态。ViewModel 不持久化权限结果，也不把权限 Map 写进设置 Repository。

SAF grant 状态不是传统 Runtime permission。备份目录重新选择、重新授权、清除与测试写入由 Backup 设置页面处理；权限管理页只展示 `PermissionManagementUiModel` 并发送语义动作。这样保留设置 URI 与操作系统 grant 的同一 owner。

[`PermissionManagementViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/permissions/PermissionManagementViewModelTest.kt) 覆盖目录 Flow 失败的可见状态与重新订阅；共享权限申请租约和迟到结果由 [`PermissionRequestSessionTest`](../../../app/src/test/java/com/aozijx/passly/app/platform/permission/PermissionRequestSessionTest.kt) 覆盖。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 页面状态与权限快照 | `PermissionManagementViewModel.runtimeAccessUpdated(access)`、`refreshDirectoryAccess()`、`retryBackupDirectoryRead()` | [`PermissionManagementViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/permissions/PermissionManagementViewModel.kt) |
| 平台 permission 状态读取与请求 | `PermissionsRoute()`、`permissionHost.access(permission)`、`permissionHost.request(permission)` | [`PermissionsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/core/PermissionsRoute.kt)、[`PermissionRequestHost.kt`](../../../app/src/main/java/com/aozijx/passly/app/platform/permission/PermissionRequestHost.kt) |
| 持久 SAF 目录 grant 语义 | `BackupDirectoryGrantInspector` | [`BackupDirectoryGrantInspector.kt`](../../../app/src/main/java/com/aozijx/passly/app/backup/BackupDirectoryGrantInspector.kt) |
