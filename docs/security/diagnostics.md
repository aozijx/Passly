# 诊断安全与隐私

状态：当前实现（代码核对：2026-11-06）。

本页只定义安全策略。Logger 结构、存储和格式分别见[日志记录与 Trace](../architecture/observability/record-and-trace.md)、
[运行时与加密存储](../architecture/observability/runtime-and-storage.md)和
[导出格式](../architecture/observability/export-formats.md)。

## 允许进入日志的数据

业务字段必须使用 `LogFieldKeys` 中带类型的 `LogFieldKey`。字段名与允许的值类型在同一处定义；
`PrivacyPolicy` 按键携带的类型校验实际值，裸键或未知键会被拒绝。允许计数、时长、有限比率、布尔、受控枚举、
错误码、原因码、操作码和待 HMAC 标识符。目标包名字段固定绑定目标包命名空间，自由字符串不是业务日志字段类型。

以下内容禁止进入任何 Sink：

- 密码、密钥、OTP secret、恢复码和 token；
- 用户名、用户输入、URL、域名和文件路径；
- 明文目标包名、Activity 全限定名、Android ID、serial 和 Build fingerprint；
- `Throwable.message`、`localizedMessage`、suppressed exception、绝对源码路径和本地变量。

目标包名只能以 `IdentifierNamespace.TARGET_PACKAGE` 进入 Keystore-backed HMAC 端口。校验或 HMAC 失败时
拒绝字段，不能回退为明文、普通 SHA 或截断值。Debug 对合同违规抛出不含原值的 `LogContractViolation`；
Release 只返回匿名拒绝结果。

## 异常与错误

异常只在 core 隐私边界转换成受限快照。WARN/ERROR/FATAL 业务语句必须携带受控错误码、原因码和异常对象；
没有异常对象的已知降级使用 `degraded`。`PrivacyPolicy.sanitize()` 将错误中的 `Throwable` 与映射后的 `ExceptionKind`
传给 `ExceptionSnapshotFactory.create(throwable, kind)`。快照最多保留 24 条通过规范化的栈帧；保留范围包含 Passly、Android
framework 和第三方库，不采用 package allowlist。Passly 的 `com.aozijx.passly.` 前缀会被去掉，外部类名保留完整包名；类名、
方法名、源码文件名和行号经字符集与长度限制。文件名只接受受限 `.kt`/`.java` 名称及合法行号，不保存绝对路径。异常快照
另计 `externalFrameCount`：统计原始栈中所有非 Passly 前缀帧，不受最多 24 条可输出帧限制；cause 只保留最多 3 层异常类型，
不保留异常 message 或 suppressed 异常。

这些清洗后的帧进入加密本地日志记录；debug build 的 `LogcatSink.emit(record)` 将每帧另写一行，Release sink 不写 Logcat。
JSONL、TSV 和 ZIP 诊断导出也会携带 `stack_frames` 与 `external_frame_count`，但必须通过新鲜 `EXPORT_DIAGNOSTICS` 授权。
这让用户主动选择的诊断文件可包含依赖库包名/类名等实现信息；若要收窄第三方帧，需先明确诊断效用与最小暴露策略，再同步
sanitizer、编码器和测试。当前实现不包含绝对路径、变量值、用户输入或异常 message。原始异常文本不得用于用户提示或日志解释。

当前证据分别由 [`ExceptionSnapshotFactoryTest`](../../core/src/test/kotlin/com/aozijx/passly/core/observability/privacy/ExceptionSnapshotFactoryTest.kt)
（包含 `com.bank.secret.CustomerActivity` 第三方帧并验证消息/路径 sentinel 不进入快照）、[`LogcatSinkTest`](../../app/src/test/java/com/aozijx/passly/app/observability/LogcatSinkTest.kt)
（Release 不写 Logcat、debug 按独立行输出帧）和 [`DiagnosticsExportEncodersTest`](../../app/src/test/java/com/aozijx/passly/app/observability/DiagnosticsExportEncodersTest.kt)
（导出 `stack_frames` 且排除异常消息污染）覆盖。它们证明当前过滤/编码行为，不单独决定是否应收窄第三方包名披露。

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
| 将异常转换为清洗后的快照 | `ExceptionSnapshotFactory.create(throwable, kind)`、`normalize(frame)` | [`ExceptionSnapshotFactory.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/ExceptionSnapshotFactory.kt) |
| 将安全日志记录写入加密存储 | `EncryptedLogRecordStore.write()`、`readPage()` | [`EncryptedLogRecordStore.kt`](../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt) |
| Debug Logcat 输出清洗后的每帧 | `LogcatSink.emit(record)` | [`LogcatSink.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/LogcatSink.kt) |
| 编码诊断导出并管理分享文件 | `DiagnosticsExportEncoder.write()`、`ObservabilityExportService.export(permit, request)` | [`DiagnosticsExportEncoders.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt)、[`ObservabilityExportService.kt`](../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt) |
