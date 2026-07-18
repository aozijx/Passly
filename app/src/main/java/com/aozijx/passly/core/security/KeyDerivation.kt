package com.aozijx.passly.core.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

object KeyDerivation {

    const val SALT_LENGTH = 16
    private const val KEY_LENGTH_BITS = 256
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8

    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_MEMORY_KB = 65536
    private const val ARGON2_PARALLELISM = 4

    private val argon2Kt: Argon2Kt by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Argon2Kt()
    }

    fun deriveKeyArgon2id(password: CharArray, salt: ByteArray): SecretKeySpec {
        val rawKey = deriveKeyBytesArgon2id(password, salt)
        return try {
            SecretKeySpec(rawKey, "AES")
        } finally {
            rawKey.fill(0)
        }
    }

    fun deriveKeyBytesArgon2id(password: CharArray, salt: ByteArray): ByteArray {
        val passBytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).array()
        try {
            val rawHash = argon2Kt.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passBytes,
                salt = salt,
                tCostInIterations = ARGON2_ITERATIONS,
                mCostInKibibyte = ARGON2_MEMORY_KB,
                parallelism = ARGON2_PARALLELISM,
                hashLengthInBytes = KEY_LENGTH_BYTES,
                version = Argon2Version.V13
            )
            return rawHash.rawHashAsByteArray()
        } finally {
            passBytes.fill(0)
        }
    }

    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
}
