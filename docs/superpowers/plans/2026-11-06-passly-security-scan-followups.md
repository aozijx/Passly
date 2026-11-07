# Passly 安全扫描遗留问题

**状态：** 待决策与验证。本文区分已确认安全问题、已确认清理缺陷和待裁定策略/资源事项；每项以证据、攻击前提和验收为准，不代表已经修复或当前实现能力。

**扫描基线：** 历史报告 `31407fa0-a844-4159-a842-24d931430f4b` 针对 `438083bdbf7e3c7cf42813d3637b22ce77fb7418`，虽生成全仓清单（1,536 个文件），人工审查仅集中于安全敏感路径。后续扫描 `8478ba3f-584f-4f5a-ae17-c91c05981435` 针对 `c41984deb4da90db11d8dfb10926dda391042a40` 完成时 HEAD 已前进，所以又对最新快照 `651ae33f552a02601ed5745e7d79aaba2a994586` 运行扫描 `5a9b077c-d16d-4ead-b5c5-25b0bc1dd29a`。最新报告重新确认三条低严重度问题；Wi-Fi 候选按当前 SECRET 策略驳回，产品策略一致性仍待明确。报告覆盖清单 1,632 个文件，但人工审查覆盖仍为 partial；详见扫描报告。

<a id="task-1"></a>

## Task 1：裁定 Wi-Fi 密码的内联显示授权策略

- [ ] 由产品/安全策略明确 Wi-Fi 密码“显示”与“复制/二维码导出”是否有意采用不同的新鲜认证要求。当前 Domain 将 `SECRET` 定义为解锁后披露类别，与 `HIGH_SENSITIVITY` 区分；现有测试明确验证 Wi-Fi 密码可在无字段级授权时内联 reveal。仅凭复制和 QR 有额外授权，不能推断内联显示一定违反策略。
- [ ] 若确认内联显示也必须新鲜认证，再将 Wi-Fi `PASSWORD` 纳入 `HIGH_SENSITIVITY` 并关联稳定键 `SensitiveFieldKey.PASSWORD`。同步打通 Data 映射 `toSensitiveFieldValues()`、`toSensitiveFieldUpdates()`、`toBundleSecret()`、`mergeSensitiveFields()`，以及 create/update、清除、未加载字段保留、Revision 和备份；不建立旧 bundle 兼容分支，变更前明确需要重置的旧开发数据范围。
- [ ] 若确认现策略有意，则在 `docs/security/sensitive-data-access.md` 和详情/二维码相关文档中说明 Wi-Fi 内联显示限于完整解锁会话、而复制与 QR 另需授权的理由和边界，避免后续审查把既定策略误判为缺失授权。
- [ ] 按决策更新锁定当前行为的测试或新增测试：策略选择前后分别验证 inline reveal、复制、QR、编辑加载，以及锁定后清理。若改变存储合同，再覆盖新格式 mapper round-trip、Revision/备份往返；旧开发格式明确拒绝，不静默读成空值。

**当前证据（2026-11-06）：** `WifiEntryDefinition` 使用 `secret(FieldKey.PASSWORD)`；Domain 的 `SECRET` 与 `HIGH_SENSITIVITY` 是不同访问级别。`RevealEntryFieldsUseCaseTest` 明确名为 `wifi password is read as low sensitivity without field authorization`，验证该读取不触发字段授权；mapper 测试也锁定 Wi-Fi 密码留在加密 aggregate。Detail 路径要求完整安全会话并在锁定时清理状态，copy 与 QR 则各有额外授权。独立复核确认上述行为，但未发现能证明内联 reveal 违反既定安全策略的合同。因此这是待裁定的策略一致性候选，而非已验证漏洞。

**验收：** 写明策略决定及依据；相关 Domain/Data/App 单测和文档一致。若改存储合同，检查调用者和跨层数据流、事务失败及新格式数据保留；旧开发数据如不兼容，给出明确重置/重新安装说明，不在运行时静默删库。持久化 key、Room schema 版本及备份格式版本保持 1。

