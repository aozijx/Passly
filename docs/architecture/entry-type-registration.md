# 条目类型与凭据注册

状态：当前实现。

本页记录当前实现。条目定义、凭据载荷转换和可创建目的地由各自所属层登记；它们不是运行时插件系统，也不共享一个跨层注册表。

## Domain 字段定义

`EntryTypeDefinitions` 提供所有 `EntryType` 的字段目录。每类定义位于 `domain/entry/policy/type/`，包含字段类型、必填约束、访问级别及敏感存储键。静态列表在索引前拒绝重复类型，且要求覆盖 `EntryType.entries`；调用方仍通过原有 `EntryTypeDefinitions[type]` 查询。

字段目录不等于创建能力。`EntryDraftMaterializer` 对全部 `EntryType` 使用穷尽分支：四种已实现类型执行物化，其余类型显式拒绝。新增类型会要求编译者明确归类。创建入口则由 App 的实际导航目的地决定。

## Data 凭据载荷

`EntryPayloadAdapterRegistry` 按 `EntryCredentialKind` 选择内部 `EntryPayloadAdapter`。当前 LOGIN、CARD、IDENTITY、SSH、WIFI、PASSKEY、OTP 均有 adapter；`NONE` 代表合法的无凭据类型，不登记 adapter。重复 kind 会在构建注册表时失败，测试确保所有非 `NONE` kind 都可解析。

每个 adapter 只编码和解码 `SecretPayload` 中对应的 slot。`EntrySecretMapper` 继续统一处理 schema 版本、slot 数量、slot 与 `EntryType` 的匹配，以及 `notes` 和自定义字段。`SecretBundleCodec` 与 Revision snapshot codec 都传入持久化条目的实际 `EntryType`，因此当前载荷和历史恢复都按条目类型校验。adapter 不做 fallback；载荷缺失或 credential 类型不匹配会明确失败。

多字段凭据的 mapper 测试用非默认字段值验证整个 payload slot 和 encode/decode 往返，防止新增字段在转换中遗漏；CARD 另有字段名与类型的结构对齐断言。反射只用于测试数据构造与 CARD 结构断言，生产 adapter 使用显式 Kotlin 构造，不做运行时反射映射。

迁移保持版本 1 的 `SecretPayload` 字段、JSON 名称与序列化设置不变。JSON 编解码和加密仍属于 `SecretBundleCodec`；高敏字段拆分、合并仍属于 `SensitiveFieldSetMapper`。adapter 注册不改变数据库键、归档键、AAD、授权或存储策略。

## App 创建入口

`VaultEditorDestinationRegistry` 从实际导航目的地列出当前可创建的 PASSWORD、TOTP、BANK_CARD 和 WIFI。这个集合与全部 `EntryType`、Domain 字段目录、可读取凭据 kind 及归档覆盖彼此独立；不能因类型存在字段定义或 adapter 就推断它可以创建。

## 添加类型时的检查

1. 在 Domain 类型定义中登记完整字段与敏感键映射；覆盖测试会要求定义列表无重复并涵盖所有 `EntryType`。
2. 若新增了可读取的凭据 kind，为其添加仅负责对应载荷 slot 的 adapter；全量注册测试会在遗漏登记时失败。
3. 只有实现了物化逻辑和真实创建导航目的地时，才扩展创建入口。
4. 检查 `EntrySecretMapperTest` 的全字段往返和固定 JSON 样本，以及备份映射、稳定存储键和敏感字段测试。

## 边界

注册表是 APK 内静态 Kotlin 代码，不使用反射发现、动态下载、卸载、Hilt 多重绑定或按类型新增 Gradle 模块。Domain 不依赖 Data DTO 或 App 资源。任何改变持久化格式、敏感数据边界或创建支持集合的工作都需要独立验证与决策。

## 实施审查记录

- [Domain 条目类型字段注册与穷尽创建分类](../reviews/2026-10-29-domain-entry-type-registration.md)
- [Data SecretPayload DTO 拆分](../reviews/2026-10-29-data-secret-payload-dto-split.md)
- [Data 凭据载荷 adapter 注册与版本兼容验证](../reviews/2026-10-30-entry-payload-adapter-registration.md)

## 代码位置

| 符号 | 源码 | 职责 |
| --- | --- | --- |
| `EntryTypeDefinitions` | [`EntryTypeDefinitions.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryTypeDefinitions.kt) | Domain 字段定义注册与覆盖校验 |
| `EntryPayloadAdapterRegistry` | [`EntryPayloadAdapterRegistry.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapterRegistry.kt) | 按 credential kind 选择 adapter |
| `EntrySecretMapper.toPayload()` / `toDomain()` | [`EntrySecretMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt) | 统一根载荷校验与扩展映射 |
| `VaultEditorDestinationRegistry` | [`VaultEditorDestinationRegistry.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt) | 从实际目的地派生创建集合 |
| 注册与格式合同测试 | [`EntrySecretMapperTest.kt`](../../data/src/test/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapperTest.kt) | adapter 覆盖、字段往返与固定 JSON 格式 |
