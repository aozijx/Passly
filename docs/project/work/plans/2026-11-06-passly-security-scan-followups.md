# Passly 安全扫描遗留问题

**状态：** 待决策与验证。本文区分已确认安全问题、已确认清理缺陷和待裁定策略/资源事项；每项以证据、攻击前提和验收为准，不代表已经修复或当前实现能力。

**扫描基线：** 历史报告 `31407fa0-a844-4159-a842-24d931430f4b` 针对 `438083bdbf7e3c7cf42813d3637b22ce77fb7418`，虽生成全仓清单（1,536 个文件），人工审查仅集中于安全敏感路径。后续扫描 `8478ba3f-584f-4f5a-ae17-c91c05981435` 针对 `c41984deb4da90db11d8dfb10926dda391042a40` 完成时 HEAD 已前进，所以又对最新快照 `651ae33f552a02601ed5745e7d79aaba2a994586` 运行扫描 `5a9b077c-d16d-4ead-b5c5-25b0bc1dd29a`。最新报告重新确认三条低严重度问题；Wi-Fi 候选按当前 SECRET 策略驳回，产品策略一致性仍待明确。报告覆盖清单 1,632 个文件，但人工审查覆盖仍为 partial；详见扫描报告。

<a id="task-1"></a>

## Task 1：裁定 Wi-Fi 密码的内联显示授权策略

- [ ] 由产品/安全策略明确 Wi-Fi 密码“显示”与“复制/二维码导出”是否有意采用不同的新鲜认证要求。当前 Domain 将 `SECRET` 定义为解锁后披露类别，与 `HIGH_SENSITIVITY` 区分；现有测试明确验证 Wi-Fi 密码可在无字段级授权时内联 reveal。仅凭复制和 QR 有额外授权，不能推断内联显示一定违反策略。
- [ ] 若确认内联显示也必须新鲜认证，再将 Wi-Fi `PASSWORD` 纳入 `HIGH_SENSITIVITY` 并关联稳定键 `SensitiveFieldKey.PASSWORD`。同步打通 Data 映射 `toSensitiveFieldValues()`、`toSensitiveFieldUpdates()`、`toBundleSecret()`、`mergeSensitiveFields()`，以及 create/update、清除、未加载字段保留、Revision 和备份；不建立旧 bundle 兼容分支，变更前明确需要重置的旧开发数据范围。
- [ ] 若确认现策略有意，则在 `docs/platform/security/sensitive-data-access.md` 和详情/二维码相关文档中说明 Wi-Fi 内联显示限于完整解锁会话、而复制与 QR 另需授权的理由和边界，避免后续审查把既定策略误判为缺失授权。
- [ ] 按决策更新锁定当前行为的测试或新增测试：策略选择前后分别验证 inline reveal、复制、QR、编辑加载，以及锁定后清理。若改变存储合同，再覆盖新格式 mapper round-trip、Revision/备份往返；旧开发格式明确拒绝，不静默读成空值。

**当前证据（2026-11-06）：** `WifiEntryDefinition` 使用 `secret(FieldKey.PASSWORD)`；Domain 的 `SECRET` 与 `HIGH_SENSITIVITY` 是不同访问级别。`RevealEntryFieldsUseCaseTest` 明确名为 `wifi password is read as low sensitivity without field authorization`，验证该读取不触发字段授权；mapper 测试也锁定 Wi-Fi 密码留在加密 aggregate。Detail 路径要求完整安全会话并在锁定时清理状态，copy 与 QR 则各有额外授权。独立复核确认上述行为，但未发现能证明内联 reveal 违反既定安全策略的合同。因此这是待裁定的策略一致性候选，而非已验证漏洞。

**验收：** 写明策略决定及依据；相关 Domain/Data/App 单测和文档一致。若改存储合同，检查调用者和跨层数据流、事务失败及新格式数据保留；旧开发数据如不兼容，给出明确重置/重新安装说明，不在运行时静默删库。持久化 key、Room schema 版本及备份格式版本保持 1。

<a id="task-3"></a>

## Task 3：限制未认证备份头触发的 Argon2 资源消耗

- [ ] 根据最低支持 Android 设备可承受的资源预算，确定导入专用的 Argon2 memory、iteration、parallelism 上限。导出强度合同与导入防滥用合同分开裁定；不要把用户已确认保留的 256 MiB 文件上限当作 KDF 资源依据。
- [ ] 补齐当前测试缺口：以注入派生器验证最高合法参数可被接受、错误口令/认证失败不进入恢复，以及导出参数和导入上限保持独立；保留超上限与畸形 Header 在派生前拒绝的现有断言。

