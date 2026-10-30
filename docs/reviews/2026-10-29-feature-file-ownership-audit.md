# Passly 功能文件归属审查

**日期：** 2026-10-29

**范围：** Vault、Settings、Autofill、Backup、Database Reset/Recovery、Scanner、Shell、Unlock，以及应用组合根与共享展示层。

本文记录该日期的源码归属审查、已完成迁移和保留决定。它是历史审查证据，不定义当前架构合同；当前模块边界见[包与模块边界](../architecture/package-boundaries.md)和[维护边界](../development/maintenance-boundaries.md)。

## 扩大后的源码布局扫描（2026-10-29）

扫描开始时，App 源码包含 `app` 下 76 个 Kotlin 文件、`feature` 下 489 个、根 `presentation` 下 29 个，另有一个直接位于源码根的 `MainActivity.kt`。架构文档已分别定义 app、feature 和共享展示边界；文件数量本身不足以证明应跨层搬迁。随后 launcher Activity 已移入应用组合根 `presentation`，4 组 Vault 专用 helper 已移出根 `presentation/shared` 并归入 `feature/vault/presentation`。

| 功能 | 当前组织 | 结论 |
|---|---|---|
| Vault | `feature/vault` 下有 entry/detail/history/trash 等非 UI 流程；当前 presentation 有 171 个文件，按 list/detail/editor/trash/navigation 分组 | 保留业务流程与页面分层；全局共享展示层中 4 组 Vault 专用 helper 已迁入 feature。其余端口、平台适配与 UI 所有权复核后保留。 |
| Settings | `feature/settings` 下有功能专属 interactor/契约；presentation 有 93 个文件，按页面、状态和 UI 分组 | 保留。功能逻辑和适配器位于功能根目录，页面状态与 UI 位于 presentation。 |
| Autofill | 按 Accessibility、Legacy、Credential、core、runtime、platform、presentation 分组；Android Activity/Service 在 app Manifest 注册 | 保留独立平台入口和生命周期分组；没有证据支持合并这些 Android 入口。 |
| Backup | 52 个实现文件位于 `internal`，16 个页面文件位于 presentation；快照适配器明确归 app | 保留协议/格式实现与 UI 的现有所有权。 |
| Database Reset / Recovery / Scanner / Shell / Unlock | 页面位于对应功能的 presentation；Reset 业务编排位于 UI 包之外 | 本轮保留。目录数量或叶子目录差异不足以证明职责错位。 |

### Settings 功能内审计（2026-10-29）

- **保留** `presentation/appearance`、`autofill`、`backup`、`main/*`、`permissions`、`security` 与 `presentation/ui/*` 的分组。状态/契约/映射、Route 和 Compose 内容仍位于同一个 Settings feature；测试目录大体镜像对应功能路径，未发现跨功能误归属。
- **保留** `presentation/backup/DataManagementSettingsViewModel`：生产入口是 `presentation/main/navigation/data/BackupRoute.kt`，职责是选择/清除 Backup 目录。名字里的 DataManagement 不代表它属于 `DataManagementRoute`；后者展示回收站、数据库重置、缓存与诊断日志。
- **保留** `feature/settings/backup/BackupDirectorySettingsInteractor` 与 `app/backup/AndroidBackupDirectoryAccess` 的分界：Settings feature 负责策略/契约，app adapter 负责 Android SAF 授权；现有 interactor/ViewModel 测试覆盖各自职责。
- **保留** 全局 Settings 分组展示组件：`BackupRestoreSettingsSection` 也使用，属于真实跨功能复用。
- 本轮未发现需要仅为目录统一而迁移的 Settings 文件；没有改动 Settings 生产文件。

### App 启动入口归位（2026-10-29）

