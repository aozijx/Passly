package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DiagnosticsLogRecordingPolicy
    @Inject
    constructor() {
        private val mode = AtomicReference<DiagnosticsLogRecordingMode?>(null)

        fun update(value: DiagnosticsLogRecordingMode?) {
            mode.set(value)
        }

        fun allows(level: LogLevel): Boolean {
            // A sanitized fatal record is retained locally even when routine diagnostics are off.
            if (level == LogLevel.FATAL) return true
            return when (mode.get()) {
                DiagnosticsLogRecordingMode.DISABLED, null -> {
                    false
                }

                DiagnosticsLogRecordingMode.WARN_AND_ABOVE -> {
                    level == LogLevel.WARN || level == LogLevel.ERROR
                }

                DiagnosticsLogRecordingMode.INFO_AND_ABOVE -> {
                    level != LogLevel.DEBUG
                }

                DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE -> {
                    true
                }
            }
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
    ): Boolean = policy.allows(record.level) && delegate.crashEmergencyWrite(record, timeoutMs)
}
