# 完整表单编辑

状态：当前实现。

详情页的 Login、OTP、Bank Card、Wi-Fi 条目使用完整表单编辑。其他 `EntryType` 保留逐逻辑字段编辑。
本文说明认证读取、表单 seed 交接及保存合同；创建与通用写入边界见[编辑与写入](entry-editor-and-persistence.md)。

## 认证读取与路由

点击完整编辑入口后，调用链从 `DetailViewModel.prepareEntryForEdit()` 开始：它从当前 `uiState.entry` 取 `sourceEntry.id`，调用 `loadEditableEntry(sourceEntry.id)`，并在返回后再次核对条目 ID、类型、认证对象身份、完整会话访问权和当前详情条目 ID。任何一项不匹配都返回 `null`，不创建导航 seed。

`LoadEditableEntryUseCase.invoke(entryId)` 先调用 `hasFullSecureSessionAccess()` 并读取 `authenticationState.value`，再调用 `entryQueryRepository.getById(entryId)` 和 `sensitiveFieldRepository.getPresence(entryId)`。它以 `EntryTypeDefinitions[type].fields` 将高敏字段键映射到表单 `FieldKey`，只接受当前类型声明的键。Login、OTP、Bank Card 中实际存在的高敏字段按存在集合构造 `AuthorizationScope.SensitiveFields(entryId, requestedKeys, REVEAL)`，再调用 `authorizationGate.authorize(scope) { permit -> ... }`；授权闭包内调用 `revealMany(entryId, requestedKeys, REVEAL, permit)`。结果必须完整、属于同一条目、键不重复且仍有同一认证会话访问权，才通过 `Entry.withRevealedField(field, value)` 组装可编辑条目。Wi-Fi 密码来自 `getById()` 返回的解密凭据，不走高敏字段 reveal。读取前后还会重读条目版本/类型；成功后调用 `activityRecorder.recordUsage(entryId.value, VIEW)`，并再次确认访问权。

目标不存在、类型不支持、授权拒绝、字段不完整或会话过期时不进入编辑页，也不回退到其他类型。加载成功后再次核对详情访问凭据与认证会话，再将条目交给编辑目的地。

当前 `EditableEntryLoadResult` 表达的是查询成功、缺失、不支持、拒绝、取消与会话不可用等预期结果；`LoadEditableEntryUseCase.invoke(entryId)` 没有将 Repository、授权或 `ActivityRecorder.recordUsage()` 抛出的普通异常映射为该结果。此调用由 `DetailViewModel.prepareEntryForEdit()` 在 `DetailRoute` 的 `editScope.launch` 中发起；异常会从协程逸出，Route 当前没有相应失败 state/effect 或重试提示。取消与 fatal `Error` 也不映射为普通加载失败。此异常反馈/重试边界见[维护 TODO Task 42](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)。

导航时，`VaultGraphRegistration` 的 `onEditFullForm(editableEntry)` 调用 `seedCoordinator.stage(editableEntry)`。`EntryEditorSeedCoordinator.stage(seed)` 检查完整会话，并要求 `seed.authentication` 与当前认证对象为同一实例，然后调用 `EntryEditorSeedStore.stage(seed)`；Store 生成 `EditorSeedToken.new()` 并以单槽保存 seed。目的地注册表用 `editRouteFor(editableEntry.entry.type, staged.token.value)` 选择 Login/OTP/Bank Card/Wi-Fi 路由，`AppRoute.Edit*.createRoute(seedToken)` 只把 token 插入路由字符串。未支持类型会调用 `seedCoordinator.discard(staged.token)`。编辑目的地组合后，`registerEditorEditDestination(...)` 安装 `DisposableEffect(seedToken, seedCoordinator)`；离开未消费目的地时解析 token 并调用 `seedCoordinator.discard(token)`，Store 只在 token 仍匹配单槽当前值时清除它。这个清理覆盖目的地已组合后的退出；若 `context.navigateToRoute(route)` 在目的地组合前抛异常，stage 与导航之间仍无 catch/finally 补偿，seed 会保留到被替换、会话撤销或 graph ViewModel 清理。关闭 Store 只释放对 seed 的受控引用，不会原位擦除 Entry 中不可变 String。该失败窗口仍由[维护 TODO Task 42](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)跟踪。路由参数不是条目 ID，也不携带 `Entry`。

## 一次性交接与敏感状态

详情路由只传 `seedToken`。完整 `Entry` 留在 Vault graph 范围内的 `EntryEditorSeedStore`，由 `EntryEditorSeedCoordinator` 持有其路由级访问控制。Store 只保留一个 seed；消费必须匹配 token，成功消费即清空槽位，路由销毁时未消费的 seed 会被丢弃。

可编辑 seed 绑定读取时的完整认证对象。锁定或认证会话切换会清空 seed；编辑器消费后还会核对条目类型和同一认证对象。锁定、撤权或编辑 ViewModel 关闭时，由编辑 ViewModel 清空已回填的表单。这里的“清空”是清除 Store/UI state 对对象的引用；`LoadEditableEntryUseCase` 当前通过 `SensitiveValue.toCharArray()` 构造不可变 `String` 并写入 `Entry`，随后只覆盖临时 `CharArray`，不能保证 JVM 中的 String 副本已被原位擦零。明文条目不进入导航参数、SavedState 或 Intent；该内存副本边界及后续缓冲类型评估见[维护 TODO Task 42](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)。

