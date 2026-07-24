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
    OPERATION_TYPE
}

sealed interface ArgumentValue {
    data class Number(val value: Long) : ArgumentValue
    data class Text(val value: String) : ArgumentValue
    data class Flag(val value: Boolean) : ArgumentValue
}

/**
 * 构造参数的工厂扩展。
 */
fun argument(key: ArgumentKey, value: Long) = key to ArgumentValue.Number(value)
fun argument(key: ArgumentKey, value: String) = key to ArgumentValue.Text(value)
fun argument(key: ArgumentKey, value: Boolean) = key to ArgumentValue.Flag(value)
