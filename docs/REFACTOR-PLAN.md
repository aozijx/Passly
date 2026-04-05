# Passly 重构执行手册（进度同步版）

> 目标：在不破坏现有可用性的前提下，完成一次“安全优先、可渐进落地”的结构与能力升级。

## 0. 使用方式（先看）

每次推进前，按下面顺序执行：

1. 先做 `0.1` 的前置核对，再开始对应 Phase。
2. 每完成一个 Phase，必须跑编译 + 关键手测。
3. 所有高风险改动都要有回滚路径（1-2 步可执行）。
4. 任何涉及 DB 的改动，必须保留向后兼容策略；备份按“当前单格式策略”执行。

### 0.1 前置核对清单

- [ ] 确认当前数据库版本号（`DatabaseConfig.VERSION`）与线上已发布版本一致。
- [ ] 确认当前备份格式版本号与实现一致（历史格式默认不兼容）。
- [ ] 确认是否已有未提交的并行改动，避免迁移脚本冲突。
- [ ] 准备至少一份真实旧数据样本用于迁移验证。

### 0.2 当前状态快照（2026-04-06）

- 已完成：`P1` 数据层对齐主链（`VaultSummary` 脱敏、`VaultDao` 查询收敛、`AppDatabase` 迁移链固定到 `VERSION = 3`）。
- 已完成：`P2` 安全链路收敛（详情与列表入口统一认证解密、移除通用静默解密路径）。
- 进行中：`P3` 备份升级（新备份实现已落地雏形，正在补齐导入导出一致性与错误处理）。
- 已完成：Argon2 备份加密接入（当前实现：`com.lambdapioneer.argon2kt:argon2kt`）与 `ImportMode` 全量解耦。

---

## 1. 范围与边界

### 1.1 本次重构范围（In Scope）

1. 认证再解锁（敏感字段按需解密）。
2. 数据库迁移链收敛（新增字段、保持兼容）。
3. 自定义图片从 DB BLOB 迁移为磁盘路径存储。
4. 备份格式升级（支持 ZIP 及可选字段导出，采用当前单格式导入导出）。

### 1.2 不在本次范围（Out of Scope）

- 不做多模块拆分。
- 不引入 Hilt/Koin（保留手动 DI）。
- 不做大规模 UI 风格重绘（只做必要适配）。

---

## 2. 重构目标（结果导向）

### 2.1 安全目标

- 列表与摘要模型不包含永久敏感字段（如密码明文解密结果）。
- 查看/复制/编辑敏感值必须经过认证链路。
- 禁止业务路径绕过认证直接调用“静默解密”。

### 2.2 数据目标

- 迁移链保持可回放（`MIGRATION_1_2` + `MIGRATION_2_3`）。
- 迁移过程中不依赖 destructive migration。
- 旧数据可升级，关键字段不丢失。

### 2.3 备份目标

- 支持新备份格式（ZIP 载荷 + 加密封装）。
- 仅支持当前备份格式（历史格式不兼容，导入时明确提示）。
- 支持按字段组/条目范围导出。

---

## 3. 数据库与模型重构（迁移链收敛）

> 说明：当前版本固定为 `3`，不新增 `3 -> 4`。本阶段以迁移链一致性与安全性收敛为主。

### 3.1 迁移策略

- 仅做 `ADD COLUMN`（可空或有默认值）。
- 不在本次迁移中执行高风险列删除。
- 对历史字段采用“代码停用、结构保留”的保守策略。

### 3.2 字段策略（建议）

- `encryptedImageData`：不再参与读写，保留列并标注 `@Deprecated`。
- 新增业务字段全部在 Entity / Domain / Mapper / Backup 中对齐。
- `VaultSummary` 移除 `password` 字段（减少敏感暴露面）。

### 3.3 实施清单

- [x] `VaultEntryEntity.kt`：新增字段已对齐，兼容字段保留。
- [x] `VaultEntry.kt`：领域模型新增字段已对齐。
- [x] `VaultEntryMapper.kt`：双向映射已补齐。
- [x] `VaultSummary.kt`：已移除 `password`。
- [x] `VaultDao.kt`：summary 查询已去掉 `password` 选择列。
- [x] `AppDatabase.kt`：已收敛为 `VERSION = 3` + `MIGRATION_1_2` + `MIGRATION_2_3`。

### 3.4 迁移模板（示意）

```kotlin
val migrationXY = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vault_entries ADD COLUMN cardholderName TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE vault_entries ADD COLUMN bankName TEXT DEFAULT NULL")
        // ...其余新增列
    }
}
```

