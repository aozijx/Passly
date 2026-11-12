package com.aozijx.passly.security.authentication.method.credential
import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPendingMaintenance
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenance
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenanceResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.authentication.flow.AuthenticationAvailabilityResolver
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.authentication.method.biometric.RotateBiometricKeyUseCase
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.envelope.EnvelopeManager
import com.aozijx.passly.security.session.VaultSessionController
import com.aozijx.passly.security.session.finishRecoveryPasswordProvisioning
import com.github.f4b6a3.uuid.UuidCreator
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Singleton
internal class DefaultAuthenticationMethodProvisioner
    @Inject
    constructor(
        private val kdfRunner: KdfRunner,
        private val kdfConfiguration: DeviceKdfConfiguration,
        private val dekManager: DekManager,
        private val session: VaultSessionController,
        private val authenticationManager: AuthenticationManager,
        private val vaultBootstrapStore: VaultBootstrapStore,
        private val hostRegistry: AuthenticationHostRegistry,
        private val rotateBiometricKey: RotateBiometricKeyUseCase,
        private val runtimeMaintenance: AuthenticationRuntimeMaintenance,
        private val availabilityResolver: AuthenticationAvailabilityResolver,
    ) : AuthenticationMethodProvisioner {
        override suspend fun authorizeAppPasswordManagement(): AuthenticationResult =
            authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.MANAGE_APP_PASSWORD),
            )

        override suspend fun setAppPassword(password: CharArray): AuthenticationResult {
            val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
            val wasRecoveryMode = session.isRecoveryMode()
            if (!AppPasswordPolicy.DEFAULT.acceptsLength(password.size)) {
                password.fill('\u0000')
                return AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION,
                        AuthenticationRequestId(correlationId),
                    ),
                )
            }
            val secret = SecretChars.take(password)
            val salt = KeyDerivation.generateSalt()
            return try {
                val parameters = kdfConfiguration.selected()
                runAppPasswordProvisioning(
                    store = vaultBootstrapStore,
                    salt = salt,
                    requestId = correlationId,
                    persist = {
                        kdfRunner
                            .execute(secret) { chars ->
                                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, salt, parameters.toCore()))
                            }.use { ownedKey ->
                                val rawKey = ownedKey.consume()
                                try {
                                    val key = SecretKeySpec(rawKey, "AES")
                                    if (dekManager.isUnlocked.value) {
                                        dekManager.rekeyWithKey(
                                            EnvelopeType.APP_PASSWORD,
                                            key,
                                            salt,
                                            KdfAlgorithm.ARGON2ID,
                                            parameters,
                                        )
                                    } else {
                                        dekManager.initializeWithKey(
                                            EnvelopeType.APP_PASSWORD,
                                            key,
                                            salt,
                                            KdfAlgorithm.ARGON2ID,
                                            parameters,
                                        )
                                    }
                                } finally {
                                    rawKey.fill(0)
                                }
                            }
                    },
                    reconcileSession = {
                        if (wasRecoveryMode) {
                            finishRecoveryPasswordProvisioning(
                                seal = {
                                    session.lock(
                                        SessionLockRequest(LockReason.RECOVERY_EXIT, LockTrigger.RECOVERY_EXIT),
                                    )
                                },
                                refreshAvailability = authenticationManager::refreshAvailability,
                            )
                        } else {
                            val pendingMaintenance = refreshAvailabilityAfterCommit()
                            session.markAuthenticated(AuthenticationPurpose.UNLOCK_VAULT)
                            pendingMaintenance
                        }
                    },
                )
            } finally {
                secret.close()
                salt.fill(0)
            }
        }

        override suspend fun changeAppPassword(
            currentPassword: CharArray,
            newPassword: CharArray,
        ): AuthenticationResult {
            val authentication =
                try {
                    try {
                        authenticationManager.authenticate(
                            AuthenticationRequest(
                                purpose = AuthenticationPurpose.MANAGE_APP_PASSWORD,
                                allowedMethods = setOf(AuthenticationMethod.APP_PASSWORD),
                            ),
                            AuthInput.AppPassword.from(currentPassword),
                        )
                    } catch (error: Throwable) {
                        newPassword.fill('\u0000')
                        throw error
                    }
                } finally {
                    currentPassword.fill('\u0000')
                }
            if (authentication !is AuthenticationResult.Success) {
                newPassword.fill('\u0000')
                return authentication
            }
            return setAppPassword(newPassword)
        }

        override suspend fun disableAppPassword(): AuthenticationResult {
            val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
            val authentication =
                authenticationManager.authenticate(
                    AuthenticationRequest(
                        purpose = AuthenticationPurpose.MANAGE_APP_PASSWORD,
                        allowedMethods = setOf(AuthenticationMethod.APP_PASSWORD),
                    ),
                )
            if (authentication !is AuthenticationResult.Success) return authentication
            // Rely on the resolver for a consistent primary-factor availability check.
            if (!availabilityResolver.hasAlternativePrimaryFactor(EnvelopeType.APP_PASSWORD)) {
                return AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.LAST_METHOD_REQUIRED,
                        AuthenticationRequestId(correlationId),
                    ),
                )
            }
            vaultBootstrapStore.delete(EnvelopeType.APP_PASSWORD)
            return AuthenticationResult.Success(
                method = AuthenticationMethod.APP_PASSWORD,
                pendingMaintenance = refreshAvailabilityAfterCommit(),
            )
        }

        override suspend fun disableBiometric(): AuthenticationResult {
            val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
            val authentication =
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY),
                )
            if (authentication !is AuthenticationResult.Success) return authentication
            // Rely on the resolver for a consistent primary-factor availability check.
            if (!availabilityResolver.hasAlternativePrimaryFactor(EnvelopeType.BIOMETRIC)) {
                return AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.LAST_METHOD_REQUIRED,
                        AuthenticationRequestId(correlationId),
                    ),
                )
            }
            val activeAlias =
                vaultBootstrapStore.loadBiometricState().binding?.activeAlias
                    ?: return AuthenticationResult.Failure(
                        AuthenticationFailure(AuthenticationFailureCode.KEY_MISSING, AuthenticationRequestId(correlationId)),
                    )
            vaultBootstrapStore.disableBiometric(activeAlias)
            val cleanup = withContext(NonCancellable) { runtimeMaintenance.reconcileStartupState() }
            return AuthenticationResult.Success(
                method = AuthenticationMethod.BIOMETRIC,
                pendingMaintenance =
                    refreshAvailabilityAfterCommit() +
                        setOfNotNull(
                            AuthenticationPendingMaintenance.BIOMETRIC_ROTATION_CLEANUP.takeIf {
                                cleanup == AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED
                            },
                        ),
            )
        }

        override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult {
            val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
            val lease =
                hostRegistry.awaitLease()
                    ?: return AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            AuthenticationRequestId(correlationId),
                        ),
                    )
            return try {
                val host =
                    lease.hostOrNull()
                        ?: return AuthenticationResult.Failure(
                            AuthenticationFailure(
                                AuthenticationFailureCode.HOST_UNAVAILABLE,
                                AuthenticationRequestId(correlationId),
                            ),
                        )
                val result =
                    rotateBiometricKey.rotate(
                        host = host,
                        invalidateOnEnrollment = invalidateOnEnrollment,
                        correlationId = correlationId,
                    )
                if (result is AuthenticationResult.Success) {
                    result.copy(pendingMaintenance = result.pendingMaintenance + refreshAvailabilityAfterCommit())
                } else {
                    result
                }
            } finally {
                lease.release()
            }
        }

        private suspend fun refreshAvailabilityAfterCommit(): Set<AuthenticationPendingMaintenance> =
            try {
                authenticationManager.refreshAvailability()
                emptySet()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setOf(AuthenticationPendingMaintenance.AUTHENTICATION_AVAILABILITY_REFRESH)
            }

        override suspend fun hasRecoveryCode(): Boolean = vaultBootstrapStore.load(EnvelopeType.RECOVERY) != null

        override suspend fun checkRecoveryCode(code: CharArray): Boolean {
            val envelope =
                vaultBootstrapStore.load(EnvelopeType.RECOVERY) ?: run {
                    code.fill('\u0000')
                    return false
                }
            val secret = SecretChars.take(code)
            return try {
                val parameters = kdfConfiguration.selected()
                val authenticatedContext = EnvelopeManager.authenticationAad(envelope, parameters)
                val ownedKey =
                    kdfRunner.execute(secret) { chars ->
                        OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, envelope.salt, parameters.toCore()))
                    }
                val rawKey = ownedKey.consume()
                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(rawKey, "AES"),
                        GCMParameterSpec(128, envelope.iv),
                    )
                    cipher.updateAAD(authenticatedContext)
                    val dek = cipher.doFinal(envelope.ciphertext)
                    dek.fill(0)
                    true
                } catch (_: Exception) {
                    false
                } finally {
                    rawKey.fill(0)
                    ownedKey.discard()
                }
            } finally {
                secret.close()
            }
        }
    }
