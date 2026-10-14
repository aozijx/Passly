# Passly Observability Logger v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用正式 Logger v1 覆盖 legacy Telemetry，使数据库、安全认证及两套 Autofill 具备安全结构化日志、统一 Trace、Android 原生诊断能力和认证导出。

**Architecture:** `core/observability` 提供强类型事件、隐私校验、Trace 和 Sink 契约；`app/observability` 装配 Debug-only Logcat、Perfetto、进程退出读取和导出；`data/observability` 复用现有 AES-GCM 文件基础设施并写入 v1 record。迁移期间只读 legacy `.elog1`，所有新事件只写 `observability_v1/*.log`，最终删除旧 API 和写入路径。内部格式版本由文件头承载，不编码进扩展名。

**Tech Stack:** Kotlin、Coroutines/CoroutineContext、Android `Log`、`Trace`、`ApplicationExitInfo`、Android Keystore HMAC-SHA256、AES-256-GCM、Hilt、JUnit、Kotlin Serialization（仅导出 JSONL）。

**Spec:** `docs/superpowers/specs/2026-10-14-observability-logger-v1-design.md`

## Global Constraints

- Android 最低版本保持 API 31，不增加 Timber、Kermit、OpenTelemetry 或远端 crash SDK。
- Release 默认完全关闭 Logcat；诊断窗口只启用内部加密日志。
- 生产代码只有 `LogcatSink.kt` 可以导入 `android.util.Log`；任何代码都不得调用 `printStackTrace()` 或 `Log.getStackTraceString()`。
- 任何 Sink 都不能接收 Throwable、自由文本字段或未 HMAC 的目标包名；Sink 只接收不可变 `SanitizedLogRecord`。
- 不记录 Throwable message/localizedMessage/toString、密码、主密码、DEK、密钥、OTP、Token、恢复码、URL、路径或目标明文包名。
- WARN/ERROR 通过方法签名强制 `errorCode + reason + Throwable`；无 Throwable 的预期降级使用 `degraded`。
- 一个用户操作/系统回调创建一个 trace；子操作继承 trace ID 并创建 span ID，禁止逐事件创建 trace。
- 两套 Autofill 平台实现全部保留，只统一事件和 Trace，不合并业务结果系统。
- 新文件写入 `noBackupFilesDir/observability_v1/*.log`；legacy `diagnostics_v1/*.elog1` 迁移期只读。`.log` 是内部加密容器，文件头包含 magic、schema version 与加密参数；扩展名不承担版本协商。
- 所有提交使用精确路径，禁止 `git add .`，不得暂存或修改用户现有的 `gradle/libs.versions.toml` 变更。
- 每个迁移任务同时更新相应正式文档，不能只提交代码。

## Review Focus

- Release 或被关闭的 Logcat Sink 收到 ERROR：不得调用任何 Android Log 方法，也不得格式化 Throwable；Task 4 增加记录调用次数的 fake gateway 测试。
- Keystore HMAC 初始化或运算失败：删除需要哈希的字段而不是退回明文/普通 SHA，并避免递归记录 Logger 自身错误；Task 2/4 增加失败测试。
- 嵌套协程取消、异常和 dispatcher 切换：所有 span 必须关闭，子事件保持同一 trace 且 parent span 正确；Task 3 增加取消/异常测试。
- Autofill PendingIntent/Activity 在进程重启后拿到过期 Trace token：创建新根 trace 并记录 `trace_resume_unavailable`，不得复用其他会话；Task 8 增加生命周期测试。
- 有界队列满、磁盘失败或 crash handler 重入：关键错误只尝试一次 emergency write，不能阻塞、无限递归或使原崩溃被替换；Task 5/6 增加背压与重入测试。

---

## Planned File Structure

