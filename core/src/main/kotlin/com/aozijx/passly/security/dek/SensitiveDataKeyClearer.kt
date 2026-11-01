package com.aozijx.passly.security.dek

/** Revokes the in-memory sensitive data key and waits for any current key use to finish. */
fun interface SensitiveDataKeyClearer {
    suspend fun clear()
}
