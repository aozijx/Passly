# Passly Logger v1 与可观测性系统设计

- 状态：Draft for review
- 日期：2026-10-14
- 范围：应用内结构化日志、链路追踪、本地指标、加密诊断存储与用户主动导出
- 不在本期：默认远端上传、第三方崩溃平台、跨设备用户画像

## 1. 背景与目标

现有实现已经具备加密日志文件、Android Logcat Sink、安全字段类型、崩溃紧急写入、文件轮换和认证后导出，
但 `TelemetryEvent` 为每条事件创建新的 `correlationId`，无法表达一次操作中的父子关系；WARN/ERROR 也没有在
API 层强制错误码、原因和异常快照。数据库会话、安全认证与两套 Autofill 路径使用的字段不一致，因而难以从
一份用户导出的日志重建故障链路。

本设计将完全自研、轻量、local-first 的 Logger v1 作为 Passly 正式日志契约，替换当前 legacy Telemetry API，
同时复用已经过审查的加密存储、文件轮换、
崩溃紧急写入与认证导出能力。业务层不负责 JSON、脱敏、Trace 生成、文件切割或过期清理。

目标：

1. 一个用户操作或系统回调只生成一个 `trace_id`，所有子事件继承该 ID。
2. WARN/ERROR 通过强类型 API 携带 `error_code`、`reason` 和真实异常；无异常的预期降级不伪造 Throwable。
3. 任何 Sink 都只能接收已验证、已脱敏的不可变事件。
4. 密码、密钥、OTP、Token、用户名、URL、文件路径和目标应用明文包名不能进入日志。
5. 同时利用 Android Logcat 的优先级、Tag、系统时间线与开发工具生态，以及 Passly 的加密持久化诊断能力。
6. 重复事件在进程内有界聚合，导出中包含可诊断指标与设备上下文，但默认不自动上传。

### 1.1 Android 官方文档与同类项目复审结论

本设计在定稿前再次核对了 Android 官方文档和三个开源密码管理器。截至 2026-10-14，结论如下：

- Android 官方把 Logcat 视为共享且可能泄露信息的缓冲区，建议非 Debug 版本避免输出潜在敏感内容，并把详细日志
  放在应用内部存储。因此 Passly Release 默认完全关闭 Logcat Sink，而不是默认输出 WARN/ERROR；加密内部日志
  才是 Release 的诊断来源。
- `android.util.Log` 可以借助 `isLoggable(tag, level)` 避免无效格式化，但不能把 Throwable 直接传给 Log 方法，
  因为系统会生成未经 Passly 安全裁剪的完整异常文本。
- API 31 起可以直接使用 `android.os.Trace.beginAsyncSection/endAsyncSection/setCounter` 接入 Perfetto。系统 Trace
  只使用固定操作名与非敏感 cookie/counter，不能携带 event fields。
- API 30 起的 `ApplicationExitInfo` 可以补充上次进程退出原因、状态、重要性和内存统计。其 description、进程名、
  原始 ANR trace 格式不稳定且可能含敏感上下文，不进入标准日志导出。
- Bitwarden Android 的 Flight Recorder 证明“用户主动开启、固定时长、自动过期、文件头设备上下文”有实际价值；
  Passly 吸收这些生命周期做法，但拒绝其自由文本、完整 Throwable 和明文日志文件实现。
- Bitwarden 把远端 crash logging 设为可开关，并为 F-Droid 提供 no-op 实现；Passly 第一版更严格，完全不上传，
  以后如引入远端传输必须保持明确同意和可替换 no-op 边界。
- Proton Pass 的 Autofill Health 会检查默认服务、inline suggestion 支持情况和系统 Autofill Logcat；Passly 吸收
  “系统能力健康快照”，但不申请 `READ_LOGS`、不启动 `logcat` 子进程，也不导出明文浏览器包名。
- KeePassDX 通过 ADB Logcat 指引强调复现窗口和元数据日志。Passly 保留开发者 ADB 工作流，同时为普通用户提供
  认证后的加密日志导出，避免要求用户开启 USB 调试。

审计依据：