`MainActivity.kt` 原来直接位于 `com.aozijx.passly` 根包，而 `PasslyApplication` 和平台适配器位于 `com.aozijx.passly.app`。Activity 装配全局认证、通知、权限和设备锁能力，并调用唯一的应用组合入口 `presentation/PasslyApp.kt`，实际所有者是应用组合根。已将文件移到 `presentation/MainActivity.kt`，包名改为 `com.aozijx.passly.presentation`。

- 更新 `AndroidManifest.xml` 的 launcher Activity 声明，以及 `autofill_service_config.xml` 和 `credential_service_config.xml` 的设置页类名。
- `PasslyApp.kt` 保留在根 `presentation`：它是明确的应用 Compose 组合入口，不是功能页面或全局控件。
- 首先尝试把 Activity 放入 `app` 时，`verifyModuleBoundaries` 拒绝其直接依赖 Security 的 `AuthenticationHostRegistry`。该对象是 Activity 宿主认证 UI 必需的 Security 所有实现；不扩大 `app` 例外，改为与 `PasslyApp` 共处应用组合根，并在包边界文档注明该 Activity host。
- 没有 Activity 专属单测；源码搜索没有其它类调用 MainActivity。Android 入口注册只存在于以上三个 XML 声明，包边界文档现明确 `presentation` 是应用组合入口及 Activity host 的所有者。
- 本批次只调整源码包名和入口类引用，不改变 Activity 生命周期、权限、认证或 Autofill 行为。

### 纯业务规则下沉到 Domain（已提交）

提交 `c79137c9c` 将纯银行卡号校验 `CardNumberValidator` 和 Wi-Fi QR 解析结果、载荷及解析器从 Vault Feature 移入 `domain/entry/policy`，对应单元测试同步移入 Domain。它们只依赖输入值和业务规则，不读取 UI 状态或 Android/Data 实现；当前依赖边界由 `verifyArchitecture` 检查。本工作区没有额外待提交的 Domain 迁移文件。

### Autofill、Backup 与应用集成边界复核（2026-10-29）

- **保留 Autofill 的 feature 分组。** Legacy、Accessibility、Credential 的 Android 服务/Activity 分别在 Manifest 注册；feature 内的 runtime/core 管理交互状态与凭据流程，presentation 管理各自的 UI 生命周期。现有 Legacy/Accessibility/Credential 测试位于 Autofill 功能测试树。
- **保留 `app/autofill` 四个文件。** `AndroidAutofillLaunchTarget` 将 feature 的启动目标合同绑定到三种 Activity；`AndroidAutofillServiceStatusSource` 把 Android Secure Settings 转为 feature 状态端口；`AutofillInboxCredentialImporter` 将 Domain 导入端口连接至 feature 保存用例；`AutofillPlatformModule` 统一绑定这些跨边界适配器。已有 instrumentation 覆盖启动目标和 Legacy response intent。它们不是应该塞进单一业务子包的页面逻辑。
- **保留 Backup 的 `feature/backup/internal` 与 presentation 分界。** archive 格式、校验、读写和操作流程由 Backup feature 拥有；Room 快照读写位于 `app/database/backup`，实现 `BackupSnapshotGateway` 并通过专属 Hilt module 连接 `:data`。正式文档已明确该适配器边界，没有发现重复快照实现。
- **保留 Database Reset 的 UseCase/端口/适配器拆分。** `ResetDatabaseUseCase` 在 feature 内处理访问门禁并依赖 `DatabaseResetGateway`；`app/database/DatabaseResetGatewayAdapter` 连接 Data reset 能力和 Domain session coordinator。已有 UseCase、ViewModel 与 Adapter 测试分别覆盖这些职责。
- **保留 Recovery、Unlock、Scanner、Shell 现有路径。** Recovery/Unlock 的页面状态在各自 feature presentation，认证秘密与安全会话仍归 Security；Scanner Route、相机宿主和图片解码器由 Scanner feature 一起拥有；`PasslyApp` 是组合根，`AppShell`/ViewModel 管壳层状态并有对应测试。没有发现需要跨层搬迁的单一所有者代码。

