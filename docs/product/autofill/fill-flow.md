# Autofill 共享填充流程

状态：当前实现（代码核对：2026-11-10）。

## 应用内部输入边界

Passly 自身的认证、备份密码、恢复及凭据编辑输入不作为系统填充目标。
Core 的 `Modifier.excludeFromAutofill()` 在真实可编辑节点声明 `ContentDataType.None`，明确表示不可填充；
不清除文本编辑、焦点或无障碍语义。密码显示切换不改变此策略。公共输入组件和直接使用的 Compose 文本框遵守同一约束。
五个 Activity 宿主在创建时调用同名的 Activity 扩展：以 `NO_EXCLUDE_DESCENDANTS` 排除自身 View 子树，并取消当前宿主的填充/保存会话。
只覆盖 Passly 内部输入，不停用系统 Autofill 服务，不取消其他应用的会话，也不改变对外候选发现和结果交付。
应用密码、备份密码、编辑页实际语义节点及宿主排除标记由 `InternalInputAutofillTest` 验证；编辑页原有 IME 焦点测试继续保留。

Android 12+ 有三套平台入口：Legacy `AutofillService` 在 API 31+ 提供填充、`SaveInfo` 和 `onSaveRequest`；
Credential Manager Provider 在 API 34+ 提供 Password get/create/clear-state。两者共享平台无关的候选发现、
授权、单条秘密解析、草稿校验和 Entry 写入，但结果合同各自独立，不互相转换 `Dataset/FillResponse/SaveCallback`
和 Credential Manager response。平台细节见[Legacy Service](legacy-service.md)与
[Credential Manager Provider](credential-manager.md)，保存现状与目标见[保存流程](save-flow.md)。

第三套入口是用户自行在 Android 无障碍设置中启用的手动回退。系统 Autofill 未显示时，用户点击 Passly 的
浮动按钮，服务才读取当前可编辑节点；它复用候选发现、认证和单条解析，但通过 `ACTION_SET_TEXT` 交付，
不参与 Legacy 保存或 Credential Manager response。Legacy 的 compatibility package 由 Android 框架提供
兼容处理，不等于 Passly 无障碍服务。
选择页先关闭，原窗口重新成为活动窗口后才执行写入；待交付凭据只在进程内暂存至多 10 秒，过期取消 interaction。
无障碍 Service 通过 `AutofillLaunchTarget` 请求不含 token 的显式 Activity Intent；选择页从同进程的单次
会话存储读取请求，具体页面类只由 App 平台适配器引用，Feature 不依赖 Presentation。
服务任务在协程启动前或执行期间取消时，都会清除本次暂存请求和 trace continuation，并继续传播取消；普通启动错误才记录为回退失败。

`AccessibilityFillSessionStore` 是进程内单槽 owner，保存 `Captured` 或 `Pending` 两种状态：capture token、目标 package/window、捕获时间、可编辑节点引用；选择完成后再附加角色映射、结果 payload 与一次性交付 receipt。`ManualAccessibilityFillService.onServiceConnected()` 绑定弱 service 引用，`onDestroy()` 调用 `unbind()` 清空会话、回调和过期任务；无障碍选择 Activity 的 Intent 只含模式，不含 token、节点或凭据。`capture()` 仅由用户点击浮动按钮/系统无障碍按钮触发，确认焦点目标属于可见、可编辑的外部应用后，最多遍历 256 个节点并提取最多 32 个字段的元数据，不读取输入框当前文本；capture 总有效期为 30 秒。

`AccessibilityFillViewModel.start()` 从 `pendingCapture()` 取得请求；候选发现、认证和选择完成后调用 `prepareDelivery()`，将 payload 与 receipt 暂存最多 10 秒。原窗口重新活动时，Service 调用 `claimWhenTargetActive()` 检查 package、window ID 和 capture 时限，并先把单槽置空，再返回一次性 `DeliveryClaim`。`DeliveryClaim.commit()` 会再次检查目标窗口并刷新每个节点，确认节点仍可编辑且支持 `ACTION_SET_TEXT` 后逐字段写入。平台强制要求的单次 `Bundle` 在每个 action 的 `finally` 中清空。页面退出、取消与服务销毁会清理各自 owner 的暂存并结束对应 interaction；capture 超时清除捕获槽，pending 交付超时调用服务回调取消 interaction。宿主 teardown 与 session lock 并发收尾的真实行为仍见 Autofill Task 21。

