package com.aozijx.passly.core.telemetry

/**
 * 遥测报告器（纯契约）。
 *
 * 使用方式：通过依赖注入注入此接口，而非调用全局单例。
 *
 * ```kotlin
 * class SomeRepository(
 *     private val telemetry: TelemetryReporter
 * )
 * ```
 */
fun interface TelemetryReporter {
    fun emit(event: TelemetryEvent)
}
