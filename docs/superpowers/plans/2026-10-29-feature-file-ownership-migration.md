# Passly 功能文件归属审计与迁移

## 目标

让各功能的文件按真实职责和所有权更容易查找，减少仅因放在全局共享目录而显得“共享”、实际只由单一功能使用的代码。

## 当前架构基线

现有架构约定：`feature/<name>` 放应用内业务流程和功能专属平台适配；页面、ViewModel、UI state/effect、导航和 Compose UI 放在 `feature/<name>/presentation`。根 `presentation/shared` 只放确实被多个功能消费的展示能力。Domain 规则/契约、Data 持久化、Security 编排和 app 级 Android 入口/适配器仍归各自现有边界。

各功能内部目录不完全相同：Autofill 按 `core/runtime/platform/presentation` 分组，Backup 把实现放在 `internal`，Vault 按业务子流程分组。目录名称不同本身不是迁移理由；审计按所有权和复用证据进行，不强行套用相同目录模板。

## 审计与迁移规则

每个功能都检查生产调用方和 import、Gradle/包边界、Hilt/KSP、Manifest/导航注册、资源和稳定键、测试、正式文档。候选项记录为**迁移、保留或待查**并附证据。仅当目标目录准确反映实际所有权，且能改善功能内发现能力时才移动；不得因此增加转发层或违反依赖方向。保留运行行为、公开契约、序列化键、数据库 schema 和资源语义。

逐功能审计和迁移，每个功能的变更保持独立可审查；同步更新 import 和受影响的路径文档，并运行改动边界要求的验证。验证完成后按精确路径暂存并提交独立批次；保留无关工作区改动，始终排除 `AGENTS.md` 与 `.agents/skills/`。

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

Picker 的两个字符串资源仍留在 app 资源集中。组件实现与行为未改动；model 从 Compose 文件拆出，避免状态/映射所有者依赖 `ui` 包。四组组件都没有直接组件测试；现有密码编辑 ViewModel 测试覆盖已安装应用选择状态。本批次没有新增或改写行为测试，也未运行测试。除 `NextFocusTextField` 的正式文档提及已更新外，没有发现 Manifest、导航、DI 或其它路径文档入口。

**验证状态：** 最终 `verifyModuleBoundaries` 与 `:app:compileDebugKotlin -Pkotlin.incremental=false` 均通过。Activity 初次放入 `app` 时被模块边界规则拒绝；最终与 `PasslyApp` 一起位于应用组合根，未增加 Security 例外。首次增量编译在 Kotlin 编译器回滚输出时报告临时 `.backup` 文件缺失，未给出源码诊断；关闭增量后完整编译通过，未清理或终止进程。最终 `git diff --check` 退出码为 0，仅报告现有文件 LF→CRLF 转换提示。本批次未运行测试。

## 全局共享展示层：保留项

- 保留 `VaultItemIcon`、`VaultIcons`、`VaultIconPalette`、`EntryVisualCategoryClassifier`：Autofill 候选页和 Vault 列表/详情/回收站都使用 `VaultItemIcon`，其图标注册表、分类与颜色映射随之共享。`toLocalIconImageModel` 被该共享图标和 Vault 详情/卡片消费，也保留共享。
- 保留 `EntryTypeUiModel` 和 `EntryTypePresentationMapper`：Backup、Autofill、Vault 均消费这些展示类型或映射。
- 保留设置分组控件：Settings 页面和 `BackupRestoreSettingsSection` 都使用。保留 `MenuOptionText` / `selectedMenuModifier`：Settings 与 Vault 菜单都使用。
- 保留 `ImagePicker` / `ImageType`：Scanner 和 Vault Route 都使用 Activity Result 图片选择流程。保留 `UiErrorMessage` / `writePreferenceWithFeedback`：调用方横跨 Settings、Vault 和 Database Reset。
- 保留 `AppPasswordSetDialog` / `PasswordFields`：Unlock 与 Settings 使用设置密码 UI；`InternalInputAutofillTest` 也验证密码字段的 Autofill 行为。
- 保留顶栏颜色/组件：Settings 和 Vault 分别使用 `PasslyNavigationTopBar` 与 `passlyTopAppBarColors`。

## 待查

- 设置分组控件虽然名称带 Settings，但 Backup 也有真实调用。不能按命名将它们搬进 Settings；如后续拆分，必须保留 Backup 使用方或先证明存在更合适的共享所有者。
- 本次静态扫描未发现源码路径与 package 声明不一致，也未发现这些迁移候选对应的 Manifest/导航注册问题。此文档扫描未运行构建或运行时验证。

## 本轮审计范围与待查项

本轮逐项核对了 Vault、Settings、Autofill、Backup、Database Reset/Recovery、Scanner、Shell、Unlock 的源码组织、关键生产调用方、Data/Security 依赖边界、Hilt/Manifest 注册、相关资源入口、既有测试位置与正式文档。除已记录的 Vault 专属共享展示 helper 和应用 Activity 归位外，其余候选按证据保留；未对整个工作区做按行数或目录名驱动的批量搬迁。后续若发现新的分散职责，应沿用同一证据表逐功能补记，不把未核对项描述为迁移结论。

- 待查：`presentation/shared` 中实际跨功能使用的组件目前依赖调用分布判断；若未来消费者变化，再复核其共享所有者，不按名称归类。
- 待查：本次检查是静态源码、注册、测试树和文档审阅；没有运行功能测试或设备级验证。

## 验收条件

- 每个移动文件都有生产所有权和真实使用方证据。
- 不残留旧 import、package、DI 注册、Manifest/导航引用、测试路径或文档路径。
- 受影响的模块边界与功能验证通过，`git diff --check` 无错误。
- 审计记录明确区分迁移、保留和待查项；不改动无关的工作区内容。
