# Data Secret Payload DTO 拆分审查

**日期：** 2026-10-29

**范围：** 将 `SecretPayload` 的嵌套 DTO 移到独立源码文件，同时保持序列化格式和类型 API 不变。

## 结果

- 九个嵌套 DTO 分别位于同包独立文件；`SecretPayload` 仍保留为扁平根 DTO。
- 固定样本覆盖全部字段，JSON 键、声明顺序、默认值和可空性保持不变；10 个 DTO 构造器签名与基线一致。
- 未改变 mapper 语义、敏感字段拆分、AAD、schema 或归档 wire format。

## 验证记录

- 全字段 JSON 表征在移动前后均通过，往返解码结果一致。
- `EntrySecretMapperTest` 和完整 `:data:testDebugUnitTest` 通过；复核记录 `git diff --check` 与新增文件行尾检查通过。
- 只读审查未发现问题。源码改动仍处于未暂存状态；本记录保留该次实施与验证证据。