---

## 4. 认证再解锁（安全主线）

### 4.1 原则

- 敏感信息不预解密。
- 交互触发后认证成功才解密。
- TOTP 可作为例外路径（明确限定用途，不泛化）。

### 4.2 关键改动

1. `VaultCryptoSupport`
   - 删除或限制通用 `decryptSilently()`。
   - 仅保留认证链路解密接口。
   - TOTP 场景使用专用解密方法（命名体现用途）。
   - 当前状态：已完成（通用静默解密已收敛）。

2. `showDetailForEdit()` + `DetailScreen`
   - 不再预填解密结果。
   - 编辑页默认掩码；点击显示后走认证。
   - 当前状态：已完成（弹窗详情与全屏详情双入口已对齐）。

3. `VaultScreen` 滑动动作
   - 所有密码复制/编辑入口统一走 ViewModel 认证解密链路。
   - 当前状态：已完成。

### 4.3 验收清单

- [x] 列表初次加载无密码解密行为。
- [x] 详情查看敏感字段时出现认证弹窗。
- [x] 认证失败时不返回明文。
- [x] 无“静默解密”绕过路径（代码搜索验证）。

---

## 5. 自定义图片改造（磁盘存储 + 备份打包）

### 5.1 目标状态

- 图片文件存储在应用私有目录。
- DB 仅保存相对路径（避免设备路径耦合）。
- 备份时将引用到的图片打包进 ZIP。

### 5.2 路径策略

- 存储值：`vault_images/<fileName>`（相对路径）。
- 运行时通过 `context.filesDir` 解析绝对路径。

### 5.3 兼容策略

- 导入旧数据时支持绝对路径兜底识别。
- 启动或导入后可执行一次“路径归一化”任务，将绝对路径转相对路径。

### 5.4 验收清单

- [ ] 新增图片条目后 DB 不再写入 BLOB。
- [ ] 备份文件包含 `images/` 目录与 `data.json`。
- [ ] 跨设备导入后图片仍可显示。

---

## 6. 备份格式升级与可选导出

### 6.1 格式策略

- 新版本：加密载荷内为 ZIP（`data.json` + `images/`）。
- 兼容策略：仅接受当前版本头；历史版本直接拒绝并提示不兼容。

当前状态：

- 新备份架构已拆分为独立实现（便于后续删除旧实现）。
- 已收敛为单一路径导入当前格式，不再维护历史版本分流。
- 文档与代码统一口径：历史备份默认不兼容。

### 6.2 `ExportConfig`（建议）

```kotlin
data class ExportConfig(
    val entryIds: Set<Int>? = null,
    val entryTypes: Set<Int>? = null,
    val includeFields: Set<ExportField>? = null,
    val includeImages: Boolean = true,
    val includeHistory: Boolean = false
)
```

### 6.3 导出实现原则

- 基础必要字段始终导出（如 `title`, `entryType`）。
- 敏感字段按字段组选择性导出。
- 导入端对未知字段 `skipValue()`，确保前后向兼容。

### 6.4 验收清单

- [ ] 当前格式可正常导出/导入。
- [ ] 历史格式导入会给出明确不兼容提示。
- [ ] 勾选排除某字段组时，导出 JSON 不含对应字段。

### 6.5 下一步执行清单（代码落地）

- [x] `gradle/libs.versions.toml`：已接入 Argon2 依赖（当前实现：`com.lambdapioneer.argon2kt:argon2kt`）。
- [x] `app/build.gradle.kts`：已接入 Argon2 依赖并用于备份密钥派生。
- [x] `app/src/main/java/com/aozijx/passly/core/backup/BackupManager.kt`：已收敛为单一路径导入当前格式，分流残留已清理。
- [x] `app/src/main/java/com/aozijx/passly/features/settings/internal/BackupActionSupport.kt`：已仅依赖新导入模式类型，且提示文案已统一。
- [x] `app/src/main/java/com/aozijx/passly/features/settings/SettingsViewModel.kt`：已切换新导入模式类型，备份提示文案已资源化。
- [x] `app/src/main/java/com/aozijx/passly/features/vault/dialogs/BackupPasswordDialog.kt`：已切换新导入模式类型，导入/导出文案已资源化。

完成定义（DoD）：

- [x] 全仓搜索无 UI/Feature 层对 `BackupManager.ImportMode` 的直接引用。
- [x] 备份导入在版本头不匹配时返回可观测错误（非崩溃）。
- [ ] 新导出与导入均只使用当前格式，历史格式不再作为兼容目标。