**当前实现与测试缺口：** Header 范围、派生前拒绝和分块终止验证见[Archive v1](../../../product/backup/passly-archive-v1.md)；现有测试覆盖及未覆盖用例见[备份验证要求](../../../product/backup/verification.md)。本 Task 只保留最低设备资源预算，以及最高合法参数、错误口令与导出参数边界的专门验收。

**验收：** 分块备份容器测试通过；KDF 超限用例证明派生函数未调用；当前分块格式参数往返和畸形输入拒绝通过；明确旧单块布局停止导入；Room/备份格式版本不变；`git diff --check` 通过。

<a id="task-5"></a>

## Task 5：对齐诊断导出的堆栈帧隐私说明

- [ ] 决定第三方 package/class/method 是否继续通过 24-frame sanitizer，并进入用户主动授权的导出；若需 allowlist/去标识化，先确定诊断效用和最小暴露规则。
- [ ] 若调整输出范围，同步 encoder、隐私策略和测试，覆盖应用、平台、第三方 frame，且确认不重新引入 Throwable message、用户数据或绝对路径。

**当前实现与策略缺口：** 第三方 frame 的保留范围、sanitizer 和测试证据见[Diagnostics 安全合同](../../../platform/security/diagnostics.md)；原扫描将“是否进一步去标识第三方 frame”列为策略候选而非漏洞，见[扫描记录](../../../platform/security/2026-11-06-repository-scan.md)。本 Task 只跟踪尚未裁定的最小化策略。

## 完成顺序与验证记录

**2026-11-11 复核：** Runtime 自身 lease 排空、关闭与取消合同已在[审查记录](../../reviews/2026-11-07-session-close-and-dek-wipe.md)验收并从主维护待办移除。后续先处理 Task 6 接收方身份，再与主待办 Task 37 协同完成 Security/Data 撤权与 Runtime/reset 跨层终态。Task 7 为待裁定策略；Task 9 已有首次 KDF 参数实现，转持久化集成/设备资源验收，不重新描述为尚未接入。Task 3 按最低设备与前置拒绝预算治理，Task 1/5/10 保持策略/提示事项。主待办保留当前验证缺口，不代替安全合同。

本轮为源码维护复核，不是新的完整 Codex Security 扫描；原报告只绑定其快照，不沿用为当前 HEAD 全面通过。实现前核对工作区、caller 和 owner，按正式测试文档验证。

## 2026-11-06 官方规范复核补充

本轮 Standard scan：`71405f9a-908a-4297-8045-13b80bcabaeb`，启动 HEAD `0e5041ecaccc27e22b36b0d15cd38bcceb4ac203`，快照 `codex-security-snapshot/v1:sha256:7ed71cecc4e82a4f76ad43f1d33a5feadfbe4976ed81cb6cae7cb12a90bd841d`。生成报告确认两项：Task 6 中严重度、Task 7 低严重度。独立基线、独立架构映射、Autofill 专项和父审查完成风险优先批次；人工语义覆盖仍为 **partial**，未执行攻击样例、设备仪器测试、KDF 基准或完整依赖漏洞审计。运行期间其他工作流推进 HEAD，生成报告保留原始修订；不以重复扫描掩盖这一限制。

Task 3 继续 **retained**，属于尚未闭合的资源边界；本轮没有新增远程无交互攻击前提，也未测得最低设备上的耗尽阈值。Task 1/5 保持策略事项。下面新增条目统一在本文跟踪，不复制另一份 TODO。

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
- [ ] 使用无关联测试 App 的自定义 ViewStructure 冒充已有站点，证明正常选择/认证不能越过 origin 控制。覆盖合法签名浏览器、关联 App、确认/取消、scope 替换和 API 31/当前 API。

**2026-11-07 实现进度：** `AutofillWebOriginResolverTest` 覆盖同 host 规范化、不同 host 歧义、无效 claim，以及截断结构中有/无可见网页 claim 的处理；独立候选复核确认该规则不会拒绝无 claim 的原生请求，并保持可见网页 claim 的截断结构 fail-closed。`DiscoverAutofillCandidatesTest.websiteCandidateSearchUsesDomainWithoutBrowserPackageAssociation` 使用无关联攻击包名和可命中网站候选，验证确认前零查询、确认后只按域名查询并返回候选；`ResolveAutofillSelectionTest` 覆盖未确认条目和跨 session generation 的 secret 读取拒绝；`AutofillWebConfirmationTest` 覆盖 entry membership、scope、单次交付 claim 与终态；`LegacyResponseFactoryInstrumentedTest` 覆盖首屏 response 只含通用确认操作，不含候选标题。当前 main-source 快照 `:feature:autofill:testDebugUnitTest` 152 项通过；`:feature:autofill:compileDebugAndroidTestKotlin` 与 `verifyArchitecture` 也在该快照通过。真实攻击方 ViewStructure 验收见下方 API 36 记录；Passly provider 认证后的确认/取消交互和 API 31 设备验证仍未完成，故 Task 6 保持打开。

