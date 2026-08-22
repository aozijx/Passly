package com.aozijx.passly.core.telemetry

/**
 * 日志字段安全类型。
 *
 * 只允许这些类型携带值，禁止任意字符串（密码、Token、邮箱都可能短于 64 字符）。
 * 需要字符串的字段应使用值类强类型化。
 */
sealed interface SafeLogValue {
    data class Count(val value: Long) : SafeLogValue
    data class DurationMs(val value: Long) : SafeLogValue
    data class Ratio(val value: Double) : SafeLogValue
    data class BooleanValue(val value: Boolean) : SafeLogValue

    /** 枚举常量名 — 由框架保证是已定义的枚举值 */
    data class EnumName(val name: String) : SafeLogValue {
        init {
            require(name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' })
        }
    }

    /** 预注册的错误码 — 由 [ErrorCode] 值类保证 */
    data class ErrorCodeValue(val code: ErrorCode) : SafeLogValue

    /** 操作码 — 由 [OperationCode] 值类保证 */
    data class OperationCodeValue(val code: OperationCode) : SafeLogValue
}

/** 预注册错误码 */
@JvmInline
value class ErrorCode(val value: String) {
    init {
        require(value.matches(Regex("^[A-Z_]{3,64}$"))) { "ErrorCode must be UPPER_SNAKE 3-64 chars" }
    }
}

/** 操作码 */
@JvmInline
value class OperationCode(val value: String) {
    init {
        require(value.matches(Regex("^[a-z_]{3,128}$"))) { "OperationCode must be lower_snake 3-128 chars" }
    }
}

/** 日志字段构建器 */
class LogFieldsBuilder {
    private val map = mutableMapOf<String, SafeLogValue>()

    fun count(key: String, value: Long) = apply { map[key] = SafeLogValue.Count(value) }
    fun durationMs(key: String, value: Long) = apply { map[key] = SafeLogValue.DurationMs(value) }
    fun ratio(key: String, value: Double) = apply { map[key] = SafeLogValue.Ratio(value) }
    fun boolean(key: String, value: Boolean) = apply { map[key] = SafeLogValue.BooleanValue(value) }
    fun enumVal(key: String, value: Enum<*>) =
        apply { map[key] = SafeLogValue.EnumName(value.name) }

    fun errorCode(key: String, value: ErrorCode) =
        apply { map[key] = SafeLogValue.ErrorCodeValue(value) }

    fun operationCode(key: String, value: OperationCode) =
        apply { map[key] = SafeLogValue.OperationCodeValue(value) }

    fun build(): Map<String, SafeLogValue> = map.toMap()
}

fun logFields(block: LogFieldsBuilder.() -> Unit): Map<String, SafeLogValue> =
    LogFieldsBuilder().apply(block).build()
