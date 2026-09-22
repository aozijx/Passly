package com.aozijx.passly.core.telemetry

data class TelemetryThrowableSnapshot(
    val type: String,
    val appStackFrames: List<String>,
)

fun Throwable.toTelemetrySnapshot(): TelemetryThrowableSnapshot {
    val frames = buildList {
        var current: Throwable? = this@toTelemetrySnapshot
        var causeDepth = 0
        while (current != null && causeDepth < MAX_CAUSE_DEPTH && size < MAX_STACK_FRAMES) {
            if (causeDepth > 0) add("caused_by:${current.safeTypeName()}")
            current.stackTrace
                .asSequence()
                .filter { it.className.startsWith(APP_PACKAGE_PREFIX) }
                .take(MAX_STACK_FRAMES - size)
                .mapTo(this) { frame ->
                    buildString {
                        append(frame.className)
                        append('.')
                        append(frame.methodName)
                        if (frame.fileName != null || frame.lineNumber > 0) {
                            append('(')
                            append(frame.fileName ?: "Unknown Source")
                            if (frame.lineNumber > 0) {
                                append(':')
                                append(frame.lineNumber)
                            }
                            append(')')
                        }
                    }
                }
            current = current.cause?.takeUnless { it === current }
            causeDepth++
        }
    }
    return TelemetryThrowableSnapshot(
        type = safeTypeName(),
        appStackFrames = frames,
    )
}

private fun Throwable.safeTypeName(): String =
    javaClass.simpleName
        .take(64)
        .filter { it.isLetterOrDigit() || it == '_' }
        .ifBlank { "Throwable" }

private const val APP_PACKAGE_PREFIX = "com.aozijx.passly."
private const val MAX_STACK_FRAMES = 24
private const val MAX_CAUSE_DEPTH = 3
