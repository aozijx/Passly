package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.AppStackFrame
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.ExceptionKind

class ExceptionSnapshotFactory {
    fun create(throwable: Throwable, kind: ExceptionKind = ExceptionKind.UNKNOWN): ExceptionSnapshot {
        var foreignFrameCount = 0
        val appFrames = buildList {
            throwable.stackTrace.forEach { frame ->
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
            kind = kind,
        )
    }

    private fun normalize(frame: StackTraceElement): AppStackFrame? {
        if (frame.lineNumber <= 0) return null
        val sourceFile = frame.fileName ?: return null
        if (!sourceFile.matches(SOURCE_FILE_PATTERN)) return null
        if (!frame.methodName.matches(METHOD_NAME_PATTERN)) return null
        val displayClassName = when {
            frame.className.startsWith(PASSLY_PACKAGE_PREFIX) ->
                frame.className.removePrefix(PASSLY_PACKAGE_PREFIX)
                    .takeIf { it.matches(CLASS_NAME_PATTERN) }

            frame.className.matches(MINIFIED_CLASS_NAME_PATTERN) ->
                sourceFile.substringBeforeLast('.')

            else -> null
        } ?: return null
        return AppStackFrame(
            "$displayClassName#${frame.methodName}($sourceFile:${frame.lineNumber})",
        )
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
        val MINIFIED_CLASS_NAME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_$]{0,31}$")
        val METHOD_NAME_PATTERN = Regex("^[A-Za-z0-9_$<>-]{1,64}$")
        val SOURCE_FILE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_$-]{0,95}\\.(?:kt|java)$")
        val EXCEPTION_TYPE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_$]{0,63}$")
    }
}
