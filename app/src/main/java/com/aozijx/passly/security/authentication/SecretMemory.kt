package com.aozijx.passly.security.authentication

import java.util.concurrent.atomic.AtomicBoolean

class SecretChars private constructor(private var value: CharArray?) : AutoCloseable {
    fun copyForWorker(): CharArray = synchronized(this) {
        checkNotNull(value) { "Secret has been destroyed" }.copyOf()
    }

    override fun close() {
        synchronized(this) {
            value?.fill('\u0000')
            value = null
        }
    }

    companion object {
        fun take(chars: CharArray): SecretChars {
            val owned = chars.copyOf()
            chars.fill('\u0000')
            return SecretChars(owned)
        }

        fun copyOf(chars: CharArray): SecretChars = SecretChars(chars.copyOf())
    }
}

interface DiscardableResult {
    fun discard()
}

class OwnedBytes(bytes: ByteArray) : DiscardableResult, AutoCloseable {
    private var value: ByteArray? = bytes
    private val consumed = AtomicBoolean(false)

    fun consume(): ByteArray = synchronized(this) {
        check(consumed.compareAndSet(false, true)) { "Bytes already consumed" }
        checkNotNull(value).also { value = null }
    }

    override fun discard() {
        synchronized(this) {
            value?.fill(0)
            value = null
            consumed.set(true)
        }
    }

    override fun close() = discard()
}
