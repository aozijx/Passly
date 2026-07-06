package com.aozijx.passly.security.crypto

import android.content.Context
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.security.envelope.Envelope
import com.aozijx.passly.security.envelope.EnvelopeManager
import com.aozijx.passly.security.envelope.EnvelopeType
import com.aozijx.passly.security.envelope.KdfParams
import com.aozijx.passly.security.vault.VerificationTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 解锁结果。
 */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data class Failed(val reason: UnlockError) : UnlockResult
}

/**
 * 解锁失败原因。
 */
enum class UnlockError {
    AUTH_FAILED,
    DEK_VERIFY_FAILED,
    ENVELOPE_CORRUPTED,
    KDF_ERROR,
    DATABASE_CORRUPTED,
    UNKNOWN
}

/**
 * DEK 管理器状态机。
 */
sealed interface DekState {
    data object Locked : DekState
    data class Unlocked(val dek: ByteArray) : DekState
    data object Deleting : DekState
}

/**
 * DEK（Data Encryption Key）管理器 —— 多信封架构核心。
 *
 * 使用 [Mutex] 协程锁，支持 Compose / Flow / Coroutines 高并发。
 * 状态管理通过 [DekState] 状态机，避免裸 null 判空。
 *
 * ## 委托关系
 * - [EnvelopeManager] — 信封创建与查询
 * - [VerificationTag] — DEK 校验
 * - [SessionManager] — 会话密钥管理
 */
