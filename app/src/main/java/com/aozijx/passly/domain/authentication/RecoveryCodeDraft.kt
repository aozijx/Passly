package com.aozijx.passly.domain.authentication

interface RecoveryCodeDraft {
    val generationId: String
    fun reveal(): CharArray?
    suspend fun commit(): AuthenticationResult
    fun clear()
}

sealed interface RecoveryCodeDraftCreation {
    data class Ready(val draft: RecoveryCodeDraft) : RecoveryCodeDraftCreation
    data class Failed(val failure: AuthenticationFailure) : RecoveryCodeDraftCreation
}

fun interface RecoveryCodeDraftFactory {
    suspend fun create(): RecoveryCodeDraftCreation
}