- [Android Log 信息泄露风险](https://developer.android.com/privacy-and-security/risks/log-info-disclosure)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [`android.util.Log`](https://developer.android.com/reference/android/util/Log)
- [`android.os.Trace`](https://developer.android.com/reference/android/os/Trace)
- [`ApplicationExitInfo`](https://developer.android.com/reference/android/app/ApplicationExitInfo)
- [Bitwarden FlightRecorderManager](https://github.com/bitwarden/android/blob/26771c22f3315c982e74dc7aa0d31005fe55bd73/data/src/main/kotlin/com/bitwarden/data/manager/flightrecorder/FlightRecorderManagerImpl.kt)
- [Bitwarden FlightRecorderWriter](https://github.com/bitwarden/android/blob/26771c22f3315c982e74dc7aa0d31005fe55bd73/data/src/main/kotlin/com/bitwarden/data/manager/flightrecorder/FlightRecorderWriterImpl.kt)
- [Bitwarden crash-log opt-in](https://github.com/bitwarden/android/blob/26771c22f3315c982e74dc7aa0d31005fe55bd73/app/src/standard/kotlin/com/x8bit/bitwarden/data/platform/manager/LogsManagerImpl.kt)
- [Proton Pass Autofill Logcat health reader](https://github.com/protonpass/android-pass/blob/956506506ff393f3ffc974c566332ae84dd750ef/pass/autofill/impl/src/main/kotlin/proton/android/pass/autofill/autofillhealth/service/AutofillLogcatReader.kt)
- [Proton Pass Autofill diagnostics](https://github.com/protonpass/android-pass/blob/956506506ff393f3ffc974c566332ae84dd750ef/pass/autofill/impl/src/main/kotlin/proton/android/pass/autofill/autofillhealth/troubleshooting/data/AutofillDiagnosticsBuilder.kt)
- [KeePassDX Log 指引](https://github.com/Kunzisoft/KeePassDX/wiki/Log)

## 2. 核心决策

### 2.1 自研 Logger v1，不引入 Timber/Kermit Facade

第三方 Facade 只能简化打印语法，无法替代 Passly 所需的字段类型、Trace 传播、隐私策略、Keystore HMAC、
加密存储和认证导出，反而会产生可以绕过策略的第二入口。因此业务代码只依赖 Passly Logger 契约。

允许直接使用 `android.util.Log` 的文件只有 `LogcatSink`。Gradle/静态规则禁止其他生产代码导入它。

### 2.2 重写事件模型，复用安全存储

Logger v1 直接覆盖现有业务日志入口，替换 `TelemetryEvent`、`correlationId` 和
`TelemetryRuntime.d/i/w/e`。旧 API 不作为公开 v1，也不与新 Logger 长期并存。复用：

- 每文件随机数据密钥；
- Android Keystore AES-GCM wrapping key；
- 每记录独立 nonce/AAD；
- 有界单线程写入队列；
- 按大小/日期轮换和过期清理；
- crash emergency writer；
- 新鲜认证后的 `cacheDir` 临时明文导出与定时删除。

新日志使用 `noBackupFilesDir/observability_v1` 和 `.log`，其内部文件头记录 magic、schema version 与加密参数，record schema 从 1 开始计数。扩展名不绑定格式版本。当前
`diagnostics_v1/.elog1` 是 legacy 存储格式，不代表 Logger v1；迁移期只读旧记录，新写入全部进入
`observability_v1`。最长保留窗口结束后删除 legacy reader 和旧目录，避免两个写入系统长期并存。

### 2.3 local-first

第一阶段不自动上传日志或指标。告警表现为本地聚合记录和诊断页状态；用户完成新鲜认证后主动导出。
远端能力只预留 `ExportTransport` 接口，未来必须经过单独隐私评审和明确用户同意。

## 3. 分层与依赖

```text
core/observability
  api/           Logger, EventKey, ErrorCode, ReasonCode, LogFields
  model/         LogEvent, ErrorPayload, ExceptionSnapshot
  trace/         TraceContext, TraceScope, TraceTokenRegistry
  privacy/       ContractValidator, PrivacyPolicy, IdentifierHasher
  aggregation/   EventAggregator, MetricSnapshot
  sink/          LogSink

app/observability
  AndroidLogger, ProcessContextProvider, DeviceContextProvider
  LogcatSink, ExportManifestFactory, CrashHandler

data/observability
  EncryptedFileSink, LogRecordCodecV1, RetentionPolicy
```

依赖规则：

```text
business -> Logger API
Logger -> validator -> redactor/hasher -> aggregator -> sinks
sinks -X-> business/domain
core/error -X-> Logger
```

错误对象仍不知道日志系统存在；在 repository/use-case/platform 边界由 `AppErrorReporter` 适配到 Logger。

## 4. Logger API

事件名和原因必须来自注册目录，字段不允许任意字符串：

```kotlin
interface Logger {
    fun debug(event: EventKey, fields: LogFields = LogFields.Empty)
    fun info(event: EventKey, fields: LogFields = LogFields.Empty)

    fun degraded(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    fun warn(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    fun error(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    suspend fun <T> withRootTrace(operation: OperationCode, block: suspend TraceScope.() -> T): T
    suspend fun <T> inSpan(operation: OperationCode, block: suspend TraceScope.() -> T): T
}
```

`degraded` 表达没有异常对象的已知降级；不得为了满足格式创建假 Throwable。真正由异常导致的 WARN/ERROR，
通过方法签名强制提供 Throwable、错误码和原因。

```kotlin
logger.error(
    event = Events.Autofill.LegacyFillRequestFailed,
    errorCode = ErrorCodes.AUTOFILL_FILL_REQUEST_FAILED,
    reason = Reasons.PLATFORM_CALLBACK_FAILED,
    throwable = error,
    fields = fields {
        enum("source", AutofillSource.LEGACY)
        count("retry_count", retryCount)
        durationMs("elapsed_ms", elapsedMs)
        hashedIdentifier("target_package", packageName)
    },
)
```

允许的值类型为 `Count`、`DurationMs`、`Ratio`、`BooleanValue`、`EnumValue`、`ErrorCodeValue`、
`ReasonCodeValue`、`OperationCodeValue` 和 `HashedIdentifierInput`。最终事件中只保存 HMAC 结果，永不保存其输入。

## 5. Trace 与 Session

### 5.1 标识语义

- `trace_id`：128-bit 随机值；在用户操作或系统回调入口创建一次。
- `span_id`：64-bit 随机值；每个同步/挂起子操作创建一个。
- `parent_span_id`：根 Span 为 null，其余指向直接父 Span。
- `process_session_id`：进程启动时创建，进程生命周期内稳定。
- `secure_session_id`：成功解锁时创建；软锁可保留，封存/退出时清除并在下次解锁重新创建。
- `diag_id`：一次可上报失败的用户可读短 ID；在错误边界创建，并被同一失败的后续事件继承。

`diag_id` 不替代 `trace_id`，`AppError.errorId` 迁移为 `DiagnosticId`，不再写入 correlation 字段。

### 5.2 传播

协程内通过 `CoroutineContext` 传播 `TraceContext`。Activity、Service、BroadcastReceiver、Credential Provider、
AutofillService 等系统入口创建根 Trace。回调脱离原协程时使用进程内、短生命周期 `TraceTokenRegistry`；Intent 只携带
不含上下文数据的 opaque token。进程重启导致 token 丢失时创建新 Trace，并记录受控的 `trace_resume_unavailable`。

不得使用全局可变的“当前 trace”，也不得让每条事件自行生成 trace。

## 6. 异常快照与隐私

### 6.1 异常处理

Logger 接收 Throwable，但只生成安全快照：

- 记录异常简单类型名；
- 最多 24 个 Passly 自身代码栈帧；
- 最多 3 层 cause 类型；
- 栈帧只保留归一化组件、类名、方法名和正行号；
- 不记录 `message`、`localizedMessage`、`toString()`、suppressed message、文件绝对路径或局部变量；
- 第三方/系统栈帧只记录归类计数，不记录完整命名空间。

“目标应用包名”与“Passly 自身代码类名”是不同数据。前者必须 HMAC，后者是定位 Passly 源码所必需的受控符号。

### 6.2 字段策略

安全主要依靠类型白名单，而非正则。底层正则只作为最后一道拒绝策略，检测疑似：

- 邮箱和 URL；
- Bearer/Basic 凭据和 JWT；
- 长 Base64/十六进制密钥；
- Android 文件路径；
- 非白名单包名形态。

命中后直接丢弃字段；Debug 抛 `TelemetryContractViolation`，Release 只增加匿名违规计数。框架绝不把命中值
写进 Logcat、文件或“脱敏失败”日志。

密码、主密码、DEK、密钥、OTP、Token、恢复码不允许进入 HMAC API。HMAC 仅用于目标包名等低熵标识符。

### 6.3 HMAC 密钥

首次安装时在 Android Keystore 生成不可导出的 HMAC-SHA256 密钥，别名包含版本号；不从服务端下发，且不要求
用户认证，以保证锁定状态的系统回调仍可安全关联。同一安装内结果稳定，卸载重装后不可关联。

输入使用带命名空间的域分离：

```text
HMAC(key, "passly-observability-v1\0" + field_namespace + "\0" + raw_value)
```

输出截取 128 bit，并编码为 `h1_<base64url>`。轮换时更新前缀；不同版本不尝试合并。

## 7. Android 系统日志集成

`LogcatSink` 是唯一 Android Log API 入口，接收的只能是已经验证的 `LogEvent`：

| Logger level | Android API | 用途 |
|---|---|---|
| DEBUG | `Log.d` | 跳过、重复、非关键生命周期细节 |
| INFO | `Log.i` | 成功状态转换与安全审计事件 |
| WARN | `Log.w` | 可恢复异常、重试、真实降级 |
| ERROR/FATAL | `Log.e` | 操作失败和崩溃 |

稳定 Tag 使用有限集合，例如 `Passly/Auth`、`Passly/Db`、`Passly/Autofill`、`Passly/App`，而不是把事件名
或包名放进 Tag。调用前先执行 `Log.isLoggable(tag, priority)`，避免为禁用等级构建 JSON。Logcat 只输出有严格
长度上限的单行摘要；超限时输出 `event_id`、`name` 和 `truncated=true`，完整安全事件只写加密 Sink。不能依赖
未形成公开稳定契约的 Logcat payload 长度，也不把一条事件拆成会与其他线程交错的多行。

Debug 默认 Logcat 最低 DEBUG，并允许开发者通过 `setprop log.tag.<TAG>` 与 `Log.isLoggable` 临时调整。
Release 默认禁用整个 Logcat Sink；用户开启诊断窗口只启用加密内部 Sink，不提升 Release Logcat。所有版本都禁止
把 Throwable 传给 `Log.e/w`，禁止调用 `Log.getStackTraceString`、`Log.println` 绕过统一入口，也禁止业务层调用
`Throwable.printStackTrace()`。

Android 系统事件时间线用于开发和 adb 现场诊断，但不是持久化审计源；用户可导出的加密日志才是稳定诊断来源。

### 7.1 Perfetto/System Trace

`TraceSink` 把受控 Span 映射为 Android 系统 Trace：

- 挂起或跨线程操作使用 `beginAsyncSection/endAsyncSection`，cookie 从 span ID 单向压缩为进程内唯一 Int；
- 同线程短操作才使用 `beginSection/endSection`，并必须在 `finally` 中配对；
- section name 只允许事件目录中的固定 `OperationCode`，满足 127 UTF-16 code units 上限；
- 队列深度、丢弃数量和活跃 Span 数可以使用 `setCounter`；
- 不写 trace ID、diag ID、目标包 HMAC 或任何业务字段；
- `Trace.isEnabled()` 为 false 时避免构造临时字符串和 cookie 映射。

系统 Trace 是性能分析旁路，不是 LogSink，也不替代应用 TraceContext。Trace API 失败不能影响业务或加密日志。

### 7.2 上次进程退出原因

应用启动后由 `PreviousProcessExitReader` 查询 `ActivityManager.getHistoricalProcessExitReasons()`，仅消费尚未记录的最近
一次退出，并映射为 `application.previous_process_exit`。允许字段：系统 reason 枚举、status、importance、PSS/RSS、
timestamp 和距离当前启动的 elapsed time。读取游标使用退出 timestamp 去重。

禁止读取或导出 `description`、`processName`、UID、PID、`traceInputStream` 原文。标准事件只记录系统稳定数值到
Passly 枚举的映射；未知 reason 保留为 `UNKNOWN`，原始 Int 仅作为有界 Count。

## 8. 等级与关键事件

### 8.1 等级规则

- ERROR：操作失败，必须有错误码、原因、真实异常、安全堆栈和 diag ID。
- WARN：存在真实异常的可恢复失败/重试，必须包含相同错误结构。
- DEGRADED：无 Throwable 的已知能力降级，必须有错误码和枚举原因。
- INFO：状态转换、安全事件和成功结果。
- DEBUG：跳过、幂等重复、预期空结果和详细生命周期事件。

`lifecycle.background_lock_skipped` 从 INFO 调整为 DEBUG。

### 8.2 数据库会话

统一事件：`database.session.transition`、`database.session.transition_failed`。

必需字段：`from_state`、`to_state`、`lock_type`、`reason`、`owner`、`elapsed_ms`。失败时还需错误结构；
排空超时增加 `timeout_ms` 和活跃 lease 数量，但不能包含 DAO 参数或数据库内容。

### 8.3 安全与认证

- `security.lock.started`
- `security.lock.completed`
- `security.unlock.started`
- `security.unlock.completed`
- `security.authentication.failed`

字段包括 `lock_reason`、`trigger`、`timeout_ms`、`result`、`authentication_method` 和耗时。认证方式只能是
`BIOMETRIC`、`MASTER_PASSWORD`、`RECOVERY` 等枚举；失败原因只能是枚举原因，不记录凭据、系统异常消息或
生物识别数据。

### 8.4 Autofill

Legacy Autofill 与 Credential Manager 使用同一事件目录，但 `source` 字段保留两套平台来源：

- `autofill.discovery.started/completed/unavailable`
- `autofill.authentication.started/completed`
- `autofill.result.delivery_started/delivered/failed`
- `autofill.session.close_started/closed/failed`
- `autofill.save.started/staged/completed/failed`

`autofill.discovery.unavailable` 必须携带枚举 `reason`，例如 `NO_FILLABLE_FIELDS`、`SESSION_RESTRICTED`、
`DATABASE_UNAVAILABLE`、`NO_MATCHING_ENTRY`。目标包名只能保存 HMAC。

关闭 Autofill 临时会话必须与填充结果交付处于同一 trace，并分别记录，确保可以判断“交付成功但会话清理失败”。

诊断导出 manifest 额外记录 Autofill 系统能力快照：Passly 是否为默认 Autofill Service、Credential Provider 是否
启用、inline suggestions 是否可用。已安装浏览器/目标应用只输出 HMAC 和能力枚举，不读取全局 Logcat，不申请
`READ_LOGS`。

## 9. 聚合、采样、指标与本地告警

ERROR、FATAL、安全状态转换和 Autofill 结果交付不采样。DEBUG 可以按事件配置确定性采样。

重复事件聚合键：

```text
(event_key, level, error_code, reason, safe_dimension_set)
```

聚合器使用有界 LRU 和时间窗口，输出：`count`、`first_seen_ms`、`last_seen_ms`、`window_ms`、
`min_elapsed_ms`、`max_elapsed_ms`、`avg_elapsed_ms`。缓存达到上限时先输出最旧桶，不能无界增长。

本地指标至少包括：

- 数据库打开/关闭失败次数与耗时；
- 锁定/解锁成功率与耗时；
- Autofill discovery unavailable 原因分布；
- Autofill 结果交付失败和会话关闭失败；
- Logger 队列丢弃、聚合驱逐、契约违规和 emergency write 次数。

本地告警规则只生成一个聚合事件和诊断页状态，例如 10 分钟内连续 3 次数据库打开失败。告警不弹出敏感细节，
只显示用户提示和 `diag_id`。

## 10. 错误码、提示与 Diagnostic ID

错误码由单一目录定义，并显式映射到用户提示代码。示例：

| Error code | 默认级别 | 用户提示 |
|---|---:|---|
| `AUTOFILL_DISCOVERY_UNAVAILABLE` | DEGRADED | 未找到可填充内容 |
| `AUTOFILL_RESULT_DELIVERY_FAILED` | ERROR | 自动填充失败，请重试 |
| `AUTOFILL_SESSION_CLOSE_FAILED` | ERROR | 操作未安全结束，请重新打开 Passly |
| `DATABASE_SESSION_OPEN_FAILED` | ERROR | 无法打开保险库 |
| `DATABASE_SESSION_CLOSE_FAILED` | ERROR | 保险库未能安全关闭 |
| `AUTHENTICATION_FAILED` | WARN | 验证失败，请重试 |
| `DIAGNOSTICS_EXPORT_FAILED` | ERROR | 无法导出诊断信息 |

UI 显示本地化提示和短 `diag_id`，例如 `DGN-7K4M-2Q9P`。该 ID 由随机数据加校验位生成，不编码时间、设备、
用户或错误类型。完整随机值保存在事件中，显示形式仅用于人工输入和日志检索。

## 11. JSON Schema v1

导出采用 JSON Lines：第一行为 export manifest，后续每行为 event 或 metric。内部加密文件可以继续使用紧凑二进制
编码，但语义必须与此 Schema 一致。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://passly.local/schema/observability-v1.json",
  "title": "Passly Observability Export v1",
  "oneOf": [
    { "$ref": "#/$defs/manifest" },
    { "$ref": "#/$defs/event" },
    { "$ref": "#/$defs/metric" }
  ],
  "$defs": {
    "id": { "type": "string", "pattern": "^[A-Za-z0-9_-]{8,64}$" },
    "identifier": { "type": "string", "pattern": "^[a-z][a-z0-9_.]{2,95}$" },
    "safeValue": {
      "oneOf": [
        { "type": "integer" },
        { "type": "number" },
        { "type": "boolean" },
        { "type": "string", "pattern": "^[A-Z][A-Z0-9_]{1,63}$" },
        { "type": "string", "pattern": "^h[0-9]+_[A-Za-z0-9_-]{16,64}$" }
      ]
    },
    "trace": {
      "type": "object",
      "additionalProperties": false,
      "required": ["trace_id", "span_id"],
      "properties": {
        "trace_id": { "$ref": "#/$defs/id" },
        "span_id": { "$ref": "#/$defs/id" },
        "parent_span_id": { "oneOf": [{ "$ref": "#/$defs/id" }, { "type": "null" }] }
      }
    },
    "error": {
      "type": "object",
      "additionalProperties": false,
      "required": ["error_code", "reason", "diag_id"],
      "properties": {
        "error_code": { "type": "string", "pattern": "^[A-Z][A-Z0-9_]{2,63}$" },
        "reason": { "type": "string", "pattern": "^[A-Z][A-Z0-9_]{2,63}$" },
        "diag_id": { "$ref": "#/$defs/id" },
        "exception_type": { "type": "string", "pattern": "^[A-Za-z][A-Za-z0-9_]{0,63}$" },
        "stack_trace": {
          "type": "array",
          "maxItems": 24,
          "items": { "type": "string", "maxLength": 256 }
        },
        "cause_types": {
          "type": "array",
          "maxItems": 3,
          "items": { "type": "string", "pattern": "^[A-Za-z][A-Za-z0-9_]{0,63}$" }
        }
      }
    },
    "manifest": {
      "type": "object",
      "additionalProperties": false,
      "required": ["record_type", "schema_version", "export", "app", "device"],
      "properties": {
        "record_type": { "const": "manifest" },
        "schema_version": { "const": 1 },
        "export": { "type": "object" },
        "app": { "type": "object" },
        "device": { "type": "object" }
      }
    },
    "event": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "record_type", "schema_version", "event_id", "timestamp_ms", "elapsed_realtime_ms",
        "level", "category", "name", "process_session_id", "trace", "fields"
      ],
      "properties": {
        "record_type": { "const": "event" },
        "schema_version": { "const": 1 },
        "event_id": { "$ref": "#/$defs/id" },
        "timestamp_ms": { "type": "integer", "minimum": 0 },
        "elapsed_realtime_ms": { "type": "integer", "minimum": 0 },
        "level": { "enum": ["DEBUG", "INFO", "WARN", "ERROR", "FATAL"] },
        "category": { "enum": ["APPLICATION", "DATABASE", "SECURITY", "AUTOFILL", "BACKUP"] },
        "name": { "$ref": "#/$defs/identifier" },
        "process_session_id": { "$ref": "#/$defs/id" },
        "secure_session_id": { "oneOf": [{ "$ref": "#/$defs/id" }, { "type": "null" }] },
        "trace": { "$ref": "#/$defs/trace" },
        "fields": {
          "type": "object",
          "maxProperties": 64,
          "additionalProperties": { "$ref": "#/$defs/safeValue" }
        },
        "error": { "$ref": "#/$defs/error" }
      },
      "allOf": [
        {
          "if": { "properties": { "level": { "enum": ["WARN", "ERROR", "FATAL"] } } },
          "then": { "required": ["error"] }
        }
      ]
    },
    "metric": {
      "type": "object",
      "additionalProperties": false,
      "required": ["record_type", "schema_version", "name", "count", "window_ms"],
      "properties": {
        "record_type": { "const": "metric" },
        "schema_version": { "const": 1 },
        "name": { "$ref": "#/$defs/identifier" },
        "count": { "type": "integer", "minimum": 1 },
        "window_ms": { "type": "integer", "minimum": 1 },
        "dimensions": {
          "type": "object",
          "maxProperties": 16,
          "additionalProperties": { "$ref": "#/$defs/safeValue" }
        }
      }
    }
  }
}
```

生产实现应把 manifest 中的宽泛 object 进一步收紧为独立 schema；上面的联合 Schema 定义跨模块契约的最低要求。

## 12. Debug、Release 与故障隔离

Debug：

- 非法事件名、未知字段、自由字符串和疑似敏感值立即抛 `TelemetryContractViolation`；
- 测试强制验证 WARN/ERROR 的错误结构；
- CI 扫描生产代码中的 `android.util.Log`、`printStackTrace` 和旧 Telemetry API。

Release：

- Logger 契约问题不能导致密码管理器崩溃；
- 丢弃违规字段或整个事件，只增加不含原值的违规计数；
- Logger、Sink、聚合器和 crash handler 全部隔离异常；
- crash handler/emergency writer 永远不能再次抛异常。

## 13. 迁移阶段与验收

### 阶段 0：契约与测试基线

冻结事件目录、错误码、原因码和 JSON Schema；增加敏感数据否定测试、异常快照测试和 Trace 继承测试。

### 阶段 1：Logger 核心

实现模型、强类型字段、Trace CoroutineContext、异常净化、Keystore HMAC 和 Debug/Release validator。此阶段使用
内存 Sink 完成单元测试，不改业务调用点。

### 阶段 2：Android 与持久化 Sink

实现唯一 `LogcatSink`，实现 `LogRecordCodecV1`，对 legacy diagnostics codec 只读兼容，并接入轮换、保留期、
队列背压和 crash writer。同时实现受控 `TraceSink` 和 `PreviousProcessExitReader`；二者失败不得阻断主 Logger。

### 阶段 3：关键链路迁移

按数据库会话 → 安全认证/锁定 → Legacy Autofill → Credential Manager → 备份/应用错误的顺序迁移。每条链路
先增加集成测试，再删除旧调用。两套 Autofill 平台实现保留，但统一事件和 Trace 契约。

### 阶段 4：聚合、指标与导出

实现聚合器、本地告警、JSONL 导出 manifest、设备上下文和诊断 ID 展示。验证认证、缓存文件删除和导出中无敏感值。

### 阶段 5：删除旧实现

确认无旧调用后删除 `TelemetryRuntime`、旧 `TelemetryEvent`/formatter、legacy codec 写入路径和兼容桥；旧
diagnostics reader 只保留到 3 天日志窗口结束，再连同 `diagnostics_v1` 目录迁移清理逻辑一起删除。

最终验收：

- 同一 Autofill 请求从 discovery 到结果交付、会话关闭只有一个 trace ID；
- 数据库和安全事件具有规定的状态/原因/耗时字段；
- WARN/ERROR 不可能缺少错误码和原因；异常路径存在安全栈帧且不存在异常消息；
- 密码、密钥、OTP、Token、URL、路径和目标明文包名的测试载荷无法到达任一 Sink；
- Release 构建不产生 Logcat，Debug Logcat 尊重 `Log.isLoggable`，且从不接收原始 Throwable；
- Perfetto async section 严格成对，名称与 counter 均来自固定非敏感目录；
- 上次进程退出只记录稳定枚举和数值，且不会导出 description/processName/raw ANR trace；
- 重复日志被聚合，关键错误不采样；
- 导出首行是设备/App manifest，后续 JSONL 通过 Schema 校验；
- 加密文件轮换、过期、崩溃写入、认证导出和临时文件删除测试通过；
- `verifyModuleBoundaries`、单元测试、lint 和目标设备 instrumentation 全部通过。

## 14. 文档维护

实现时同步更新：

- `docs/architecture/telemetry.md`：当前实现、调用规范和模块边界；
- `docs/architecture/error-handling.md`：ErrorCode、DiagnosticId 与 UI 提示映射；
- `docs/security/diagnostics.md`：威胁、脱敏、HMAC、加密存储和导出策略；
- 新增 ADR：采用 Logger v1 并废弃 per-event correlation ID；
- Autofill/Authentication 文档：关键事件和 Trace 生命周期。

代码、Schema、测试与正式文档必须在同一迁移阶段提交，不能只提交实现计划。
