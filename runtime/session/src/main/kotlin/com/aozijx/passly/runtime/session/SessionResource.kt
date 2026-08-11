package com.aozijx.passly.runtime.session

/** Resource controlled by the secure session lifecycle. */
interface SessionResource<Handle : Any> {
    suspend fun open(key: ByteArray): Handle

    suspend fun close(handle: Handle)

    suspend fun <T> transaction(
        handle: Handle,
        block: suspend Handle.() -> T,
    ): T
}

/** Supplies an owned key copy. The runtime always wipes the returned array after opening. */
fun interface SessionKeySource {
    suspend fun copyKey(): ByteArray
}
