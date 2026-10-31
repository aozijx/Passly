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

`SecretPayload` 保持扁平的版本 1 JSON 结构。`EntrySecretMapper` 是根边界：编码时核对凭据种类与预期
`EntryType`；解码时拒绝未知 schema 版本、多个凭据槽位及槽位与条目类型不匹配。凭据槽位由按
`EntryCredentialKind` 索引的 `EntryPayloadAdapterRegistry` 转换；`LOGIN` 等多个条目类型可共享同一凭据
适配器。适配器只负责自己的凭据槽位，根映射仍负责 `notes`、自定义字段和 schema/类型校验。缺失适配器、
不支持的值或适配失败都会显式失败，不会回退到其他凭据映射。

当前适配器覆盖 OTP、Login、Card、Wi-Fi、Identity、SSH 和 Passkey；`NONE` 不需要适配器。适配器拆分
不改变 `SecretPayload` 的字段名、默认值、顺序或版本，也不改变 Room schema、加密/AAD 或备份格式。
新增凭据槽位时，需同步验证注册覆盖、所有字段双向往返和根边界的条目类型校验。

银行卡发卡行、支行、产品名称、币种、客服电话及账单地址位于 `CardCredential` / `CardCredentialPayload`，
经 Secret bundle 加密保存，不新增 Profile 明文列。持卡人的真相源为 `cardHolder`；字段读取与编辑不借用用户名。
扩展字段保持 Payload 版本 1，读取、详情、修订快照和备份使用同一组凭据属性。

修订的 `iconCustomReference` 是可查询的资源引用，与快照中的图标路径同步写入并建立索引；当前条目路径也建索引。
资源回收无需解密修订，必须在写事务内同时确认两种引用均不存在。历史裁剪与条目永久删除先收集路径，
提交后通过可重试队列回收；附件 `PENDING_GC` 状态在同一事务持久化，并在完整 Vault 会话恢复时重试。
附件 drain 和图标队列 drain 分阶段执行，某一阶段的普通异常不会阻止另一阶段登记/重试资源。普通 post-commit
清理异常记录结构化 `ResourceCleanupFailed`（未分类的存储阶段异常使用 `STORAGE_FAILURE`；明确的文件删除失败
保留 `FILE_DELETE_FAILED`），但保留数据库事务原结果；`CancellationException` 与 fatal `Error`
继续传播。取消可能发生在事务提交之后，此时事务不会回滚，调用方应重新读取 Repository/Room 真相后决定下一步，
不把取消本身解释为写入失败。图标文件扫描覆盖普通 `favicon_*.webp` 与备份恢复的 `favicon_restored_*.webp`，
也兼容旧 `restored_*.webp` 名称；删除前仍由事务查询当前及历史引用。数据库失败不能退化为直接删除文件。
Room schema 仍为 1，不迁移旧开发数据库，也不在结构不匹配时静默删库。

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
- `mapper/entry`：Entity、Payload 与 Domain 的纯转换、拆分和组装；根映射验证 schema/类型，凭据适配器映射各自槽位。
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


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 将数据库条目映射为领域 Profile | `EntryProfileMapper.fromEntity()` | [`EntryProfileMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryProfileMapper.kt) |
| 拆分并组装低敏与高敏字段 | `EntrySecretMapper`、`EntrySecret.toSensitiveFieldUpdates()` | [`EntrySecretMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt)、[`SensitiveFieldSetMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/SensitiveFieldSetMapper.kt) |
| 按凭据种类注册并转换 Secret payload 槽位 | `EntryPayloadAdapterRegistry`、`EntryPayloadAdapter` | [`EntryPayloadAdapterRegistry.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapterRegistry.kt)、[`EntryPayloadAdapter.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapter.kt) |
| 编排秘密字段的持久化读取与写入 | `SecretFieldStore.readAll()`、`readField()` | [`SecretFieldStore.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt) |
