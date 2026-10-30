# Domain Entry Type 注册审查

**日期：** 2026-10-29

**范围：** `EntryTypeDefinitions` 的定义拆分与注册表完整性校验。

## 结果

- 将 17 种 `EntryType` 的字段定义拆到独立定义文件；仅对既有字段合同相同的类型复用纯字段构造器。
- 保留 `EntryTypeDefinitions` 的公开查询接口和字段合同。
- 注册表在建表前拒绝重复类型，并拒绝缺失或意外的类型。
- 未改变敏感字段映射、持久化结构或外部格式。

## 验证记录

- 新增的重复注册和缺失类型用例先因注册构建器缺失而失败，随后修复。
- `EntryTypeDefinitionsTest`、`EntryDraftMaterializerTest` 与完整 `:domain:test` 均通过。
- 对 17 项定义做了只读对比审查，无发现；`git diff --check` 通过，新文件另行检查了行尾空格。

当前字段和创建规则以[条目编辑与写入](../features/vault/entry-editor-and-persistence.md)为准；本记录仅保留实施审查证据。

## 后续穷尽式创建分类（2026-10-30）

`EntryDraftMaterializer` 对全部 `EntryType` 采用穷尽 `when`：已实现类型执行物化，其余类型显式拒绝并保留既有错误消息。新增类型会要求编译器指出尚未分类的分支，不另建策略注册表。

新增 NOTE 不支持创建的表征用例后移除 fallback，Kotlin 编译按预期报告遗漏类型；补全显式拒绝分支后，完整 `:domain:test` 的 178 项测试通过，独立只读审查无发现，`git diff --check` 通过。

当前合同以[条目类型与凭据注册](../architecture/entry-type-registration.md)为准；具体创建边界以[条目编辑与写入](../features/vault/entry-editor-and-persistence.md)为准。
