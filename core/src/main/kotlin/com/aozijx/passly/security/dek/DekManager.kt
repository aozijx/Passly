package com.aozijx.passly.security.dek

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.dek.VerificationTag
import com.aozijx.passly.security.envelope.EnvelopeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DekManager @Inject constructor(
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val fieldKeyManager: FieldKeyManager,
    private val logger: Logger,
) {
    private val mutex = Mutex()

    @Volatile
    private var _state: DekState = DekState.Locked

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lockCallback: (suspend () -> Unit)? = null

    fun setLockCallback(callback: suspend () -> Unit) {
        lockCallback = callback
    }

    val state: DekState get() = _state

    suspend fun initializeWithKey(
        type: EnvelopeType,
        wrappingKey: SecretKeySpec,
        salt: ByteArray,
        algorithm: KdfAlgorithm
    ) {
        logger.inSpan(Operations.VaultKeyManagement) {
        mutex.withLock {
            check(vaultBootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

            val dek = EnvelopeManager.generateDek()
            try {
                vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                vaultBootstrapStore.save(
                    EnvelopeManager.wrapWithKey(type, dek, wrappingKey, salt, algorithm)
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
        cipher: Cipher
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

    suspend fun unlock(type: EnvelopeType, cipher: Cipher): DekUnlockResult {
        val keyEnvelope = vaultBootstrapStore.load(type)
            ?: return DekUnlockResult.Failed(DekUnlockError.ENVELOPE_CORRUPTED)
        val tag = vaultBootstrapStore.loadVerificationTag()
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
                logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.AuthenticationFailed, degraded = true))
                DekUnlockResult.Failed(DekUnlockError.AUTH_FAILED)
            } catch (e: IllegalArgumentException) {
                logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.DekVerificationFailed))
                DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.UnlockFailed))
                DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
            }
        }
        }
    }

    suspend fun setDek(type: EnvelopeType, dek: ByteArray): DekUnlockResult {
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
            } catch (e: IllegalArgumentException) {
                logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.DekVerificationFailed))
                DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.DekInstallFailed))
                DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
            }
        }
        }
    }

    suspend fun rekey(type: EnvelopeType, cipher: Cipher) {
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
        algorithm: KdfAlgorithm
    ) {
        logger.inSpan(Operations.VaultKeyManagement) {
        mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK not loaded, current: ${current::class.simpleName}"
            }
            vaultBootstrapStore.save(
                EnvelopeManager.wrapWithKey(type, current.dek, wrappingKey, salt, algorithm)
            )
            logger.log(LogStatements.Security.envelopeRekeyed())
        }
        }
    }

    suspend fun <T> withDek(block: (ByteArray) -> T): T {
        val dek = mutex.withLock {
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

    suspend fun verifyEnvelope(type: EnvelopeType, key: SecretKeySpec): Boolean {
        val keyEnvelope = vaultBootstrapStore.load(type) ?: return false
        return EnvelopeManager.verify(keyEnvelope, key)
    }

    suspend fun lock() {
        logger.inSpan(Operations.VaultKeyManagement) {
        mutex.withLock {
            _state = DekState.Locking
            _isUnlocked.value = false
            logger.log(LogStatements.Security.vaultLockStarted())
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.CallbackFailed))
        }
        mutex.withLock {
            wipeCurrentDek()
            fieldKeyManager.clear()
            _state = DekState.Locked
            logger.log(LogStatements.Security.vaultLocked())
        }
        }
    }

    suspend fun deleteVault() {
        logger.inSpan(Operations.VaultKeyManagement) {
        mutex.withLock {
            _state = DekState.Deleting
            _isUnlocked.value = false
            logger.log(LogStatements.Security.vaultDeletionStarted())
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            logger.log(LogStatements.Security.vaultOperationFailed(e, Reasons.CallbackFailed))
        }
        mutex.withLock {
            wipeCurrentDek()
            fieldKeyManager.clear()
            vaultBootstrapStore.clear()
            _state = DekState.Locked
            logger.log(LogStatements.Security.vaultDeleted())
        }
        }
    }

    private fun cacheAndUnlock(dek: ByteArray) {
        val cloned = dek.clone()
        wipeCurrentDek()
        _state = DekState.Unlocked(cloned)
        _isUnlocked.value = true
        fieldKeyManager.deriveAndSet(dek)
        MemoryCleaner.wipeByteArray(dek)
    }

    private fun wipeCurrentDek() {
        when (val s = _state) {
            is DekState.Unlocked -> MemoryCleaner.wipeByteArray(s.dek)
            else -> {}
        }
    }
}