<a id="task-2"></a>

## Task 2：避免活动会话 lease 未退出时关闭数据库

- [ ] 重新设计 `SessionLeaseGate.closeAfterDrainTimeout()` 的超时合同。`Job.cancel()` 只是取消请求；100 ms grace 到期后不得在 `activeLeases > 0` 时调用通用 `SessionResource.close()`。
- [ ] 优先保持资源打开并让 vault 留在不可用/封存状态，直到所有 lease 退出；若必须强制释放，则在 `SessionResource` 增加资源端可验证的 abort 合同，并明确其终止正在执行事务/读写的语义，再执行 close。
- [ ] 增加不响应协程取消的活动 lease 测试：断言普通 close 不会与操作重叠；若走 abort 路径，证明 abort 完成后 lease 不再使用 handle 才 close。保留现有合作式取消测试，并覆盖超时、清理失败和重试/恢复行为。
- [ ] 同步更新锁定/数据库会话文档与事件语义，区分 lease drain 超时、取消请求、资源 abort 和资源 close；记录失败后用户可见的封存状态及恢复方式。

**当前证据（2026-11-06）：** `SessionLeaseGate.seal()` 先将会话置为 `SEALED` 并等待 lease drain；超时后取消 lease owner，再最多等待 `POST_CANCEL_GRACE_MS = 100`，之后无条件调用 `close()`。`AppDatabaseSessionResource.close()` 直接关闭 Room database，`SessionResource` 没有 abort 合同。现有 `seal timeout cancels active write before closing resource` 测试的操作通过 `awaitCancellation()` 合作退出，没有覆盖忽略取消且 active lease 仍存活时的关闭竞态。源代码路径成立；运行时是否存在具体非合作 caller 尚未确认。

**验收：** 非合作 lease 测试证明 close 不早于 lease 退出/资源 abort；既有锁定、取消和 close 失败测试保持通过；用户提供的 Runtime lease 规则与实现和文档一致。

<a id="task-3"></a>

## Task 3：限制未认证备份头触发的 Argon2 资源消耗

- [ ] 根据最低支持 Android 设备可承受的资源预算，确定导入专用的 Argon2 memory、iteration、parallelism 上限。导出强度合同与导入防滥用合同分开裁定；不得因复用 validator 而意外削弱导出保护，也不得沿用当前 256 MiB 导入内存上限而不说明依据。
- [ ] 在昂贵派生开始前拒绝超限参数；保持格式 v1 的头部解析与认证语义，不假设可以在 AES-GCM 验证前信任 header。评估完整文件缓冲与 ciphertext 拷贝叠加后的峰值内存，必要时一并收紧导入文件上限或避免重复持有大数组。
- [ ] 用注入的 KDF 派生函数验证每个超限参数都在派生前失败；覆盖允许的边界值、畸形 header、错误口令/认证失败和导出路径，证明既不执行超额 KDF，也不改变有效 v1 备份互操作。
- [ ] 更新备份安全/格式文档，区分导出参数、导入上限、用户选择文件这一前置条件和设备资源假设。

**当前证据（2026-11-06）：** `EncryptedBackupContainerCodec` 仍允许导入 memory `256 * 1024 KiB`、iterations 10、parallelism 8；`decrypt()` 解析未认证 header 后先调用 `deriveKey(...)`，之后才执行 AES-GCM `doFinal`。`BackupFileStore.MAX_READ_BYTES` 仍为 256 MiB，Android 实现会将输入读入内存。

**验收：** 精确备份容器测试通过；KDF 超限用例证明派生函数未调用；新格式参数往返和畸形输入拒绝通过；若导入预算淘汰旧开发备份，明确报错，不提供隐式兼容分支；Room/备份格式版本不变；`git diff --check` 通过。