`registerEditorEditDestination()` 从 `backStackEntry.arguments["seedToken"]` 取字符串，并建立 `consumeSeed(rawToken)`：该闭包调用 `EditorSeedToken.parse(rawToken)`，有效时调用 `seedCoordinator.consume(token)`，无效时返回 `Missing`。各编辑 Route 将消费结果交给对应 ViewModel 的同步 `loadExistingEntry(result)`；它只验证 seed 类型、完整访问和认证对象身份，再从 seed 中的 `entry` 映射表单，不调用 Repository 或 `LoadEditableEntryUseCase`。无效/缺失 seed 会进入 `failEditLoad()` 并由 Route 显示 Snackbar；有效 seed 设置 `editingEntryId`，后续重复载入会被忽略。Route 的 `DisposableEffect` 在目的地销毁时调用 `discard(token)`，仅在 seed 尚未消费且 token 仍匹配时清除它。若 seed 无效，用户需返回详情页重新发起编辑以生成新 token；此路径与 Detail 预加载异常后的重试不是同一 owner。

## 更新合同

编辑器把 seed 映射到对应类型的表单，不重复读取条目或重新请求敏感字段授权。保存使用 `EntryEdit.ReplaceForm`；更新用例重新读取当前条目、执行完整 Domain 校验，并通过预期版本提交，冲突时最多重新读取并重试一次。

具体参数链为：类型专属 ViewModel 的 `EditorWriteSession.save { current.form.toEntryDraft() }` 调用其 `write(draft)` 闭包；编辑模式将 `editingEntryId` 与 `EntryEdit.ReplaceForm(draft)` 传给 `EditEntryUseCase.edit(entryId, edit)`。该用例在 mutex 内调用 `performEdit(entryId, edit, observeType)`，每轮以 `entryQueryRepository.getById(entryId)` 取得 `latest`，再执行 `edit.applyTo(latest)`。有效变更通过 `entryCommandRepository.updateEntry(id = entryId, expectedVersion = latest.version, changes = changes)` 提交；首次 `Conflict` 才重读并重试，成功后再次 `getById(entryId)` 返回持久化结果。无变化时直接返回 `latest`。创建模式仍调用 `entryCreation(draft)`（Domain port 为 `EntryCreation.invoke(draft)`），不走编辑用例。

完整表单不拥有的字段继续保留，包括图标、收藏、过期时间、域名关联、自定义字段及非表单关联。密码表单可以修改主关联应用，但保存仍保留其他已有应用关联。Wi-Fi 编辑使用回填数据，不触发扫码 seed 或创建初始化。失败保留用户编辑内容；成功后清空敏感表单并返回详情。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 认证后读取当前详情条目并准备完整编辑 seed | `DetailViewModel.prepareEntryForEdit()` -> `loadEditableEntry(sourceEntry.id)` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| 认证后读取可编辑条目 | `LoadEditableEntryUseCase.invoke()` | [`LoadEditableEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/LoadEditableEntryUseCase.kt) |
| 将可编辑 seed 存入 graph owner 并把一次性 token 用于选路 | `VaultGraphRegistration.onEditFullForm(editableEntry)` -> `seedCoordinator.stage(editableEntry)` -> `EntryEditorSeedCoordinator.stage(seed)` -> `EntryEditorSeedStore.stage(seed)` -> `VaultEditorDestinationRegistry.editRouteFor(editableEntry.entry.type, staged.token.value)` | [`VaultGraphRegistration.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultGraphRegistration.kt)、[`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt)、[`VaultEditorDestinationRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt) |
| 将 seed token 编入类型专属编辑路由 | `AppRoute.EditPassword/EditOtp/EditBankCard/EditWifi.createRoute(seedToken)` | [`AppRoute.kt`](../../../app/src/main/java/com/aozijx/passly/app/navigation/AppRoute.kt) |
| 一次性 seed 与认证会话归属 | `EntryEditorSeedStore`、`EntryEditorSeedCoordinator` | [`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt)、[`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt) |
| 解析路由 token、消费 seed 并在目的地销毁时丢弃未消费 seed | `EditorSeedToken.parse(seedToken)`、`seedCoordinator.consume(token)`、`seedCoordinator.discard(token)` | [`VaultGraphRegistration.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultGraphRegistration.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
| 编辑器校验 seed 类型和认证身份 | `matchesCurrentAccess()` | [`EditableEntrySeedAccess.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditableEntrySeedAccess.kt) |
| 将表单状态映射为 EntryDraft 并提交编辑/创建分支 | `EditorWriteSession.save { form.toEntryDraft() }` -> `write(draft)` | [`EditorWriteSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt)、[`AddPasswordViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordViewModel.kt)、[`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt)、[`AddBankCardViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/bankcard/AddBankCardViewModel.kt)、[`AddWifiViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiViewModel.kt) |
| 更新条目并检查预期版本 | `EditEntryUseCase.edit(entryId, edit)` | [`EditEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EditEntryUseCase.kt) |
| 将完整表单应用为 Domain 编辑 | `EntryFormEditor.apply()`、`EntryEdit.ReplaceForm` | [`EntryFormEditor.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryFormEditor.kt)、[`EntryEdit.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryEdit.kt) |
