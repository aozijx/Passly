# 设置功能总览

状态：当前实现。

设置功能提供偏好读取/写入入口、系统权限状态与安全选项。设置页面负责呈现并启动相应工作流；偏好持久化、系统授权、认证、备份和诊断能力仍由各自的存储或功能 owner 管理。

## 功能路径

| 工作流 | 权威专题 |
| --- | --- |
| 应用外观、主题和界面样式 | [Vault 卡片样式](../vault/card-styles.md)；通用持久化映射见[设置存储](../../platform/data/settings-storage.md) |
| 通知权限申请与通知偏好 | [通知设置](notification-settings.md) |
| 系统权限状态和权限管理入口 | [权限管理](permission-management.md) |
| Autofill 服务偏好与平台启用状态 | [Autofill 功能总览](../autofill/overview.md)及其[共享流程](../autofill/fill-flow.md) |
| 密码、安全和隐私选项 | [安全总览](../../platform/security/overview.md)、[认证与恢复码](../../platform/security/authentication.md) |
| 备份与数据库操作 | [Backup 工作流总览](../backup/backup-workflows.md)、[数据库](../../platform/data/database.md) |
| 诊断日志设置、查看与导出 | [诊断日志总览](../diagnostics/overview.md) |

## 状态和所有权

- 设置屏幕由 Route/ViewModel 按屏幕状态和系统生命周期组合；系统权限必须由宿主在 Android 允许的时机请求，状态刷新由相应页面 owner 管理。
- 偏好通过 Domain settings port 被读取/更新，Data 的 Proto DataStore adapter 是持久设置事实来源；UI 中的开关/下拉选项是可观察投影或编辑值，不是第二份长期真相。
- Android API、权限、文件选择器与 Activity Result 留在 Route/Activity/Service；ViewModel 不持有 Activity、Context、NavController 或资源对象。
- 安全设置可能触发认证、密钥或会话流程，由 Security owner 执行；设置页不得把展示状态当作授权事实。
- Backup 与诊断导出有独立操作生命周期和清理合同；设置页面可宿主导航和系统选择器，但不接管归档、数据库或日志存储实现。

## 本地化文案

设置页面专属标题、选项、对话框与反馈按中英文分别维护在 `strings_settings.xml`，并按 Settings 导航分组排列；页面所属的对话框和错误文案留在对应分组。共享认证提示、通用操作标签和其他功能专属文案保留在各自资源 owner。中英文资源保持相同 key 顺序和对应占位符，避免搬移设置文案时破坏跨页面复用或格式化参数。

平台级设置存储合同见[Data 设置存储](../../platform/data/settings-storage.md)；Route/ViewModel 与生命周期要求见[层间契约与通信](../../platform/architecture/layer-interactions.md)和[UI 宿主、导航与命名](../../platform/architecture/ui-shell-and-naming.md)。

## 代码位置

| 职责 | 代表类型 | 源码 |
| --- | --- | --- |
| Settings 主导航宿主 | `SettingsRoute`、`SettingsDetailRoute` | [`SettingsRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsRoute.kt)、[`SettingsDetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsDetailRoute.kt) |
| 权限管理状态 owner | `PermissionManagementViewModel` | [`PermissionManagementViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/permissions/PermissionManagementViewModel.kt) |
| 持久设置实现 | `ProtoAppPreferencesStore` | [`ProtoAppPreferencesStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoAppPreferencesStore.kt) |
| 中英文设置文案 | `strings_settings.xml` | [`strings_settings.xml`](../../../app/src/main/res/values/strings_settings.xml)、[`strings_settings.xml`](../../../app/src/main/res/values-en/strings_settings.xml) |