```text
core/src/main/kotlin/com/aozijx/passly/core/observability/
  Logger.kt                         # 唯一业务日志 API
  EventCatalog.kt                   # EventKey/ErrorCode/ReasonCode/OperationCode
  LogFields.kt                      # 无自由字符串的字段 builder
  LogRecord.kt                      # PendingLogEvent/SanitizedLogRecord/ErrorPayload
  privacy/PrivacyPolicy.kt          # 字段和标识符验证/净化
  privacy/ExceptionSnapshotFactory.kt
  privacy/IdentifierHasher.kt       # 平台无关端口
  trace/TraceContext.kt
  trace/TraceContextElement.kt
  trace/TraceTokenRegistry.kt
  internal/DefaultLogger.kt         # 校验、上下文、聚合与 Sink 分发
  sink/LogSink.kt
  sink/LogRecordStore.kt
  metrics/EventAggregator.kt

app/src/main/java/com/aozijx/passly/app/observability/
  ObservabilityRuntimeController.kt # 初始化、flush、shutdown、framework bridge
  LoggerRuntime.kt                  # 仅 Android/framework 无法注入的入口
  LogcatSink.kt                     # 唯一 android.util.Log 使用点
  AndroidTraceSink.kt               # android.os.Trace/Perfetto
  AndroidKeystoreIdentifierHasher.kt
  PreviousProcessExitReader.kt
  ExportManifestFactory.kt
  ObservabilityExportService.kt
  ObservabilityAppErrorReporter.kt
  di/ObservabilityModule.kt

data/src/main/java/com/aozijx/passly/data/observability/
  LogRecordCodecV1.kt
  EncryptedLogRecordStore.kt
  EncryptedLogFileManagerV1.kt
  LegacyDiagnosticsReader.kt
  di/ObservabilityPersistenceModule.kt
```

---

### Task 1: Logger v1 强类型契约与事件目录

**Files:**
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/EventCatalog.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/LogFields.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/LogRecord.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/Logger.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/sink/LogSink.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/LogContractTest.kt`
- Modify: `docs/architecture/telemetry.md`

**Interfaces:**
- Produces: `EventKey`, `ErrorCode`, `ReasonCode`, `OperationCode`, `DiagnosticId`, `LogFields`, `PendingLogEvent`, `SanitizedLogRecord`, `LogSink.emit(SanitizedLogRecord)` and the `Logger` signatures fixed by the spec.
- Consumes: no new production interfaces.

- [ ] **Step 1: Write the failing contract tests**

Add tests named:

```kotlin
fun `event and field identifiers reject free text and invalid grammar`()
fun `log fields expose no arbitrary string value`()
fun `warn and error require code reason and throwable at the API boundary`()
fun `diagnostic id display form contains no time device or error data`()
```

Use reflection in the second test to assert that no permitted `LogValue` subtype contains a public raw `String` constructor except validated catalog/value classes and `HashedIdentifierInput`, which is pending-only.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*LogContractTest"`

Expected: FAIL because `core.observability` does not exist.

- [ ] **Step 3: Implement the catalog, value types and Logger interface**

Use these signatures:

```kotlin
@JvmInline value class EventKey(val value: String)
@JvmInline value class ErrorCode(val value: String)
@JvmInline value class ReasonCode(val value: String)
@JvmInline value class OperationCode(val value: String)
@JvmInline value class DiagnosticId(val value: String)

interface Logger {
    fun debug(event: EventKey, fields: LogFields = LogFields.Empty)
    fun info(event: EventKey, fields: LogFields = LogFields.Empty)
    fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields = LogFields.Empty): DiagnosticId
    fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields = LogFields.Empty): DiagnosticId
    fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields = LogFields.Empty): DiagnosticId
    suspend fun <T> withRootTrace(operation: OperationCode, block: suspend TraceScope.() -> T): T
    suspend fun <T> inSpan(operation: OperationCode, block: suspend TraceScope.() -> T): T
}
```

Register only events/codes/reasons needed by later tasks; do not build a code generator in v1.

- [ ] **Step 4: Run contract tests**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*LogContractTest"`

Expected: PASS.

- [ ] **Step 5: Commit the contract**

```powershell
git add -- core/src/main/kotlin/com/aozijx/passly/core/observability core/src/test/kotlin/com/aozijx/passly/core/observability/LogContractTest.kt docs/architecture/telemetry.md
git commit -m "feat(observability): define logger v1 contract"
```

### Task 2: 隐私策略、安全异常快照与 HMAC 端口

**Files:**
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/PrivacyPolicy.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/ExceptionSnapshotFactory.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/privacy/IdentifierHasher.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/privacy/PrivacyPolicyTest.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/privacy/ExceptionSnapshotFactoryTest.kt`
- Modify: `docs/security/diagnostics.md`

**Interfaces:**
- Consumes: Task 1 `PendingLogEvent`, `LogFields`, `HashedIdentifierInput`.
- Produces: `PrivacyPolicy.sanitize(event, context): SanitizationResult`, `ExceptionSnapshotFactory.create(Throwable): ExceptionSnapshot`, `IdentifierHasher.hash(namespace, rawValue): HashedIdentifier?`.

- [ ] **Step 1: Write failing privacy tests**

Cover:

```kotlin
fun `snapshot never reads or emits throwable messages suppressed messages or foreign frames`()
fun `snapshot keeps at most 24 Passly frames and 3 cause types`()
fun `password token otp url path and package shaped raw fields are rejected`()
fun `hasher failure drops hashed field and never falls back to raw or digest`()
fun `release violation returns rejected result without echoing offending value`()
```

Use sentinel secrets in Throwable messages and assert they do not occur in any `SanitizationResult` property or `toString()`.

- [ ] **Step 2: Run tests and confirm failure**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*PrivacyPolicyTest" --tests "*ExceptionSnapshotFactoryTest"`

