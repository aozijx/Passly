# 2026-11-06 Presentation 与映射边界审查记录

## 范围

归档维护 TODO Task 70–72 的完成证据。当前行为合同留在对应功能页与开发规范；测试状态为实现时记录，本次没有重跑。

## Route 到 ViewModel 的 seed 所有权（Task 70）

`VaultGraphRegistration` 解析 `backStackEntry.arguments` 中的 seed token，并由 Route 调用 `EntryEditorSeedCoordinator.consume()` 一次性读取 `AddWifiInitialContent`。Route 先调用 `AddWifiViewModel.beginInitialization()` 预约初始化资格；只有 `NeedsInitialContent` 才消费 seed 并把 `AddWifiInitialContent` 值传给 `AddWifiViewModel.initialize(initialContent)`。`AlreadyInitialized` 防止 Route 重建重复消费；锁定时 `Unavailable` 不读取 seed；编辑模式绕过创建 seed。ViewModel 不再执行 Route 提供的 `() -> AddWifiInitialContent` 闭包，也不持有 NavBackStackEntry/coordinator。

历史验证：Blank、Scanned、Unavailable、锁定、编辑优先、重复初始化及 seed 一次性消费的定向测试 33 项通过；`:app:ktlintCheck` 与 `verifyArchitecture` 通过。没有新增 Nav Compose 集成测试或设备 UI 验证。

## ViewModel 参数边界与静态门禁（Task 71）

- `SourceBoundaryVerifier` 的 ViewModel 声明扫描检查构造器、属性、函数签名、嵌套泛型和可执行引用；解析同包、import alias、FQN、typealias chain、可见传递基类、成员调用，以及 Kotlin `${...}` / `$name` 模板代码，同时忽略注释、普通字符串和转义美元。
- 门禁阻止 ViewModel 跨 ViewModel 注入/调用，业务函数与纯 UI Content 不接收整个 ViewModel。Route/Activity composition owner 仍可持有多个独立 ViewModel，并将不可变 state 与窄 action/effect callback 交给 Content。`VaultRoute(vaultViewModel, scannedOtpImportViewModel)` 的双 owner 由 Activity-scoped Scanner 流程解释，不复制状态。
- 删除 `DiagnosticsRoute.kt` 的 `DiagnosticsViewModel.onDocumentSelected(uri)` 私有扩展。三个 SAF launcher 在 Route 内将 nullable URI 映射为 `SaveToDocument` / `SaveDocumentCancelled` Action，保留 URI 字符串化和 pending request 清理。
- 局限：这是轻量源码符号解析，不等价于 Kotlin 编译器；复杂 lambda、多行表达式、destructuring、扫描输入外的自定义父类型和 typealias 不完整解析。

历史验证：SourceBoundaryVerifier 正反例 27 项、Diagnostics ViewModel/Reducer 20 项、`:buildSrc:test`、`:buildSrc:ktlintCheck`、`:app:ktlintCheck`、`verifyArchitecture` 通过。复审发现的同文件漏报、alias 误分类、限定名误配、值名遮蔽和泛型边界均有回归覆盖。

## 穷尽映射与分支热点（Task 72）

- `VaultAddTypeUiModel` 保留九种筛选/创建共用的 UI 项；同一 Presentation 定义集中提供 `AddType`、label、description、icon。`AddType.toUiModel()` 保留显式穷尽 `when`，删除反向只读桥和无人调用的 `AddType.fabMenuOptions`。`VaultEditorDestinationRegistry` 仍只为四种已支持创建类型提供目标；`AddType.entryTypes` 的业务分组不迁入 UI 元数据。
- `SensitiveFieldSetMapper` 以穷尽 `EntryCredential` projection 共用敏感字段提取与编辑更新；bundle redaction/reassembly 保留独立 typed copy 分支，因为其清除、保留、列表和嵌套 OTP 转换语义不同。`None` / Wi-Fi 也使用显式分支。Backup 通过 `SecretFieldStore` 读写，mapper 收窄为 Data `internal`，不再以 App API 暴露。
- 其他 `when` / `is` 热点按合同保留：sealed reducer/state/action 转换、字段读取/资源映射、稳定存储键和二进制 tag 映射保持穷尽分支；不为减少分支数引入无覆盖保证的 Map、反射或纯转发 handler registry。
- 扫描静态计数：1,406 个 Kotlin/KTS 文件中生产代码 `when` 721、`is` 1,043；测试 `when` 33、`is` 303。计数用于范围/热点定位，不表示代码异味数量，也不等于行为测试。

历史验证：Vault 映射覆盖 9 个 UI 类型及 4 个创建目标；Data 覆盖 11 个高敏 key 的提取、替换、清除、保留和 bundle 往返；定向 Data 15/15、App 14/14、Data/App KtLint、`verifyArchitecture` 与 `git diff --check` 通过。故意遗漏 `CARD_PAYMENT_PIN` 的 projection 时，全量敏感键测试按预期失败。

## 代码位置

| 职责 | 关键函数/类型 | 源码 |
| --- | --- | --- |
| 一次性 seed 所有权 | `beginInitialization()`、`initialize(initialContent)`、`consume()` | [`AddWifiViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiViewModel.kt)、[`VaultGraphRegistration.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultGraphRegistration.kt)、`EntryEditorSeedCoordinator` |
| ViewModel 签名门禁 | ViewModel 声明扫描、source boundary verification | [`SourceBoundaryVerifier.kt`](../../buildSrc/src/main/kotlin/passly/SourceBoundaryVerifier.kt)、[`EditorSourceBoundaryVerifier.kt`](../../buildSrc/src/main/kotlin/passly/EditorSourceBoundaryVerifier.kt) |
| 九种新增类型 UI 映射 | `VaultAddTypeUiModel`、`AddType.toUiModel()` | [`VaultAddTypeUiModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/model/VaultAddTypeUiModel.kt) |
| EntryCredential 敏感字段投影 | `SensitiveFieldSetMapper` 与 `EntryCredential` subtype projection | [`SensitiveFieldSetMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/SensitiveFieldSetMapper.kt)、[`EntryCredential.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/credential/EntryCredential.kt) |
