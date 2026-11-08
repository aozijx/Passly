# Vault App Picker 状态验证

状态：实现与当前合同已归档；尚待单测、源码编译和架构门禁验证。

当前状态投影、搜索、失败重试及页面 owner 合同见[条目编辑与写入](../../features/vault/entry-editor-and-persistence.md#应用关联选择器)。旧 loaded/failed 标记及 `availableAppItems` 已从生产和测试源码移除。

## 待完成

- [ ] 运行 `AppPackagePickerUiStateTest`、`AddPasswordViewModelTest`、`DetailReducerTest` 和 `DetailViewModelTest`，确认结果中包含全部四个测试类。
- [ ] 运行 `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin verifyArchitecture --no-parallel --max-workers=2`；检查 `git diff --check`。
