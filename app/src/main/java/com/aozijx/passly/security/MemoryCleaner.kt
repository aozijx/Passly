package com.aozijx.passly.security

import sun.misc.Unsafe

/**
 * 敏感数据安全清理工具。
 *
 * 使用 [Unsafe.storeFence] 确保写入操作在 wipe 返回前对所有线程可见，
 * 防止编译器优化掉零填充操作。
 */
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