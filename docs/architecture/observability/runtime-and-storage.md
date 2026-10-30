# 日志运行时与加密存储

状态：当前实现（代码核对：2026-10-30）。

| 层 | 职责 |
| --- | --- |
| `core/observability` | Logger API、事件与字段目录、Trace、隐私策略、聚合和 Sink 契约 |
| `app/observability` | Android 装配、Logcat、Perfetto、崩溃捕获、导出编码和目标分发 |
| `data/observability` | 已脱敏记录 codec、加密日志文件、轮换和分页读取 |
| `feature/settings/diagnostics` | 查看与导出的应用层合同，不依赖 Android UI |
| Settings presentation | 查看、筛选、搜索、复制、清除、保存与分享交互 |

唯一数据链是 `Logger → DefaultLogger → PrivacyPolicy → EventAggregator → Sink`。Sink 只接受不可变的
`SanitizedLogRecord`；隐私边界前的 `Throwable`、待 HMAC 标识符和业务对象不能进入 codec、Logcat 或导出编码器。

## Android 运行时

`PasslyApplication` 启动一个 `ObservabilityRuntimeController`。Logger 与 store 装配完成后注册未捕获异常
handler；首次 DataStore 采集策略加载后才读取并记录 `ApplicationExitInfo` 的受控数值，避免未加载策略时
消费并丢弃上次退出记录。系统 description、进程名和原始 ANR trace 不进入标准日志。

`LogcatSink.kt` 是 observability 生产代码中唯一允许导入 `android.util.Log` 的文件。它只接收已脱敏记录，
输出有限 Tag 和有界单行摘要，并保留 `event_id/trace_id/span_id`。Release 默认关闭 Logcat；Debug 在格式化前
检查 `Log.isLoggable`，且永远不把原始 `Throwable` 交给 Android Log API。

本地加密日志默认保存 INFO 及以上；诊断页可设为关闭、WARN 及以上、INFO 及以上或全部等级。设置只在加密
`LogRecordStore` 边界过滤，不改变 `LocalAlertPolicy`、内存崩溃捕获、原崩溃委托、业务控制流或 Android Trace。
设置尚未从 DataStore 加载时暂不写入本地 store。降低等级或关闭采集不删除既有记录；Release Logcat 仍默认关闭，
Debug Logcat 仍由 Android Log 可见性控制，两者独立于本地加密记录策略。

受控 Span 名称同时写入 `android.os.Trace`，供 Perfetto 分析。业务层不得调用 `printStackTrace` 或创建第二套
静态 Logger/Service Locator。

## 非阻塞写入与背压

加密 store 使用容量 256 的单线程队列，生产者线程只入队，不执行文件 I/O。队列满时优先淘汰待写 DEBUG，
再淘汰 INFO；无法接收的记录仅按等级累计数量。ERROR/FATAL 可尝试一次独立 emergency write。

队列恢复并发生生命周期 flush 时，运行时写入一条 `application.log.overflow` 计数汇总。汇总不包含被丢弃内容，
也不递归进入已饱和队列。

清除会在写入接受锁内等待普通队列和 emergency writer 排空，等待上限为 500 ms。若未排空或文件删除未完成，
返回失败且界面不得报告清除成功；若排空并删除成功，清除前已接受的记录不会在成功提示后重新出现。清除边界之后
的写入在清除完成后继续入队。`LogRecordStore.clear()` 与诊断界面使用 `LogClearResult` 表达明确结果。

崩溃路径先等待普通 flush 300 ms，再最多等待 emergency writer 200 ms。应急数据密钥在运行时预生成；崩溃时
不访问 Keystore、不获取普通 writer lock，最后始终委托原 handler。

## 文件和轮换

日志只存放在 `noBackupFilesDir/logs`，扩展名为 `.log`。文件名不表达协议版本；文件头固定为 schema 1。
每个文件拥有随机 256-bit AES 数据密钥，再由 Android Keystore wrapping key 使用 AES-256-GCM 包装。

- Header AAD：magic、format version、16-byte file ID、created time。
- Record AAD：magic、format version、file ID、严格递增 sequence、level。
- wrapping nonce 与 record nonce 均为独立随机 12 bytes；GCM tag 为 128 bits。
- codec 校验长度、UTF-8、枚举、sequence、外层/载荷 level 和尾随字节。
- 读取使用 file ID、byte offset 和 sequence 游标，每页最多 500 条，不把全部文件载入内存。

文件按日期或 1 MiB 轮换，最多 3 个文件、3 MiB、3 天。清理与容量统计只处理 canonical parent 正好是日志
目录的普通 `.log` 文件，并保护当前活动文件。轮换、清理和关闭会覆盖不再使用的内存数据密钥。

诊断页面读取最多 500 条用于查看；导出最多读取 5000 条，并按 500 条分页。日志实际占用只在“数据管理”页
展示，查看页不维护第二份容量状态。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 观测运行时启动、读取、刷新与关闭 | `ObservabilityRuntimeController.start()`、`readLines()`、`flush()`、`shutdown()` | [`ObservabilityRuntimeController.kt`](../../../app/src/main/java/com/aozijx/passly/app/observability/ObservabilityRuntimeController.kt) |
| 加密日志记录读写与清理 | `EncryptedLogRecordStore.write()`、`readPage()`、`clear()`、`LogClearResult` | [`EncryptedLogRecordStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt)、[`LogRecordStore.kt`](../../../core/src/main/kotlin/com/aozijx/passly/core/observability/sink/LogRecordStore.kt) |
| 日志文件轮转与过期文件清理 | `EncryptedLogFileManager.cleanup()`、`clear()` | [`EncryptedLogFileManager.kt`](../../../data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogFileManager.kt) |
