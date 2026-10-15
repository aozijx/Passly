# Logger 与诊断日志

## Logger 契约

`core/observability` 已定义唯一业务 API：事件、错误码、原因码、操作码和字段名均为验证后的值类型；字段只允许
计数、时长、比率、布尔、枚举、受控代码和待 HMAC 的标识符。`Logger.warn/error` 的签名强制要求
`ErrorCode + ReasonCode + Throwable`，已知但没有异常对象的降级使用 `degraded`。

App 业务层统一调用 `logger.log(LogStatements.<领域>.<事件>(...))`。`LogStatements` 是全 App 的类型化语句目录，
当前包含 Application、Database、Security、Autofill，后续领域在同一目录扩展；它负责绑定 level、event、
error_code、reason 和允许字段，业务代码无需重复导入或拼装协议常量。它不是 Autofill 专用 facade，也不创建第二套
Logger。Application、LifecycleObserver、Service、Activity、ViewModel 和运行时控制器均通过依赖注入获得同一个
Logger；非注入式工具只返回结果或错误，由注入所有者记录，不设全局 runtime bridge。未捕获异常由运行时控制器在
原线程建立根 Trace，并通过 `CrashRecordCaptureSink` 取得同一条
已脱敏 FATAL 记录交给应急写入。

业务调用不能构造 `SanitizedLogRecord` 或直接调用 Sink。后续隐私层必须把 pending 标识符替换为 HMAC、把
Throwable 转换为不含 message/path 的安全快照后，才能将不可变记录交给 `LogSink`。Trace API 已固定在 Logger
契约中。`TraceContextElement` 通过 `CoroutineContext` 与线程上下文共同传播同一个 Trace；根操作创建 Trace，
每个子操作只创建 Span 并指向直接父 Span。跨系统回调只传递最长 10 分钟、默认单次消费、最多 128 个的 opaque
`TraceToken`，且 token 与 `process_session_id` 绑定，进程或会话不匹配时立即失效。

`DefaultLogger` 对每个事件只执行一次隐私策略，再把同一个不可变记录分发给所有 Sink。`trace_id` 是一次操作唯一的
关联标识，子 Span 只继承该 Trace，不再为失败生成第二套诊断标识。Span 生命周期独立输出固定操作码、结果和单调时钟
耗时，平台 Trace 或日志 Sink 的异常不会改变业务控制流；业务异常和取消仍按原实例向上传播。

错误码与原因码的唯一来源是 `config/observability/error-catalog.json`。Gradle 从该结构化目录生成 core 的
`ErrorCodes`/`Reasons`、App 的 `GeneratedErrorMessageResolver` 和已提交的 `docs/ERROR_CODES.md`；生产源码不再
手写第二份常量。目录解析会拒绝未知属性、重复 symbol/code、未知默认原因和非法 exposure。只有
`USER_ACTIONABLE` 错误允许声明 `message_key`，且 `verifyErrorCodes` 要求默认与英文资源都存在；`INTERNAL` 错误
不得映射用户文案，UI 也不得直接展示原始错误码或异常文本。

## 当前分层

- `core/observability`：强类型事件/字段/错误目录、Trace、隐私策略和 Sink 契约。
- `app/observability`：应用级装配、Logcat/Perfetto、崩溃处理、上次进程退出读取和认证导出。
- `data/observability`：`SanitizedLogRecord` codec 与应用内部加密文件存储。
- `LogcatSink.kt`：observability 生产代码中唯一允许导入 `android.util.Log` 的文件。

生产事件只有一条执行链：

```text
注入 Logger 的调用点
  -> Logger.withRootTrace / inSpan
  -> logger.log(LogStatements...)
  -> DefaultLogger
  -> PrivacyPolicy（校验、异常快照、HMAC）
  -> EventAggregator（安全维度聚合、仅 DEBUG 采样）
  -> CrashCapture / LocalAlert / Logcat / EncryptedLogStore
```

`TraceContinuationStore` 只持有进程内、单次消费的 Trace token，用于 Autofill Service → 认证 Activity → 结果交付的
回调断点；它不代理 Logger、不持久化业务 ID，也不改变 Legacy Autofill 与 Credential Manager 各自的结果对象。

业务事件名使用受控 lower-dot 标识符。持久化字段只允许计数、时长、有限比率、布尔、枚举名、错误码、原因码、
操作码和由底层 HMAC 的标识符。自由文本 message、Throwable message、密码、Token、邮箱、URL 与文件路径不得进入
事件。普通业务对象必须注入 `Logger`；Android framework 回调由其注入所有者捕获并进入根 Trace，不提供静态
Logger、Service Locator 或安装前 no-op 桥。

## 本地加密文件

