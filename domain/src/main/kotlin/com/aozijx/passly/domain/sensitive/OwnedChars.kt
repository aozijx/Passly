package com.aozijx.passly.domain.sensitive

/**
 * 拥有所有权的字符数组 —— 领域内唯一的可擦除敏感值容器。
 *
 * - 使用 [take] 获得所有权（源数组将被擦除）；
 * - 使用 [consume] 转移所有权（当前实例将置空）；
 * - 使用 [close] 或 [clear] 擦除内容；
 * - [fromString]/[copyOf] 构造副本，[toPlainString]/[toCharArray] 读取。
 */
class OwnedChars private constructor(
    chars: CharArray,
) : AutoCloseable,
    SensitiveValue {
    private var value: CharArray? = chars

    val length: Int get() = value?.size ?: 0

    override val isEmpty: Boolean get() = value?.isEmpty() ?: true

    val isWiped: Boolean get() = value?.all { it == '\u0000' } ?: true

    /** 消费：转移所有权给调用者，当前实例置空。 */
    fun consume(): CharArray =
        synchronized(this) {
            (requireNotNull(value) { "OwnedChars already consumed" }).also { value = null }
        }

    /** 擦除内容。 */
    fun clear() {
        synchronized(this) {
            value?.fill('\u0000')
            value = null
        }
    }

    override fun close() = clear()

    override fun wipe() = clear()

    override fun toCharArray(): CharArray =
        synchronized(this) {
            (value ?: return CharArray(0)).copyOf()
        }

    fun toPlainString(): String = String(value ?: CharArray(0))

    override fun toString(): String = if (isEmpty) "" else "***"

    companion object {
        val EMPTY = OwnedChars(CharArray(0))

        /** 获得所有权：源数组将被擦除。 */
        fun take(source: CharArray): OwnedChars {
            val owned = source.copyOf()
            source.fill('\u0000')
            return OwnedChars(owned)
        }

        fun copyOf(source: CharArray): OwnedChars = OwnedChars(source.copyOf())

        fun fromString(s: String): OwnedChars = OwnedChars(s.toCharArray())

        fun fromNullableString(s: String?): OwnedChars? = if (s.isNullOrEmpty()) null else fromString(s)
    }
}
