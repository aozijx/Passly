package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.SanitizedStackFrame

private const val PASSLY_PACKAGE_PREFIX = "com.aozijx.passly."
private const val MAX_STACK_FRAMES = 24
private const val MAX_CAUSE_TYPES = 3
private const val UNKNOWN_METHOD = "unknown"
private const val MAX_FRAME_LENGTH = 256
private const val MAX_CLASS_NAME_LENGTH = 160
private const val MAX_METHOD_NAME_LENGTH = 64
private const val MIN_CLASS_NAME_LENGTH = 8
private const val MAX_LINE_NUMBER = 999_999
private const val FRAME_SEPARATOR_LENGTH = 1
private const val SAFE_TOKEN_CHARACTERS = "._$<>-"
private val CLASS_NAME_PATTERN = Regex("[A-Za-z0-9_$.-]+")
private val METHOD_NAME_PATTERN = Regex("[A-Za-z0-9_$<>-]+")
private val SOURCE_FILE_PATTERN = Regex("[A-Za-z][A-Za-z0-9_$-]{0,95}\\.(?:kt|java)")
private val EXCEPTION_TYPE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_$]{0,63}$")

internal class ExceptionSnapshotFactory {
    fun create(
        throwable: Throwable,
        kind: ExceptionKind = ExceptionKind.UNKNOWN,
    ): ExceptionSnapshot {
        var externalFrameCount = 0
        val stackFrames =
            buildList {
                throwable.stackTrace.forEach { frame ->
                    if (!frame.className.startsWith(PASSLY_PACKAGE_PREFIX)) externalFrameCount += 1
                    if (size >= MAX_STACK_FRAMES) {
                        return@forEach
                    }
                    val normalized = normalize(frame)
                    normalized?.let(::add)
                }
            }

        val causeTypes =
            buildList {
                var cause = throwable.cause
                while (cause != null && size < MAX_CAUSE_TYPES) {
                    add(exceptionType(cause))
                    cause = cause.cause
                }
            }

        return ExceptionSnapshot(
            type = exceptionType(throwable),
            stackFrames = stackFrames,
            causeTypes = causeTypes,
            externalFrameCount = externalFrameCount,
            kind = kind,
        )
    }

    private fun normalize(frame: StackTraceElement): SanitizedStackFrame? {
        val rawClassName = frame.className.removePrefix(PASSLY_PACKAGE_PREFIX)
        val className = rawClassName.safeToken(CLASS_NAME_PATTERN, MAX_CLASS_NAME_LENGTH) ?: return null
        val methodName = frame.methodName.safeToken(METHOD_NAME_PATTERN, MAX_METHOD_NAME_LENGTH) ?: UNKNOWN_METHOD
        val sourceFile = frame.fileName?.takeIf { it.matches(SOURCE_FILE_PATTERN) }
        val location =
            sourceFile?.let { file ->
                if (frame.lineNumber in 1..MAX_LINE_NUMBER) "$file:${frame.lineNumber}" else file
            }
        val suffix = location?.let { "($it)" }.orEmpty()
        val fixedLength = FRAME_SEPARATOR_LENGTH + methodName.length + suffix.length
        val availableClassLength = MAX_FRAME_LENGTH - fixedLength
        if (availableClassLength < MIN_CLASS_NAME_LENGTH) return null
        val boundedClassName = className.take(availableClassLength)
        return SanitizedStackFrame("$boundedClassName#$methodName$suffix")
    }

    private fun String.safeToken(
        pattern: Regex,
        maxLength: Int,
    ): String? =
        filter { it.isLetterOrDigit() || it in SAFE_TOKEN_CHARACTERS }
            .take(maxLength)
            .takeIf { it.isNotEmpty() && pattern.matches(it) }

    private fun exceptionType(throwable: Throwable): ExceptionType {
        val simpleName = throwable.javaClass.simpleName
        return if (simpleName.matches(EXCEPTION_TYPE_PATTERN)) {
            ExceptionType(simpleName)
        } else {
            ExceptionType("Throwable")
        }
    }
}
