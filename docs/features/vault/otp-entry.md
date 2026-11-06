# 动态口令创建

状态：当前实现。

手动输入经 Domain 的 `OtpConfigInput.parse` 返回 `Valid(config)` 或 `Invalid(fields)`；页面状态与草稿映射
使用同一规则。数字输入为空、损坏或溢出时禁止保存，不补成默认配置。初始表单的 6 位、30 秒和计数器 0
是可见的初始值，用户清空后必须重新填写。

- TOTP/HOTP：位数为 6–8，保留项目的 7 位支持。
- TOTP：周期为可表示为 Int 的正整数秒数；不读取隐藏的 HOTP counter。
- HOTP：计数器为非负 Long，保留完整精度；不读取隐藏的周期输入。
- Steam：固定 SHA1、5 位、30 秒，页面禁止修改固定项；切换回数字 OTP 时恢复为 6 位。

密钥按所选编码校验：Base32 输入删除空白并规范为无补位大写形式，Base64 输入去除首尾空白后校验并保留
Base64 编码。临时解码字节立即擦除。输入及成功解析结果的字符串表示隐藏密钥、账户和 issuer。
失败字段映射为中英文提示，不把字段值放进错误、日志或导航。

`OtpFormMapper` 只接受有效配置，并与扫码创建复用 Domain 的 `OtpConfig.toNewEntryDraft`；标题、标签和备注仍由表单提供。
配置到草稿的纯转换由 `OtpConfig.toNewEntryDraft()` 完成，不依赖页面或应用服务。直接导入保留非空白身份元数据，
不对 issuer 或 account 做表单裁剪；手动表单仍由 `OtpConfigInput` 规范化后传入。
首页导入与编辑器共用 `suggestedEntryTitle`，组合完整的 `issuer: account`，不按冒号截断身份。
显示标题只裁剪各部分首尾空白；没有身份资料时，直接导入按类型使用 `TOTP / HOTP / Steam`，
编辑器保留已有标题。用户保存表单时以表单标题为准，不先生成默认标题再覆盖。
首页 OTP 扫描在用户确认后由 Activity 作用域的 `ScannedOtpImportViewModel` 持有写入任务；退出 Scanner、Vault 图或切换到认证壳层只取消页面等待，不会取消已确认的保存。任务启动前会核对确认时的完整访问与认证状态；若会话变化则不写入。写入开始后由数据库 session lease 与锁定串行化，已提交事务不因离页或结果等待者取消而回滚。同一 Activity 生命周期内同一时间只接受一个扫描导入，避免 Scanner 重建造成并发重复创建。Scanner 仍在前台时按仓库结果关闭或显示可重试失败；离开后不再向旧页面发送迟到反馈。调用者和参数传递见下方代码位置表。
`AddOtpViewModel` 在无效输入下不调用写入用例，保留原始输入供修改；合法保存经 Domain `EntryCreation` 能力写入，
成功后清除表单敏感内容。URI 解析及第三方导入边界见[动态口令 URI 导入与导出](otp-uri.md)。

URI/扫码提示只保留当前输入请求的最新结果，在消费时核对请求身份和完整认证对象；手动编辑、新 URI、保存、清理及销毁会使旧反馈失效。
反馈只包含 `UriParsed / UriParseFailed`，不携带 URI 或密钥。`tryApplyScannedConfig` 明确返回配置是否已被当前编辑会话接受；
锁定、保存中或销毁后的拒绝不会填入表单或发送成功反馈，Scanner Route 据此返回 `Rejected`，不能把忽略的配置报告为 `Applied`。

创建和归档读取共用 Domain 的参数规则；原生恢复校验合法性后保留密钥表示和身份元数据，
不套用手动输入的规范化。没有批量改写既有条目。归档缺省参数与兼容隔离见[Passly Archive v1](../backup/passly-archive-v1.md)。

最终创建校验也由 `EntryValidation` 调用 `OtpConfigValidation`，而非仅检查密钥非空。即使草稿绕过页面表单，
非法 Base32/Base64、数字 OTP 的 5 位配置或不符合 Steam 固定项的配置也会在 Repository 写入前失败。
缺失密钥返回 `REQUIRED`，非法密钥及参数返回对应字段的 `INVALID_FORMAT`；校验不重写密钥编码和身份资料，
临时解码字节立即擦除。该检查面向携带完整凭据的创建条目，不能把未揭示的详情 bundle 当作创建输入。

草稿物化前另检查字段契约：`EntryDraft.invalidProvidedFields` 拒绝不属于条目类型的字段、值类型不匹配，
以及显式提供但无数值的 `Number(null)` / `LongNumber(null)`。创建用例在生成 ID 和时间戳前返回校验失败，
直接调用物化器也受同一检查约束。未提供的可选 OTP 参数仍可使用物化器默认值，显式空数字不能获得默认值。

物化器将预期的字段错误表达为 `InvalidEntryDraft(fields)`，只携带字段标识与固定错误说明；未知 OTP
类型、算法或编码不回显原始文本，OTP 参数在构造配置模型前复用 `OtpConfigValidation` 检查。创建用例只将
这一明确的输入失败转换为 `ValidationError`，不会吞掉仓库异常或其他程序错误。Wi-Fi 安全类型缺失或未知也
走同一失败通路；有效的稳定键和既有别名继续由 `WifiSecurityType.parseStoredValue` 解析。

验证码刷新、HOTP 计数器提交、详情复制和列表复制路径见[OTP 运行时与复制](otp-runtime.md)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 将编辑表单状态映射为新建草稿，解析配置、标题、备注与标签 | `OtpFormState.toEntryDraft()` | [`OtpFormMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/OtpFormMapper.kt) |
| 处理 OTP 编辑页 Action，快照当前状态并在 Save 时提交草稿 | `AddOtpViewModel.onAction(AddOtpAction.Save)`、`save()`、`EditorWriteSession.save { current.form.toEntryDraft() }` | [`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt)、[`EditorWriteSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt) |
| 将新 OTP 配置转换为带默认标题的草稿 | `OtpConfig.toNewEntryDraft(title = suggestedEntryTitle())`、`OtpConfig.suggestedEntryTitle()` | [`OtpEntryDraft.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpEntryDraft.kt) |
| 确认首页扫码导入的 owner、认证与访问状态并串行接收任务 | `ScannedOtpImportViewModel.importScannedOtp(config)` | [`ScannedOtpImportViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModel.kt) |
| 将扫码配置转成草稿并调用创建能力 | `ImportScannedOtpUseCase.invoke(config)`、`createEntry(draft)` | [`ImportScannedOtpUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/ImportScannedOtpUseCase.kt) |
| 从 Vault 扫码结果派发导入并将布尔结果映射为 Applied / Rejected | `VaultRoute` 的 scanner import callback、`ScannedOtpImportViewModel.importScannedOtp(config)` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt) |
| 在 OTP 编辑器中接收扫码配置，只填充当前编辑会话而不直接创建 | `AddOtpEditorRoute` 的 ApplyToOtpEditor callback、`AddOtpViewModel.tryApplyScannedConfig(config)` | [`AddOtpEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt)、[`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt) |
| 从新建或编辑 ViewModel 的写入闭包进入创建/完整表单替换 | `createEntryUseCase(draft)`、`editDetailEntry.edit(entryId, EntryEdit.ReplaceForm(draft))` | [`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt) |
| 最终验证 OTP 配置并在创建前拒绝不合法值 | `OtpConfigValidation.validate()`、`EntryValidation.validate()` | [`OtpConfigValidation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpConfigValidation.kt)、[`EntryValidation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryValidation.kt) |
