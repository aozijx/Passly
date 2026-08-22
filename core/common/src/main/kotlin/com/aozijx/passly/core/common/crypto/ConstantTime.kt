package com.aozijx.passly.core.common.crypto

import java.security.MessageDigest

/** Utilities for constant-time byte-array comparisons. */
object ConstantTime {

    /**
     * Compares two byte arrays in constant time.
     */
    fun isEqual(a: ByteArray?, b: ByteArray?): Boolean {
        if (a == null || b == null) {
            return a === b
        }
        return MessageDigest.isEqual(a, b)
    }
}
