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

fun TelemetryReporter.report(
    level: EventLevel,
    category: EventCategory,
    name: String,
    throwable: Throwable? = null,
    fields: Map<String, SafeLogValue> = emptyMap()
) {
    emit(
        TelemetryEvent(
            level = level,
            category = category,
            name = name,
            fields = fields,
            throwableType = throwable?.javaClass?.simpleName?.take(64),
            appStackFrames = throwable?.stackTrace
                ?.asSequence()
                ?.filter { it.className.startsWith("com.aozijx.passly.") }
                ?.take(16)
                ?.map { "${it.className}.${it.methodName}" }
                ?.toList()
                .orEmpty()
        )
    )
}
