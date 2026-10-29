package com.aozijx.passly.domain.access.port

/** Serializes a database reset with security-owned lock, unlock, and recovery transitions. */
fun interface DatabaseResetSessionCoordinator {
    suspend fun withExclusiveDatabaseReset(operation: suspend () -> Throwable?): Throwable?
}
