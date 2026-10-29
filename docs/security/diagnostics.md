# 诊断安全与隐私

状态：当前实现（代码核对：2026-10-18）。

本页只定义安全策略。Logger 结构、存储和格式分别见[日志记录与 Trace](../architecture/observability/record-and-trace.md)、
[运行时与加密存储](../architecture/observability/runtime-and-storage.md)和
[导出格式](../architecture/observability/export-formats.md)。

## 允许进入日志的数据

业务字段必须来自 `LogFieldKey -> FieldKind` 白名单。允许计数、时长、有限比率、布尔、受控枚举、错误码、
原因码、操作码和待 HMAC 标识符。自由字符串不是业务日志字段类型。

以下内容禁止进入任何 Sink：

- 密码、密钥、OTP secret、恢复码和 token；
- 用户名、用户输入、URL、域名和文件路径；
- 明文目标包名、Activity 全限定名、Android ID、serial 和 Build fingerprint；
- `Throwable.message`、`localizedMessage`、suppressed exception 和第三方栈帧名。

目标包名只能以 `IdentifierNamespace.TARGET_PACKAGE` 进入 Keystore-backed HMAC 端口。校验或 HMAC 失败时
拒绝字段，不能回退为明文、普通 SHA 或截断值。Debug 对合同违规抛出不含原值的 `LogContractViolation`；
Release 只返回匿名拒绝结果。

## 异常与错误

异常只在 core 隐私边界转换成受限快照。WARN/ERROR/FATAL 业务语句必须携带受控错误码、原因码和异常对象；
没有异常对象的已知降级使用 `degraded`。快照可包含 Passly 相对类名、方法、受限源码文件名和行号，但不包含
绝对路径、第三方帧名或异常 message。原始异常文本不得用于用户提示或日志解释。

用户提示只来自错误目录中标记为 `USER_ACTIONABLE` 的资源映射。`INTERNAL` 错误没有用户文案，UI 不得显示
原始错误码作为兜底。

## 本地和导出边界

- 内部日志只存放在 `noBackupFilesDir/logs`，不进入 Android Auto Backup 或 Passly Vault 备份。
- Logcat 与加密文件经过同一隐私管线；Release 关闭 Logcat。
- App 内查看器只接收结构化索引和已脱敏 TSV 行，不接触原始 record 或 Throwable。
- 明文只允许存在于用户选择的 SAF 文档或分享临时缓存；分享缓存有界清理。
- 导出必须持有新鲜 `EXPORT_DIAGNOSTICS` 授权，不得因为 App 已解锁而跳过复验。
- Autofill 配置、Manifest 和其他模块快照不写入通用 manifest；模块状态只能作为允许的事件字段出现。

隐私回归测试必须向字段、异常和 codec 注入可识别秘密，并断言 JSONL、TSV、ZIP、Logcat 与加密 round trip
均不包含原值。测试必须覆盖包名与路径，不只覆盖示例密码。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 按白名单脱敏结构化日志字段 | `PrivacyPolicy.sanitize()` | [`PrivacyPolicy.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/PrivacyPolicy.kt) |
| 将安全日志记录写入加密存储 | `EncryptedLogRecordStore.write()`、`readPage()` | [`EncryptedLogRecordStore.kt`](../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt) |
| 编码诊断导出并管理分享文件 | `DiagnosticsExportEncoder.write()`、`ObservabilityExportService.export()` | [`DiagnosticsExportEncoders.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt)、[`ObservabilityExportService.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
