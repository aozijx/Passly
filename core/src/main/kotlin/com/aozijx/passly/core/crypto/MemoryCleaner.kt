package com.aozijx.passly.core.crypto

import sun.misc.Unsafe

/**
 * 尽力覆盖调用方仍持有的可变敏感缓冲。
 *
 * 可用时在覆盖后执行 [Unsafe.storeFence]，用于写入排序。
 * 这不保证 JVM、密码学提供者或系统产生的其他副本已被擦除。
 */
object MemoryCleaner {
    private val unsafe =
        runCatching {
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
