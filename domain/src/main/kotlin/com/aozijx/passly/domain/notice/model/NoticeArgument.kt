package com.aozijx.passly.domain.notice.model

/**
 * 消息参数键。
 * 每种 [NoticeCode] 应声明其使用的参数键集合。
 * 值在 [ArgumentValue] 中按类型存储。
 */
enum class ArgumentKey {
    SECONDS,
    ITEM_COUNT,
    DURATION_MS,
    REASON_CODE,
    OPERATION_CODE
}

sealed interface ArgumentValue {
    data class Count(val value: Long) : ArgumentValue {
        init {
            require(value >= 0) { "Count must not be negative" }
        }
    }

    data class DurationMs(val value: Long) : ArgumentValue {
        init {
            require(value >= 0) { "Duration must not be negative" }
        }
    }

    data class EnumCode(val value: String) : ArgumentValue {
        init {
            require(value.matches(SAFE_CODE)) { "Invalid enum code" }
        }
    }

    data class ReasonCode(val value: String) : ArgumentValue {
        init {
            require(value.matches(SAFE_CODE)) { "Invalid reason code" }
        }
    }

    data class Flag(val value: Boolean) : ArgumentValue

    private companion object {
        val SAFE_CODE = Regex("[a-z][a-z0-9_.-]{0,95}")
    }
}

fun countArgument(key: ArgumentKey, value: Long) = key to ArgumentValue.Count(value)
fun durationArgument(key: ArgumentKey, value: Long) = key to ArgumentValue.DurationMs(value)
fun enumArgument(key: ArgumentKey, value: String) = key to ArgumentValue.EnumCode(value)
fun reasonArgument(key: ArgumentKey, value: String) = key to ArgumentValue.ReasonCode(value)
fun flagArgument(key: ArgumentKey, value: Boolean) = key to ArgumentValue.Flag(value)
