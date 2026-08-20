package com.aozijx.passly.core.telemetry

/**
 * 组合遥测报告器。
 *
 * 将事件分发给所有注册的子发射器。任一子发射器失败不影响其他。
 */
class CompositeTelemetryReporter(
    private vararg val reporters: TelemetryReporter
) : TelemetryReporter {

    override fun emit(event: TelemetryEvent) {
        reporters.forEach { reporter ->
            runCatching { reporter.emit(event) }
        }
    }
}
