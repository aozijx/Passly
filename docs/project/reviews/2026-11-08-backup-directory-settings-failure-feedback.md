# Backup 目录设置失败处理审查

## 当前合同

`BackupDirectorySettingsInteractor` 串行执行目录选择和清除。选择新目录时先取得系统 grant 并持久化新 URI，再回收旧 grant；持久化失败后会在不可取消区重读当前 URI，仅当确认新 URI 未被引用时才回收新 grant。清除时同样在失败后核实持久值，只有确认旧 URI 已不再被引用才释放旧 grant。提交后的普通授权回收异常记录日志，不把已提交设置报告为失败；取消和 fatal `Error` 仍按原类型传播。

`DataManagementSettingsViewModel` 只对活动页面 owner 发出结果。普通异常被记录并映射为失败 effect，取消继续传播；清除成功才关闭确认框，失败保留对话框供重试，清除进行时禁用重复操作。正式行为说明见[快照、SAF 目录与目标 URI](../../product/backup/storage-and-permissions.md#saf-目录和指定目标)。

## 验收证据

2026-11-08 的维护记录报告：`BackupDirectorySettingsInteractorTest` 16 项、`DataManagementSettingsViewModelTest` 5 项通过；完整 App 单测汇总 191 份报告、1064 项、0 failures/errors；`:app:compileDebugAndroidTestKotlin`、`verifyArchitecture` 和 `git diff --check` 通过。本次文档整理没有重跑这些命令。`BackupDirectoryClearDialogTest` 只验证忙碌态禁用及用户确认回调，尚未在连接设备上执行；这项设备验收仍由主维护 TODO Task 40 跟踪。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| SAF grant 提交、失败补偿与清除 | `BackupDirectorySettingsInteractor.select()`、`clear()` | [`BackupDirectorySettingsInteractor.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/backup/BackupDirectorySettingsInteractor.kt) |
| 页面 owner、失败 effect 与重试交付 | `DataManagementSettingsViewModel.handleSelection()`、`onAction(ClearBackupDirectory)` | [`DataManagementSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModel.kt) |
| 目录选择/清除互斥及补偿回归 | `BackupDirectorySettingsInteractorTest` | [`BackupDirectorySettingsInteractorTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/backup/BackupDirectorySettingsInteractorTest.kt) |
| 页面失败反馈与 owner 回归 | `DataManagementSettingsViewModelTest` | [`DataManagementSettingsViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModelTest.kt) |
| 清除确认 busy/confirm UI 行为 | `BackupDirectoryClearDialogTest` | [`BackupDirectoryClearDialogTest.kt`](../../../app/src/androidTest/java/com/aozijx/passly/feature/settings/presentation/ui/data/BackupDirectoryClearDialogTest.kt) |
