package com.aozijx.passly.domain.access.model

@JvmInline
value class RecoveryCredentialId(val value: String) {
    init {
        require(value.isNotBlank()) { "Recovery credential ID cannot be blank" }
    }
}

/** Owns temporary recovery-code plaintext until committed or closed. */
interface RecoveryCredentialDraft : AutoCloseable {
    val id: RecoveryCredentialId
    fun reveal(): CharArray?
    suspend fun commit(): AuthenticationResult
    override fun close()
}

sealed interface RecoveryCredentialCreation {
    data class Ready(val draft: RecoveryCredentialDraft) : RecoveryCredentialCreation
    data class Failed(val failure: AuthenticationFailure) : RecoveryCredentialCreation
}

fun interface RecoveryCredentialFactory {
    suspend fun create(): RecoveryCredentialCreation
}
