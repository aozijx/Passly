# Mapper 层设计文档

> **最后更新**：2026-07-13
> **核心理念**：解耦领域模型 (Domain)、持久化实体 (Entity) 与加密传输负载 (Payload)。

## 1. 架构分层

Mapper 层分为三个主要维度，确保数据在不同层级间转换时的安全性与一致性：

| 组件类型               | 职责描述                              | 转换方向             | 核心类/位置                              |
|:-------------------|:----------------------------------|:-----------------|:------------------------------------|
| **Assembler**      | 聚合器。将解密后的多个 Payload 组装成一个完整的领域模型。 | Payload → Domain | `assembler/VaultAggregateMapper.kt` |
| **Payload Mapper** | 处理业务字段与解密后 JSON 对象之间的映射。          | Payload ↔ Domain | `metadata/`, `credential/`          |
| **Entity Mapper**  | 处理领域模型与 Room 实体（加密 Blob）之间的映射。    | Entity ↔ Domain  | `entity/`                           |

---

## 2. 核心转换矩阵 (CRUD Matrix)

| 场景              | 转换流程                      | 关键方法                                         | 备注                             |
|:----------------|:--------------------------|:---------------------------------------------|:-------------------------------|
| **读取 (Read)**   | Entity → Payload → Domain | `assembleVaultEntry`                         | 从数据库读取加密 Blob，解密后由聚合器组装。       |
| **写入 (Write)**  | Domain → Payload → Entity | `toMetadataPayload`<br>`toCredentialPayload` | 领域对象拆分为两个负载，加密后存入不同表的 Blob 字段。 |
| **更新 (Update)** | Payload + Domain → Domain | `mergeInto`                                  | 将解密后的新数据合并到现有的领域对象中，支持增量更新。    |

---

## 3. 详细组件说明

### 3.1 Assembler (聚合器)

聚合器不感知数据库、DAO 或加密算法。它只认识解密后的 `Payload` 和 `Domain` 模型。

* **方法**: `assembleVaultEntry`
* **逻辑**: 创建 `VaultEntry` 骨架 → 注入 `Metadata` → 注入 `Credential` (可选)。

### 3.2 Payload Mappers

Payload 是为了方便序列化（JSON）和加密（AES-GCM）设计的。

* **MetadataMapper**: 处理标题、图标、分类、标签、时间戳等非敏感数据。
* **CredentialMapper**: 处理用户名、密码、TOTP、笔记、信用卡等敏感数据。
* **安全准则**: 敏感字段**严禁**出现在 Metadata 中。

### 3.3 Entity Mappers

负责将领域模型转化为 Room 能够存储的格式。

* **MetadataEntity**: 包含 `entryId`、`vaultId`、版本号及加密后的 `metadataBlob`。
* **CredentialEntity**: 仅包含 `entryId` 和加密后的 `credentialBlob`。

---

## 4. 字段扩展流程

若需添加新字段（如：`recovery_email`），请遵循以下步骤：

1. **Domain**: 在 `VaultEntry` 中添加属性。
2. **Payload**: 在 `CredentialPayload` (或相关子类) 中添加字段及 `@Serializable` 注解。
3. **Mapper**:
    * 在 `toCredentialPayload` 中添加导出逻辑。
    * 在 `mergeInto` 中添加注入逻辑（使用 `?:` 确保空安全）。
4. **Database**: 若涉及索引字段，需同步更新 `LookupIndex`。

---

## 5. 性能与安全红线

* ❌ **禁止**在 `VaultEntry` 列表展示中加载 `CredentialPayload`（性能开销大且不安全）。
* ❌ **禁止**在 Mapper 内部进行加密操作（由 Repository 层的 Cipher 处理）。
* ✅ **必须**使用 `mergeInto` 的增量合并模式，防止在部分解密时抹掉其他字段。
