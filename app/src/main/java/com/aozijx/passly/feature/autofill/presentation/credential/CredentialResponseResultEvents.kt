package com.aozijx.passly.feature.autofill.presentation.credential

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Buffered across route collection gaps and consumed by one Activity result owner. */
internal class CredentialResponseResultEvents {
    private val channel = Channel<CredentialResponseCompletion>(Channel.BUFFERED)

    val completions: Flow<CredentialResponseCompletion> = channel.receiveAsFlow()

    fun publish(completion: CredentialResponseCompletion): Boolean =
        channel.trySend(completion).isSuccess

    fun close() {
        channel.close()
    }
}