备注：`core/designsystem/components/BackupPassword.kt` 在当前仓库中不存在，相关耦合清理已在 `BackupPasswordDialog.kt` 落地。

---

## 7. 分阶段执行计划

| Phase | 目标      | 关键任务                                         | 风险等级 | 状态 |
|-------|---------|----------------------------------------------|------|------|
| P1    | 数据层对齐   | Entity/Domain/Mapper/Summary/DAO + Migration | 中    | 已完成 |
| P2    | 安全链路统一  | 认证再解锁，移除静默解密通路                               | 中    | 已完成 |
| P3    | 备份升级    | ZIP 载荷、当前格式稳定性、字段过滤导出                     | 高    | 进行中 |
| P4    | UI 适配收尾 | 新字段输入展示、导出配置对话框                              | 中    | 待开始 |

### 每个 Phase 的完成定义（DoD）

- [ ] `:app:compileFullDebugKotlin` 通过。
- [ ] 关键流程手测通过（新增、编辑、查看、备份导入导出）。
- [ ] 迁移/备份改动附验证证据（日志或截图）。
- [ ] 回滚方案已可执行。

---

## 8. 风险与回滚

| 风险     | 触发信号         | 快速止损              | 回滚策略                 |
|--------|--------------|-------------------|----------------------|
| 迁移失败   | 冷启动崩溃/迁移异常日志 | 停止发布，锁定版本         | 保留版本号，追加修复 Migration |
| 敏感字段泄露 | 列表/日志出现明文    | 立即下线相关入口          | 回滚到前一认证链路稳定版本        |
| 备份不兼容  | 历史备份导入失败      | 明确提示格式不兼容并中止导入 | 提供格式转换工具或维持单格式策略     |
| 图片丢失   | 导入后 icon 不显示 | 启用路径归一化修复任务       | 回退到旧路径解析逻辑           |

---

## 9. 关键代码关注点（评审时必查）

- `VaultDao.kt`：summary 查询列是否仍包含 `password`。
- `VaultViewModel.kt` / `VaultScreen.kt` / `DetailScreen.kt`：是否存在绕过认证的解密调用。
- `VaultCryptoSupport.kt`：是否仍暴露可泛用的静默解密 API。
- `BackupManager.kt`：单一路径导入是否清晰，异常映射是否清晰。
- `AppDatabase.kt`：迁移链是否仅保留 `MIGRATION_1_2` 与 `MIGRATION_2_3`（`VERSION = 3`）。

---

## 10. 建议的提交拆分（便于审查）

1. `feat(db): add migration and align models`
2. `refactor(security): enforce authenticated decryption flow`
3. `feat(backup): support zip payload and selective export`
4. `feat(ui): adapt detail/export screens for new fields`

---

## 11. 本地验证命令

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
```

如需跑更完整验证，可补充：

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:testFullDebugUnitTest
```

---

## 12. 结论

这版方案从“可执行性”优先：

- 先锁住高风险主线（迁移 + 认证）。
- 再推进中高复杂度改造（备份升级 + 图片磁盘化）。
- 最后做 UI 收尾，保证每一步都可独立验收、可回滚。

如需，我可以在此文档基础上继续补一份 `PR 模板版验收清单`（可直接贴到每次 PR 描述中）。

## 13. 附录 A：Phase 1 逐文件任务单（可直接开工）

> 说明：`Owner` 与 `ETA` 先用占位，开工前在周会中补齐。