<a id="task-4"></a>

## Task 4：限制各类备份导入的内存放大

- [ ] 在输入变成完整 String 和对象图之前定义并执行 JSON 专属原始字节上限；为记录数、aggregate 资源字节数、Base64 扩张及解码对象分配建立一致预算。若选择流式解析，确保所有输入读取都受同一上限约束；单纯在反序列化后检查单资源长度不能作为前置保护。
- [ ] 将同一内存预算审查扩展到 Bitwarden JSON 与加密 archive：确认 JSON tree 重序列化/再次反序列化、archive ciphertext/decrypted payload/ZIP entry 解码的峰值副本；ZIP 路径约束和单条目限制不能替代容器读取及累计字节上限。明确每种格式在哪一步拒绝、所有副本是否能及时释放。
- [ ] 核对 `BackupFileStore.readBytesSafely()` 的全局默认上限和 `BackupArchiveServiceImpl` 格式分派方式；避免为了某个格式收紧后破坏其他格式，或在不同调用路径绕过限制。
- [ ] 为超限原始文件、单资源及 aggregate 资源、畸形 UTF-8/Base64、恰好达到上限的有效备份添加边界测试。验证拒绝发生在大型文本/资源集合完整物化之前，并确保失败清理资源字节且不把敏感内容写入错误消息。
- [ ] 更新备份导入格式文档，记录各格式输入预算、验证顺序和峰值内存假设；保留现有 JSON key 与格式互操作。

**当前证据（2026-11-06）：** `BackupOperationRoute` 仅在用户触发导入后启动 `ActivityResultContracts.OpenDocument()`，收到 URI 后以 `BackupUiAction.StartImport(uri.toString(), owner, requestId)` 交给 ViewModel；因此当前可见路径要求用户主动选择文件，尚未发现远程或后台输入源。`BackupArchiveServiceImpl.import(request)` 在 `BackupIoDispatcher` 上调用 `fileStore.readBytesSafely(request.sourceUri)`，接口默认 `maxBytes = BackupFileStore.MAX_READ_BYTES = 256 * 1024 * 1024L`；Android 实现以 `LimitedInputStream(input, maxBytes)` 包装 ContentResolver 流，再调用 `readBytes()` 物化完整 `payload`。之后 `BackupFormatRegistry.importer(request.format, payload)` 将完整数组传给指定 Adapter 的 `probe(payload)`，或传给每个候选 Adapter 的 `probe(payload)`；选中后 `validatePassword(adapter.requiresPassword, request.password)`，再原样调用 `adapter.decode(payload, request.password, request.strategy)`。

格式分支的物化次序不同。Passly JSON 的 Adapter 调 `JsonBackupImporter.import(payload, strategy)`，先 `decodeStrictUtf8()` 创建完整 String，再反序列化整个 `JsonBackupPackage`（包括 Base64 字符串），逐资源检查 `MAX_ENCODED_RESOURCE_CHARS` 后解码 Base64，组成 bundle 并调用 `BackupImportPlanner.plan(bundle, strategy)`；该单资源检查发生在 JSON 对象图已创建之后。Bitwarden Adapter 同样先创建完整 UTF-8 String，再 `parseToJsonElement(...).jsonObject`，通过 `rawRoot.toString()` 重序列化并 `decodeFromString<BitwardenExport>()`，再映射记录及调用 `BackupBundleValidator.validate(bundle, requireResourceData = false)`。加密 Adapter 调 `EncryptedBackupImporter.import(payload, password, strategy)`；容器解析复制 ciphertext，KDF 后 AES-GCM `doFinal()` 生成完整 ZIP 明文，`BackupArchiveCodec.readZip(zipContent)` 再逐条读取到独立数组并保存在资源 Map 中，之后严格 UTF-8 解码文档、构建 bundle、调用 planner。ZIP 读取限制解压总字节 128 MiB、文档 16 MiB、单资源 16 MiB、条目 100,001 个；这些是 ZIP 层限制，不计入原始 payload、密文/明文容器、文本/JSON 对象和 bundle 的同时存活副本。

