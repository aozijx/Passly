# 动态口令 URI 导入与导出

状态：当前实现。

`domain/entry/otp/OtpAuthUriCodec` 是扫码、手动 URI 输入、Bitwarden 导入和二维码导出的同一协议实现，使用纯 JVM API。
协议依据 [Key URI Format](https://github.com/google/google-authenticator/wiki/Key-Uri-Format)，不依赖 Android Uri。

## 解析

结果为 `Parsed / NotOtp / Malformed / Unsupported`。标签和查询值只按 UTF-8 百分号编码解码一次，字面的
`+` 与 `%` 不按表单语义改写。标签可带 issuer 前缀；同时提供 issuer 参数时，两者必须一致。

只在参数缺失时使用 SHA1、6 位和 30 秒的默认值。未知算法、重复关键参数、损坏 Base32、无效数字与
损坏 URI 不生成配置。HOTP 必须显式携带非负且可表示为 Long 的 counter。数字 OTP 保留项目现有的
6–8 位支持；Steam 以 issuer 标记表达，固定 SHA1、5 位、30 秒，拒绝冲突参数。
非标准 `encoder/encoding` 参数也返回 `Unsupported`，避免猜测密钥编码或生成器类型；普通装饰参数不影响计算。

Scanner 将损坏或不支持的 OTP URI 保留为普通文本；编辑器报告解析失败并保留已经输入的凭据，不改用
另一种算法或猜测计数器。

Bitwarden 的 `login.totp` 若以 `otpauth:` 开头，也通过上述 Domain 解析器；损坏或不支持的 URI 在适配阶段
拒绝整个源文件，不因兼容导入策略改成默认配置。归档字段通过 `toOtpArchiveFields` 与原生导出共用稳定键映射。
裸密钥和 `steam://` 是 Bitwarden 适配器的独立源格式分支，不作为 `otpauth:` URI 解析；两者同样使用
`OtpSecretCodec.canonicalBase32` 校验并规范密钥，再经共享归档字段映射输出。裸密钥允许输入空白；
Steam 前缀按大小写无关方式识别和截取。无效密钥统一转为不含原始值的备份失败，不保留半成品条目。

## 导出与密钥

结果为 `Formatted / InvalidConfiguration`。标签优先使用 accountName 与 issuer 元数据，缺少账户名时才
使用显示标题；同名 issuer 前缀只处理一次。各组成部分只编码一次，算法使用显式稳定字符串。

URI 的 secret 始终为无补位 Base32。内部 Base64 密钥先解码，再转换成等价 Base32；缺失、损坏的密钥或
无法保持语义的配置不生成二维码，也不记录成功查看行为。授权与二维码生命周期见[条目二维码展示](qr-display.md)。

`OtpSecretCodec` 对 App 暴露密钥验证及 Base32 规范化，原始字节编解码仅在 Domain 内部使用，生成器与协议编解码共用。解码失败
保留 `OtpGenerationError.InvalidSecret`；临时解码数组、HMAC 输入与摘要在退出路径中擦除。URI、密钥、
账户和 issuer 不写入异常消息、日志、导航或 SavedState。JVM 测试覆盖 RFC 向量、导出后验证码不变、
特殊字符、重复参数和拒绝路径；这些文本协议测试不代替真实设备的二维码扫描测试。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 解析与格式化 otpauth URI | `OtpAuthUriCodec.parse()`、`format()` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt) |
| 校验和规范化 OTP Base32 密钥 | `OtpSecretCodec.canonicalBase32()` | [`OtpSecretCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpSecretCodec.kt) |
| 将 OTP 配置映射到备份稳定字段 | `toOtpArchiveFields()` | [`OtpArchiveFields.kt`](../../../app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/OtpArchiveFields.kt) |
