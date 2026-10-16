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

Trace 的所有者是用户/系统操作，不是模块。Security 统一使用 `Logger.inSpan()`：位于 Autofill 或 App 操作中时继承
该操作的 `trace_id` 并创建子 Span，独立调用时由 Logger 自动创建根 Trace。认证事件始终携带受控 `purpose`；锁定
调用必须提供 `SessionLockRequest(reason, trigger)`，其中 reason 表示锁定策略，trigger 表示实际触发信号。screen-off、
翻转、后台、空闲计时器、Autofill 完成、恢复模式退出和 App 退出因此不会再被折叠为同一语义。

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
- writer 是容量 256 的单线程队列，Logger 装配完成后即可接收启动阶段事件，调用线程只入队、不执行文件 I/O。
  队列满时，高等级事件依次淘汰待写 DEBUG、INFO；仍无法入队的事件按等级原子计数，不保留被丢弃事件的内容。
  下一次生命周期 flush 在队列恢复后写一条 `application.log.overflow` 计数汇总并再次 flush，避免递归写入饱和队列。
- 文件按日期或 1 MiB 轮换，最多 3 个文件、3 MiB、3 天；按时间和文件名稳定地从最旧文件清理，并始终保护
  当前活动文件。清理和占用统计只处理 canonical parent 为日志目录的 `.log` 文件；轮换、清理和关闭会覆盖不再使用的
  内存数据密钥。设置层读取的是这些加密文件的实际总字节数，而不是当前可见文本的估算值。

## 崩溃和导出

运行时预生成 emergency 数据密钥。崩溃时先 flush 普通队列 300 ms；失败后 emergency writer 最多等待
200 ms，且不调用 Keystore、不等待普通 writer lock。队列溢出只为 ERROR/FATAL 尝试一次独立 fallback。

查看最多读取当前 store 的 500 条。运行时为每条查看记录保留结构化 `LogLevel`、精确 `trace_id` 和已脱敏的固定八列
TSV 行，presentation 不解析文本恢复字段。查看页默认显示 INFO/WARN/ERROR，FATAL 归入 ERROR 组，DEBUG 默认隐藏；
等级筛选与精确 trace-id 查询只作用于这 500 条内存记录，绝不传入导出请求或改变导出内容。复制单行只使用对应的
已脱敏 TSV 行；复制 Trace 最多输出同一 trace 的 100 行，不能访问原始 record、Throwable 或未脱敏字段。复制通过
Android ClipboardManager 标记为敏感内容，不写入 Compose 可保存状态。页面进入时立即刷新记录和当前视图字节数，完整
会话校验失败时不读取日志；清理成功后同时清空 retained/visible 状态。

诊断日志是 `SettingsDestination.Diagnostics` 独立二级页面。General 只发出导航，不持有日志 ViewModel、SAF launcher、
导出或清理状态；诊断 Route 独占页面进入刷新、三种 MIME 的 `CreateDocument`、效果收集与敏感剪贴板写入。Top bar 菜单
提供保存、分享、清理，保存/分享共用格式选择 sheet。页面固定显示 E/W/I/D FilterChip 与精确 trace 搜索；行使用等宽
字体，INFO/DEBUG 保持中性色，WARN 使用有限强调色，ERROR/FATAL 使用错误色，同时始终保留等级文本以满足无障碍识别。
长按行只提供“复制该行”和“复制该 Trace”，选择器取消会清除 pending 格式，不会遗留可误用的导出请求。

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
临时文件。明文导出不属于备份内容，也不复用备份目录设置。

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
默认最多维护 256 个 LRU bucket、窗口 1 分钟；淘汰、窗口到期、App 进入后台和运行时关闭时生成有界
`MetricSnapshot`，包含 count、first/last timestamp，以及存在 `elapsed_ms` 时的 min/max/average。

只有带类型化 `from_state`/`to_state` 且两者相等的 DEBUG/INFO 重复观察会在一分钟窗口内抑制；窗口首条仍输出。
`from_state != to_state` 的真实转换以及所有 WARN/ERROR/FATAL 永远绕过抑制。聚合快照回调带递归保护，Logger 自身
指标不会再次进入聚合器。

## Security 与 Autofill 链路语义

认证目的使用固定协议词汇，例如 `VAULT_UNLOCK`、`AUTOFILL`、`SECRET_REVEAL` 与 `DIAGNOSTICS_EXPORT`，不直接
序列化业务枚举名。锁定的 `lock_reason` 与 `trigger` 分列记录；调用方必须显式选择组合，不提供旧的单参数重载。
Legacy Autofill 和 Credential Manager 各自在平台回调入口创建随机 Passly 根 Trace；系统 request id 不作为 Trace。
opaque continuation 只恢复 Passly 已捕获的上下文，Security 认证在其中是普通子 Span，不存在“Security 继承 Autofill
模块”的依赖关系。

可观测的 Autofill 阶段为发现、认证、结果交付和 Passly 会话关闭。成功终态均携带用 `System.nanoTime()` 计算并
归一为非负值的 `elapsed_ms`；结果交付与会话关闭使用两个独立 Span，但保持同一 `trace_id`。没有 Android 回调能证明
字段确实写入时，不记录虚构的 `field_filled` 事件。

`ExceptionMapper` 以错误码上下文为主，将异常归入 `DATABASE_IO`、`CRYPTO`、`AUTHENTICATION`、`PLATFORM`、
`LIFECYCLE`、`IO` 或 `UNKNOWN`。未知错误只按 JDK 稳定父类型兜底，绝不读取 message；安全快照仍保留最多 24 个
Passly 栈帧、3 层 cause 类型和外部帧计数。`exception_kind` 因此不依赖 Release 混淆后的类名，具体异常类型只作为
辅助快照字段。

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