当前安全边界结论：未发现无需用户选择文件即可到达此导入路径的调用者，所以扫描候选不能仅凭这些分配认定为外部可利用漏洞；但用户选择极大/特制文件可能让应用进程承受明显内存压力，仍应按资源预算和导入健壮性事项设计、验证。尚无最低设备内存预算、并发/峰值测量或“拒绝发生在文本/对象图物化前”的证据，故整体累计预算仍未闭合；不要把 `readBytesSafely` 的单文件 cap 或 ZIP 层 cap 描述为全链路内存预算。

**验收：** 精确 JSON 导入和格式互操作测试通过；超限输入在资源物化前被拒绝；格式版本不变；相关模块验证、`verifyArchitecture` 与 `git diff --check` 通过。

<a id="task-5"></a>

## Task 5：对齐诊断导出的堆栈帧隐私说明

- [ ] 决定第三方 package/class/method 是否继续通过 24-frame sanitizer，并进入用户主动授权的导出；若需 allowlist/去标识化，先确定诊断效用和最小暴露规则。
- [ ] 若调整输出范围，同步 encoder、隐私策略和测试，覆盖应用、平台、第三方 frame，且确认不重新引入 Throwable message、用户数据或绝对路径。

**当前证据（2026-11-06）：** `ExceptionSnapshotFactory.normalize(frame)` 对 package/class/method/file/line 做长度和字符约束，但不做 package allowlist；Passly 前缀被剥除，第三方包名原样保留。快照被加密日志、debug Logcat 和新鲜 `EXPORT_DIAGNOSTICS` 授权后的 JSONL/TSV/ZIP 使用。`ExceptionSnapshotFactoryTest` 验证第三方 frame 可保留且异常 message/路径 sentinel 被剔除；`LogcatSinkTest` 验证 debug/release 边界。安全文档已经与当前实现一致，后续只剩是否进一步限制第三方 frame 的策略决定；本轮未发现帧中含用户输入或绝对路径的证据。

## 完成顺序与验证记录

**2026-11-07 排序：** Task 8 已完成并移出执行批次。接下来先处理 Task 6 接收方身份，再与主待办 26/37 协同定义 Task 2 非合作 lease 关闭、生命周期和撤权；详细清单只留本页。Task 7 为待裁定策略；Task 9 已有首次 KDF 参数实现，转持久化集成/设备资源验收，不重新描述为尚未接入。Task 3/4 按最低设备与前置拒绝预算治理，Task 1/5/10 保持策略/提示事项。主待办 Task 86 可先恢复验证基线，但不代替安全合同。

本轮为源码维护复核，不是新的完整 Codex Security 扫描；原报告只绑定其快照，不沿用为当前 HEAD 全面通过。实现前核对工作区、caller 和 owner，按正式测试文档验证。

## 2026-11-06 官方规范复核补充

本轮 Standard scan：`71405f9a-908a-4297-8045-13b80bcabaeb`，启动 HEAD `0e5041ecaccc27e22b36b0d15cd38bcceb4ac203`，快照 `codex-security-snapshot/v1:sha256:7ed71cecc4e82a4f76ad43f1d33a5feadfbe4976ed81cb6cae7cb12a90bd841d`。生成报告确认两项：Task 6 中严重度、Task 7 低严重度。独立基线、独立架构映射、Autofill 专项和父审查完成风险优先批次；人工语义覆盖仍为 **partial**，未执行攻击样例、设备仪器测试、KDF 基准或完整依赖漏洞审计。运行期间其他工作流推进 HEAD，生成报告保留原始修订；不以重复扫描掩盖这一限制。

