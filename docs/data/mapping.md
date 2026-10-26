# Data 映射边界

`:data` 把 Room、加密 payload 与 Domain 模型隔离，不承载界面或跨 Feature 业务流程。

## 当前路径

Entry 数据分为三类，不能用一条统一的“Blob 解密链”描述：

| 数据 | 当前存储 | 主要映射 |
|---|---|---|
| Profile 元数据 | `entries` 结构化列 | `EntryProfileMapper.fromEntity()` |
| 低敏 Secret 结构 | `entry_secret_fields/STRUCT_BUNDLE` 密文 | `SecretBundleCodec` + `EntrySecretMapper` |
| 高敏单字段 | `entry_secret_fields/<fieldKey>` 独立密文 | `SecretFieldCodec` + `SensitiveFieldSetMapper` |

读取普通详情时：

```text
EntryEntity -> EntryProfileMapper -> EntryProfile
STRUCT_BUNDLE row -> SecretBundleCodec -> EntrySecret (高敏值为 null)
EntryAssembler -> Entry
```

读取已授权单字段时，仅查询对应 `fieldKey` 并经 `SecretFieldCodec` 解密。读取完整凭据时，
`SecretFieldStore.readAll()` 才把 bundle 与各高敏字段合并。

写入时，Profile 直接映射到 `EntryEntity`；Secret 由 `SensitiveFieldSetMapper` 拆成 bundle 与逐字段值，
再由各自 Codec 加密。Revision 另有独立的 `EntryContentSnapshotCodec` 和
`SensitiveRevisionSnapshotCodec`；其中 `SummaryPayload` 是历史快照格式，不是 `entries` 的当前格式。

银行卡发卡行、支行、产品名称、币种、客服电话及账单地址位于 `CardCredential` / `CardCredentialPayload`，
经 Secret bundle 加密保存，不新增 Profile 明文列。持卡人的真相源为 `cardHolder`；字段读取与编辑不借用用户名。
扩展字段保持 Payload 版本 1，读取、详情、修订快照和备份使用同一组凭据属性。

## 已存储条目的能力标记

普通查询返回的 Secret bundle 中，高敏值为 null，表示未揭示；不能据此推断密码或 OTP 不存在。
`EntryCapabilities.from` 用于创建时的完整凭据。已存储条目通过 `fromStoredSecret` 组合结构信息和实际高敏键：
`PASSWORD` / `SSH_PASSPHRASE` 行提供密码能力，`OTP_SECRET` 行提供 OTP 能力，其余高敏键不冒充这些能力。
Wi-Fi 密码保存在加密 bundle 中，没有独立密码行；其密码能力仍由 bundle 内的实际值决定。

更新先检查条目版本，在同一事务内执行高敏字段的 Preserve/Clear/Replace，再读取实际键集并更新能力位图、
条目、修订和活动记录。只读键名，不为能力计算解密高敏内容。保留字段仍保留能力，显式清空则撤销对应能力；
失败或取消由现有事务边界回滚。修订恢复也使用同一 Domain 规则，从恢复的高敏快照键重建标记。

## 目录职责

- `local/database/entity`：Room 表结构与约束。
- `codec/entry`：Secret bundle 和单字段密文编码。
- `codec/revision`：Revision 快照编码。
- `codec/entry/payload`：加密内容解密后的稳定序列化模型。
- `mapper/entry`：Entity、Payload 与 Domain 的纯转换、拆分和组装。
- `repository/entry`：在 session/事务边界内编排 DAO、Codec 和 Mapper。

## 约束

- Mapper 不访问 DAO、不开事务、不执行加密，也不依赖 Android UI。
- Codec 不实现业务规则，只维护稳定序列化格式、版本和记录级 AAD。
- Entity 不泄漏到一般 Feature。备份快照适配器是受测试约束的明确例外，但备份文件仍使用自己的 DTO。
- 需要查询、排序、索引或约束的非敏感 Profile 数据使用结构化列；不能为了省字段把它重新塞回不透明
  Blob。
- 高敏字段保持逐字段密文，不进入 Profile、日志、普通详情 bundle 或查询索引。
- 持久化枚举使用显式稳定键；不使用 ordinal，也不在未知值上静默回退。

备份协议与流程归 `app/feature/backup`。它可以通过受控快照边界读取完整数据，但不属于 `:data` 的
通用映射职责。