**2026-11-07 API 36 结构注入验收：** 新增 AndroidTest APK 内的独立攻击方包 `com.aozijx.passly.feature.autofill.test`，其 `AutofillOriginAttackActivity` 通过自定义 `EditText.onProvideAutofillStructure()` 将 `login.example.com` 写入系统 `ViewStructure`；测试 APK 没有 `asset_statements` manifest 声明。临时启用的 `CaptureAutofillService` 收到系统 `FillRequest` 后由生产 `AutofillStructureParser` 解析，验证 Activity package 仍是攻击方包、webDomain 保留为伪报 host、结构包含至少两个可编辑字段且没有多 origin 歧义。`AutofillOriginAttackInstrumentedTest` 在 Pixel_8_Pro API 36 通过 1/1；随后完整 `:feature:autofill:connectedDebugAndroidTest` 在同一设备通过 13/13（PendingIntent 2 项、响应工厂 10 项、攻击结构 1 项）。`secure/autofill_service` 在 `finally` 后恢复到 Passly Debug Legacy 服务。该证据证明未关联测试包可以向系统结构声明别站 host，不证明 Passly provider 已在真实 App/认证交互中阻止绕过；还需用同一攻击方直接验收 Passly provider 的未确认与确认流程，并在 API 31 与当前 API 覆盖关联/可信调用方和 scope/取消情形。此前 capture Service 返回空 `FillResponse` 曾触发平台参数异常，现改为合法无数据响应；首次失败报告保留在 Gradle 报告目录。

**2026-11-07 API 31 设备检查：** 当前 `D:\AndroidSDK\system-images\android-31` 目录为空，未找到 `avdmanager` 或 `sdkmanager`；因此本机尚无可启动的 API 31 system image，未运行该级别测试。API 31 验收仍是待办，不能从 API 36 结果外推。

<a id="task-7"></a>

## Task 7：统一主密码创建策略

**状态：deferred；已确认安全问题，低严重度，CWE-521。**

**证据：** `AppPasswordPolicy` 默认长度 6，`DefaultAuthenticationMethodProvisioner.setAppPassword()` 只调用 `acceptsLength(password.size)`；声明的 `validate()`/字符种类未参与真实创建校验，没有常见值阻止列表。Argon2id 包装 DEK 后将自足 envelope 存入 `bootstrap.pb`；取得私有文件且用户口令弱时可离线猜测，5 次/30 秒的进程内限制不约束离线副本。Android 沙盒、禁 OS 备份和现有 KDF 提高攻击门槛，因此不是直接认证绕过。

- [ ] 裁定面向离线密码库的最小长度与长口令规则，以 NIST 的长口令/全值 blocklist 原则为参考；不要简单接通现有“至少两类字符”作为整改完成。
- [ ] 设置、改密、恢复重设和安全 provisioner 使用同一创建规则；UI 提示与最终校验一致。常见值校验离线完成，不上传用户密码。
- [ ] 创建与解锁入口一次性采用新策略；不实现旧六位口令兼容或旧信封升级路径。切换前说明已有开发数据可能无法解锁及所需的显式重置/重新安装步骤，绝不在解锁失败时静默删库。
- [ ] 明确 Unicode code point 长度、是否规范化及字符/UTF-8 字节预算；同一新合同贯穿创建和解锁，不允许意外 trim 或截断。
- [ ] 覆盖所有新创建和解锁入口、长 passphrase、常见值、Unicode、取消、durable commit 前后失败及资源清理；旧开发信封按新格式明确拒绝。

<a id="task-9"></a>

## Task 9：首次创建并固定设备 Argon2id 参数

**状态：** 首次校准、参数持久化与认证路径读取已接入。当前参数合同见[密钥管理专题](../../../platform/security/key-management.md#参数与清理的已知边界)；此前编译、测试和 lint 结果见[参数接入审查记录](../../reviews/2026-11-12-authentication-kdf-parameter-rollout.md)。

- [ ] 做真实低内存和热状态设备验证，确认 128 MiB 选择不会造成进程内存压力；测量启动等待时间和 KDF 串行并发预算。
- [ ] 新信封写入成功为 commit point；提交前失败不得覆盖当前有效信封，提交后取消/availability refresh 失败按持久事实恢复。资源不足时明确报告失败，不静默改参。
- [ ] 验证新参数往返、进程重启后读取、参数篡改/缺失/超限在派生前拒绝、新合同下不变的 Vault DEK、改密失败重试和敏感缓冲清理。
- [ ] 为 `ProtoVaultBootstrapStore` 增加真实 DataStore 持久化往返及并发首次写入测试；当前纯参数与信封 AAD 测试未覆盖 Proto 字段 10 的落盘、重启读取和 first-writer-wins 合同。

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
