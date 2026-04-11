# Passly 改动操作手册（Change Playbook）

本文档是 `getting-started/DEVELOPER_GUIDE.md` 的执行版补充，面向以下场景：

- 你准备改数据库结构
- 你准备改备份格式/导入导出
- 你准备改 Vault 卡片样式
- 你准备改 Autofill 引擎/服务

目标：**给出可以直接执行的步骤、风险提醒、验证与回滚方案**。

---

## 0. 使用方式（先看）

每次改动前，请先完成以下固定动作：

1. 选择对应章节（数据库 / 备份 / 卡片样式 / Autofill）。
2. 按章节中的"改动步骤"执行，不要跳步。
3. 在 PR 描述粘贴章节中的"验证清单"并逐条勾选。
4. 若出现异常，按"回滚建议"优先止损。

通用本地编译命令：

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
```

### 0.1 执行准入（DoD）

以下条件满足后，才视为该类改动"完成"：

- 已完成对应章节"验证清单"全部项。
- PR 已附关键证据（命令输出/截图/日志）。
- 已明确回滚路径，且可在 1-2 步内执行。
- 如触发架构决策变化，已同步更新 `architecture/ARCHITECTURE_DECISIONS.md`。

---

## 1. 数据库改动（Room / Migration）

适用范围：新增字段、删字段、改字段类型、改索引、改表关系。

关键文件：

- `data/local/AppDatabase.kt`
- `data/entity/` 下实体
- `data/mapper/VaultEntryMapper.kt`
- `core/backup/BackupManager.kt`（联动）

### 1.1 改动步骤

1. 明确 schema 改动目标（字段名、类型、默认值、可空性）。
2. 修改对应 `Entity`。
3. 在 `AppDatabase.kt` 提升 DB 版本。
4. 编写并注册 migration（禁止只依赖 destructive migration）。
5. 同步更新 mapper（Entity <-> Domain）。
6. 检查导入/导出路径是否需要兼容处理。

### 1.2 风险点

- 旧数据迁移失败导致启动崩溃。
- 可空性变化导致 NPE 或业务逻辑偏差。
- mapper 漏改导致字段"看似存在但 UI 不生效"。
- 数据库改了但备份格式未适配，导入失败。

### 1.3 验证清单

- [ ] 冷启动可通过，且 DB migration 无异常。
- [ ] 旧版本数据可升级并保留关键字段。
- [ ] 新建/编辑/删除条目行为正常。
- [ ] 详情页与列表页字段显示正确。
- [ ] 备份导出后可导入并正确还原改动字段。

### 1.4 回滚建议

1. 保留版本号变更提交，优先修复 migration 而不是回退版本号。
2. 若线上已发版，不要删已发布 migration，追加修复 migration。
3. 对高风险发布，先灰度验证真实旧数据样本。

---

## 2. 备份改动（Backup Format / Import / Export）

适用范围：备份版本升级、字段映射变更、导入兼容逻辑修改。

关键文件：

- `core/backup/BackupManager.kt`
- `data/mapper/VaultEntryMapper.kt`
- `data/local/AppDatabase.kt`（联动）

### 2.1 改动步骤

1. 先定义版本策略（是否提升 `BACKUP_VERSION`）。
2. 修改导出结构（字段命名、可选字段、默认值策略）。
3. 修改导入逻辑（向后兼容旧版本）。
4. 为缺失字段提供保守默认值，避免导入崩溃。
5. 若涉及加密参数变化，评估旧备份可读性。

### 2.2 风险点

- 导入旧备份失败（向后兼容破坏）。
- 导入成功但数据静默丢失（更危险）。
- 备份内容结构变化导致跨版本不可恢复。

### 2.3 验证清单

- [ ] 当前版本导出 -> 当前版本导入成功。
- [ ] 历史版本备份 -> 当前版本导入成功。
- [ ] 导入后条目数量、类型、关键字段一致。
- [ ] 密码/TOTP/关联域名等敏感字段完整可用。
- [ ] 错误密码、损坏文件时有明确失败提示。

### 2.4 回滚建议

1. 不删除旧导入路径，临时保留旧分支解析器。
2. 格式升级失败时，优先恢复读取兼容而非强推新格式。
3. 线上异常时提供"只读导入诊断"能力（日志 + 提示）。

---

## 3. 卡片样式改动（Vault Cards）

适用范围：圆角、间距、背景、阴影、字体、标签、预览一致性。

关键文件：

- `core/common/ui/VaultCardStyleTokens.kt`
- `core/designsystem/base/VaultItem.kt`
- `features/vault/components/entries/PasswordStyleVaultItem.kt`
- `features/vault/components/entries/TotpStyleVaultItem.kt`
- `features/vault/components/entries/VaultCardStyleRegistry.kt`
- `features/settings/components/CardStyleSettingsSection.kt`

### 3.1 改动步骤

1. 优先修改 `VaultCardStyleTokens.kt`，避免散落硬编码。
2. 仅在必要时改具体组件结构（`VaultItem` / `Password` / `Totp`）。
3. 通过 `VaultCardStyleRegistry` 检查所有 style 分支都可渲染。
4. 验证设置页预览与真实列表视觉一致。

### 3.2 风险点

- 设置页预览与 Vault 实际卡片不一致。
- 某个 style 分支漏改，导致视觉割裂。
- 深色模式/动态色下对比度不足。

### 3.3 验证清单

- [ ] `DEFAULT` / `BASE` / `PASSWORD` / `TOTP` 都可正常渲染。
- [ ] 列表页与设置页预览一致。
- [ ] 深色模式、浅色模式都可读。
- [ ] 关键信息（标题、副标题、标签、进度）无遮挡。
- [ ] 点击区域与视觉边界一致，无误触。

### 3.4 回滚建议

1. 保留 token 结构，先回滚 token 数值，不急于回滚组件代码。
2. 出现局部异常时，先恢复 `VaultCardStyleRegistry` 对应分支。
3. 若仅预览异常，优先修复设置页调用链，避免影响主列表。

---

## 4. Autofill 改动（Service / Engine）

适用范围：匹配规则、候选筛选、填充展示、保存请求处理。

关键文件（按模块）：

- `service/autofill/`
- `service/autofill/engine/`
- `service/autofill/presentation/`

### 4.1 改动步骤

1. 先定义匹配策略变化（域名、包名、权重规则）。
2. 在 `engine` 层实现，不在 UI 展示层写业务规则。
3. 在 `presentation` 层只做展示映射与 RemoteViews 组装。
4. 评估慢操作日志与超时路径，必要时加保护。

### 4.2 风险点

- 匹配变慢导致 Autofill 体验劣化。
- 匹配过宽导致候选泄漏风险。
- 仅改展示层导致"看起来正确，实际匹配错误"。

### 4.3 验证清单

- [ ] 常见登录场景匹配命中正常。
- [ ] 非匹配站点不会泄漏无关候选。
- [ ] 候选排序符合预期（域名/包名优先）。
- [ ] 慢操作日志无明显新增告警。
- [ ] `onFillRequest` / `onSaveRequest` 异常有可观测日志。

### 4.4 回滚建议

1. 优先回滚匹配规则变更，保留日志增强代码。
2. 若表现层异常，先恢复旧 presentation 组装逻辑。
3. 对线上问题优先"降低风险面"而不是"扩展功能"。

---

## 5. PR 前统一检查（建议复制到 PR 描述）

- [ ] 已说明改动属于哪一类（数据库/备份/卡片/Autofill）。
- [ ] 已按对应章节步骤执行。
- [ ] 已完成本地编译：`:app:compileFullDebugKotlin`。
- [ ] 已完成对应章节验证清单。
- [ ] 已评估回滚路径且可执行。

---

## 6. 变更记录建议

每次高风险改动建议在 PR 里附：

1. 改动动机（一句话）
2. 影响面（模块 + 文件）
3. 兼容性声明（是否影响历史数据/备份）
4. 验证证据（命令、截图、关键日志）
5. 回滚方案（1-2 步）

---

## 7. 后续扩展

可继续新增章节：

- 条目类型策略改动（`domain/strategy`）
- 加密链路改动（`core/crypto`）
- 扫码链路改动（`features/scanner` + `core/qr`）

这样能把项目所有高风险改动都纳入统一 playbook。
