package com.aozijx.passly.core.telemetry

import java.util.concurrent.atomic.AtomicLong

interface TelemetryFileStore {
    fun write(event: TelemetryEvent)
    fun readEvents(limit: Int): List<TelemetryEvent>
    fun flush(timeoutMs: Long = 300L): Boolean
    fun clear()
    fun close()
    fun crashEmergencyWrite(event: TelemetryEvent, timeoutMs: Long = 200L): Boolean
}

fun interface TelemetryFileStoreFactory {
    fun create(loggingEnabledUntil: AtomicLong): TelemetryFileStore
}
