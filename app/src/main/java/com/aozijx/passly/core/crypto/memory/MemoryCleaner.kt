package com.aozijx.passly.core.crypto.memory

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