此前 Task 2/3/4 继续 **retained**，属于尚未闭合的资源/完整性边界；本轮没有新增远程无交互攻击前提，也未测得最低设备上的耗尽阈值。Task 1/5 保持策略事项。下面新增条目统一在本文跟踪，不复制另一份 TODO。

### 官方依据与适用范围

- [Android AutofillService：Web security](https://developer.android.com/reference/android/service/autofill/AutofillService#web-security)：自报网页域名需要验证与应用的关联及签名；签名可信浏览器可单独信任。无法验证时，应先明确提示凭据泄露风险并取得确认。
- [NIST SP 800-63B-4 §3.1.1](https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver)：远程单因素密码以至少 15 字符、支持长口令、全密码常见值阻止列表为参考，不依赖字符种类拼凑规则。该规范主要针对远程数字身份，不能据此宣称本地 Vault 已合规或直接违规；本项目还要评估离线破解和恢复路径。
- [RFC 9106 §3.1、§4、§7.4](https://www.rfc-editor.org/rfc/rfc9106.html)：m/t/p 是公开算法输入，当前 64 MiB、3 次、p=4 对应其内存受限推荐配置；可按设备校准，但更大 p 不是安全性单调提高的保证。
- [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)：使用 Argon2id 并在目标设备测量可承受成本。本项目主密码用于解包可恢复的 DEK，而库内托管密码必须可解密，不可机械改为不可逆密码哈希。
- [Android 敏感剪贴板](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling)：敏感标记用于抑制预览。现实现已有敏感标记、所有权令牌和定时清理，未据此报告缺失控制。
- [Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html#property-names)：常量采用大写下划线；“文件顶部”是本项目更具体的本地约定，并非所有 Kotlin 项目必须遵守的官方位置规则。

<a id="task-6"></a>

## Task 6：验证 Legacy Autofill 的网页接收方身份

**状态：in progress；已确认安全问题，中严重度，CWE-346。**

**当前调用链证据：** `AutofillStructureParser` 汇总整棵 `AssistStructure` 中全部非空 `node.webDomain`，按规范 host 比较；多 host、有效与无效 claim 混合，或含可见网页 claim 的有界遍历被截断都会标记为歧义并在候选查询前失败。当前实现不验证 DAL、应用签名或可信浏览器签名，所以网页 claim 仍属于未验证输入。`DiscoverAutofillCandidates` 在公开任何候选 metadata 前先返回通用确认操作；用户查看应用标签/包名和域名并确认后，才按域名查询候选。选中后再展示条目和账号并要求二次确认；interaction 中的确认绑定规范 scope、条目 ID 和当前认证 session generation。`ResolveAutofillSelection` 在 resolver 读取 secret 前再次验证确认及 session generation；Fresh Authentication 失效会取消该 Legacy web interaction。平台交付前再次校验 session、scope、entry 并原子 claim，回调失败时释放 claim。`LegacyAutofillEntry.deliverLegacyResult()` 构造平台返回值，App `AutofillAuthActivity` 执行最终 `setResult`/`finish`。

**攻击前提：** 普通恶意应用已安装、用户启用 Passly Autofill、库中有其冒充站点的条目，并主动选择/完成所需认证。高影响但有这些前提，故定为中严重度；不是无需交互的静默提取。系统 BIND 权限、严格域名匹配和普通解锁认证均不能证明目标应用拥有自报域名。Credential Manager 目前拒绝 populated origin，本问题限 Legacy 网页路径。

- [x] 对当前不支持 DAL/签名证明的 Legacy 网页 claim 明确按未验证接收方处理，不按包名或普通解锁推断域名所有权；未验证路径只能进入显式双重确认。
- [x] 在候选 metadata 与 secret 读取前，分别展示应用标签/包名+请求域名，以及选中的条目+账号；普通解锁不能替代两次确认。确认绑定 process-local interaction、规范 scope、条目 ID 和 session generation；锁定/重新认证后失效。
- [ ] 使用无关联测试 App 的自定义 ViewStructure 冒充已有站点，证明正常选择/认证不能越过 origin 控制。覆盖合法签名浏览器、关联 App、确认/取消、scope 替换和 API 31/当前 API。
- [x] 同步 Autofill 安全文档和中英文风险文案；未声称 DAL 或签名验证已经实现。

**2026-11-07 实现进度：** `AutofillWebOriginResolverTest` 覆盖同 host 规范化、不同 host 歧义、无效 claim，以及截断结构中有/无可见网页 claim 的处理；独立候选复核确认该规则不会拒绝无 claim 的原生请求，并保持可见网页 claim 的截断结构 fail-closed。`DiscoverAutofillCandidatesTest.websiteCandidateSearchUsesDomainWithoutBrowserPackageAssociation` 使用无关联攻击包名和可命中网站候选，验证确认前零查询、确认后只按域名查询并返回候选；`ResolveAutofillSelectionTest` 覆盖未确认条目和跨 session generation 的 secret 读取拒绝；`AutofillWebConfirmationTest` 覆盖 entry membership、scope、单次交付 claim 与终态；`LegacyResponseFactoryInstrumentedTest` 覆盖首屏 response 只含通用确认操作，不含候选标题。当前 main-source 快照 `:feature:autofill:testDebugUnitTest` 152 项通过；`:feature:autofill:compileDebugAndroidTestKotlin` 与 `verifyArchitecture` 也在该快照通过。真实攻击方 ViewStructure 验收见下方 API 36 记录；Passly provider 认证后的确认/取消交互和 API 31 设备验证仍未完成，故 Task 6 保持打开。

**2026-11-07 API 36 结构注入验收：** 新增 AndroidTest APK 内的独立攻击方包 `com.aozijx.passly.feature.autofill.test`，其 `AutofillOriginAttackActivity` 通过自定义 `EditText.onProvideAutofillStructure()` 将 `login.example.com` 写入系统 `ViewStructure`；测试 APK 没有 `asset_statements` manifest 声明。临时启用的 `CaptureAutofillService` 收到系统 `FillRequest` 后由生产 `AutofillStructureParser` 解析，验证 Activity package 仍是攻击方包、webDomain 保留为伪报 host、结构包含至少两个可编辑字段且没有多 origin 歧义。`AutofillOriginAttackInstrumentedTest` 在 Pixel_8_Pro API 36 通过 1/1；随后完整 `:feature:autofill:connectedDebugAndroidTest` 在同一设备通过 13/13（PendingIntent 2 项、响应工厂 10 项、攻击结构 1 项）。`secure/autofill_service` 在 `finally` 后恢复到 Passly Debug Legacy 服务。该证据证明未关联测试包可以向系统结构声明别站 host，不证明 Passly provider 已在真实 App/认证交互中阻止绕过；还需用同一攻击方直接验收 Passly provider 的未确认与确认流程，并在 API 31 与当前 API 覆盖关联/可信调用方和 scope/取消情形。此前 capture Service 返回空 `FillResponse` 曾触发平台参数异常，现改为合法无数据响应；首次失败报告保留在 Gradle 报告目录。

<a id="task-7"></a>

## Task 7：统一主密码创建策略

**状态：deferred；已确认安全问题，低严重度，CWE-521。**

**证据：** `AppPasswordPolicy` 默认长度 6，`DefaultAuthenticationMethodProvisioner.setAppPassword()` 只调用 `acceptsLength(password.size)`；声明的 `validate()`/字符种类未参与真实创建校验，没有常见值阻止列表。Argon2id 包装 DEK 后将自足 envelope 存入 `bootstrap.pb`；取得私有文件且用户口令弱时可离线猜测，5 次/30 秒的进程内限制不约束离线副本。Android 沙盒、禁 OS 备份和现有 KDF 提高攻击门槛，因此不是直接认证绕过。

- [ ] 裁定面向离线密码库的最小长度与长口令规则，以 NIST 的长口令/全值 blocklist 原则为参考；不要简单接通现有“至少两类字符”作为整改完成。
- [ ] 设置、改密、恢复重设和安全 provisioner 使用同一创建规则；UI 提示与最终校验一致。常见值校验离线完成，不上传用户密码。
- [ ] 创建与解锁入口一次性采用新策略；不实现旧六位口令兼容或旧信封升级路径。切换前说明已有开发数据可能无法解锁及所需的显式重置/重新安装步骤，绝不在解锁失败时静默删库。
- [ ] 明确 Unicode code point 长度、是否规范化及字符/UTF-8 字节预算；同一新合同贯穿创建和解锁，不允许意外 trim 或截断。
- [ ] 覆盖所有新创建和解锁入口、长 passphrase、常见值、Unicode、取消、durable commit 前后失败及资源清理；旧开发信封按新格式明确拒绝。

<a id="task-8"></a>

## Task 8：在状态替换前覆盖原缓存 DEK

**状态：已完成（2026-11-07）；已确认的清理缺陷已修复，未独立认定为可利用漏洞。**

**修复：** 锁定时先保留原 `Unlocked` 状态引用，再撤权并发布 `Locking`，随后覆盖原 DEK、清字段 key，最后发布 `Locked`。清理段在 `NonCancellable` 中完成；日志或清理失败仍完成状态收尾并传播失败。DEK 派生字段 key 成功后才发布 `Unlocked`；失败会擦除候选缓存、清除字段 key 并回到 `Locked`。认证解封和初始化产生的 DEK 在其 owner 的 `finally` 中覆盖，传入 `setDek` 的数组由安装流程覆盖。

- [x] 在替换持有敏感数组的状态前保留并覆盖该数组；锁定遇到取消、日志失败或 fatal Error 时仍完成字段 key 清理和 `Locked` 发布。
- [x] 回归测试保留实际缓存数组引用，锁定后断言其内容归零，并验证重复锁定及之后重新解锁。
- [x] 注入锁定日志异常、取消、fatal Error 和字段密钥派生异常，验证清理、状态发布与重新认证的一致性；派生成功后才发布 `Unlocked`。
- [x] 复核解封 DEK 由调用 owner 覆盖；JCA `SecretKeySpec`/provider 内部副本不纳入可验证擦除保证。
- [x] 首次随机 key 的 nonce 生成、wrap、envelope 保存失败（包括取消和 fatal Error）都会覆盖尚未交接的 key；成功交接后由接收方 owner 负责，保留真实数组引用断言。

**验证：** `:core:testDebugUnitTest` 94 项、`:security:testDebugUnitTest` 149 项通过；`verifyArchitecture`、`:app:compileDebugKotlin`、`:core:ktlintCheck` 及 security 主源码/测试源码定向 ktlint 通过。覆盖锁定时实际缓存数组清零、logger 抛普通异常/取消/fatal Error、字段 key 派生失败与取消，以及两类首次随机 key 的生成/保存失败和成功交接清理。未执行设备仪器测试；不声称 JVM/JCA 全部临时副本可擦除。

<a id="task-9"></a>

## Task 9：首次创建并固定设备 Argon2id 参数

**状态：代码已接入；全新安装的一次性校准、持久化、认证路径读取及首次设置提示已实现。App 主源码编译通过；设备校准仍待验证。**

**当前事实：** 全新安装进入应用时测量 64 MiB/3 次基线，选择参数并原子保存到 `bootstrap.pb`。主密码和恢复码共用该配置，解锁不校准。加密备份 v1 使用独立头部参数。没有参数回退或试参路径。

**参数公开：** memory、iterations、parallelism、算法版本、salt 不属于秘密。允许读取它们是正常设计；攻击者通常本就知道应用默认值。安全目标是强口令、足够派生成本、参数完整性和派生前资源约束，不能把隐藏参数当安全边界。

- [ ] 做真实低内存和热状态设备验证，确认 128 MiB 选择不会造成进程内存压力；测量启动等待时间和 KDF 串行并发预算。
- [ ] 新信封写入成功为 commit point；提交前失败不得覆盖当前有效信封，提交后取消/availability refresh 失败按持久事实恢复。资源不足时明确报告失败，不静默改参。
- [ ] 验证新参数往返、进程重启后读取、参数篡改/缺失/超限在派生前拒绝、新合同下不变的 Vault DEK、改密失败重试和敏感缓冲清理。
- [ ] 为 `ProtoVaultBootstrapStore` 增加真实 DataStore 持久化往返及并发首次写入测试；当前纯参数与信封 AAD 测试未覆盖 Proto 字段 10 的落盘、重启读取和 first-writer-wins 合同。

**本次验证：** `:domain:test` 187 项、`:core:testDebugUnitTest` 80 项、`:security:testDebugUnitTest` 149 项及 `:data:testDebugUnitTest` 通过；`:core:ktlintCheck`、`:security:ktlintCheck`、`:data:ktlintCheck`、`verifyArchitecture`、`:app:processDebugResources`、`:app:compileDebugKotlin` 通过。App 单测编译被并行 Autofill 迁移下 `RecoveryModeBoundaryTest` 访问 `internal StartAutofillInteraction` 的错误阻断。全仓 KtLint 被其他工作流的 Autofill、Scanner、诊断文件问题阻断；本次改动文件在报告中无违例。无连接设备，未做首次启动时长、低内存和实际解锁仪器验证。

<a id="task-10"></a>

## Task 10：避免把字符池估算当作可证明的密码强度

**状态：deferred；安全提示质量事项，未独立报告漏洞。**

`PasswordStrengthEvaluator` 使用 trim、字符池与长度近似打分，并对弱词做 substring 判断。该分数不能代表真实猜测成本；长自然语言口令中包含一个弱词也不等于整条密码常见。

- [ ] 分开“建议性强度提示”和最终创建策略；全值阻止列表不能用 substring 拒绝所有长口令。
- [ ] 选择可解释的本地 guessability 模型或明确标注启发式提示，覆盖重复/顺序、常见变体、长 passphrase 和 Unicode；不在日志或遥测保存输入。

## 审查记录与验证

- **removed：** `AndroidSecureClipboard` 与 `FieldKeyManager` 仅承载私有常量的尾部 companion，常量保持原值并移至各自文件顶部。
- **removed：** 认证 Argon2id 隐式默认值和信封未认证元数据路径；新建认证统一采用首次持久化参数与 AAD。备份容器继续显式使用独立参数合同。
- **removed：** `MemoryCleaner` 注释中对防优化/全局擦除的过强保证；改为可控缓冲尽力覆盖和可选写入排序的准确说明。`DatabaseProvider` 参数注释改为实际调用方传入 DEK 副本，不虚构额外派生。
- **deferred：** Task 9 的设备、故障注入与完整 App 构建验证；其他 Task 仍按各自状态跟进。
- 验证结果在实际命令结束后追加；不以历史测试或其他工作流结果代替本轮证据。
- 扫描工具返回累计计量 10,392,571 tokens，其中 cached input 9,910,016、非缓存 input 433,894、output 48,661；这是工具按 5 个关联 rollout 汇总的 token 计量，不是本轮独立计费金额。报告目录内的 `scan-manifest.json` 保留工具原始统计。
- 本轮规范性代码整理验证：`:core:testDebugUnitTest` 实际执行 79 个测试，0 failure/0 error/0 skipped；`:core:ktlintCheck`、`:data:ktlintCheck`、`verifyArchitecture`、`:core:lintDebug` 均通过。首轮 KtLint 曾发现两个文件尾空行，已修正并在第二轮通过。最后再运行 `git diff --check`；本轮未执行设备仪器测试。
