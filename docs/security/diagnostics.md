# 安全诊断

当前实现和加密文件规范统一维护在
[Telemetry 与诊断日志](../architecture/telemetry.md)。安全评审、格式变更和导出策略必须同步更新该文档与 codec 测试。

安全底线：任何 Sink 均不得接收 Throwable message、密码、密钥、OTP、Token、URL、文件路径或目标应用明文包名。
Android Logcat 与加密文件必须经过同一验证和脱敏管线，不得把 Logcat 作为绕过隐私策略的调试入口。

Logger v1 的隐私层采用“类型与字段目录白名单优先、检测规则兜底”：业务字段只能使用注册字段及其指定值类型；目标
应用包名只能通过 `IdentifierNamespace.TARGET_PACKAGE` 进入 HMAC 端口，合法性校验或 Keystore HMAC 失败时直接
拒绝/丢弃该字段，绝不回退为明文或普通摘要。Debug 构建对契约违规抛出不含原值的
`LogContractViolation`，Release 构建返回仅含匿名违规枚举的拒绝结果。

Throwable 只转换为安全快照：保留简单异常类型、最多 24 个 Passly 源码栈帧、最多 3 层 cause 类型以及外部帧
计数。转换过程不读取或保存异常 message、localizedMessage、suppressed、文件名/绝对路径以及第三方类名。所有
Sink 只接受经过该策略生成的不可变 `SanitizedLogRecord`。

错误语义集中在 `config/observability/error-catalog.json`。目录中的 code、默认 reason、category、exposure 和说明由
生成任务统一校验；内部错误禁止声明用户提示。只有显式标记为 `USER_ACTIONABLE` 的错误可生成资源映射，并且默认与
英文 `strings.xml` 必须同时包含该 key。用户界面只能使用生成的资源 ID，不能把错误码、Throwable message 或诊断
细节作为回退文案。

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

## Logger 本地加密容器

- 新记录只进入应用内部 `noBackupFilesDir/logs/*.log`。`.log` 是 Passly 私有二进制加密容器，
  不是明文 Logcat 导出；格式协商由文件头 magic 和 schema version 完成，扩展名不带版本号。
- 每个文件使用随机 256-bit AES 数据密钥和 AES-GCM；数据密钥由独立 Android Keystore wrapping key
  `com.aozijx.passly.observability.wrap.v1` 加密。文件头和每条记录的 sequence、level、file id 均进入 AAD，
  篡改、乱序和跨文件复制会读取失败。
- 单文件上限 1 MiB，最多保留 3 个文件、总计 3 MiB、最长 3 天。轮换和删除只匹配新目录中的 `.log`，
  不触碰其他文件。
- 正常写入使用容量 256 的单线程队列；队列满时优先淘汰待写 DEBUG。ERROR/FATAL 只能进行一次预初始化
  emergency write。紧急路径不访问 Keystore、不获取正常 writer 锁；所有存储异常被 Sink 边界吞掉，不允许递归
  写日志或替换原始崩溃。
- codec 只接受 `SanitizedLogRecord`，并重新校验事件目录、category、level/error 契约、严格 UTF-8、长度、
  枚举、trace/error 结构和尾随字节。未 HMAC 的标识符无法构造为可持久化字段。
- 私有 record codec 当前 schema 为 2；它删除了旧失败诊断标识，旧 schema 记录会被明确拒绝，不提供兼容读取器。
  错误通过同一操作的 `trace_id` 关联，语义由 `error_code + reason + exception snapshot` 表达。

## 运行时与导出边界

- `PasslyApplication` 只启动 `ObservabilityRuntimeController`。Logger 与 store 完成装配后才注册
  uncaught-exception handler 并读取上次进程退出；重复启动、flush、shutdown 均不能重复安装或关闭资源。
- FATAL 事件与普通 Sink 共享同一份已脱敏 record。崩溃路径先等待正常队列 300 ms，失败时再用同一 record 尝试
  200 ms emergency write，最后始终委托原 handler；重入时不再写日志，避免递归崩溃。
- 后台与退出操作按回调顺序串行执行：后台生命周期先完成会话锁定再 flush；应用退出先等待前序操作、完成
  `APP_EXIT` 锁定再 shutdown，防止数据库/认证关闭事件被提前截断。无需后台锁定的重复生命周期事件降为 DEBUG。
- JSONL/TSV/ZIP 导出必须持有 `EXPORT_DIAGNOSTICS` 的 fresh `AuthorizationPermit`。调用方用单一请求同时声明
  `action + format`：分享不得携带目标 URI，本地导出必须携带 SAF 返回的 `content://` URI。六种组合共用同一个
  授权入口、当前记录快照、manifest 工厂和编码器，不允许在 UI 或存储层复制格式逻辑。JSONL 是唯一 canonical 数据模型；
  TSV 只从该 JSON 投影固定八列，未知字段进入 `details`。独立 JSONL/TSV 各自携带 manifest，ZIP 则只在
  `manifest.json` 携带一次，并包含 manifest-free 的 JSONL/TSV 与 README。编码器只接受 `SanitizedLogRecord`，
  特殊字符按目标格式转义，禁止 Android ID、fingerprint、serial、明文包名和任意 Throwable message。
- 用户选择“保存”时，系统用所选格式的 MIME 与扩展名启动 `CreateDocument` 并返回目标 URI，认证成功后直接写入该 URI；认证取消、拒绝或写入失败时
  尝试删除选择器预创建的未完成文档。
  该链路不创建应用内明文副本，也不读取或复用备份目录设置。
- 只有“分享”会在 `cacheDir` 创建与格式匹配的明文临时文件，并用对应 MIME 交给系统分享：分享失败立即删除，
  分享成功后 10 分钟删除，下一次分享前清理陈旧文件。
