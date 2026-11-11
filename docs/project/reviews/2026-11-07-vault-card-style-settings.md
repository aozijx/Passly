# Vault 卡片样式设置实施审查

日期：2026-11-07

## 范围

审查按条目类型选择 Vault 列表样式的 Interface 设置、Proto DataStore 持久化、稳定键映射、兼容筛选和运行时回退。当前实现合同见[Vault 卡片样式](../../product/vault/card-styles.md)。

## 完成内容

- 在 `VaultViewPreferences` 新增重复字段 `entry_card_presentations = 3`，使用 `entry_type_key` 与 `variant_key` 等显式字符串字段；既有数据缺字段时映射成空列表。
- Domain 增加 `LibraryViewSettingsRepository.setEntryCardPresentation(presentation)` 写端口。`ProtoAppPreferencesStore.setEntryCardPresentation(presentation)` 通过 `LibraryProtoMappings.upsert` 按 `entryTypeKey` 替换单一类型并保留其他选择，再清空/重写重复列表；未更改原有偏好格式版本。
- `CardStyleRegistry.entryTypeKey(entryType)` 显式穷尽映射所有 `EntryType`；`stylesFor(entryType)` 过滤不支持该类型的样式。注册表同时要求样式 key 不区分大小写时唯一，且默认样式是首项。
- Interface 设置展示 Login 和 OTP 的兼容预览组选项；`InterfaceSettingsViewModel` 组合界面设置与 Vault view 设置，保存动作在 Ready 状态和有效页面 owner 下调用 Repository。
- 实施快照中，`VaultDisplayViewModel` 将库偏好放入 `VaultStyleConfig`，再由 `VaultRoute` 转成 UI presentation；该中间 ViewModel 后续已并入 `VaultViewModel`。当前单一页面 owner 与设置投影见[Vault 列表与搜索](../../product/vault/list-and-search.md)；`VaultEntryRow` 仍通过 `CardStyleRegistry.resolveStyle(item, presentations)` 解析样式，并检查类型支持和条目能力，未知、缺失或不兼容选择回退默认样式。

## 旧逻辑与 TODO 结论

旧 LOGIN 条目选择 TOTP 卡片样式的双轨行为已由 `supportsEntryType` 和 `resolveStyle` 的类型检查取代；TOTP 样式只支持 OTP，密码样式只支持 LOGIN。此为已完成的切换，不保留兼容行为或活动 TODO。条目缺少所需能力时仍有默认样式回退，这是当前显式合同，不作为旧路由清理项。

此次审查未发现需要新增的卡片样式待办。之前各功能扫描发现且仍未完成的旧逻辑、桥接和调整项继续由 README “待办按开发流程归档”所列唯一 owner 跟踪；本记录不复制那些条目。

## 验证记录

原实施计划记录了以下验证结果：

- `CardStyleRegistryTest` 通过，覆盖 LOGIN 仅显示默认/密码样式及运行时拒绝 LOGIN/TOTP 选择。
- `InterfaceSettingsViewModelTest`、`InterfaceUiMapperTest` 和 registry 测试通过。
- Data focused test 与 Proto 映射测试任务记录为通过。
- 实施计划勾选 `git diff --check` 完成。

这些是原计划记录的历史结果，本次文档扫描没有重跑测试或构建。当前回合仅核对生产源码调用链并检查文档链接与 Markdown diff。
