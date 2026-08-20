package com.aozijx.passly.security.dek

import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
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
    private val telemetry: TelemetryReporter
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
        mutex.withLock {
            check(vaultBootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

            val dek = EnvelopeManager.generateDek()
            try {
                vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                vaultBootstrapStore.save(
                    EnvelopeManager.wrapWithKey(type, dek, wrappingKey, salt, algorithm)
                )
                report(EventLevel.INFO, "security.vault_initialized")
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
            check(vaultBootstrapStore.loadAll().isEmpty()) { "Vault already initialized" }

            val dek = EnvelopeManager.generateDek()
            try {
                vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                vaultBootstrapStore.save(EnvelopeManager.wrapWithCipher(type, dek, cipher))
                report(EventLevel.INFO, "security.vault_initialized")
                cacheAndUnlock(dek)
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }
    }

    suspend fun unlock(type: EnvelopeType, cipher: Cipher): DekUnlockResult {
        val keyEnvelope = vaultBootstrapStore.load(type)
            ?: return DekUnlockResult.Failed(DekUnlockError.ENVELOPE_CORRUPTED)
        val tag = vaultBootstrapStore.loadVerificationTag()
            ?: return DekUnlockResult.Failed(DekUnlockError.ENVELOPE_CORRUPTED)

        return mutex.withLock {
            try {
                check(_state is DekState.Locked) { "Already unlocked" }
                val dek = EnvelopeManager.unwrap(keyEnvelope, cipher)
                try {
                    VerificationTag.verify(dek, tag, type.value)
                    cacheAndUnlock(dek)
                } finally {
                    MemoryCleaner.wipeByteArray(dek)
                }
                report(EventLevel.INFO, "security.vault_unlocked")
                DekUnlockResult.Success
            } catch (e: javax.crypto.AEADBadTagException) {
                report(EventLevel.WARN, "security.authentication_failed", e)
                DekUnlockResult.Failed(DekUnlockError.AUTH_FAILED)
            } catch (e: IllegalArgumentException) {
                report(EventLevel.ERROR, "security.dek_verification_failed", e)
                DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                report(EventLevel.ERROR, "security.unlock_failed", e)
                DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
            }
        }
    }

    suspend fun setDek(type: EnvelopeType, dek: ByteArray): DekUnlockResult {
        val tag = vaultBootstrapStore.loadVerificationTag()
        return mutex.withLock {
            try {
                check(_state is DekState.Locked) { "Already unlocked" }

                if (tag != null) {
                    VerificationTag.verify(dek, tag, type.value)
                } else {
                    vaultBootstrapStore.saveVerificationTag(VerificationTag.create(dek))
                    report(EventLevel.INFO, "security.verification_tag_created")
                }

                cacheAndUnlock(dek)
                report(EventLevel.INFO, "security.vault_unlocked")
                DekUnlockResult.Success
            } catch (e: IllegalArgumentException) {
                report(EventLevel.ERROR, "security.dek_verification_failed", e)
                DekUnlockResult.Failed(DekUnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                report(EventLevel.ERROR, "security.dek_install_failed", e)
                DekUnlockResult.Failed(DekUnlockError.UNKNOWN)
            }
        }
    }

    suspend fun rekey(type: EnvelopeType, cipher: Cipher) {
        mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK not loaded, current: ${current::class.simpleName}"
            }
            vaultBootstrapStore.save(EnvelopeManager.wrapWithCipher(type, current.dek, cipher))
            report(EventLevel.INFO, "security.envelope_rekeyed")
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
            vaultBootstrapStore.save(
                EnvelopeManager.wrapWithKey(type, current.dek, wrappingKey, salt, algorithm)
            )
            report(EventLevel.INFO, "security.envelope_rekeyed")
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
        mutex.withLock {
            _state = DekState.Locking
            _isUnlocked.value = false
            report(EventLevel.INFO, "security.lock_started")
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            report(EventLevel.ERROR, "security.lock_callback_failed", e)
        }
        mutex.withLock {
            wipeCurrentDek()
            fieldKeyManager.clear()
            _state = DekState.Locked
            report(EventLevel.INFO, "security.lock_completed")
        }
    }

    suspend fun deleteVault() {
        mutex.withLock {
            _state = DekState.Deleting
            _isUnlocked.value = false
            report(EventLevel.INFO, "security.vault_deletion_started")
        }
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            report(EventLevel.ERROR, "security.deletion_callback_failed", e)
        }
        mutex.withLock {
            wipeCurrentDek()
            fieldKeyManager.clear()
            vaultBootstrapStore.clear()
            _state = DekState.Locked
            report(EventLevel.INFO, "security.vault_deleted")
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

    private fun report(level: EventLevel, name: String, throwable: Throwable? = null) {
        telemetry.report(level, EventCategory.SECURITY, name, throwable)
    }
}
