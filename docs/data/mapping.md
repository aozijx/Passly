# Data 映射边界

`data` 只负责把持久化格式转换为领域模型，不承载备份、界面或业务流程。

## 结构

- `local/database/entity`：Room 实体，只描述数据库表结构和约束。
- `codec/entry/payload`：解密后的 JSON 负载，只描述持久化格式。
- `codec/entry`：负载的序列化、加密与解密。
- `mapper/entry`：Payload、Entity 查询结果与 Domain 之间的纯转换和组装。
- `repository`：编排 DAO、Codec 和 Mapper，实现 Domain 仓储接口。

读取路径：`Entity(encrypted blob) -> Codec -> Payload -> Mapper -> Domain`。

写入路径：`Domain -> Mapper -> Payload -> Codec -> Entity(encrypted blob)`。

## 约束

- Mapper 不访问 DAO、不开启事务、不执行加密，也不依赖 Android UI。
- Codec 不实现业务规则，只维护稳定的存储格式和记录级 AAD。
- Entity 不泄漏到一般 Feature；应用内备份的数据库快照适配器是明确、受测试约束的例外。
- 新增字段优先放入现有 Summary 或 Secret Payload；只有需要查询、索引或约束时才修改表结构。
- 高敏感字段继续使用独立密文存储，不进入普通摘要或密钥负载。

备份归 `app/feature/backup`，它可以读取数据库快照，但不属于 `data` 的映射职责。
