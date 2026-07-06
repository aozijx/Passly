package com.aozijx.passly.security.crypto

import sun.misc.Unsafe

object MemoryCleaner {
    private val unsafe = runCatching {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as Unsafe
    }.getOrNull()

    fun wipeByteArray(array: ByteArray?) {
        array?.let {
            it.fill(0)
            unsafe?.storeFence()
        }
    }

    fun wipeCharArray(array: CharArray?) {
        array?.let {
            it.fill('\u0000')
            unsafe?.storeFence()
        }
    }
}