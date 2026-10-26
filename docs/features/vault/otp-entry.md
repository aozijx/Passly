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

`OtpFormMapper` 只接受有效配置，并与扫码创建复用 `OtpConfig.toNewEntryDraft`；标题、标签和备注仍由表单提供。
`AddOtpViewModel` 在无效输入下不调用写入用例，保留原始输入供修改；合法保存经 `CreateEntryUseCase` 写入，
成功后清除表单敏感内容。URI 解析及第三方导入边界见[动态口令 URI 导入与导出](otp-uri.md)。

创建和归档读取共用 Domain 的参数规则；原生恢复校验合法性后保留密钥表示和身份元数据，
不套用手动输入的规范化。没有批量改写既有条目。归档缺省参数与兼容隔离见[归档格式](../backup/archive-format.md)。

最终创建校验也由 `EntryValidation` 调用 `OtpConfigValidation`，而非仅检查密钥非空。即使草稿绕过页面表单，
非法 Base32/Base64、数字 OTP 的 5 位配置或不符合 Steam 固定项的配置也会在 Repository 写入前失败。
缺失密钥返回 `REQUIRED`，非法密钥及参数返回对应字段的 `INVALID_FORMAT`；校验不重写密钥编码和身份资料，
临时解码字节立即擦除。该检查面向携带完整凭据的创建条目，不能把未揭示的详情 bundle 当作创建输入。

## HOTP 生成边界

`OtpGenerator` 的 HOTP 成功结果必须包含非负、可表示为 Long 的下一计数器。当前计数器为
`Long.MAX_VALUE - 1` 时可以生成并返回 `Long.MAX_VALUE`；当前或覆盖计数器已为 `Long.MAX_VALUE` 时，
在解码密钥和计算验证码前返回 `OtpGenerationError.CounterExhausted`。负覆盖计数器仍为 `InvalidCounter`。
耗尽状态可存储和归档，但不能继续生成；不得通过溢出、回绕或重置为 0 继续使用。

该边界约束生成器及 `OtpCodeRuntime.generateHotpCode` 的结果，不代表计数器已推进到数据库。
当前列表与详情订阅 HOTP 时为 Pending，页面尚未连接手动生成动作，计数器消费与持久化流程仍需实现。
