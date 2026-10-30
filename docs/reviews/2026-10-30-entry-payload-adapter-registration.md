# Entry Payload Adapter 注册实施审查

**日期：** 2026-10-30

**范围：** 按 Domain `EntryCredentialKind` 登记 `SecretPayload` 各凭据 slot 的编码和解码实现，保持版本 1 JSON 与 Root mapper 校验边界。

## 结果

- LOGIN、CARD、IDENTITY、SSH、WIFI、PASSKEY、OTP 均由静态 `EntryPayloadAdapterRegistry` 按 kind 解析；`NONE` 是合法无凭据类型，不登记 adapter。
- `EntrySecretMapper` 继续穷尽处理 Domain `EntryCredential` 和 `EntryCredentialKind`，并集中校验载荷 schema 版本、单 slot 限制、预期 `EntryType`、扩展字段和自定义字段。
- 各 adapter 仅负责对应 payload slot。OTP 载荷保持密钥分离；WIFI 仍严格解析原有稳定安全类型值，不对缺失或未知值静默默认。
- Flat `SecretPayload`、slot 名称、字段顺序、JSON golden、AAD、归档键和 Room schema 未改变。

## 映射完整性与验证

- CARD、IDENTITY、SSH、PASSKEY 的 Domain 与 payload DTO 字段集合由测试反射比对，并用所有非默认字段验证 encode/decode 往返；新增字段不对齐时测试失败。DTO 仍留在 Data，当前 adapter 对字段的转换是显式 Kotlin 构造，不使用运行时反射映射。
- 各 kind 的适配批次先验证缺失注册/错误映射回归，再运行 `EntrySecretMapperTest` 和完整 `:data:testDebugUnitTest`；OTP 批次还将真实 `EntryType` 校验传至 bundle 与 revision codec。
- 每批独立只读审查通过，完整 Data 测试及 `git diff --check` 均通过；版本 1 固定 JSON 样本持续覆盖全部 slot。

## 当前代码位置

| 职责 | 源码 |
| --- | --- |
| 静态按 kind 注册和覆盖校验 | [`EntryPayloadAdapterRegistry.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapterRegistry.kt)、[`EntryPayloadAdapter.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapter.kt) |
| 根载荷类型、slot 与扩展校验 | [`EntrySecretMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt) |
| Data 层稳定载荷 DTO | [`SecretPayload.kt`](../../data/src/main/java/com/aozijx/passly/data/codec/entry/payload/SecretPayload.kt) |
| 字段对齐、全字段往返与 JSON golden | [`EntrySecretMapperTest.kt`](../../data/src/test/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapperTest.kt) |
