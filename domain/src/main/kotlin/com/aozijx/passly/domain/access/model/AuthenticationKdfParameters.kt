package com.aozijx.passly.domain.access.model

private const val ARGON2_VERSION_13 = 0x13
private const val STANDARD_AUTH_MEMORY_KIB = 65_536
private const val HIGH_AUTH_MEMORY_KIB = 131_072
private const val MIN_AUTH_ITERATIONS = 3
private const val MAX_AUTH_ITERATIONS = 4
private const val AUTH_PARALLELISM = 4
private const val AUTH_KEY_LENGTH_BITS = 256

/** Public, persisted Argon2id parameters for authentication envelopes. */
data class AuthenticationKdfParameters(
    val version: Int,
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
    val keyLengthBits: Int,
) {
    init {
        require(version == ARGON2_VERSION_13) { "Unsupported authentication KDF version" }
        require(memoryKiB == STANDARD_AUTH_MEMORY_KIB || memoryKiB == HIGH_AUTH_MEMORY_KIB) {
            "Invalid authentication KDF memory cost"
        }
        require(iterations in MIN_AUTH_ITERATIONS..MAX_AUTH_ITERATIONS) { "Invalid authentication KDF iteration count" }
        require(memoryKiB != HIGH_AUTH_MEMORY_KIB || iterations == MIN_AUTH_ITERATIONS) {
            "Unsupported authentication KDF work factor combination"
        }
        require(parallelism == AUTH_PARALLELISM) { "Invalid authentication KDF parallelism" }
        require(keyLengthBits == AUTH_KEY_LENGTH_BITS) { "Invalid authentication KDF output length" }
    }
}
