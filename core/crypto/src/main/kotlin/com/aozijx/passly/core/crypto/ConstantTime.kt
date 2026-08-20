package com.aozijx.passly.core.crypto

import java.security.MessageDigest

/**
 * Utilities for constant-time comparisons to prevent timing attacks.
 */
object ConstantTime {

    /**
     * Compares two byte arrays in constant time.
     */
    fun isEqual(a: ByteArray?, b: ByteArray?): Boolean {
        if (a == null || b == null) {
            return a == b
        }
        return MessageDigest.isEqual(a, b)
    }

    /**
     * Compares two strings in constant time by comparing their UTF-8 encoded hashes.
     * Note: Comparing raw strings in constant time is difficult due to varying lengths.
     * Usually, we compare hashes of the strings.
     */
    fun isEqual(a: String?, b: String?): Boolean {
        if (a == null || b == null) {
            return a == b
        }
        // Use a dummy hash to ensure comparison happens even if values are known early
        val digest = MessageDigest.getInstance("SHA-256")
        val hashA = digest.digest(a.toByteArray(Charsets.UTF_8))
        val hashB = digest.digest(b.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(hashA, hashB)
    }
}