Expected: FAIL because privacy classes do not exist.

- [ ] **Step 3: Implement allowlist-first sanitization**

`PrivacyPolicy` must validate field keys/types first. Regex detectors are rejection-only defense for legacy/framework inputs; they must never redact-and-keep passwords. `IdentifierHasher` accepts only namespace constants from `IdentifierNamespace` and returns null on platform failure.

- [ ] **Step 4: Run privacy tests**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*PrivacyPolicyTest" --tests "*ExceptionSnapshotFactoryTest"`

Expected: PASS with every sentinel absent.

- [ ] **Step 5: Commit privacy core**

```powershell
git add -- core/src/main/kotlin/com/aozijx/passly/core/observability/privacy core/src/test/kotlin/com/aozijx/passly/core/observability/privacy docs/security/diagnostics.md
git commit -m "feat(observability): enforce safe log payloads"
```

### Task 3: Trace、Span、Session 与 DefaultLogger

**Files:**
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/trace/TraceContext.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/trace/TraceContextElement.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/trace/TraceTokenRegistry.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/internal/DefaultLogger.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/trace/TracePropagationTest.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/internal/DefaultLoggerTest.kt`

**Interfaces:**
- Consumes: Task 1 Logger/Sink and Task 2 sanitization/snapshot/hasher.
- Produces: `TraceContext`, `TraceScope`, `TraceToken`, `TraceTokenRegistry.capture/consume`, `DefaultLogger`, `ProcessSessionProvider`, `SecureSessionProvider`.

- [ ] **Step 1: Write failing propagation tests**

Add tests asserting:

```kotlin
fun `root and nested spans share trace and maintain direct parent relationship`()
fun `dispatcher switch preserves trace context`()
fun `cancelled span closes once and restores parent context`()
fun `exceptional span records elapsed time and rethrows the original exception`()
fun `expired or unknown trace token cannot attach to another session`()
fun `each reportable failure creates one diagnostic id reused by child events`()
```

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*TracePropagationTest" --tests "*DefaultLoggerTest"`

Expected: FAIL because trace implementation is absent.

- [ ] **Step 3: Implement CoroutineContext propagation and bounded token registry**

Use `withContext(TraceContextElement(...))` and `try/finally` for span closure. Registry entries expire after 10 minutes, are single-use by default, have a capacity of 128, and store no fields or Throwable. `DefaultLogger` constructs IDs/context, invokes privacy policy exactly once, then fans out the same immutable record to sinks.

- [ ] **Step 4: Run trace/logger tests**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*TracePropagationTest" --tests "*DefaultLoggerTest"`

Expected: PASS, including cancellation tests.

- [ ] **Step 5: Commit tracing core**

```powershell
git add -- core/src/main/kotlin/com/aozijx/passly/core/observability/trace core/src/main/kotlin/com/aozijx/passly/core/observability/internal core/src/test/kotlin/com/aozijx/passly/core/observability/trace core/src/test/kotlin/com/aozijx/passly/core/observability/internal
git commit -m "feat(observability): propagate trace context"
```

### Task 4: Android Keystore、Debug Logcat、Perfetto 与进程退出信息

**Files:**
- Create: `app/src/main/java/com/aozijx/passly/app/observability/AndroidKeystoreIdentifierHasher.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/AndroidLogGateway.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/LogcatSink.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/AndroidTraceSink.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/PreviousProcessExitReader.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/LogcatSinkTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/AndroidTraceSinkTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/PreviousProcessExitReaderTest.kt`
- AndroidTest: `app/src/androidTest/java/com/aozijx/passly/app/observability/AndroidKeystoreIdentifierHasherTest.kt`

**Interfaces:**
- Consumes: `SanitizedLogRecord`, `IdentifierHasher`, Trace span lifecycle.
- Produces: Keystore-backed hasher, `LogcatSink`, `AndroidTraceSink`, `PreviousProcessExitReader.readUnrecorded(): PreviousExit?`.

- [ ] **Step 1: Write failing JVM tests using platform gateways**

Assert:

```kotlin
fun `release sink performs zero log calls even for fatal record`()
fun `disabled tag skips formatting and log call`()
fun `debug sink emits one bounded line and never receives Throwable`()
fun `oversized summary emits identifiers and truncated flag in one line`()
fun `async trace begin and end use same fixed name and cookie once`()
fun `trace failure is swallowed without suppressing logger delivery`()
fun `previous exit omits description process name uid pid and raw trace`()
fun `same previous exit timestamp is emitted once`()
```

- [ ] **Step 2: Run JVM tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*LogcatSinkTest" --tests "*AndroidTraceSinkTest" --tests "*PreviousProcessExitReaderTest"`

Expected: FAIL because Android adapters do not exist.

- [ ] **Step 3: Implement Android adapters**

Wrap static Android calls behind package-internal gateways for JVM tests. HMAC key alias is `com.aozijx.passly.observability.hmac.v1`, algorithm `HmacSHA256`, purpose `PURPOSE_SIGN`, key size 256, no user authentication. Logcat summary maximum is a Passly-owned conservative constant and is never treated as a platform contract. Trace names come only from `OperationCode`.

- [ ] **Step 4: Run JVM tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*LogcatSinkTest" --tests "*AndroidTraceSinkTest" --tests "*PreviousProcessExitReaderTest"`

Expected: PASS.

- [ ] **Step 5: Run Keystore instrumentation when a device is available**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aozijx.passly.app.observability.AndroidKeystoreIdentifierHasherTest`

Expected: same namespace/value hashes consistently; different namespace changes output; output contains no input. If no device is connected, record this as an explicit pending acceptance item rather than claiming pass.

- [ ] **Step 6: Commit Android-native integration**

```powershell
git add -- app/src/main/java/com/aozijx/passly/app/observability app/src/test/java/com/aozijx/passly/app/observability app/src/androidTest/java/com/aozijx/passly/app/observability
git commit -m "feat(observability): add Android diagnostic sinks"
```

### Task 5: 加密 Record v1、轮换与 legacy 只读迁移

**Files:**
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/sink/LogRecordStore.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/observability/LogRecordCodecV1.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogFileManagerV1.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/observability/LegacyDiagnosticsReader.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/observability/di/ObservabilityPersistenceModule.kt`
- Test: `data/src/test/java/com/aozijx/passly/data/observability/LogRecordCodecV1Test.kt`
- Test: `data/src/test/java/com/aozijx/passly/data/observability/EncryptedLogRecordStoreTest.kt`
- Modify: `docs/security/diagnostics.md`

**Interfaces:**
- Consumes: `SanitizedLogRecord`, existing encrypted file primitives and legacy `TelemetryRecordCodec` for read-only migration.
- Produces: `LogRecordStore.write/readPage/readBetween/flush/clear/close/crashEmergencyWrite` and factory binding.

- [ ] **Step 1: Write failing codec/store tests**

Cover round-trip of every safe value, trace hierarchy, error snapshot, strict UTF-8/length/enum validation, trailing-byte rejection, ciphertext tamper failure, monotonic sequence enforcement, date/size rotation, 3-day/3-file/3-MiB retention, and legacy records returned before expiry without entering the new writer.

Add overload tests:

```kotlin
fun `queue overflow drops debug first and emergency writes error at most once`()
fun `store failure cannot recursively log store failure`()
fun `crash writer does not access keystore or normal writer lock`()
```

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :data:testDebugUnitTest --tests "*LogRecordCodecV1Test" --tests "*EncryptedLogRecordStoreTest"`

Expected: FAIL because v1 store is absent.

- [ ] **Step 3: Implement codec and store by extracting proven crypto mechanics**

Copy behavior, not legacy event semantics, from `EncryptedLogStore`/`EncryptedLogFileManager`. New directory/magic/version are independent. Do not edit or delete legacy files in this task; `LegacyDiagnosticsReader` is read-only and bounded by the existing three-day retention horizon.

- [ ] **Step 4: Run storage tests**

Run: `./gradlew.bat :data:testDebugUnitTest --tests "*LogRecordCodecV1Test" --tests "*EncryptedLogRecordStoreTest"`

Expected: PASS.

- [ ] **Step 5: Commit storage v1**

```powershell
git add -- core/src/main/kotlin/com/aozijx/passly/core/observability/sink/LogRecordStore.kt data/src/main/java/com/aozijx/passly/data/observability data/src/test/java/com/aozijx/passly/data/observability docs/security/diagnostics.md
git commit -m "feat(observability): persist encrypted log records"
```

### Task 6: 运行时装配、崩溃处理与 JSONL 导出

**Files:**
- Create: `app/src/main/java/com/aozijx/passly/app/observability/LoggerRuntime.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/ObservabilityRuntimeController.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/ExportManifestFactory.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/ObservabilityExportService.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/di/ObservabilityModule.kt`
- Modify: `app/src/main/java/com/aozijx/passly/app/PasslyApplication.kt`
- Modify: `app/src/main/java/com/aozijx/passly/app/AppLifecycleObserver.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/settings/diagnostics/DiagnosticsLogStore.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/settings/diagnostics/ExportDiagnosticsUseCase.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/ObservabilityRuntimeControllerTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/ObservabilityExportServiceTest.kt`
- Create: `app/src/test/resources/observability-v1.schema.json`

**Interfaces:**
- Consumes: Logger core, Android sinks, record store, previous exit reader.
- Produces: application-scoped `Logger`, framework-only `LoggerRuntime`, authenticated JSONL export with manifest first.

- [ ] **Step 1: Write failing runtime/export tests**

Assert startup ordering, single crash-handler installation, handler delegation, 300-ms flush/200-ms emergency fallback, reentrant crash suppression, lifecycle flush, and shutdown idempotence.

Export tests must assert:

```kotlin
fun `first JSONL record is schema v1 manifest and every following line validates`()
fun `manifest contains app device and autofill capability context once`()
fun `manifest never exposes Android ID fingerprint serial or package names`()
fun `export requires existing fresh authorization flow and deletes temporary file`()
fun `legacy and v1 records are time ordered without duplicate event ids`()
```

- [ ] **Step 2: Run tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*ObservabilityRuntimeControllerTest" --tests "*ObservabilityExportServiceTest"`

Expected: FAIL because runtime/export v1 is absent.

- [ ] **Step 3: Implement runtime and export**

Device header may contain manufacturer/model/SDK/release/locale/timezone/app version, but not Build fingerprint, Android ID, serial or raw installed-package list. Register previous exit after Logger/store initialization. `LoggerRuntime` must default to a no-op before installation and accept only typed APIs.

- [ ] **Step 4: Run runtime/export tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*ObservabilityRuntimeControllerTest" --tests "*ObservabilityExportServiceTest"`

Expected: PASS and schema validation succeeds.

- [ ] **Step 5: Commit runtime/export**

```powershell
git add -- app/src/main/java/com/aozijx/passly/app/observability app/src/main/java/com/aozijx/passly/app/PasslyApplication.kt app/src/main/java/com/aozijx/passly/app/AppLifecycleObserver.kt app/src/main/java/com/aozijx/passly/feature/settings/diagnostics app/src/test/java/com/aozijx/passly/app/observability app/src/test/resources/observability-v1.schema.json
git commit -m "feat(observability): assemble logger and secure export"
```

### Task 7: 数据库会话与安全认证链路迁移

**Files:**
- Modify: `runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionRuntimeEvent.kt`
- Modify: `runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt`
- Modify: `security/src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt`
- Modify: `security/src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt`
- Test: `runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManagerTest.kt`
- Test: `data/src/test/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionObservabilityTest.kt`
- Test: `security/src/test/java/com/aozijx/passly/security/authentication/VaultSessionObservabilityTest.kt`
- Modify: `docs/security/authentication.md`

**Interfaces:**
- Consumes: injected `Logger`, catalog constants and Trace API.
- Produces: one database/security trace with required state, owner, reason, trigger, timeout and elapsed fields.

- [ ] **Step 1: Extend runtime event tests before changing event shape**

Replace enum-only callback with typed `SessionTransitionEvent(fromState, toState, owner, reason, lockType, elapsedMs, timeoutMs, error)` and assert transitions for open, resume, soft lock, seal timeout and close failure.

- [ ] **Step 2: Run runtime tests and confirm failure**

Run: `./gradlew.bat :runtime:session:test --tests "*RuntimeSessionManagerTest"`

Expected: FAIL until typed transition events are emitted.

- [ ] **Step 3: Implement typed runtime transitions without depending on Logger**

Keep `runtime:session` platform-neutral. `AppDatabaseSession` adapts typed runtime events to Logger; runtime does not import observability packages.

- [ ] **Step 4: Add failing integration tests for database/security logging**

Assert `database.session.transition`, `security.lock.started/completed`, authentication method enum, same trace across start/completion, DEBUG for skipped background lock, and ERROR with safe exception snapshot on failure.

- [ ] **Step 5: Implement adapters and run focused tests**

Run:

```powershell
./gradlew.bat :runtime:session:test --tests "*RuntimeSessionManagerTest"
./gradlew.bat :data:testDebugUnitTest --tests "*AppDatabaseSessionObservabilityTest"
./gradlew.bat :security:testDebugUnitTest --tests "*VaultSessionObservabilityTest"
```

Expected: all PASS.

- [ ] **Step 6: Commit database/security migration**

```powershell
git add -- runtime/session/src/main runtime/session/src/test data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt data/src/test/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionObservabilityTest.kt security/src/main/java/com/aozijx/passly/security/authentication security/src/test/java/com/aozijx/passly/security/authentication/VaultSessionObservabilityTest.kt docs/security/authentication.md
git commit -m "refactor(observability): trace secure session lifecycle"
```

### Task 8: Legacy Autofill 与 Credential Manager 全链路迁移

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/ModernCredentialService.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/credential/CredentialResponseViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillUnlockLease.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/autofill/AutofillObservabilityTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/autofill/AutofillTraceLifecycleTest.kt`
- Modify: `docs/security/autofill.md`

**Interfaces:**
- Consumes: Logger root trace/span/token APIs, typed Autofill events/reasons, HMAC target-package field.
- Produces: independent Legacy/Credential flows with a common observability contract.

- [ ] **Step 1: Write failing event-contract tests**

For each platform source assert discovery start/completed/unavailable, auth start/completed, delivery start/delivered/failed, close start/closed/failed, and save start/staged/completed/failed. `unavailable` must include one reason enum; no record may contain raw package/domain.

- [ ] **Step 2: Write failing lifecycle/ownership tests**

Assert:

```kotlin
fun `delivery and session close remain in one trace but separate spans`()
fun `session closes only after platform result delivery`()
fun `legacy and credential result systems remain independent`()
fun `expired pending intent trace token creates new root and resume unavailable event`()
fun `legacy save exception reaches logger as throwable without exposing message`()
```

- [ ] **Step 3: Run tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*AutofillObservabilityTest" --tests "*AutofillTraceLifecycleTest"`

Expected: FAIL because existing code emits legacy Telemetry events and drops some exceptions/reasons.

- [ ] **Step 4: Migrate Legacy then Credential paths without changing result ownership**

Root traces start in `LegacyAutofillService.onFillRequest/onSaveRequest` and `ModernCredentialService` callbacks. Pass trace token through authentication UI only where coroutine context cannot survive. Keep `AutofillResultDelivery` before `AUTOFILL_REQUEST_FINISHED` lock/close.

- [ ] **Step 5: Run Autofill tests including existing lifecycle regression suites**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "*AutofillObservabilityTest" --tests "*AutofillTraceLifecycleTest" --tests "*AutofillResultDeliveryTest" --tests "*AutofillInteractionLifecycleTest" --tests "*LegacySaveFlowTest"
```

Expected: all PASS.

- [ ] **Step 6: Commit Autofill migration**

```powershell
git add -- app/src/main/java/com/aozijx/passly/feature/autofill app/src/main/java/com/aozijx/passly/presentation/feature/autofill app/src/test/java/com/aozijx/passly/feature/autofill docs/security/autofill.md
git commit -m "refactor(observability): trace autofill workflows"
```

### Task 9: 聚合、指标与本地告警

**Files:**
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/metrics/EventAggregator.kt`
- Create: `core/src/main/kotlin/com/aozijx/passly/core/observability/metrics/MetricSnapshot.kt`
- Create: `app/src/main/java/com/aozijx/passly/app/observability/LocalAlertPolicy.kt`
- Modify: `core/src/main/kotlin/com/aozijx/passly/core/observability/internal/DefaultLogger.kt`
- Test: `core/src/test/kotlin/com/aozijx/passly/core/observability/metrics/EventAggregatorTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/LocalAlertPolicyTest.kt`

**Interfaces:**
- Consumes: sanitized events before Sink fan-out.
- Produces: bounded `MetricSnapshot` records and local alert state without remote transport.

- [ ] **Step 1: Write failing aggregation tests**

Assert identical safe dimensions aggregate, trace/diag IDs are excluded from aggregation keys, ERROR/FATAL and Autofill delivery are never sampled, LRU is bounded, eviction flushes oldest bucket, time window emits count/first/last/min/max/avg, and Logger self-metrics cannot recursively enter the aggregator.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*EventAggregatorTest"`

Expected: FAIL because aggregator is absent.

- [ ] **Step 3: Implement aggregator and deterministic DEBUG sampling**

Use capacity 256 and a five-minute default window. Sampling decision uses stable event key plus process-session seed; never use target identifiers. Flush on app background and runtime shutdown.