| 入口与展示 | 触发 | 候选 UI | 交付 | 边界与回退 |
| --- | --- | --- | --- | --- |
| Legacy 悬浮 | 系统 `FillRequest` | 系统下拉菜单 `RemoteViews` | `Dataset` | 用户选“悬浮候选”或键盘不支持 Inline 时使用 |
| Legacy Inline | 系统 `FillRequest` 且键盘提供 Inline 规格 | 键盘候选条 `InlinePresentation` | 认证后 `Dataset` | 受键盘最大数量和规格约束；无请求时回到系统菜单 |
| Legacy BottomSheet | 系统 `FillRequest` | 认证入口打开应用内 `ModalBottomSheet` | 选中后 `Dataset` | 无系统请求则不会出现 |
| 无障碍浮动按钮 | 用户点按钮 | 应用内候选选择器 | 返回原窗口后 `ACTION_SET_TEXT` | 仅针对原窗口和包名，目标 30 秒过期，待交付最多 10 秒 |
| Credential Manager | Android 14+ 凭据请求 | 系统凭据 UI | Provider response | 不受 Legacy 展示设置控制 |

这里的“悬浮候选”指系统 Autofill 下拉菜单；“无障碍浮动按钮”是独立的手动触发入口。Android 是否最终显示
Inline 仍由输入法决定。无障碍入口不尝试根据浏览器包名猜网页来源，网页没有可验证 origin 时不会扩大作用域。

## Legacy 结构解析与字段分类

