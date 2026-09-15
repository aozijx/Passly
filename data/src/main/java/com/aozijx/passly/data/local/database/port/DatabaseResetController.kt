package com.aozijx.passly.data.local.database.port

/** Destructively replaces the encrypted Vault database and its managed file resources. */
fun interface DatabaseResetController {
    suspend fun reset(): Throwable?
}