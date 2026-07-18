# 数据库

状态：当前实现（Room Schema v1）。本文只描述全新安装，不承诺旧 Schema 迁移。

## 组成

```mermaid
flowchart TB
    Repo["Domain Repository 实现"] --> Session["DatabaseSession"]
    Session --> Provider["DatabaseProvider"]
    Provider --> Room["Room AppDatabase"]
    Room --> SQLCipher["SQLCipher passphrase"]
    Repo --> Mapper["Entity ↔ Domain Mapper"]
    Mapper --> DAO
    DAO --> Room
```

`DatabaseProvider` 负责用已解锁 DEK 创建加密数据库；`DatabaseSession` 延迟打开并在 Vault
锁定或进程进入后台时关闭实例。Repository 必须经 Session 访问数据库，不能长期缓存 DAO 或 Room 实例。

## 当前表

| 表                   | 用途                                 |
|---------------------|------------------------------------|
| `vault_metadata`    | 列表所需的低敏元数据                         |
| `vault_credentials` | 加密业务负载                             |
| `vault_historys`    | 条目快照历史；表名存在历史拼写问题                  |
| `vault_activities`  | 操作活动记录                             |
| `vault_attachments` | 附件元数据和内容引用                         |
| `lookup_index`      | Blind Index 检索数据                   |
| `key_envelopes`     | 遗留表，当前信封真相源已迁至 Bootstrap Proto，待移除 |

Schema 的唯一事实源是 `AppDatabase`、Entity 和导出的 `app/schemas`，本文不复制完整字段声明。

## 热冷分离与聚合

列表优先读取 metadata，详情再读取 credential 并解密；Repository 将两者聚合成领域模型。搜索使用基于会话密钥的
Blind Index，不能对明文敏感字段使用 SQLite `LIKE`。

历史、备份和导出共享 Vault Snapshot 语义，但数据库 Entity、备份 DTO 与 Domain model 仍应由 Mapper 隔离。

## 生命周期约束

```mermaid
stateDiagram-v2
    [*] --> Closed
    Closed --> Open: 首次数据库访问且 DEK 已解锁
    Open --> Closed: 锁定 / 后台关闭
    Closed --> Open: 再次认证
```

- 错误密钥或 Schema 不匹配必须返回明确错误，禁止自动删库。
- 锁定顺序为：拒绝新操作 → 关闭数据库 → 擦除 DEK 与会话密钥。
- 导入的覆盖/合并操作必须在事务中完成，失败整体回滚。
- 数据库关闭超时不能制造“已关闭”的假象；详见[代码审查](../reviews/2026-07-code-review.md)。

## 相关实现

- `data/local/database/AppDatabase.kt`
- `data/local/database/DatabaseProvider.kt`
- `data/local/database/DatabaseSession.kt`
- `data/model/entity/`
- [ADR-0018](../decisions/ADR-0018-lookup-metadata-strategy.md)

