# Vault 卡片样式

状态：当前实现。

Vault 列表卡片样式由 `CardStyleRegistry` 注册和解析；用户在设置的“界面 → Vault 卡片样式”中按条目类型选择样式。当前设置仅对存在多个兼容样式的类型显示选择器：登录条目可选默认/密码样式，OTP 条目可选默认/一次性验证码样式。混合登录与 OTP 凭据的条目仍以主条目类型 `LOGIN` 解析卡片样式。

## 设置与保存流程

`InterfaceScreen` 将卡片样式主分组初始设为折叠；展开后按 `CardStyleRegistry.configurableEntryTypes` 显示条目类型分组，登录与 OTP 类型分组也都初始折叠，并以图标切换折叠状态。每个选项使用统一高度的预览框，卡片和单选按钮都可选择样式。点击时以当前 `EntryCardPresentation`（不存在时新建仅含稳定类型键的默认值）复制出 `variantKey = style.key`，发出 `SetEntryCardPresentation(presentation)`。

`InterfaceSettingsViewModel` 将 `InterfaceSettingsRepository.interfaceSettings` 与 `LibraryViewSettingsRepository.libraryViewSettings` 合并为 `InterfaceSettingsUiState.Ready`，把卡片偏好映射到界面状态。保存动作经 `saveSetting` 检查页面处于 Ready 且存在当前页面 owner，再由 `writePreferenceWithFeedback` 调用 `LibraryViewSettingsRepository.setEntryCardPresentation(presentation)`；写入失败发出界面错误反馈。

Data 层的 `ProtoAppPreferencesStore.setEntryCardPresentation(presentation)` 在 Proto DataStore 更新中读取现有重复字段，映射为 Domain 后按 `entryTypeKey` upsert，再清空并写回卡片偏好列表。只替换同一条目类型的选择，保留其他类型和 Vault view preference 的其余字段。`VaultViewPreferences.entry_card_presentations = 3` 是新增的重复字段；缺省数据映射为空列表，因此旧设置数据继续使用默认样式，不需要 Room 迁移或调整偏好格式版本。

## 运行时解析与回退

`VaultViewModel` 将库设置与页面所需的界面、交互设置放入同一个可恢复读取；[`VaultSettingsSnapshot`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultSettingsSnapshot.kt) 投影为 `VaultDisplayUiState.Ready`。[`VaultListUiMapper`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultListUiMapper.kt) 将 Domain `EntryCardPresentation` 映射为列表 UI presentation，`VaultEntryRow` 调用 `CardStyleRegistry.resolveStyle(item, content.cardPresentations)` 选取样式。

注册表以稳定 `key`（当前为 `default`、`password`、`totp`）索引组件，并要求 key 忽略大小写后唯一、默认样式位于首位。`entryTypeKey(entryType)` 用穷尽 `when` 映射稳定类型字符串，不持久化 enum 名称或序号。设置选项由 `stylesFor(entryType)` 按 `supportsEntryType` 过滤；运行时再同时检查已选组件的类型支持和 `supports(entry)` 条目能力。缺失或未知的样式 key、类型不兼容的旧选择、或条目缺少所需字段时均回退至默认样式。密码样式要求 `LOGIN` 且条目有密码；TOTP 样式要求 `OTP` 且条目有 OTP 凭据。旧的 LOGIN/TOTP 不兼容双轨已由运行时兼容性检查替代，不再作为待保留路径。

新增样式时，在 `VaultCardStyleComponent` 实现稳定 key、标题、`supportsEntryType(entryType)`、`supports(entry)`、列表 `Render(...)` 和 `Preview(...)`，然后加入 `CardStyleRegistry.registrations`。同一注册信息同时驱动设置可选项与列表解析，避免设置与运行时各维护一份样式表。

## 代码位置

| 符号 | 文件 | 职责 |
| --- | --- | --- |
| `CardStyleChoices(...)`、`InterfaceScreen(...)` | [`InterfaceScreen.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/appearance/InterfaceScreen.kt) | 按可配置类型绘制分组、预览、选中态并发出设置动作 |
| `InterfaceSettingsViewModel.onAction(...)`、`saveSetting(...)` | [`InterfaceSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/appearance/InterfaceSettingsViewModel.kt) | 组合偏好状态并将选择写入 Repository |
| `LibraryViewSettingsRepository.setEntryCardPresentation(...)` | [`LibraryViewSettingsRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/settings/port/LibraryViewSettingsRepository.kt) | Domain 写入端口 |
| `ProtoAppPreferencesStore.setEntryCardPresentation(...)` | [`ProtoAppPreferencesStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoAppPreferencesStore.kt) | DataStore upsert 并保留其他类型偏好 |
| `EntryCardPresentationPreference`、`VaultViewPreferences.entry_card_presentations` | [`app_preferences.proto`](../../../data/src/main/proto/app_preferences.proto) | 稳定 Proto 持久化字段 |
| `CardStyleRegistry.stylesFor(...)`、`entryTypeKey(...)`、`resolveStyle(...)` | [`CardStyleRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/cardstyle/CardStyleRegistry.kt) | 配置筛选、稳定类型映射与运行时兼容解析 |
| `VaultViewModel`、`VaultSettingsSnapshot`、`VaultListUiMapper`、`VaultRoute`、`VaultEntryRow` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt)、[`VaultSettingsSnapshot.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultSettingsSnapshot.kt)、[`VaultListUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultListUiMapper.kt)、[`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`VaultEntryRow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryRow.kt) | 单一页面状态 owner、设置投影与列表样式解析 |
