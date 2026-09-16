package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.envelope.EnvelopeManager
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultRecoveryCodeDraftFactory @Inject internal constructor(
    private val authorizationGate: AuthorizationGate,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val generator: RecoveryCredentialGenerator,
) : RecoveryCredentialFactory {
    override suspend fun create(): RecoveryCredentialCreation {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) return restrictedSessionFailure()

        return when (val authorization = authorizationGate.authorize(
            AuthorizationScope.Global(AuthenticationPurpose.MANAGE_RECOVERY_CODE),
        ) {
            val authorizedSession = secureSessionAccessState.authenticationState.value
                as? AuthenticationState.Authenticated
                ?: return@authorize restrictedSessionFailure()
            when (val creation = generator.generate()) {
                is RecoveryCredentialCreation.Ready -> {
                    val currentSession = secureSessionAccessState.authenticationState.value
                    if (currentSession != authorizedSession) {
                        creation.draft.close()
                        restrictedSessionFailure()
                    } else {
                        RecoveryCredentialCreation.Ready(
                            SessionBoundRecoveryCredentialDraft(
                                delegate = creation.draft,
                                secureSessionAccessState = secureSessionAccessState,
                                authorizedSession = authorizedSession,
                            )
                        )
                    }
                }
                is RecoveryCredentialCreation.Failed -> creation
                RecoveryCredentialCreation.Cancelled -> RecoveryCredentialCreation.Cancelled
            }
        }) {
            is AuthorizationResult.Allowed -> authorization.value
            is AuthorizationResult.Denied -> RecoveryCredentialCreation.Failed(authorization.failure)
            AuthorizationResult.Cancelled -> RecoveryCredentialCreation.Cancelled
        }
    }
}

internal fun interface RecoveryCredentialGenerator {
    suspend fun generate(): RecoveryCredentialCreation
}

@Singleton
internal class DefaultRecoveryCredentialGenerator @Inject constructor(
    private val kdfRunner: KdfRunner,
    private val dekManager: DekManager,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val authenticationManager: AuthenticationManager,
) : RecoveryCredentialGenerator {
    private val random = SecureRandom()

    override suspend fun generate(): RecoveryCredentialCreation {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        val code = CharArray(CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        val salt = KeyDerivation.generateSalt()
        val secret = SecretChars.copyOf(code)
        var envelope: KeyEnvelope? = null
        var ownershipTransferred = false
        return try {
            val ownedKey = kdfRunner.execute(secret) { chars ->
                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, salt))
            }
            val rawKey = ownedKey.consume()
            try {
                envelope = dekManager.withDek { dek ->
                    EnvelopeManager.wrapWithKey(
                        type = EnvelopeType.RECOVERY,
                        dek = dek,
                        wrappingKey = SecretKeySpec(rawKey, "AES"),
                        salt = salt,
                        algorithm = KdfAlgorithm.ARGON2ID,
                    )
                }
            } finally {
                rawKey.fill(0)
                ownedKey.discard()
            }
            val draft = SecureRecoveryCodeDraft(
                generationId = correlationId,
                sourceCode = code,
                sourceEnvelope = requireNotNull(envelope),
                vaultBootstrapStore = vaultBootstrapStore,
                authenticationManager = authenticationManager,
            )
            ownershipTransferred = true
            envelope = null
            RecoveryCredentialCreation.Ready(draft)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            RecoveryCredentialCreation.Failed(transitionFailure(correlationId))
        } finally {
            code.fill('\u0000')
            envelope?.let(KeyEnvelope::destroy)
            if (!ownershipTransferred) salt.fill(0)
            secret.close()
        }
    }

    private companion object {
        const val CODE_LENGTH = 20
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

internal class SessionBoundRecoveryCredentialDraft(
    private val delegate: RecoveryCredentialDraft,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizedSession: AuthenticationState.Authenticated,
) : RecoveryCredentialDraft {
    override val id: RecoveryCredentialId = delegate.id

    override fun reveal(): CharArray? =
        if (hasOriginalSession()) delegate.reveal() else expireAndReturnNull()

    override suspend fun commit(): AuthenticationResult {
        if (!hasOriginalSession()) {
            close()
            return AuthenticationResult.Failure(
                AuthenticationFailure(AuthenticationFailureCode.SESSION_MODE_RESTRICTED, AuthenticationRequestId(id.value))
            )
        }
        return delegate.commit()
    }

    override fun close() = delegate.close()

    private fun hasOriginalSession(): Boolean =
        secureSessionAccessState.authenticationState.value == authorizedSession

    private fun expireAndReturnNull(): CharArray? {
        close()
        return null
    }
}

private class SecureRecoveryCodeDraft(
    generationId: String,
    sourceCode: CharArray,
    sourceEnvelope: KeyEnvelope,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val authenticationManager: AuthenticationManager,
) : RecoveryCredentialDraft {
    override val id = RecoveryCredentialId(generationId)
    private val mutex = Mutex()
    private var code: CharArray? = sourceCode.copyOf()
    private var envelope: KeyEnvelope? = sourceEnvelope

    @Synchronized
    override fun reveal(): CharArray? = code?.copyOf()

    override suspend fun commit(): AuthenticationResult = mutex.withLock {
        val current = envelope ?: return@withLock AuthenticationResult.Failure(
            AuthenticationFailure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, AuthenticationRequestId(id.value))
        )
        val copy = current.copy(
            ciphertext = current.ciphertext.copyOf(),
            iv = current.iv.copyOf(),
            salt = current.salt.copyOf(),
        )
        try {
            vaultBootstrapStore.save(copy)
            close()
            try {
                authenticationManager.refreshAvailability()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // The envelope is already committed; availability is refreshed before the next authentication request.
            }
            AuthenticationResult.Success(AuthenticationMethod.RECOVERY_CODE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            AuthenticationResult.Failure(transitionFailure(id.value))
        } finally {
            KeyEnvelope.destroy(copy)
        }
    }

    @Synchronized
    override fun close() {
        code?.fill('\u0000')
        code = null
        envelope?.let(KeyEnvelope::destroy)
        envelope = null
    }
}

private fun restrictedSessionFailure(): RecoveryCredentialCreation.Failed =
    RecoveryCredentialCreation.Failed(
        AuthenticationFailure(AuthenticationFailureCode.SESSION_MODE_RESTRICTED)
    )

private fun transitionFailure(requestId: String): AuthenticationFailure =
    AuthenticationFailure(
        AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
        AuthenticationRequestId(requestId),
    )
