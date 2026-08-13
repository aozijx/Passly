package com.aozijx.passly.domain.access.model

import com.aozijx.passly.domain.access.model.OwnedChars.Companion.take


/**
 * 拥有所有权的字符数组。
 *
 * 使用 [take] 获得所有权（源数组将被擦除），
 * 使用 [consume] 转移所有权（当前实例将置空），
 * 使用 [close] 或 [clear] 擦除内容。
 */
class OwnedChars private constructor(chars: CharArray) : AutoCloseable {
    private var _value: CharArray? = chars

    /** 消费：转移所有权给调用者 */
    fun consume(): CharArray = synchronized(this) {
        (requireNotNull(_value) { "OwnedChars already consumed" }).also { _value = null }
    }

    /** 擦除内容 */
    fun clear() {
        synchronized(this) {
            _value?.fill('\u0000')
            _value = null
        }
    }

    override fun close() = clear()

    companion object {
        /** 从外部字符数组获取所有权（源数组被擦除） */
        fun take(source: CharArray): OwnedChars {
            val owned = source.copyOf()
            source.fill('\u0000')
            return OwnedChars(owned)
        }

        /** 从外部字符数组复制 */
        fun copyOf(source: CharArray): OwnedChars = OwnedChars(source.copyOf())
    }
}