- [ ] **Step 4: Add and pass local alert tests**

Test rule: three `DATABASE_SESSION_OPEN_FAILED` events in ten minutes create one alert with latest diag ID; repeated evaluation does not create duplicate alerts; expiry clears it.

Run: `./gradlew.bat :core:testDebugUnitTest --tests "*EventAggregatorTest"; ./gradlew.bat :app:testDebugUnitTest --tests "*LocalAlertPolicyTest"`

Expected: PASS.

- [ ] **Step 5: Commit metrics**

```powershell
git add -- core/src/main/kotlin/com/aozijx/passly/core/observability/metrics core/src/main/kotlin/com/aozijx/passly/core/observability/internal/DefaultLogger.kt core/src/test/kotlin/com/aozijx/passly/core/observability/metrics app/src/main/java/com/aozijx/passly/app/observability/LocalAlertPolicy.kt app/src/test/java/com/aozijx/passly/app/observability/LocalAlertPolicyTest.kt
git commit -m "feat(observability): aggregate local diagnostics"
```

### Task 10: AppError、剩余调用点与用户 Diagnostic ID

**Files:**
- Create: `app/src/main/java/com/aozijx/passly/app/observability/ObservabilityAppErrorReporter.kt`
- Modify: `core/src/main/kotlin/com/aozijx/passly/core/telemetry/reporting/AppErrorReporter.kt` (move to observability package, then delete old file)
- Modify: `core/src/main/kotlin/com/aozijx/passly/core/telemetry/reporting/ErrorReportContext.kt` (move to observability package, then delete old file)
- Modify: `app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotRestorer.kt`
- Modify: `app/src/main/java/com/aozijx/passly/app/DeviceLockController.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/scanner/BarcodeImageDecoder.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerCameraHost.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/main/general/DiagnosticsSettingsViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/VaultViewModel.kt`
- Modify: `core/src/main/kotlin/com/aozijx/passly/core/platform/packageinfo/InstalledAppIconLoader.kt`
- Modify: `core/src/main/kotlin/com/aozijx/passly/core/platform/qr/QrCodeEncoder.kt`
- Modify: `core/src/main/kotlin/com/aozijx/passly/security/dek/DekManager.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/local/database/DatabaseProvider.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/local/database/port/VaultDatabaseResetController.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/repository/attachment/AttachmentResourceGarbageCollector.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryResourceCleaner.kt`
- Modify: `security/src/main/java/com/aozijx/passly/security/authentication/host/ActivityAuthUiHost.kt`
- Modify: `security/src/main/java/com/aozijx/passly/security/keystore/AndroidKeystoreKeyProvider.kt`
- Modify: `app/src/main/java/com/aozijx/passly/app/message/mapping/AppErrorNoticeMapper.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/shared/error/UiErrorMessage.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/observability/ObservabilityAppErrorReporterTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/app/message/mapping/AppErrorNoticeMapperTest.kt`
- Modify: `docs/architecture/error-handling.md`

**Interfaces:**
- Consumes: Logger error/degraded APIs and existing `AppError`.
- Produces: one diagnostic event per boundary failure and UI-safe message plus short diag ID.

- [ ] **Step 1: Write failing reporter/message tests**

Assert `AppError.errorId` maps to `DiagnosticId` without becoming trace ID, report boundaries do not duplicate child failures, known errors map to stable user notices, and unknown errors show a generic notice plus diag ID but no exception message.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*ObservabilityAppErrorReporterTest" --tests "*AppErrorNoticeMapperTest"`

Expected: FAIL until reporter and mapper migrate.

- [ ] **Step 3: Migrate every caller listed in this task and enforce one boundary report**

Use `rg` inventory as the source of truth. Framework callbacks use `LoggerRuntime`; injectable objects receive `Logger`. Convert skip/repeat lifecycle events to DEBUG and attach reason enums to every degraded event.