当前文件位于 `noBackupFilesDir/logs`，扩展名为普通 `.log`。路径与文件名不携带协议版本，格式版本只存在于
文件头；每个文件生成独立 256-bit 数据密钥，数据密钥由 Android Keystore wrapping key 以 AES-256-GCM 包装。

- Header AAD：magic、format version、16-byte fileId、createdAt。
- Record AAD：magic、format version、fileId、严格递增 sequence、level。
- wrapping nonce 与每条 record nonce 都独立随机生成且必须为 12 bytes；GCM tag 为 128 bits。
- 解码严格校验长度、UTF-8、枚举范围、sequence、外层/载荷 level 一致性和尾随字节。
- 读取使用 `fileId + byte offset + sequence` 游标，不把整个文件载入内存。
- writer 是容量 256 的单线程队列；文件按日期或 1 MiB 轮换，最多 3 个文件、3 MiB、3 天。

## 崩溃和导出

运行时预生成 emergency 数据密钥。崩溃时先 flush 普通队列 300 ms；失败后 emergency writer 最多等待
200 ms，且不调用 Keystore、不等待普通 writer lock。队列溢出只为 ERROR/FATAL 尝试一次独立 fallback。

查看最多读取当前 store 的 500 条。运行时为每条查看记录保留结构化 `LogLevel`、精确 `trace_id` 和已脱敏的固定八列
TSV 行，presentation 不解析文本恢复字段。查看页默认显示 INFO/WARN/ERROR，FATAL 归入 ERROR 组，DEBUG 默认隐藏；
等级筛选与精确 trace-id 查询只作用于这 500 条内存记录，绝不传入导出请求或改变导出内容。复制单行只使用对应的
已脱敏 TSV 行；复制 Trace 最多输出同一 trace 的 100 行，不能访问原始 record、Throwable 或未脱敏字段。页面进入时
立即刷新记录和当前视图字节数，完整会话校验失败时不读取日志；清理成功后同时清空 retained/visible 状态。

明文导出最多读取当前 store 的 5000 条记录，并在存储层以最多 500 条一页跨文件读取；
按 `timestamp_ms + elapsed_realtime_ms + event_id` 确定性排序并按 `event_id` 去重。manifest 集中包含 schema、生成时间、
记录数及 App/设备/系统环境，禁止 Android ID、Build fingerprint、serial、明文包名和已安装应用列表。

导出必须在现有 `EXPORT_DIAGNOSTICS` 新鲜认证授权块内写入内容。调用方只提交一个
`DiagnosticsExportRequest(action, format, documentUri)`；`SHARE` 禁止携带 URI，`EXPORT_LOCAL` 必须携带 SAF 返回的
`content://` URI。动作与格式保持正交，JSONL/TSV/ZIP 均可用于保存或分享；每次请求只 flush、读取、排序并生成一次
manifest，然后由同一编码器输出所选格式，不为不同目的维护第二套导出逻辑。

保存通过 Storage Access Framework 的 `CreateDocument` 选择与格式匹配的目标（JSONL 为
`application/x-ndjson`、TSV 为 `text/tab-separated-values`、ZIP 为 `application/zip`），认证成功后直接写入该 URI，
不创建分享缓存；认证取消、拒绝或写入失败时尝试删除选择器预创建的未完成文档。分享才在 `cacheDir` 创建带正确扩展名
的临时明文文件，并以对应 MIME 经 FileProvider 只读发送。分享失败立即删除，成功后 10 分钟删除，新分享前先清理旧
临时文件。明文导出不属于备份内容，也不复用备份目录设置。当前旧设置入口固定请求 JSONL；格式选择由专用日志页接管。

JSONL 是唯一 canonical 事件模型。独立 JSONL 第一行是 manifest JSON，后续事件字段稳定按 `time`、
`timestamp_ms`、`elapsed_realtime_ms`、`level`、`event`、`event_id`、`context`、`error`、`trace`、`session`
输出；缺失的可选对象/字段不输出，事件本身不重复 schema/version sentinel。TSV 只能从 canonical JSON 投影，固定八列为
`time level event outcome details exception trace session`，空列保留，bag 使用转义后的 `key=value`，未知 JSON 路径归入
`details` 而不丢失。独立 TSV 在表头前输出一行 `# key=value` manifest header。

ZIP 只包含 `manifest.json`、`diagnostics.jsonl`、`diagnostics.tsv`、`README.txt`；其中 JSONL/TSV payload 不重复
manifest。所有格式共享同一编码器、ISO-8601 UTC 动态时间、字段排序和安全异常快照。Autofill 等模块状态只通过模块事件
进入 payload，不写入通用 manifest。未知 schema major 在写出任何字节前拒绝，不生成部分文件。

