package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

/** Fixed wire contract and safety limits for the chunked Passly container numbered version 1. */
internal object BackupContainerV1Spec {
    const val MAGIC = "PSLYBKP1"
    const val CONTAINER_VERSION = 1
    const val KDF_ARGON2ID_ID = 1
    const val CIPHER_AES_256_GCM_ID = 1
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    const val SALT_LENGTH = 16
    const val NONCE_PREFIX_LENGTH = 4
    const val CHUNK_SIZE = 1024 * 1024
    const val TAG_LENGTH = 16

    const val DATA_RECORD_TYPE = 1
    const val TERMINAL_RECORD_TYPE = 2
    const val RECORD_HEADER_LENGTH = Byte.SIZE_BYTES + Long.SIZE_BYTES + Int.SIZE_BYTES
    const val TERMINAL_PLAINTEXT_LENGTH = Long.SIZE_BYTES * 2

    const val MIN_KDF_ITERATIONS = 1
    const val MAX_KDF_ITERATIONS = 10
    const val MIN_KDF_MEMORY_KIB = 8 * 1024
    const val MAX_KDF_MEMORY_KIB = 256 * 1024
    const val MIN_KDF_PARALLELISM = 1
    const val MAX_KDF_PARALLELISM = 8

    const val MAGIC_LENGTH = 8
    private const val HEADER_INTEGER_COUNT = 13
    const val HEADER_LENGTH =
        MAGIC_LENGTH +
            HEADER_INTEGER_COUNT * Int.SIZE_BYTES +
            SALT_LENGTH +
            NONCE_PREFIX_LENGTH
    const val FIXED_HEADER_LENGTH = HEADER_LENGTH - SALT_LENGTH - NONCE_PREFIX_LENGTH
}
