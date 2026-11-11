# 详情页密码关系设备验收

状态：绑定与解绑实现已完成；只剩 Compose 设备交互验收。

当前用户合同、页面状态和代码 owner 见[条目编辑与写入](../../../product/vault/entry-editor-and-persistence.md#详情页密码关联)。单元测试、Room 事务与静态架构验证已有记录；本计划只跟踪尚未执行的仪器运行。

## 待完成

- [ ] 在连接的 Android 设备或 AVD 上运行 `RelatedEntriesSectionTest` 和 `RelatedPasswordPickerSheetTest`，记录 API 级别与结果。核实滑动打开确认框、取消保留关系、确认/失败重试交互，以及空候选、无搜索匹配和加载失败状态。