业务代码禁止直接 `printStackTrace` 或使用 `android.util.Log`；Android 系统日志统一由 `LogcatSink.kt` 输出。

## Android 系统日志边界

当前 Logger 只允许单一 Android Sink 调用 `android.util.Log`。Logcat 用于 adb、
Android Studio 和系统
时间线中的即时诊断，不替代加密持久化日志。事件必须先完成字段校验、异常快照和脱敏，再按 Android 原生
DEBUG/INFO/WARN/ERROR 优先级输出；业务层不得直接打印异常或敏感字段。

Logger 使用有限稳定 Tag 和有界单行摘要，并与加密记录共享 `event_id/trace_id/span_id`。根据 Android 官方
关于 Log 信息泄露的建议，Release 默认完全关闭 Logcat Sink；显式诊断窗口只启用内部加密日志，不提升 Logcat。
Debug Sink 在格式化前使用 `Log.isLoggable`，且永远不把原始 Throwable 交给 Android Log API。

Logger 还会把受控 Span 名称接入 `android.os.Trace`，用于 Perfetto 性能分析；并在下次启动时从
`ApplicationExitInfo` 读取稳定的退出 reason/status/importance/内存数值。系统提供的 description、进程名和原始
ANR trace 不进入标准日志或导出。

## 聚合、采样与本地告警

所有领域共用一个进程内 `EventAggregator`，输入只能是已经通过隐私策略的 `SanitizedLogRecord`。聚合键由
event、level、category、脱敏字段、error_code 和 reason 组成，不包含 trace_id、span_id 或任何原始标识符。
默认最多维护 256 个 LRU bucket、窗口 5 分钟；淘汰、窗口到期、App 进入后台和运行时关闭时生成有界
`MetricSnapshot`，包含 count、first/last timestamp，以及存在 `elapsed_ms` 时的 min/max/average。

仅 DEBUG 可做基于 `event + process_session_id` 的稳定采样，不使用目标包名等标识符。ERROR/FATAL 以及 Autofill
结果交付事件始终保留。聚合快照回调带递归保护，Logger 自身指标不会再次进入聚合器。

`LocalAlertPolicy` 只在本机观察脱敏后的失败记录，不发起网络传输。当前规则是 10 分钟内连续出现 3 次
`DATABASE_SESSION_OPEN_FAILED` 时生成一个告警，并附带触发阈值时最新的 `trace_id`；同一窗口不重复告警，窗口过期
后自动清除。

## 错误接入规范

Observability 可以观察 Error，但 Error 不知道 Logger 的存在。依赖方向：

```
core/error -X-> core/observability
core/error -X-> Logger
```

### AppErrorReporter

```kotlin
fun interface AppErrorReporter {
    suspend fun report(error: AppError, context: ErrorReportContext)
}

data class ErrorReportContext(
    val operation: OperationCode,
    val category: LogCategory,
)
```

Reporter 属于 observability 层，不属 error 层。在 repository / use case 协程边界统一调用一次，避免重复记录。
旧 `AppError.errorId` 只用于进程内有界去重，不转换为 Trace 或导出字段。

### 白名单字段

实现只能写入以下字段，禁止自由文本：

| 允许                    | 禁止                   |
|-----------------------|----------------------|
| error_code            | error.message        |
| error_layer           | Throwable.message    |
| severity              | entryId              |
| recoverable           | 文件路径                 |
| operation             | URL / 域名             |
| throwable_type（白名单校验） | 用户名、备份 URI、密码、密钥、OTP |

### 记录策略

| 错误类型      | 日志策略             |
|-----------|------------------|
| 输入校验失败    | 通常不记录            |
| 条目不存在     | 通常不记录            |
| 乐观锁冲突     | WARN，可计数         |
| 用户取消认证    | 不记录              |
| 密码错误      | 匿名计数，不能记录密码或次数明细 |
| 数据库初始化失败  | ERROR            |
| 加密 Tag 失败 | ERROR            |
| 备份格式错误    | WARN             |
| 未知异常      | ERROR            |

### 接入示例

```kotlin
// DatabaseTransactionRunner 在数据库操作失败后报告
private suspend fun report(error: AppError, operation: String) {
    errorReporter.report(
        error = error,
        context = ErrorReportContext(
            operation = OperationCode(operation),
            category = LogCategory.DATABASE
        )
    )
}

// BackupArchiveServiceImpl 在导入/导出失败后报告
private suspend fun report(error: AppError, operation: String) {
    errorReporter.report(
        error = error,
        context = ErrorReportContext(
            operation = OperationCode(operation),
            category = LogCategory.BACKUP
        )
    )
}
```
