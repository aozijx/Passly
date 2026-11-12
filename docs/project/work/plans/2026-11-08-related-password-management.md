# 详情页密码关系设备验收

状态：绑定与解绑实现已完成；只剩 Compose 设备交互验收。

当前用户合同、实现函数和单元/Room/Compose 测试源码见[条目编辑与写入](../../../product/vault/entry-editor-and-persistence.md#详情页密码关联)。本计划只跟踪尚未执行的设备验收；测试源码或编译记录不代表设备交互已验证。

## 待完成

- [ ] 在连接的 Android 设备或 AVD 上运行 `RelatedEntriesSectionTest` 和 `RelatedPasswordPickerSheetTest`，记录 API 级别与结果。核实滑动打开确认框、取消保留关系、确认/失败重试交互，以及空候选、无搜索匹配和加载失败状态。