| 文件                                                                                  | 主要改动点                                                    | 验收方式                    | 风险 | Owner             | ETA     |
|-------------------------------------------------------------------------------------|----------------------------------------------------------|-------------------------|----|-------------------|---------|
| `app/src/main/java/com/aozijx/passly/data/entity/VaultEntryEntity.kt`               | 新增业务字段；兼容字段标注废弃；保证默认值可迁移                                 | 编译通过；Room schema 变更符合预期 | 中  | `@data-owner`     | `D1`    |
| `app/src/main/java/com/aozijx/passly/domain/model/VaultEntry.kt`                    | 领域模型字段与 Entity 对齐                                        | 编译通过；详情页字段不崩溃           | 低  | `@domain-owner`   | `D1`    |
| `app/src/main/java/com/aozijx/passly/data/mapper/VaultEntryMapper.kt`               | `toDomain()`/`toEntity()` 补齐映射                           | 单测或手测：新增字段读写一致          | 中  | `@data-owner`     | `D1-D2` |
| `app/src/main/java/com/aozijx/passly/domain/model/VaultSummary.kt`                  | 移除 `password` 等永久敏感字段                                    | 列表仍正常展示；无密码字段依赖报错       | 中  | `@security-owner` | `D2`    |
| `app/src/main/java/com/aozijx/passly/data/local/VaultDao.kt`                        | Summary 查询去掉 `password` 列；保持排序/筛选逻辑                      | 列表加载正常；搜索/分类正常          | 中  | `@data-owner`     | `D2`    |
| `app/src/main/java/com/aozijx/passly/data/local/AppDatabase.kt`                     | 注册并校验 `MIGRATION_1_2` + `MIGRATION_2_3`；保持 `VERSION = 3` | 冷启动迁移通过；旧数据可读           | 高  | `@data-owner`     | `D2-D3` |
| `app/src/main/java/com/aozijx/passly/features/vault/VaultViewModel.kt`              | 修复 Summary 脱敏后的调用链（避免隐式依赖）                               | 列表/详情流转正常；无空指针          | 中  | `@feature-owner`  | `D3`    |
| `app/src/main/java/com/aozijx/passly/features/detail/page/DetailScreen.kt`          | 全屏详情入口对齐无预解密策略                                           | 双入口编辑行为一致；需认证后解密        | 中  | `@feature-owner`  | `D3`    |
| `app/src/main/java/com/aozijx/passly/features/vault/VaultScreen.kt`                 | 交互入口改走认证解密链路（不直解）                                        | 复制/编辑敏感值均触发认证           | 高  | `@security-owner` | `D3-D4` |
| `app/src/main/java/com/aozijx/passly/features/vault/internal/VaultCryptoSupport.kt` | 删除/限制通用静默解密 API；保留 TOTP 专用接口                             | 代码搜索无绕过入口；认证失败不返明文      | 高  | `@security-owner` | `D4`    |

### Phase 1 完成判定

- [x] `:app:compileFullDebugKotlin` 通过（历史阶段验证已多次通过）。
- [ ] 旧数据样本迁移成功。
- [ ] 列表、搜索、分类、详情链路手测通过。
- [x] 敏感字段查看必须认证。

---

## 14. 附录 B：PR 模板版验收清单（可直接复制）

```markdown
## 变更类型
- [ ] 数据库迁移
- [ ] 安全链路
- [ ] 备份格式
- [ ] UI 适配

## 影响文件
- `app/src/main/java/com/aozijx/passly/data/local/AppDatabase.kt`
- `app/src/main/java/com/aozijx/passly/data/local/VaultDao.kt`
- `app/src/main/java/com/aozijx/passly/domain/model/VaultSummary.kt`
- （按实际补充）

## 验收清单
- [ ] 本地编译通过：`:app:compileFullDebugKotlin`
- [ ] 旧数据可迁移并可读
- [ ] 列表/搜索/分类/详情流程正常
- [ ] 敏感字段查看需认证，认证失败不返回明文
- [ ] 无静默解密绕过路径（代码搜索已确认）
- [ ] 如涉及备份：当前格式可导入/导出，历史格式不兼容提示准确

## 风险与回滚
- 风险点：
  1. 
  2. 
- 回滚步骤：
  1. 
  2. 

## 证据
- 命令输出截图：
- 关键日志：
- 功能录屏/截图：
```

---

## 15. 附录 C：两周推进节奏（建议）

### Week 1（稳定主线）

- `D1-D2`：完成 P1 数据层改造（Entity/Domain/Mapper/Summary/DAO）。
- `D2-D3`：完成 Migration 注册与旧数据样本验证。
- `D3-D4`：完成认证链路统一（移除静默解密通路）。
- `D5`：集中回归（列表、详情、复制、编辑、搜索、分类）。

### Week 2（兼容与体验）

- `D6-D7`：备份当前格式（ZIP）实现稳定化（不做历史兼容）。
- `D7-D8`：可选字段导出能力接入（`ExportConfig`）。
- `D9`：图片路径归一化与跨设备导入验证。
- `D10`：收尾 PR、补文档、发布前回归。

### 里程碑门禁

- [x] M1（Week 1 结束）：迁移 + 安全链路全绿。
- [ ] M2（Week 2 中段）：备份当前格式导入/导出全绿（进行中）。
- [ ] M3（Week 2 结束）：主流程回归 + 发布材料齐全。
