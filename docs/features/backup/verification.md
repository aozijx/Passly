# Backup 验证要求

状态：当前实现合同（代码核对：2026-10-18）。

协议或流程变化至少验证：

- JSON 与加密格式解码到相同 canonical records；
- golden JSON 只含显式小写稳定键，不含 Kotlin enum 名/ordinal；
- compatible 与 strict 使用同一输入覆盖不同处置；
- 格式身份、版本、KDF、GCM、大小、摘要、ZIP 路径和重复 ID 错误在两种策略下都失败；
- `OVERWRITE` 在可恢复集合为空时不清库，执行失败整体回滚；
- 资源 journal 能补偿数据库/文件失败，并记录强杀窗口限制；
- 默认目录 grant 丢失、provider 不可用、测试文件清理失败都不能显示“可写”；
- 指定 URI 导出与默认目录导出不共享隐含目标；
- TXT 不可导入且不包含 favorite 字段；
- 新增 Domain 类型/字段前，archive registry 和 round-trip 测试已覆盖。

Wire model 不导入 Room DTO；Adapter 不访问数据库或 Android URI；Restorer 不解析 JSON。Room Schema、文档
version 和 container version 的测试必须分开。
