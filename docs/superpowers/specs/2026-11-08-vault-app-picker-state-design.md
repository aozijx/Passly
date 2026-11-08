# Vault App Package Picker 状态合同

## 意图

为 Detail 关联应用和 Password 编辑器关联应用使用同一明确的 picker 展示状态，移除“列表 + loaded/failed 布尔值”组合，避免未请求、加载、空结果、无匹配、成功和失败被压成互相冲突的字段。

## 范围

统一 `AppPackagePickerUiState<T>` 的 `NotRequested`、`Loading`、`Ready` 和 `Failed` 状态；`Ready` 保留完整应用目录、当前查询和 `NoApps`/`NoMatches`/`Matches` 结果。Detail 与 Password 页面分别拥有自己的查询和异步请求，不能共用跨页面可变状态。

Search 按 label/package name 忽略大小写匹配，空白查询显示完整目录；展示文案留在 Compose UI。普通目录读取异常进入 Failed 并可重试；CancellationException 传播；fatal Error 不降级为 Failed；旧页面/认证 owner 的迟到结果不可写入新状态。

## 旧路径清理

迁移所有当前生产与测试调用方后，删除 picker 的独立 loaded/failed 布尔字段和只为旧列表 API 服务的转发属性。不得保留默认值或第二条状态路径。

## 验收

纯状态测试覆盖空目录、空查询、label/package 匹配、无匹配及类型映射结果；Detail 和 Password ViewModel 测试覆盖 Loading/Ready/Failed、重试、锁定/替换/销毁后的旧结果、取消和 fatal Error。AndroidTest 源码编译证明 BottomSheet 调用已完成迁移。