- [ ] **Step 4: Run tests and prove no production legacy callers remain**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "*ObservabilityAppErrorReporterTest" --tests "*AppErrorNoticeMapperTest"
rg -n "TelemetryRuntime|TelemetryReporter|TelemetryEvent" app/src/main core/src/main data/src/main security/src/main runtime/session/src/main
```

Expected: tests PASS; `rg` returns only migration comments or zero results before Task 11 deletes compatibility files.

- [ ] **Step 5: Commit error migration**

Stage only paths listed by the migration inventory plus the two tests and document; verify `git diff --cached --name-only` before commit.

Commit: `git commit -m "refactor(observability): unify application errors"`

### Task 11: 删除 legacy、静态守卫、ADR 与全量验收

**Files:**
- Delete: `core/src/main/kotlin/com/aozijx/passly/core/telemetry/` (all legacy files after Tasks 1-10 remove callers)
- Delete: `app/src/main/java/com/aozijx/passly/app/diagnostics/DiagnosticsExportService.kt`
- Delete: `app/src/main/java/com/aozijx/passly/app/diagnostics/DiagnosticsRuntimeController.kt`
- Delete: `app/src/main/java/com/aozijx/passly/app/diagnostics/TelemetryAppErrorReporter.kt`
- Delete: `app/src/main/java/com/aozijx/passly/app/diagnostics/di/DiagnosticsModule.kt`
- Delete: `data/src/main/java/com/aozijx/passly/data/diagnostics/EncryptedLogStore.kt`
- Delete: `data/src/main/java/com/aozijx/passly/data/diagnostics/EncryptedLogFileManager.kt`
- Delete: `data/src/main/java/com/aozijx/passly/data/diagnostics/TelemetryRecordCodec.kt`
- Delete: `data/src/main/java/com/aozijx/passly/data/diagnostics/di/TelemetryPersistenceModule.kt`
- Create: `app/src/test/java/com/aozijx/passly/architecture/ObservabilityBoundaryTest.kt`
- Create: `docs/decisions/ADR-0024-structured-observability-logger-v1.md`
- Modify: `docs/architecture/telemetry.md`
- Modify: `docs/security/diagnostics.md`
- Modify: `docs/architecture/error-handling.md`
- Modify: `docs/security/authentication.md`
- Modify: `docs/security/autofill.md`

**Interfaces:**
- Consumes: every v1 component and migrated caller.
- Produces: a single Logger v1 production path with build-time boundary guards and complete current documentation.

- [ ] **Step 1: Write failing architecture guards**

Read production source roots and fail if any file except `LogcatSink.kt` imports `android.util.Log`, or if any production file contains `printStackTrace`, `Log.getStackTraceString`, `TelemetryRuntime`, `TelemetryReporter`, or `TelemetryEvent`. Assert no source references `diagnostics_v1` except `LegacyDiagnosticsReader` and its expiry cleanup.

- [ ] **Step 2: Run guard and confirm it fails before deletion**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*ObservabilityBoundaryTest"`

Expected: FAIL and list remaining legacy files/imports.

- [ ] **Step 3: Delete the exact legacy files identified by the guard**

`LegacyDiagnosticsReader` created in Task 5 owns the minimum read-only `.elog1` parser needed for the three-day window，so the
old data diagnostics classes above must be removed rather than kept as a second subsystem. Do not delete the new read-only reader
until its bounded compatibility window expires in a later dedicated cleanup commit.

- [ ] **Step 4: Update ADR and formal docs to describe implemented code**

The ADR records why Logger v1 is self-owned, why Release Logcat is disabled, why Android system Trace is a non-sensitive performance side-channel, and why remote upload is absent. Replace future-tense migration text in architecture/security docs with actual file paths and verified behavior.

- [ ] **Step 5: Run full verification**

Run:

```powershell
./gradlew.bat :core:testDebugUnitTest :runtime:session:test :data:testDebugUnitTest :security:testDebugUnitTest :app:testDebugUnitTest
./gradlew.bat lintDebug
./gradlew.bat verifyModuleBoundaries
./gradlew.bat :app:assembleDebug
git diff --check
```

Expected: every Gradle command exits 0; architecture guard has zero violations; `git diff --check` emits no errors.

- [ ] **Step 6: Run device acceptance when available**

Run `./gradlew.bat :app:connectedDebugAndroidTest` and manually verify one Legacy Autofill trace, one Credential Manager trace, result-before-close ordering, Logcat Debug output, Perfetto spans, HMAC stability and authenticated export. If no compatible device is connected, report this exact gap.

- [ ] **Step 7: Commit cleanup and implemented documentation**

Stage exact source/test/doc paths after inspecting `git status --short`; do not include `gradle/libs.versions.toml`.

Commit: `git commit -m "refactor(observability): replace legacy telemetry"`

---

## Final Acceptance Record

Before declaring completion, record in the final implementation commit or handoff:

- exact commit hash tested;
- counts for core/runtime/data/security/app unit tests;
- lint and module-boundary results;
- connected-device result or explicit absence of a device;
- one redacted sample JSONL manifest/event/metric validated against schema v1;
- confirmation that `rg` found no legacy production API or direct Android Log usage outside `LogcatSink.kt`;
- `git status --short`, explicitly identifying any preserved user-owned changes.