`AutofillStructureParser` 只负责从 Android `AssistStructure` 抽取可编辑字段的静态元数据和值，不判断
username/password/OTP 语义。`ParsedStructure.toAutofillRequest()` 将结果映射为平台无关的 `AutofillRequest`，随后仅由
`HeuristicMatchStrategy` 的规则链分类字段，避免解析层和策略层各维护一套关键词规则。该映射由
[`AutofillRequestMapping.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/mapping/AutofillRequestMapping.kt) 实现。

结构遍历在 Autofill 后台协程中执行，并使用迭代式先序遍历；单次请求最多访问 10,000 个节点、最多下探 64 层，
避免复杂页面或异常 WebView 造成回调线程卡顿或递归栈溢出。限制不会改变限制范围内的窗口和节点顺序。

提交按钮不作为 Legacy 保存协议的输入。`SaveInfo` 只声明已分类的密码字段、可选用户名字段，并使用
`FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE` 判断表单离场，因此不恢复无消费方的 `submitId`。

保存草稿不会再以 `roleMap` 或节点的偶然顺序覆盖候选：每种角色优先唯一的聚焦非空字段，否则只接受唯一非空字段。
多个未聚焦密码候选会拒绝保存；多个未聚焦用户名候选会留空，避免把不同表单的用户名和密码拼成一条凭据。

## 发现与授权

`DiscoverAutofillCandidates` 从 Domain 的 `AutofillSettingsSource.autofillSettings` 读取 Autofill 专属策略，
再规范化目标作用域、创建/恢复 interaction，并只查询
`AutofillCandidateSummary` 元数据。候选阶段不读取 Entry 聚合、密码或 OTP secret。

### 设置项与生效阶段

| 设置 | 生效位置 | 当前效果 |
| --- | --- | --- |
| `enabled` | 每次 `DiscoverAutofillCandidates.invoke()`；Legacy 保存回调与 Credential Manager create 入口也分别检查 | 关闭后拒绝新的发现/保存操作。 |
| `credentialManagerEnabled` | Credential Manager 候选发现、begin-create 与最终 create 保存 | 只关闭 Credential Manager 流程，不控制 Legacy 或 Accessibility。Create 保存时会再次读取。 |
| `requireAuthentication` | 新建 get/fill interaction 时传给 `StartAutofillInteraction` | 控制新交互的授权要求；Credential Manager create 由系统显式创建动作和 Vault session 状态处理，不使用该开关。 |
| `strictDomainMatching` | 发现候选时传入 `AutofillCandidateQuery`，并随发现上下文传入凭据解析 | 控制网页候选和 OTP 条目自身网址配置的匹配严格度；不改变 interaction 绑定的精确 host。 |
| `otpAutofillEnabled` | 随发现上下文传入凭据解析 | 全局 OTP 填充开关，默认关闭；只影响请求中识别出 OTP 字段的填充。 |
| `presentation` | Legacy Autofill response 构造 | 选择 Legacy 的 FLOATING、SYSTEM_INLINE 或 BOTTOM_SHEET 展示；Credential Manager 与 Accessibility 使用各自的平台展示流程。 |

设置在 discovery 时读取；解锁后重新发现会重新读取策略。候选展示后的 `ResolveAutofillSelection` 不重读这些设置，Credential Manager create 则在最终保存时再次检查两个开关。因而关闭开关后已展示候选是否仍可完成，当前调用路径已有明确差异，但整体产品策略仍待裁定，见 [Task 82](../../project/work/plans/2026-11-07-passly-autofill-followups.md#task-82)。

包名和网页域名由 Domain `AutofillScope` 统一规范化。包名 trim 后按 Locale.ROOT 小写，空值拒绝；网页域名缺失
表示原生应用 scope，畸形域名返回 `INVALID_SCOPE`，不会降级为包名搜索。网页 scope 使用精确 host：忽略 scheme、
port、path，保留 `www` 子域，去掉一个末尾 DNS 根点；Unicode IDN 由 `java.net.IDN` 转为小写 ASCII，RFC 3490
有损映射会被拒绝（例如 `faß.de` 及其现代 A-label），避免与 `fass.de` 合并；bracketed IPv6 按 RFC 5952
规范化并拒绝 zone identifier。这样网页请求按域名检索和保存，不会将浏览器
包名写成网站凭据的 application ID。
“严格匹配网站主机”默认开启，Data 对 DAO 子串检索结果再做相同的精确 host 比较。关闭后，候选 host 与请求 host 只有在
Guava Public Suffix List 给出相同可注册域名时才互认；`co.uk` 等多段公共后缀正确计入，`blogspot.com` 等私有后缀继续
隔离不同租户。未知后缀、公共后缀本身及 IP 仍只允许精确匹配。宽松规则只扩大候选查询，interaction 授权仍绑定请求的
精确 host，保存关联仍写入规范化的精确 host。候选只返回 app/domain 匹配的条目，没有未关联条目回退。

Legacy 和手动无障碍入口可用 `HeuristicMatchStrategy` 识别非标准表单；Credential Manager 使用自己的严格 option/request 解析。
平台 adapter 不直接依赖 Entry repository 或认证实现。

需要解锁或设置要求每次验证时，`AuthorizeAutofillInteraction` 以 `AuthenticationPurpose.AUTOFILL` 授权同一个
interaction。并发授权合并为一次认证；成功后重新发现或最终选择不再次认证。interaction 绑定来源、规范化作用域和
实际展示过的 Entry IDs，不是按包名授予的时间窗口。

“本轮没有认证弹窗”不等于无密钥解密：只有设置关闭“填充前验证”且已有完整 `Authenticated` Vault 会话时，或已有
Autofill lease 仍持有本轮临时会话时，interaction 才可从创建时进入 `Authorized`。Vault 锁定或设置要求新鲜认证时，
先完成 Autofill 认证，之后才查候选和响应选择。候选的 title/username 是 SQLCipher 数据库中的元数据列，并非单独的
字段密文；Vault 锁定时数据库会话不可读。选择解析只读取请求中实际出现的凭据字段：账号来自已读取的条目元数据列；
只有请求密码时才调用 `SecretFieldStore.readField(PASSWORD)`，只有请求 OTP 且开关开启时才查找 OTP 来源并调用
`SecretFieldStore.readOtpGenerationMaterial()` 读取低敏配置和独立 OTP seed。未请求的密文字段不解密，返回的
`AutofillCredentialPayload.fields` 只包含实际解析到的请求字段。读取操作调用前经过
`ResolveAutofillSelection` 的 interaction 状态与展示候选校验，以及 resolver 的 Autofill session-access 检查；字段密钥
由 DEK 派生，SEALED 锁定会在数据库 session 排干后清除 DEK 和派生 field key。这里没有“提前解密、等认证完成再填充”的
缓存路径。单元回归测试覆盖新鲜认证未完成时 resolver 调用次数为零。

正常返回的取消、失败与拒绝由各平台流程映射为取消或不可用状态。并发授权请求共享同一 attempt；拥有 attempt 的调用在认证、lease 和终结阶段失败时，会在 `NonCancellable` 收尾中终结共享结果并移除当前 attempt。若已获取 Autofill lease，失败清理会释放 lease；普通清理异常作为 suppressed failure 保留，fatal `Error` 继续传播。取消写入取消终态后仍传播原取消，不会让等待者永久挂起。

Legacy 的 `AutofillFillViewModel.initialize(request)` 先调用 `authorizeInteraction(request.interactionId)`；授权成功后，`continueAuthorized(request)` 调用 `discover(request)`，再把 `request.toDomainRequest()`、同一个 `request.interactionId`、`request.roleMap()` 和 `request.directEntryId` 分别传给 `DiscoverAutofillCandidates.invoke(request, interactionId, classifiedRoles, directEntryId)`。已分类 role map 因而不会在 Domain use case 中被第二次启发式解析。发现结果若为 `Candidates` 且请求含 `directEntryId`，ViewModel 直接以该 ID 进入选择解析；底部候选模式则先保存 trace continuation，再发布候选 UI；系统菜单/Inline 模式直接构造 discovery response。

`DiscoverAutofillCandidates.invoke()` 用 `request.source` 选择启发式或严格字段匹配器，并从 `request.packageName`、`request.domain` 构造 `AutofillGrantContext(...).normalized()`。新交互时，它将规范化 scope、`request.source.toInteractionSource()` 和设置中的 `requireAuthentication` 传给 `StartAutofillInteraction`；已有交互则按 `interactionId` 读取 snapshot 并要求 scope 完全相同。候选查询只按应用包名或网页域名匹配，统一限制为最多 10 条，不提供未关联条目回退，也不扫描密码明文寻找相同密码。网页 scope 时 `packageName = null`，原生应用才传规范化 package；返回的 candidate IDs 再传给 `interactionStore.recordCandidates(snapshot.id, ids)`，使最终选择必须属于实际展示集合。

候选查询返回后、记录或发布候选前会重新检查协程取消；若请求在此期间取消，流程会在 `NonCancellable` 清理中终结当前 interaction，再传播原取消，避免查询已返回但 interaction 仍处于活动状态。该边界由 [`DiscoverAutofillCandidatesTest.cancellationAfterCandidateQueryClosesInteraction`](../../../feature/autofill/src/test/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidatesTest.kt) 覆盖。

传统 Autofill 的保存提示不再有独立开关：全局 Autofill 启用且字段分类识别出有效密码字段时，响应始终带上系统 SaveInfo；全局关闭时不返回填充响应，也不接受保存请求。Credential Manager 的创建流程仍由其自身平台能力和设置控制。

## 解析秘密

`ResolveAutofillSelection` 不重读发现时的设置。它验证 interaction 存在、scope 相同且已授权，再通过状态机确认 Entry ID 确实展示过。
Credential Manager 的 `allowedUserIds` 在 `CredentialManagerFlow.selectPassword()` 收到解析结果后独立校验，Legacy 不使用该限制。
`ResolveAutofillSelection` 将本次请求的 `AutofillCredentialField` 集合传给 `AutofillCredentialResolver`；Credential Manager 请求账号和密码，Legacy 与 Accessibility 按识别出的字段角色请求对应字段。账号只从已查询的条目元数据构造，密码仅在请求密码时通过 `SecretFieldStore.readField(PASSWORD)` 解密。
请求 OTP 且 `otpAutofillEnabled` 开启时，resolver 优先查找选中登录条目的 `OTP_FOR` 关联。关联 OTP 必须符合它自身的包名或网址范围，否则本次不填 OTP。登录条目没有关联时，才从符合各自包名或网址范围的活动 OTP 条目中选择；仅唯一候选可回退，零个或多个候选都不填 OTP。通过范围匹配后，`SecretFieldStore.readOtpGenerationMaterial()` 读取该 OTP 的配置和独立加密 seed 并生成验证码。解析结果仅包含请求且成功取得的非空字段；未请求字段不会解密或进入 payload。
Base32 secret 的字符、补位和尾部位由 Domain 的 `OtpSecretCodec` 统一校验；`OtpGenerator` 遇到损坏密钥时
返回 `InvalidSecret`，不生成或填充 OTP 码。

Legacy 选择时，`AutofillFillViewModel` 将 `request.roleMap()` 转换为 `requestedFields`，并与 interaction、scope、选中的
`EntryId`、OTP 开关和域名匹配策略一起传给 `ResolveAutofillSelection`。use case 在解析前再次规范化并比较 scope，再由
`interactionStore.beginSelection(interactionId, entryId, sessionAtMs)` 原子检查展示资格，最后将显式字段集合封入
`AutofillCredentialSelection` 交给 resolver。OTP 开关关闭时，use case 从集合中移除 OTP 字段。Credential Manager
请求 `USERNAME` 和 `PASSWORD`，不会请求 OTP；平台解析的用户限制由 Credential Manager flow 在拿到结果后额外校验 username，
Legacy 不使用该参数。

Legacy 网页请求的 `webDomain` 在当前版本未经过 DAL 或应用签名验证，故先返回不含候选账户信息的通用确认操作。
用户确认应用标签/包名与规范网页 host 后才查询并展示候选；选中条目后，还要在 secret resolver 读取前确认条目标题和账号。
接收方与条目确认存于 process-local interaction，并绑定精确 scope、当前认证 session generation 和已展示 `EntryId`；锁定或
换 session 后旧确认失效。不同网页 host、无效 claim 混合或带网页 claim 的截断结构在候选查询前拒绝。

## 结果与关闭顺序

Legacy Activity 的输入解析和领域请求映射分别由 `AutofillFillRequestParser`、`AutofillFillRequestMapping` 负责；
Activity 只编排平台结果，ViewModel 只编排授权、发现和选择。Legacy 结果用 `DiscoveryResponse`、
`SelectedResponse`、`SelectedDataset`、`Cancelled` 明确区分，不使用 nullable receipt 猜测是否需要关闭临时会话。
Credential Manager 同样用 `Immediate` 与 `CommittedSelection` 区分普通平台响应和已选择凭据。

`LegacyAutofillResultTarget` 是 Legacy 启动协议的一部分。`LegacyResponseFactory.buildUnlockResponse(context, interactionId, discoveryContext, parsed, inlineRequest)` 根据 `discoveryContext.presentation` 调用 `forLockedPresentation(presentation)`：`BOTTOM_SHEET` 映射为 `DATASET`，`FLOATING` 与 `SYSTEM_INLINE` 映射为 `RESPONSE`。同一目标传给 `AutofillPendingIntentFactory.createUnlockIntent(..., uiMode, roleIds, resultTarget, inlineRequest)` 并决定 builder 构造需认证占位 `Dataset` 还是 `FillResponse.setAuthentication()`；单候选和已解锁底部 picker 则由 candidate/picker Intent 显式使用 `DATASET`。PendingIntent 工厂将目标的稳定 wire value 写入 extras，`Intent.toAutofillFillRequest()` 拒绝缺失或未知值。认证结果由 Activity 按目标交付，列表选择回调仍传类型化 `EntryId`，不经过字符串往返转换。完整平台分支见[Legacy Service](legacy-service.md#认证结果目标与-bottomsheet-选择)。

Compose 路由将工作流状态映射为显式的 `Hidden` 或 `Candidates` 显示状态；终态提交属于独立副作用，不再通过
nullable 候选列表或组合函数提前返回表达“当前不显示 UI”。

BottomSheet 显示选择账号标题和候选数量；返回键或点按外部区域会取消 interaction。Legacy picker
Activity 在结果尚未提交给 Android 时离开前台，会取消认证、发现或选择任务并释放本轮 Autofill lease（配置变更除外），避免后台任务继续占用临时会话。
Credential Manager 和手动无障碍选择页同样在宿主停止时放弃未提交工作；即使页面任务尚未开始，也按启动请求或待处理捕获清理。
这一边界依据 Android 的 [Activity 生命周期](https://developer.android.com/guide/components/activities/activity-lifecycle)：`ON_STOP` 表示宿主已不可见；配置变更期间保留 ViewModel 工作。
60 秒 process-local interaction timeout 仍作为 Activity/进程异常退出的兜底。主动取消只向 Android
返回 `RESULT_CANCELED`；当前没有“每个空结果都必须弹 Toast”的合同，避免把用户主动关闭误报为失败。Activity 内认证
被拒绝时记录结构化诊断并取消结果；服务入口异常走 `FillCallback.onFailure()`，Android 是否显示提示由系统 Autofill UI
决定。

Legacy、Credential Manager 与无障碍宿主在未提交结果时退出，记录 `autofill.host.abandoned`；系统取消填充请求记录
`autofill.request.cancelled`。平台结果提交异常使用 `autofill.result.delivery_failed` 并标记来源；候选查询异常在
`autofill.discovery.unavailable` 中保留异常类型与原因。日志只使用来源枚举、经哈希处理的目标标识及安全的计数/原因，
不记录候选标题、用户名、密码、OTP 或异常原始消息。取消在候选查询返回和结果发布前显式检查，并释放已创建的 interaction；
Credential Manager 服务在已构造响应但系统取消回调时，也立即取消对应 interaction。

平台 Activity 在 `AutofillResultDelivery.commitResult(receipt, commitToPlatform)` 的回调内连续执行 `setResult`
和 `finish`；仅调用 `setResult` 只是保存待返回值，不表示 Android 已收到结果。Legacy 的 receipt 由
`AutofillFillViewModel.returnDataset(request, outcome)` 以 `request.interactionId` 和 `outcome.payload.entryId`
创建，平台结果按 `request.resultTarget` 选 `Dataset` 或 `FillResponse`，再通过宿主的 `AutofillHostCompletion.ActivityResult` 交付
交付；Credential Manager 的 receipt 使用当前 `interactionId` 与 `payload.entryId`，`resultIntent` 经相同提交回调
交付。提交开始即关闭本轮 lease 的借用窗口；callback 返回后，delivery 才标记平台已提交，并异步调用
`CompleteAutofillSelection(interactionId, entryId)`。该用例在 `NonCancellable` 中调用
`finishInteraction.complete(id) { activityRecorder.recordUsage(entryId.value, ActivityType.AUTOFILL) }`，随后处理遗弃
interaction 并释放 lease。首次终态转换记录使用活动；重复的交付完成仍幂等。

Credential Manager Route 只在 Activity 处于 `STARTED` 时消费进程内缓冲的单次结果事件；结果 Intent 不保存在 UI
StateFlow 或 SavedState。Credential 与 Legacy Activity 均用 `AutofillActivityResultClaim` 约束实例内重复提交。平台 callback
失败会释放 claim：Credential Manager 取消 interaction 并返回 `RESULT_CANCELED`，Legacy Autofill 取消 ViewModel
工作流并返回取消结果，`onDestroy()` 不会因残留的防重标志跳过清理。callback 已成功后，日志/trace 的普通
`Exception` 不会触发第二次平台提交或把成功结果覆盖为取消；结果已交给 Android 后，interaction 完成与 lease
释放由 runtime scope 异步排队。fatal `Error` 不映射为普通失败：平台提交标记前会先清理可回滚状态再传播失败；标记后保留已提交结果，
触发异步收尾后继续传播原错误。Activity callback 部分副作用后失败、completion scope 取消和同步桥接等待合同仍待宿主验收，
见[Autofill Task 21](../../project/work/plans/2026-11-07-passly-autofill-followups.md#task-21)及[宿主验收记录](../../project/reviews/2026-11-08-autofill-host-validation.md)。

最后一个已提交结果完成后，仍未向 Android 提交结果的列表、认证和选择 interaction 全部过期，再立即回收临时会话。
关闭期间的新请求等待该会话完成锁定后再创建 interaction，不能落入“旧数据库仍开着但已禁止借用”的中间态。
多个已经提交的 Legacy/Credential 结果分别完成，不互相误标为过期。

最后一个 Autofill borrower 释放时，lease 保留临时会话所有权直到 `SessionLockController.lock()` 返回；锁请求在
不可取消收尾中执行。失败会立即反馈给当前调用，并将 pending obligation 交给 lease 的进程级 retry owner；重试
采用 1 秒起步、最多 60 秒的指数退避，等待期间不持有 lease mutex。新交互的 readiness gate 在 pending lock
未完成时拒绝继续，Autofill 不会借用尚未锁定的临时会话。因此取消发生在等待 session mutex 之前或期间都不能
静默丢失锁定责任。

无障碍填充页面由 `AutofillAuthActivity` 承载；`AccessibilityAutofillEntry` 观察宿主销毁，在 Activity 正在结束时调用
`AccessibilityFillViewModel.cancel()`。ViewModel 取消当前页面任务，并在 `NonCancellable + Dispatchers.IO` 中清理
interaction，因此该 Activity 不会在主线程同步等待 session lock。无障碍系统服务有独立生命周期：
`ManualAccessibilityFillService.onDestroy()` 会同步结束 Accessibility interactions，具体等待与失败收尾仍待验收，
见[并发计划的 Service teardown 审查](../../project/reviews/2026-11-12-accessibility-service-destroy.md)。

这保证 Android 收到结果以前不会关库，同时填充完成后再次点击输入框会从全新锁定状态开始。Android 没有确认字段
实际写入的回调时，日志不能伪造 `field_filled`。

## 临时会话

`AutofillAuthenticated` 只允许候选、单条秘密解析、使用记录和已确认凭据保存，不挂载普通 Vault。多个已授权
interaction 可以借用同一个临时数据库会话，但不共享候选或选择权限。任一凭据成功交付会结束这一轮临时授权；
遗留 borrower 不得把数据库继续保持为解锁状态。

每个 interaction 有 60 秒 process-local 超时。进程死亡后 extras 不能恢复授权；系统必须重新发起请求。主 App
完成普通解锁后会话提升为 `Authenticated`，迟到的 Autofill 释放不得锁掉它。

## 状态字段归属

- `AutofillInteractionSnapshot.state` 表达发现、授权、选择和终态；网页授权另由 `webConsent` 表达未确认、已确认接收方、已确认单条凭据。授权分支携带认证会话标识，单条确认分支才允许持有平台交付 claim。交付 claim 是该分支内的短期占用标记，提交失败时释放；普通来源不携带网页授权。
- 设置页的持久偏好与 Android 系统服务状态来自不同来源，分别投影为 `AutofillPreferencesState` 和 `AutofillSystemServiceStatus`。偏好读取失败不展示旧值作为可编辑设置；系统服务重查可展示上一次观察值，失败时提供重试。
- Legacy 选择页、Credential Manager 响应页和 Accessibility 选择页的加载、可操作、失败阶段分别由各自页面状态持有。宿主完成结果区分 Android Activity result 与 Accessibility 流程完成；解析字段、候选摘要和请求中的可选值仍按来源事实保留 nullable，不把缺少的平台字段当成页面加载态。

平台依据：[Autofill 与 IME Inline 集成](https://developer.android.com/identity/autofill/ime-autofill)、
[AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)、
[AccessibilityNodeInfo](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 发现可填充凭据候选 | `DiscoverAutofillCandidates` | [`DiscoverAutofillCandidates.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt) |
| 解析用户选择并准备填充结果 | `ResolveAutofillSelection` | [`ResolveAutofillSelection.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt) |
| 按需读取关联 OTP secret、生成并清理 OTP 材料 | `RoomAutofillCredentialResolver.resolve()`、`generateAutofillOtpCode()` -> `OtpGenerator.generate()` | [`RoomAutofillCredentialResolver.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/autofill/RoomAutofillCredentialResolver.kt)、[`OtpGenerator.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpGenerator.kt)、[`OtpSecretCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpSecretCodec.kt) |
| 合并授权 attempt 并终结共享结果 | `AuthorizeAutofillInteraction.invoke()`、`finish()` | [`AuthorizeAutofillInteraction.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/AuthorizeAutofillInteraction.kt) |
| 向平台提交 Autofill 结果 | `AutofillResultDelivery.commitResult()` | [`AutofillResultDelivery.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
| 维护交互授权、候选与结束状态 | `AutofillInteractionStore.create()`、`beginAuthorization()`、`recordCandidates()`、`finish()` | [`AutofillInteractionStore.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt) |
| 保留、重试临时 Autofill session 锁责任 | `AutofillUnlockLease.release()`、`awaitReadyForInteraction()` | [`AutofillUnlockLease.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillUnlockLease.kt) |
| Accessibility 模式宿主观察 Activity 销毁并清理选择流程 | `AccessibilityAutofillEntry` 的 `ON_DESTROY` observer、`AccessibilityFillViewModel.cancel()` | [`AccessibilityAutofillEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityAutofillEntry.kt)、[`AccessibilityFillViewModel.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillViewModel.kt) |
| 无障碍填充事件与服务清理 | `ManualAccessibilityFillService.onAccessibilityEvent()`、`onDestroy()` | [`ManualAccessibilityFillService.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/accessibility/ManualAccessibilityFillService.kt) |
| 保存捕获请求、短期结果和一次性交付 claim | `AccessibilityFillSessionStore.bind()`、`capture()`、`pendingCapture()`、`prepareDelivery()`、`claimWhenTargetActive()`、`unbind()` | [`AccessibilityFillSessionStore.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/accessibility/AccessibilityFillSessionStore.kt) |
