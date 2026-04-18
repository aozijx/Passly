package com.aozijx.passly.core.security

/**
 * 内存清理工具：批量擦除敏感数据。
 * 在锁定、切后台、超时等场景下调用，确保敏感字段不残留在堆内存中。
 */
object MemoryCleaner {

    fun wipe(targets: List<SecureString?>) {
        for (target in targets) {
            target?.wipe()
        }
    }

    fun wipeByteArray(vararg arrays: ByteArray?) {
        for (array in arrays) {
            array?.fill(0)
        }
    }

    fun wipeCharArray(vararg arrays: CharArray?) {
        for (array in arrays) {
            array?.fill('\u0000')
        }
    }
}
