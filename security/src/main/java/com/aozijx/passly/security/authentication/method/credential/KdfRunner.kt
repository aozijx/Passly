package com.aozijx.passly.security.authentication.method.credential
internal fun interface KdfOperation<T> {
    fun run(secret: CharArray): T
}

internal interface KdfRunner : AutoCloseable {
    suspend fun <T> execute(
        secret: SecretChars,
        operation: KdfOperation<T>,
    ): T
}
