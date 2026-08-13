package com.aozijx.passly.domain.sensitive

import com.aozijx.passly.domain.sensitive.SensitiveValue

@JvmInline
value class SecureString(private val value: CharArray) : SensitiveValue {

    val length: Int get() = value.size

    override val isEmpty: Boolean get() = value.isEmpty()

    override fun toCharArray(): CharArray = value.copyOf()

    fun toPlainString(): String = String(value)

    override fun wipe() {
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