### Vault 工作流与数据适配边界复核（2026-10-29）

- **保留 Vault 的非 UI 流程子包。** `entry` 持有创建、导入、移动回收站及编辑器导航数据流程；`detail` 持有详情编辑、揭示与二维码等应用服务；`history` 持有修订读取/恢复编排；`trash` 持有回收站命令；`otp` 持有运行时口令状态。各组都有相应单元测试，详情与列表的 Presentation 测试镜像具体 UI 状态和异步发布职责。没有证据支持把这些流程搬入 ViewModel 或 `presentation`。
- **保留分页端口和 app/Data 适配器拆分。** Vault 的 `VaultEntryPageSource`、`VaultDataChangeSignal` 是 feature 所需能力合同；`app/entry/paging` 中的 `DataVaultEntryPageSource`、`DataVaultDataChangeSignal` 分别接到 Data 的 `EntryPagingStore` 与刷新通知端口，并由 `VaultEntryDataAdapterModule` 绑定。具体 Data 实现未进入 feature，DI 注册位置与应用组合职责吻合。
- **保留 detail/history 的独立包。** 详情编辑与修订恢复分别由各自应用服务编排，详情 ViewModel 显式依赖这些用例；历史用例有单测覆盖。单纯因页面汇总展示历史而将历史流程并入 detail，会模糊恢复/授权职责，没有带来更清晰的所有权。
- 静态审查没有发现上述流程合同的重复转发实现或需要改动的导航、Manifest、资源、持久化键。此结论依据代码调用、Hilt binding 与现有测试路径；本轮没有运行测试。

## 全局共享展示层：Vault 首批迁移（2026-10-29）

静态生产调用搜索确认以下代码位于根 `presentation/shared`，但只由 Vault 消费。没有发现这些符号的 Hilt/KSP 绑定或 Android Manifest/导航注册；目标均在既有的 `feature/vault/presentation` 页面边界内。

| 原位置 | 新位置 | 调用方/配套改动 |
|---|---|---|
| `presentation/shared/components/NextFocusTextField.kt` | `feature/vault/presentation/editor/ui/common/NextFocusTextField.kt` | 6 个密码、OTP、银行卡、Wi-Fi 编辑器调用点；`docs/development/compose-ui.md` 已改为记录 Vault 局部位置。 |
| `presentation/shared/components/common/DropdownSelector.kt` | `feature/vault/presentation/editor/ui/otp/DropdownSelector.kt` | `OtpConfigForm` 的 3 个调用点。 |
| `presentation/shared/media/ImagePalette.kt` | `feature/vault/presentation/list/ui/component/cardstyle/ImagePalette.kt` | `PasswordCardStyle` 的调用点。 |
| `presentation/shared/components/AppPackagePickerBottomSheet.kt` | `feature/vault/presentation/common/ui/AppPackagePickerBottomSheet.kt` | Vault 详情页和密码编辑页的 Compose 使用点。 |
| Picker 文件中的 `AppPackagePickerItemUiModel` | `feature/vault/presentation/common/AppPackagePickerItemUiModel.kt` | 独立于 Compose UI；供密码编辑 Contract/ViewModel、详情 mapper、详情 presentation model 和关联条目 UI 使用。 |

Picker 的两个字符串资源仍留在 app 资源集中。组件实现与行为未改动；model 从 Compose 文件拆出，避免状态/映射所有者依赖 `ui` 包。四组组件都没有直接组件测试；现有密码编辑 ViewModel 测试覆盖已安装应用选择状态。本批次没有新增或改写行为测试。除 `NextFocusTextField` 的正式文档提及已更新外，没有发现 Manifest、导航、DI 或其它路径文档入口。