@Singleton
class DekManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val envelopeManager: EnvelopeManager
) {
    companion object {
        private const val TAG = "DekManager"
        private const val DEK_LENGTH = 32
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val ALGORITHM = "AES/GCM/NoPadding"

        private const val PREFS_NAME = AppDefaults.Auth.PREFS_NAME
        private const val KEY_VERIFY_TAG = "dek_verify_tag"
    }

    private val mutex = Mutex()
    private val verificationTag = VerificationTag(context, PREFS_NAME, KEY_VERIFY_TAG)

    @Volatile
    private var _state: DekState = DekState.Locked

    private val _lockState = MutableStateFlow(LockState.LOCKED)

    /** 公开的锁状态流，供 [VaultLockManager] 观察 */
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    val state: DekState get() = _state
    val isUnlocked: Boolean get() = _state is DekState.Unlocked

    private fun onUnlocked() {
        _lockState.value = LockState.UNLOCKED
    }

    private fun onLocked() {
        _lockState.value = LockState.LOCKED
    }

    // ─────────────────────────────────────────────────────────
    //  Vault 初始化（Cipher 版本，用于 Biometric/DeviceCredential）
    // ─────────────────────────────────────────────────────────

    /**
     * 用 AndroidKeystore Cipher 的加密结果完成 Vault 初始化。
     *
     * @param primaryType 信封类型（BIOMETRIC / DEVICE_CREDENTIAL）
     * @param dek 已生成的 DEK（由调用方负责加密前持有，本方法结束后清理）
     * @param iv Cipher 使用的 IV
     * @param dekCiphertext Cipher 对 DEK 加密后的密文+标签
     */
    suspend fun initializeVaultWithCipher(
        primaryType: EnvelopeType,
        dek: ByteArray,
        iv: ByteArray,
        dekCiphertext: ByteArray
    ) {
        mutex.withLock {
            check(envelopeManager.hasAny().not()) { "Vault 已初始化" }

            try {
                verificationTag.save(dek)
                envelopeManager.createFromCipher(primaryType, iv, dekCiphertext)
                val clonedDek = dek.clone()
                wipeCurrentDek()
                _state = DekState.Unlocked(clonedDek)
                onUnlocked()
                SessionManager.deriveAndSet(dek)

                Logcat.i(TAG, "Vault initialized via cipher: $primaryType")
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }
    }

    /**
     * 用 AndroidKeystore Cipher 解密信封并解锁 DEK。
     */
    suspend fun unlockWithCipher(
        envelope: Envelope,
        dek: ByteArray
    ): UnlockResult {
        return mutex.withLock {
            try {
                verificationTag.verify(dek, envelope.id)
                SessionManager.deriveAndSet(dek)
                val clonedDek = dek.clone()
                wipeCurrentDek()
                _state = DekState.Unlocked(clonedDek)
                onUnlocked()
                Logcat.i(TAG, "Unlocked via cipher: ${envelope.id}")
                UnlockResult.Success
            } catch (e: IllegalArgumentException) {
                Logcat.cryptoError(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                Logcat.cryptoError(TAG, "Unlock via cipher", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    suspend fun initializeVault(
        primaryType: EnvelopeType,
        wrappingKey: SecretKeySpec,
        kdfParams: KdfParams? = null
    ) {
        mutex.withLock {
            if (envelopeManager.hasAny()) {
                throw IllegalStateException(
                    "Vault 已初始化。如需重置请调用 deleteVault()"
                )
            }

            val dek = ByteArray(DEK_LENGTH).also { SecureRandom().nextBytes(it) }

            try {
                verificationTag.save(dek)
                val envelope = envelopeManager.create(primaryType, wrappingKey, kdfParams, dek)
                Logcat.i(TAG, "Vault initialized with primary envelope: ${envelope.id}")

                val clonedDek = dek.clone()
                wipeCurrentDek()
                _state = DekState.Unlocked(clonedDek)
                onUnlocked()
                SessionManager.deriveAndSet(dek)

                Logcat.i(TAG, "Vault initialization complete (${dek.size * 8} bits)")
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  解锁
    // ─────────────────────────────────────────────────────────

    suspend fun unlock(
        envelope: Envelope,
        wrappingKey: SecretKeySpec
    ): UnlockResult {
        return mutex.withLock {
            try {
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(
                    Cipher.DECRYPT_MODE, wrappingKey,
                    GCMParameterSpec(GCM_TAG_BITS, envelope.iv)
                )
                val dek = cipher.doFinal(envelope.dekCiphertext)

                try {
                    verificationTag.verify(dek, envelope.id)
                    SessionManager.deriveAndSet(dek)

                    val clonedDek = dek.clone()
                    wipeCurrentDek()
                    _state = DekState.Unlocked(clonedDek)
                    onUnlocked()

                    Logcat.i(TAG, "Unlocked via envelope: ${envelope.id}")
                    UnlockResult.Success
                } finally {
                    MemoryCleaner.wipeByteArray(dek)
                }
            } catch (e: javax.crypto.AEADBadTagException) {
                Logcat.cryptoError(TAG, "DEK unlock", e)
                UnlockResult.Failed(UnlockError.AUTH_FAILED)
            } catch (e: java.security.InvalidKeyException) {
                Logcat.cryptoError(TAG, "DEK unlock", e)
                UnlockResult.Failed(UnlockError.KDF_ERROR)
            } catch (e: IllegalArgumentException) {
                Logcat.cryptoError(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: java.io.IOException) {
                Logcat.cryptoError(TAG, "DEK unlock", e)
                UnlockResult.Failed(mapDataError(e))
            } catch (e: Exception) {
                Logcat.cryptoError(TAG, "DEK unlock", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    private fun mapDataError(e: Exception): UnlockError = when {
        e.message?.contains("Base64", ignoreCase = true) == true -> UnlockError.ENVELOPE_CORRUPTED
        e.message?.contains("corrupt", ignoreCase = true) == true -> UnlockError.DATABASE_CORRUPTED
        else -> UnlockError.ENVELOPE_CORRUPTED
    }

    suspend fun setDekForMigration(dek: ByteArray) {
        mutex.withLock {
            check(_state is DekState.Locked) { "DEK already loaded" }
            SessionManager.deriveAndSet(dek)
            _state = DekState.Unlocked(dek.clone())
            onUnlocked()
            Logcat.i(TAG, "DEK set for migration")
        }
    }

    /**
     * 首次引导 DEK：创建 VerificationTag 并解锁（不创建信封）。
     *
     * 用于 AppPassword 首次设置的场景，此时尚无任何信封。
     * 验证标签在此创建，确保后续解锁可以通过 [unlockWithVerifiedDek] 校验。
     */
    suspend fun bootstrapDek(dek: ByteArray) {
        mutex.withLock {
            check(_state is DekState.Locked) { "DEK already loaded" }
            check(envelopeManager.hasAny().not()) { "Vault already initialized" }

            verificationTag.save(dek)
            SessionManager.deriveAndSet(dek)
            _state = DekState.Unlocked(dek.clone())
            onUnlocked()
            Logcat.i(TAG, "DEK bootstrapped with verification tag")
        }
    }

    /**
     * 用已验证的 DEK 解锁（DEK 由调用方从外部解密源获取）。
     *
     * VerificationTag 必须已存在（通过 [bootstrapDek] 或 [initializeVault] 创建）。
     */
    suspend fun unlockWithVerifiedDek(dek: ByteArray, envelopeId: String): UnlockResult {
        return mutex.withLock {
            try {
                check(_state is DekState.Locked) { "Already unlocked" }
                verificationTag.verify(dek, envelopeId)
                SessionManager.deriveAndSet(dek)
                wipeCurrentDek()
                _state = DekState.Unlocked(dek.clone())
                onUnlocked()
                Logcat.i(TAG, "Unlocked with verified DEK via: $envelopeId")
                UnlockResult.Success
            } catch (e: IllegalArgumentException) {
                Logcat.cryptoError(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                Logcat.cryptoError(TAG, "Unlock via verified DEK", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  作用域 DEK 访问
    // ─────────────────────────────────────────────────────────

    suspend fun <T> withDek(block: (ByteArray) -> T): T {
        val dek = mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK 未加载，当前状态: ${current::class.simpleName}"
            }
            current.dek.clone()
        }
        try {
            return block(dek)
        } finally {
            MemoryCleaner.wipeByteArray(dek)
        }
    }

    // ─────────────────────────────────────────────────────────
    //  创建信封
    // ─────────────────────────────────────────────────────────

    suspend fun createEnvelope(
        type: EnvelopeType,
        wrappingKey: SecretKeySpec,
        kdfParams: KdfParams? = null
    ): Envelope {
        return mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK 未加载，当前状态: ${current::class.simpleName}"
            }
            envelopeManager.create(type, wrappingKey, kdfParams, current.dek)
        }
    }

    // ─────────────────────────────────────────────────────────
    //  信封管理委托
    // ─────────────────────────────────────────────────────────

    fun getEnvelope(type: EnvelopeType): Envelope? = envelopeManager.get(type)
    fun getAllEnvelopeIds(): Set<String> = envelopeManager.getAllIds()
    fun hasAnyEnvelope(): Boolean = envelopeManager.hasAny()
    fun removeEnvelope(type: EnvelopeType) = envelopeManager.remove(type)

    /**
     * 用 AndroidKeystore Cipher 解锁 BIOMETRIC / DEVICE_CREDENTIAL 信封。
     *
     * 本方法执行 [Cipher.doFinal] → DEK 提取 → VerificationTag 校验 → 缓存。
     * [BiometricKeyProvider] 只知道 Keystore 密钥，不知道 DEK。
     */
    suspend fun unlockBiometric(cipher: Cipher): UnlockResult {
        val envelope = getEnvelope(EnvelopeType.BIOMETRIC)
            ?: return UnlockResult.Failed(UnlockError.ENVELOPE_CORRUPTED)

        return mutex.withLock {
            try {
                val dek = cipher.doFinal(envelope.dekCiphertext)
                try {
                    verificationTag.verify(dek, envelope.id)
                    SessionManager.deriveAndSet(dek)
                    val clonedDek = dek.clone()
                    wipeCurrentDek()
                    _state = DekState.Unlocked(clonedDek)
                    onUnlocked()
                    Logcat.i(TAG, "Unlocked via biometric cipher")
                    UnlockResult.Success
                } finally {
                    MemoryCleaner.wipeByteArray(dek)
                }
            } catch (e: javax.crypto.AEADBadTagException) {
                Logcat.cryptoError(TAG, "Biometric unlock", e)
                UnlockResult.Failed(UnlockError.AUTH_FAILED)
            } catch (e: IllegalArgumentException) {
                Logcat.cryptoError(TAG, "Biometric DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                Logcat.cryptoError(TAG, "Biometric unlock", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    /**
     * 首次引导：生成 DEK 并用 AndroidKeystore Cipher 加密创建 Biometric 信封。
     *
     * DEK 生成、加密、VerificationTag 创建、信封持久化全部在本方法内完成。
     * [BiometricKeyProvider] 只传递 Cipher，不知道 DEK。
     */
    suspend fun bootstrapBiometric(cipher: Cipher): UnlockResult {
        return mutex.withLock {
            try {
                check(envelopeManager.hasAny().not()) { "Vault 已初始化" }

                val dek = ByteArray(DEK_LENGTH).also { SecureRandom().nextBytes(it) }
                try {
                    val dekCiphertext = cipher.doFinal(dek)
                    verificationTag.save(dek)
                    envelopeManager.createFromCipher(
                        EnvelopeType.BIOMETRIC, cipher.iv, dekCiphertext
                    )
                    val clonedDek = dek.clone()
                    wipeCurrentDek()
                    _state = DekState.Unlocked(clonedDek)
                    onUnlocked()
                    SessionManager.deriveAndSet(dek)
                    Logcat.i(TAG, "Vault bootstrapped with biometric envelope")
                    UnlockResult.Success
                } finally {
                    MemoryCleaner.wipeByteArray(dek)
                }
            } catch (e: Exception) {
                Logcat.cryptoError(TAG, "Biometric bootstrap", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            }
        }
    }

    /**
     * 用 AndroidKeystore Cipher 完成 Biometric 信封 Rekey。
     *
     * 本方法使用当前已解锁的 DEK 重新加密 Biometric 信封。
     */
    suspend fun rekeyBiometric(cipher: Cipher) {
        mutex.withLock {
            val current = _state
            check(current is DekState.Unlocked) {
                "DEK 未加载，当前状态: ${current::class.simpleName}"
            }
            val dekCiphertext = cipher.doFinal(current.dek)
            envelopeManager.createFromCipher(
                EnvelopeType.BIOMETRIC, cipher.iv, dekCiphertext
            )
            Logcat.i(TAG, "Biometric envelope re-keyed")
        }
    }

    // ─────────────────────────────────────────────────────────
    //  生命周期
    // ─────────────────────────────────────────────────────────

    fun lock() = runBlocking {
        mutex.withLock {
            wipeCurrentDek()
            _state = DekState.Locked
            onLocked()
            SessionManager.clearSessionKey()
            Logcat.i(TAG, "Locked, DEK cleared from memory")
        }
    }

    fun deleteVault() = runBlocking {
        mutex.withLock {
            _state = DekState.Deleting
            wipeCurrentDek()
            SessionManager.clearSessionKey()
            envelopeManager.removeAll()
            verificationTag.delete()
            _state = DekState.Locked
            onLocked()
            Logcat.i(TAG, "Vault deleted")
        }
    }

    // ─────────────────────────────────────────────────────────
    //  内部工具
    // ─────────────────────────────────────────────────────────

    private fun wipeCurrentDek() {
        when (val s = _state) {
            is DekState.Unlocked -> MemoryCleaner.wipeByteArray(s.dek)
            else -> {}
        }
    }
}