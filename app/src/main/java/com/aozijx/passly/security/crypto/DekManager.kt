package com.aozijx.passly.security.crypto

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.auth.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.auth.model.envelope.KeyEnvelope
import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.envelope.BootstrapStore
import com.aozijx.passly.security.vault.VerificationTag
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
    private val bootstrapStore: BootstrapStore,
    private val sessionKeyManager: SessionKeyManager
) {
    companion object {
        private const val TAG = "DekManager"
    }

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
        mutex.withLock {
            check(bootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

            val dek = EnvelopeCrypto.generateDek()
            try {
                bootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                bootstrapStore.save(
                    EnvelopeCrypto.wrapWithKey(type, dek, wrappingKey, salt, algorithm)
                )
                AppLog.i(TAG, "Vault initialized with envelope: ${type.value}")
                cacheAndUnlock(dek)
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }
    }

    suspend fun initializeWithCipher(
        type: EnvelopeType,
        cipher: Cipher
    ) {
        mutex.withLock {
            check(bootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

            val dek = EnvelopeCrypto.generateDek()
            try {
                bootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                bootstrapStore.save(EnvelopeCrypto.wrapWithCipher(type, dek, cipher))
                AppLog.i(TAG, "Vault initialized with cipher envelope: ${type.value}")
                cacheAndUnlock(dek)
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }
    }

    suspend fun unlock(type: EnvelopeType, cipher: Cipher): UnlockResult {
        val keyEnvelope = bootstrapStore.load(type)
            ?: return UnlockResult.Failed(UnlockError.ENVELOPE_CORRUPTED)
        val tag = bootstrapStore.loadVerificationTag()
            ?: return UnlockResult.Failed(UnlockError.ENVELOPE_CORRUPTED)

        return mutex.withLock {
            try {
                check(_state is DekState.Locked) { "Already unlocked" }
                val dek = EnvelopeCrypto.unwrap(keyEnvelope, cipher)
                try {
                    VerificationTag.verify(dek, tag, type.value)
                    cacheAndUnlock(dek)
                } finally {
                    MemoryCleaner.wipeByteArray(dek)
                }
                AppLog.i(TAG, "Unlocked via ${type.value}")
                UnlockResult.Success
            } catch (e: javax.crypto.AEADBadTagException) {
                AppLog.logCryptoException(TAG, "Cipher unlock", e)
                UnlockResult.Failed(UnlockError.AUTH_FAILED)
            } catch (e: IllegalArgumentException) {
                AppLog.logCryptoException(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                AppLog.logCryptoException(TAG, "Unlock", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    suspend fun setDek(type: EnvelopeType, dek: ByteArray): UnlockResult {
        val tag = bootstrapStore.loadVerificationTag()
        return mutex.withLock {
            try {
                check(_state is DekState.Locked) { "Already unlocked" }

                if (tag != null) {
                    VerificationTag.verify(dek, tag, type.value)
                } else {
                    bootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                    AppLog.i(TAG, "VerificationTag created (bootstrap: ${type.value})")
                }

                cacheAndUnlock(dek)
                AppLog.i(TAG, "Unlocked with DEK via ${type.value}")
                UnlockResult.Success
            } catch (e: IllegalArgumentException) {
                AppLog.logCryptoException(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                AppLog.logCryptoException(TAG, "setDek", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    suspend fun rekey(type: EnvelopeType, cipher: Cipher) {
        mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK not loaded, current: ${current::class.simpleName}"
            }
            bootstrapStore.save(EnvelopeCrypto.wrapWithCipher(type, current.dek, cipher))
            AppLog.i(TAG, "Envelope re-keyed: ${type.value}")
        }
    }

    suspend fun rekeyWithKey(
        type: EnvelopeType,
        wrappingKey: SecretKeySpec,
        salt: ByteArray,
        algorithm: KdfAlgorithm
    ) {
        mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK not loaded, current: ${current::class.simpleName}"
            }
            bootstrapStore.save(
                EnvelopeCrypto.wrapWithKey(type, current.dek, wrappingKey, salt, algorithm)
            )
            AppLog.i(TAG, "Envelope re-keyed with key: ${type.value}")
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

    suspend fun getEnvelope(type: EnvelopeType): KeyEnvelope? = bootstrapStore.load(type)
    suspend fun removeEnvelope(type: EnvelopeType) = bootstrapStore.delete(type)

    suspend fun verifyEnvelope(type: EnvelopeType, key: SecretKeySpec): Boolean {
        val keyEnvelope = bootstrapStore.load(type) ?: return false
        return EnvelopeCrypto.verify(keyEnvelope, key)
    }

    suspend fun lock() {
        mutex.withLock {
            _state = DekState.Locking
            _isUnlocked.value = false
            AppLog.i(TAG, "Lock initiated")
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            AppLog.e(TAG, "Database close failed during lock, continuing", e)
        }
        mutex.withLock {
            wipeCurrentDek()
            sessionKeyManager.clearSessionKey()
            _state = DekState.Locked
            AppLog.i(TAG, "Lock completed")
        }
    }

    suspend fun deleteVault() {
        mutex.withLock {
            _state = DekState.Deleting
            _isUnlocked.value = false
            AppLog.i(TAG, "Vault deletion initiated")
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            AppLog.e(TAG, "Database close failed during deletion, continuing", e)
        }
        mutex.withLock {
            wipeCurrentDek()
            sessionKeyManager.clearSessionKey()
            bootstrapStore.clear()
            _state = DekState.Locked
            AppLog.i(TAG, "Vault deleted")
        }
    }

    private fun cacheAndUnlock(dek: ByteArray) {
        val cloned = dek.clone()
        wipeCurrentDek()
        _state = DekState.Unlocked(cloned)
        _isUnlocked.value = true
        sessionKeyManager.deriveAndSet(dek)
        MemoryCleaner.wipeByteArray(dek)
    }

    private fun wipeCurrentDek() {
        when (val s = _state) {
            is DekState.Unlocked -> MemoryCleaner.wipeByteArray(s.dek)
            else -> {}
        }
    }
}
