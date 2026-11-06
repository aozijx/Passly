package com.aozijx.passly.core.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

private const val BASELINE_ITERATIONS = 3
private const val BASELINE_MEMORY_KIB = 65_536
private const val BASELINE_PARALLELISM = 4
private const val KEY_LENGTH_BITS = 256
private const val MIN_ITERATIONS = 1
private const val MAX_ITERATIONS = 20
private const val MIN_MEMORY_KIB = 8 * 1024
private const val MAX_MEMORY_KIB = 1024 * 1024
private const val MIN_PARALLELISM = 1
private const val MAX_PARALLELISM = 16

object KeyDerivation {
    const val SALT_LENGTH = 16
    const val ARGON2_VERSION_13 = 0x13

    data class Argon2idParameters(
        val version: Int,
        val iterations: Int,
        val memoryKiB: Int,
        val parallelism: Int,
        val keyLengthBits: Int,
    ) {
        init {
            require(version == ARGON2_VERSION_13) { "Unsupported Argon2 version: $version" }
            require((iterations in MIN_ITERATIONS..MAX_ITERATIONS)) { "Invalid Argon2 iteration count" }
            require(memoryKiB in MIN_MEMORY_KIB..MAX_MEMORY_KIB) { "Invalid Argon2 memory cost" }
            require(parallelism in MIN_PARALLELISM..MAX_PARALLELISM) { "Invalid Argon2 parallelism" }
            require(keyLengthBits == KEY_LENGTH_BITS) { "Only 256-bit derived keys are supported" }
        }
    }

    val BASELINE_ARGON2ID_PARAMETERS =
        Argon2idParameters(
            version = ARGON2_VERSION_13,
            iterations = BASELINE_ITERATIONS,
            memoryKiB = BASELINE_MEMORY_KIB,
            parallelism = BASELINE_PARALLELISM,
            keyLengthBits = KEY_LENGTH_BITS,
        )

    private val argon2Kt: Argon2Kt by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Argon2Kt()
    }
    private val secureRandom = SecureRandom()

    fun deriveKeyBytesArgon2id(
        password: CharArray,
        salt: ByteArray,
        parameters: Argon2idParameters,
    ): ByteArray {
        val encodedPassword = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val passBytes = ByteArray(encodedPassword.remaining()).also(encodedPassword::get)
        try {
            val rawHash =
                argon2Kt.hash(
                    mode = Argon2Mode.ARGON2_ID,
                    password = passBytes,
                    salt = salt,
                    tCostInIterations = parameters.iterations,
                    mCostInKibibyte = parameters.memoryKiB,
                    parallelism = parameters.parallelism,
                    hashLengthInBytes = parameters.keyLengthBits / Byte.SIZE_BITS,
                    version = Argon2Version.V13,
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
