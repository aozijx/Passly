package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.AppStackFrame
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType

class ExceptionSnapshotFactory {
    fun create(throwable: Throwable): ExceptionSnapshot {
        var foreignFrameCount = 0
        val appFrames = buildList {
            throwable.stackTrace.forEach { frame ->
                if (!frame.className.startsWith(PASSLY_PACKAGE_PREFIX)) {
                    foreignFrameCount += 1
                    return@forEach
                }
                if (size >= MAX_APP_FRAMES) return@forEach

                normalize(frame)?.let(::add) ?: run { foreignFrameCount += 1 }
            }
        }

        val causeTypes = buildList {
            var cause = throwable.cause
            while (cause != null && size < MAX_CAUSE_TYPES) {
                add(exceptionType(cause))
                cause = cause.cause
            }
        }

        return ExceptionSnapshot(
            type = exceptionType(throwable),
            appStackFrames = appFrames,
            causeTypes = causeTypes,
            foreignFrameCount = foreignFrameCount,
        )
    }

    private fun normalize(frame: StackTraceElement): AppStackFrame? {
        if (frame.lineNumber <= 0) return null
        val relativeClassName = frame.className.removePrefix(PASSLY_PACKAGE_PREFIX)
        if (!relativeClassName.matches(CLASS_NAME_PATTERN)) return null
        if (!frame.methodName.matches(METHOD_NAME_PATTERN)) return null
        return AppStackFrame("$relativeClassName#${frame.methodName}:${frame.lineNumber}")
    }

    private fun exceptionType(throwable: Throwable): ExceptionType {
        val simpleName = throwable.javaClass.simpleName
        return if (simpleName.matches(EXCEPTION_TYPE_PATTERN)) {
            ExceptionType(simpleName)
        } else {
            ExceptionType("Throwable")
        }
    }

    private companion object {
        const val PASSLY_PACKAGE_PREFIX = "com.aozijx.passly."
        const val MAX_APP_FRAMES = 24
        const val MAX_CAUSE_TYPES = 3
        val CLASS_NAME_PATTERN = Regex("^[A-Za-z0-9_$]+(?:\\.[A-Za-z0-9_$]+)*$")
        val METHOD_NAME_PATTERN = Regex("^[A-Za-z0-9_$<>-]{1,64}$")
        val EXCEPTION_TYPE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_$]{0,63}$")
    }
}
