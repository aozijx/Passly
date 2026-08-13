package com.aozijx.passly.core.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

object KeyDerivation {

    const val SALT_LENGTH = 16
    const val ARGON2_VERSION_13 = 0x13

    data class Argon2idParameters(
        val version: Int = ARGON2_VERSION_13,
        val iterations: Int = 3,
        val memoryKiB: Int = 65_536,
        val parallelism: Int = 4,
        val keyLengthBits: Int = 256
    ) {
        init {
            require(version == ARGON2_VERSION_13) { "Unsupported Argon2 version: $version" }
            require(iterations in 1..20) { "Invalid Argon2 iteration count" }
            require(memoryKiB in 8 * 1024..1024 * 1024) { "Invalid Argon2 memory cost" }
            require(parallelism in 1..16) { "Invalid Argon2 parallelism" }
            require(keyLengthBits == 256) { "Only AES-256 backup keys are supported" }
        }
    }

    val DEFAULT_ARGON2ID_PARAMETERS = Argon2idParameters()

    private val argon2Kt: Argon2Kt by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Argon2Kt()
    }
    private val secureRandom = SecureRandom()

    fun deriveKeyBytesArgon2id(
        password: CharArray,
        salt: ByteArray,
        parameters: Argon2idParameters = DEFAULT_ARGON2ID_PARAMETERS
    ): ByteArray {
        val encodedPassword = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val passBytes = ByteArray(encodedPassword.remaining()).also(encodedPassword::get)
        try {
            val rawHash = argon2Kt.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passBytes,
                salt = salt,
                tCostInIterations = parameters.iterations,
                mCostInKibibyte = parameters.memoryKiB,
                parallelism = parameters.parallelism,
                hashLengthInBytes = parameters.keyLengthBits / Byte.SIZE_BITS,
                version = Argon2Version.V13
            )
            return rawHash.rawHashAsByteArray()
        } finally {
            passBytes.fill(0)
            if (encodedPassword.hasArray()) {
                encodedPassword.array().fill(0)
            } else {
                encodedPassword.clear()
                while (encodedPassword.hasRemaining()) encodedPassword.put(0)
            }
        }
    }

    fun generateSalt(): ByteArray = ByteArray(SALT_LENGTH).also(secureRandom::nextBytes)
}