**验证状态：** 当前最终目录下 `verifyArchitecture` 和 `:app:compileDebugKotlin -Pkotlin.incremental=false` 均通过。`DetailUiMapperTest` 与 `AddPasswordViewModelTest` 定向单测通过。Activity 初次放入 `app` 时被模块边界规则拒绝；最终与 `PasslyApp` 一起位于应用组合根，未增加 Security 例外。首次增量编译在 Kotlin 编译器回滚输出时报告临时 `.backup` 文件缺失，未给出源码诊断；关闭增量后完整编译通过。最终 `git diff --check` 退出码为 0，仅报告现有文件 LF→CRLF 转换提示。

## 全局共享展示层：保留项

- **保留 `presentation/mapping/entry/EntryTypePresentationMapper` 与共享 EntryType 展示模型/标签。** Mapper 被 Vault、Backup、Autofill 的选择、保存或展示流程调用；共享 UI model 和标签被这三个 feature 的页面消费。EntryType Domain 注册/元数据当前有未提交修改，后续若改变展示映射合同，应一并复核，而不是在本次审计中并行重写。
- 保留 `VaultItemIcon`、`VaultIcons`、`VaultIconPalette`、`EntryVisualCategoryClassifier`：Autofill 候选页和 Vault 列表/详情/回收站都使用 `VaultItemIcon`，其图标注册表、分类与颜色映射随之共享。`toLocalIconImageModel` 被该共享图标和 Vault 详情/卡片消费，也保留共享。
- 保留设置分组控件：Settings 页面和 `BackupRestoreSettingsSection` 都使用。保留 `MenuOptionText` / `selectedMenuModifier`：Settings 与 Vault 菜单都使用。
- 保留 `ImagePicker` / `ImageType`：Scanner 和 Vault Route 都使用 Activity Result 图片选择流程。保留 `UiErrorMessage` / `writePreferenceWithFeedback`：调用方横跨 Settings、Vault 和 Database Reset。
- 保留 `AppPasswordSetDialog` / `PasswordFields`：Unlock 与 Settings 使用设置密码 UI；`InternalInputAutofillTest` 也验证密码字段的 Autofill 行为。
- 保留顶栏颜色/组件：Settings 和 Vault 分别使用 `PasslyNavigationTopBar` 与 `passlyTopAppBarColors`。

## 应用组合根与跨功能集成层复核（2026-10-29）

- 根 `presentation` 当前只包含 `PasslyApp`、其 Activity host、按多消费者归类的 `shared` 和 `mapping/entry`；没有发现遗留在根包的业务页面或单 feature mapper。Activity 的 Manifest 和 Autofill/Credential Manager 设置入口已在上一批迁移中同步。
- `app/message` 按 contract、model、runtime、presentation 和 system gateway 分组，为全局通知/消息宿主提供组合能力；`PasslyApp` 连接宿主，功能调用方横跨应用页面。将其归入某一个 feature 会让其它 feature 反向依赖该 feature，故保留在 app 集成边界。
- `app/observability`、`app/cache`、`app/clipboard`、`app/platform/permission` 分别承载 Android 系统能力适配与生命周期控制；DI 或系统 API 使用点位于相应 app 模块，未发现只为目录统一而应迁移到 feature 的实现。
- `app/entry/favicon` 与 `app/entry/paging` 连接 Entry Domain/Data 能力合同与 Android/Data 实现；feature 只依赖能力接口。数据库 Backup 与 Reset adapter 同理，保留其跨模块 adapter 所有权。

## 范围限制与后续触发条件

- 设置分组控件虽然名称带 Settings，但 Backup 也有真实调用。不能按命名将它们搬进 Settings；如后续拆分，必须保留 Backup 使用方或先证明存在更合适的共享所有者。

## 审查限制

本轮逐项核对了源码组织、关键生产调用方、Data/Security 依赖边界、Hilt/Manifest 注册、资源入口、测试位置与正式文档。未按文件数量或目录名称批量搬迁。审查仅为静态检查，没有运行功能测试或设备级验证；后续只有在消费者、边界或目录职责变化时才需要重新评估保留项。
