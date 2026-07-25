package com.aozijx.passly.security.crypto

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