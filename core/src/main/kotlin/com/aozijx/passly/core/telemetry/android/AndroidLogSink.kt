package com.aozijx.passly.core.telemetry.android

import android.util.Log
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEventFormatter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryReporter

/**
 * Android Logcat 遥测发射器实现。
 *
 * 将 [TelemetryEvent] 输出到 [Log.v]/[Log.d]/[Log.i]/[Log.w]/[Log.e]。
 */
class AndroidLogSink(
    private val minimumLevel: EventLevel,
    private val enabled: () -> Boolean = { true }
) : TelemetryReporter {

    override fun emit(event: TelemetryEvent) {
        if (!enabled()) return
        if (event.level.ordinal < minimumLevel.ordinal) return

        val tag = "Passly/${event.category.name.take(12)}"
        val message = TelemetryEventFormatter.format(event)

        when (event.level) {
            EventLevel.VERBOSE -> Log.v(tag, message)
            EventLevel.DEBUG -> Log.d(tag, message)
            EventLevel.INFO -> Log.i(tag, message)
            EventLevel.WARN -> Log.w(tag, message)
            EventLevel.ERROR, EventLevel.FATAL -> Log.e(tag, message)
        }
    }

}
