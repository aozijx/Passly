package com.aozijx.passly.core.security

/**
 * 可擦除的敏感字符串。
 * 底层使用 CharArray 而非 String，支持主动清零内存。
 *
 * 使用约束：
 * - 仅用于真正敏感的字段（密码、TOTP Secret、种子短语等）
 * - 用完后必须调用 [wipe] 或通过 [MemoryCleaner] 批量清除
 * - [toString] 故意返回掩码，防止日志泄露
 */
@JvmInline
value class SecureString(private val value: CharArray) {

    val length: Int get() = value.size

    val isEmpty: Boolean get() = value.isEmpty()

    fun toCharArray(): CharArray = value.copyOf()

    fun toPlainString(): String = String(value)

    fun wipe() {
        value.fill('\u0000')
    }

    val isWiped: Boolean get() = value.all { it == '\u0000' }

    override fun toString(): String = if (value.isEmpty()) "" else "***"

    companion object {
        val EMPTY = SecureString(CharArray(0))

        fun fromString(s: String): SecureString = SecureString(s.toCharArray())

        fun fromNullableString(s: String?): SecureString? =
            if (s.isNullOrEmpty()) null else fromString(s)
    }
}
