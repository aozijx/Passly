package com.aozijx.passly.security.authentication.method.credential
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPendingMaintenance
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Keeps the result truthful when persistence succeeds but session publication fails. */
internal suspend fun runAppPasswordProvisioning(
    store: VaultBootstrapStore,
    salt: ByteArray,
    requestId: String,
    persist: suspend () -> Unit,
    reconcileSession: suspend () -> Set<AuthenticationPendingMaintenance>,
): AuthenticationResult =
    try {
        persist()
        AuthenticationResult.Success(
            method = AuthenticationMethod.APP_PASSWORD,
            pendingMaintenance = reconcileSession(),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        if (hasCommittedAppPasswordEnvelope(store, salt)) {
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        } else {
            AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    AuthenticationRequestId(requestId),
                ),
            )
        }
    }

private suspend fun hasCommittedAppPasswordEnvelope(
    store: VaultBootstrapStore,
    salt: ByteArray,
): Boolean =
    withContext(NonCancellable) {
        val persisted = store.load(EnvelopeType.APP_PASSWORD) ?: return@withContext false
        try {
            persisted.salt.contentEquals(salt)
        } finally {
            KeyEnvelope.destroy(persisted)
        }
    }
