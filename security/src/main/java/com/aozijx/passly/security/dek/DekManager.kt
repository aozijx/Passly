package com.aozijx.passly.security.dek

import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.dek.VerificationTag
import com.aozijx.passly.security.envelope.EnvelopeManager
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class DekManager
    @Inject
    constructor(
        private val vaultBootstrapStore: VaultBootstrapStore,
        private val fieldKeyLifecycle: FieldKeyLifecycle,
        private val logger: Logger,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var _state: DekState = DekState.Locked

        private val _isUnlocked = MutableStateFlow(false)
        val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

        val state: DekState get() = _state

        suspend fun initializeWithKey(
            type: EnvelopeType,
            wrappingKey: SecretKeySpec,
            salt: ByteArray,
            algorithm: KdfAlgorithm,
            parameters: AuthenticationKdfParameters,
        ) {
            logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    check(vaultBootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

                    val dek = EnvelopeManager.generateDek()
                    try {
                        vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                        vaultBootstrapStore.save(
                            EnvelopeManager.wrapWithKey(type, dek, wrappingKey, salt, algorithm, parameters),
                        )
                        logger.log(LogStatements.Security.vaultInitialized())
                        cacheAndUnlock(dek)
                    } finally {
                        MemoryCleaner.wipeByteArray(dek)
                    }
                }
            }
        }

        suspend fun initializeWithCipher(
            type: EnvelopeType,
            cipher: Cipher,
        ) {
            logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    check(vaultBootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

                    val dek = EnvelopeManager.generateDek()
                    try {
                        vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                        vaultBootstrapStore.save(EnvelopeManager.wrapWithCipher(type, dek, cipher))
                        logger.log(LogStatements.Security.vaultInitialized())
                        cacheAndUnlock(dek)
                    } finally {
                        MemoryCleaner.wipeByteArray(dek)
                    }
                }
            }
        }

        suspend fun unlock(
            type: EnvelopeType,
            cipher: Cipher,
        ): DekUnlockResult {
            val keyEnvelope =
                vaultBootstrapStore.load(type)
                    ?: return DekUnlockResult.Failed(DekUnlockError.ENVELOPE_CORRUPTED)
            val tag =
                vaultBootstrapStore.loadVerificationTag()
                    ?: return DekUnlockResult.Failed(DekUnlockError.ENVELOPE_CORRUPTED)

            return logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    try {
                        check(_state is DekState.Locked) { "Already unlocked" }
                        val dek = EnvelopeManager.unwrap(keyEnvelope, cipher)
                        try {
                            VerificationTag.verify(dek, tag, type.value)
                            cacheAndUnlock(dek)
                        } finally {
                            MemoryCleaner.wipeByteArray(dek)
                        }
                        logger.log(LogStatements.Security.vaultUnlocked())
                        DekUnlockResult.Success
                    } catch (e: javax.crypto.AEADBadTagException) {
                        logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.Security.Authentication.AuthenticationFailed, degraded = true))
                        DekUnlockResult.Failed(DekUnlockError.AUTH_FAILED)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: IllegalArgumentException) {
                        logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.Security.KeyManagement.DekVerificationFailed))
                        DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
                    } catch (e: Exception) {
                        logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.Security.Vault.UnlockFailed))
                        DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
                    }
                }
            }
        }

        suspend fun setDek(
            type: EnvelopeType,
            dek: ByteArray,
        ): DekUnlockResult {
            val tag = vaultBootstrapStore.loadVerificationTag()
            return logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    try {
                        check(_state is DekState.Locked) { "Already unlocked" }

                        if (tag != null) {
                            VerificationTag.verify(dek, tag, type.value)
                        } else {
                            vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                            logger.log(LogStatements.Security.verificationTagCreated())
                        }

                        cacheAndUnlock(dek)
                        logger.log(LogStatements.Security.vaultUnlocked())
                        DekUnlockResult.Success
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: IllegalArgumentException) {
                        logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.Security.KeyManagement.DekVerificationFailed))
                        DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
                    } catch (e: Exception) {
                        logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.Security.KeyManagement.DekInstallFailed))
                        DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
                    }
                }
            }
        }

        suspend fun rekey(
            type: EnvelopeType,
            cipher: Cipher,
        ) {
            logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    val current = _state
                    check(current is DekState.Unlocked) {
                        "DEK not loaded, current: ${current::class.simpleName}"
                    }
                    vaultBootstrapStore.save(EnvelopeManager.wrapWithCipher(type, current.dek, cipher))
                    logger.log(LogStatements.Security.envelopeRekeyed())
                }
            }
        }

        suspend fun rekeyWithKey(
            type: EnvelopeType,
            wrappingKey: SecretKeySpec,
            salt: ByteArray,
            algorithm: KdfAlgorithm,
            parameters: AuthenticationKdfParameters,
        ) {
            logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    val current = _state
                    check(current is DekState.Unlocked) {
                        "DEK not loaded, current: ${current::class.simpleName}"
                    }
                    vaultBootstrapStore.save(
                        EnvelopeManager.wrapWithKey(type, current.dek, wrappingKey, salt, algorithm, parameters),
                    )
                    logger.log(LogStatements.Security.envelopeRekeyed())
                }
            }
        }

        suspend fun <T> withDek(block: (ByteArray) -> T): T {
            val dek =
                mutex.withLock {
                    val current = _state
                    check(current is DekState.Unlocked) {
                        "DEK not loaded, current: ${current::class.simpleName}"
                    }
                    current.dek.clone()
                }
            try {
                return block(dek)
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }

        suspend fun getEnvelope(type: EnvelopeType): KeyEnvelope? = vaultBootstrapStore.load(type)

        suspend fun removeEnvelope(type: EnvelopeType) = vaultBootstrapStore.delete(type)

        suspend fun lock() {
            logger.inSpan(Operations.VaultKeyManagement) {
                mutex.withLock {
                    withContext(NonCancellable) {
                        val previousState = _state
                        _state = DekState.Locking
                        _isUnlocked.value = false

                        var failure: Throwable? = null

                        fun attempt(action: () -> Unit) {
                            try {
                                action()
                            } catch (error: Throwable) {
                                failure = mergeFailure(failure, error)
                            }
                        }

                        attempt { logger.log(LogStatements.Security.vaultLockStarted()) }
                        if (previousState is DekState.Unlocked) {
                            attempt { MemoryCleaner.wipeByteArray(previousState.dek) }
                        }
                        attempt { fieldKeyLifecycle.clear() }

                        _state = DekState.Locked
                        _isUnlocked.value = false
                        attempt { logger.log(LogStatements.Security.vaultLocked()) }
                        failure?.let { throw it }
                    }
                }
            }
        }

        private fun cacheAndUnlock(dek: ByteArray) {
            val cloned = dek.clone()
            val previousState = _state
            _state = DekState.Locking
            _isUnlocked.value = false
            try {
                if (previousState is DekState.Unlocked) {
                    MemoryCleaner.wipeByteArray(previousState.dek)
                }
                fieldKeyLifecycle.clear()
                fieldKeyLifecycle.deriveAndSet(dek)
                _state = DekState.Unlocked(cloned)
                _isUnlocked.value = true
            } catch (error: Throwable) {
                MemoryCleaner.wipeByteArray(cloned)
                var failure: Throwable = error
                try {
                    fieldKeyLifecycle.clear()
                } catch (cleanupError: Throwable) {
                    failure = mergeFailure(failure, cleanupError)
                }
                _state = DekState.Locked
                _isUnlocked.value = false
                throw failure
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }

        private fun mergeFailure(
            primary: Throwable?,
            next: Throwable,
        ): Throwable =
            when {
                primary == null -> next
                primary === next -> primary
                next is Error && primary !is Error -> next.apply { addSuppressed(primary) }
                else -> primary.apply { addSuppressed(next) }
            }
    }
