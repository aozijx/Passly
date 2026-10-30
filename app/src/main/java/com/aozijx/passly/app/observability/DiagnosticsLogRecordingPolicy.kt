package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DiagnosticsLogRecordingPolicy @Inject constructor() {
    private val mode = AtomicReference<DiagnosticsLogRecordingMode?>(null)

    fun update(value: DiagnosticsLogRecordingMode) {
        mode.set(value)
    }

    fun allows(level: LogLevel): Boolean = when (mode.get()) {
        DiagnosticsLogRecordingMode.DISABLED, null -> false
        DiagnosticsLogRecordingMode.WARN_AND_ABOVE -> when (level) {
            LogLevel.DEBUG, LogLevel.INFO -> false
            LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL -> true
        }
        DiagnosticsLogRecordingMode.INFO_AND_ABOVE -> level != LogLevel.DEBUG
        DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE -> true
    }
}

internal class PolicyLogRecordStore(
    private val delegate: LogRecordStore,
    private val policy: DiagnosticsLogRecordingPolicy,
) : LogRecordStore by delegate {
    override fun emit(record: SanitizedLogRecord) = write(record)

    override fun write(record: SanitizedLogRecord) {
        if (policy.allows(record.level)) delegate.write(record)
    }

    override fun crashEmergencyWrite(
        record: SanitizedLogRecord,
        timeoutMs: Long,
    ): Boolean =
        policy.allows(record.level) && delegate.crashEmergencyWrite(record, timeoutMs)
}
