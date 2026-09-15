package com.aozijx.passly.security.authentication

fun interface KdfOperation<T> {
    fun run(secret: CharArray): T
}

interface KdfRunner : AutoCloseable {
    suspend fun <T> execute(secret: SecretChars, operation: KdfOperation<T>): T
}
