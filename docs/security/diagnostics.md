# 安全诊断

当前实现和加密文件 v1 规范统一维护在
[Telemetry 与诊断日志](../architecture/telemetry.md)。安全评审、格式变更和导出策略必须同步更新该文档与 codec 测试。

正式 Logger v1 的隐私模型、Keystore HMAC、异常安全快照、Trace、设备导出 manifest 和分阶段迁移见
[Passly Logger v1 与可观测性系统设计](../superpowers/specs/2026-10-14-observability-logger-v1-design.md)。

安全底线：任何 Sink 均不得接收 Throwable message、密码、密钥、OTP、Token、URL、文件路径或目标应用明文包名。
Android Logcat 与加密文件必须经过同一验证和脱敏管线，不得把 Logcat 作为绕过隐私策略的调试入口。

Logger v1 的隐私层采用“类型与字段目录白名单优先、检测规则兜底”：业务字段只能使用注册字段及其指定值类型；目标
应用包名只能通过 `IdentifierNamespace.TARGET_PACKAGE` 进入 HMAC 端口，合法性校验或 Keystore HMAC 失败时直接
拒绝/丢弃该字段，绝不回退为明文或普通摘要。Debug 构建对契约违规抛出不含原值的
`LogContractViolation`，Release 构建返回仅含匿名违规枚举的拒绝结果。

Throwable 只转换为安全快照：保留简单异常类型、最多 24 个 Passly 源码栈帧、最多 3 层 cause 类型以及外部帧
计数。转换过程不读取或保存异常 message、localizedMessage、suppressed、文件名/绝对路径以及第三方类名。所有
Sink 只接受经过该策略生成的不可变 `SanitizedLogRecord`。

遵循 Android 官方 Log 信息泄露指导，Release 默认关闭 Logcat Sink；用户开启诊断窗口只启用内部加密记录。
Logger 不把 Throwable 直接交给 Android Log API。Perfetto 只接收固定操作名与匿名 counter；`ApplicationExitInfo`
只读取稳定枚举/数值，不导出 description、processName 或原始 ANR trace。

## Android 原生诊断边界

- HMAC-SHA256 密钥由 Android Keystore 在本机生成，别名为
  `com.aozijx.passly.observability.hmac.v1`，仅授权 `PURPOSE_SIGN`，不要求用户认证且不可导出。输入按
  `passly-observability-v1\0<namespace>\0<value>` 域分离，输出只保留 128 bit 并编码为 `h1_` 前缀；异常时返回
  `null`，不得回退到明文或普通 SHA 摘要。
- Logcat 只在 Debug 构建启用，先调用 `Log.isLoggable` 再格式化；只使用固定 Tag 和单行有界摘要。Release 构建
  在所有等级（包括 FATAL）都不会调用 Logcat。`LogcatSink.kt` 是 observability 目录中唯一允许导入
  `android.util.Log` 的文件。
- 系统 Trace 只使用 `OperationCode` 到固定 section name 的映射，并以同一 name/cookie 成对调用异步 begin/end；
  重复完成和平台异常都被隔离，不能改变业务结果。
- `ApplicationExitInfo` 只读取 timestamp、受控 reason、status、importance、PSS/RSS。禁止读取或导出
  description、processName、UID/PID 和 ANR trace；timestamp 游标保证同一退出记录最多上报一次，游标持久化失败
  也不能阻断应用启动。

Android API 依据：[Log](https://developer.android.com/reference/android/util/Log)、
[Trace](https://developer.android.com/reference/android/os/Trace)、
[KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec) 与
[ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo)。
