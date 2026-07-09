package com.aozijx.passly.security.crypto

import android.content.Context
import com.aozijx.passly.core.log.Logcat
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.Cipher
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
    class Unlocked(val dek: ByteArray) : DekState
    data object Locking : DekState  // 中间状态：正在锁定，防止新操作进入
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

        private const val PREFS_NAME = AppDefaults.Auth.PREFS_NAME
        private const val KEY_VERIFY_TAG = "dek_verify_tag"
    }

    private val mutex = Mutex()
    private val verificationTag = VerificationTag(context, PREFS_NAME, KEY_VERIFY_TAG)

    @Volatile
    private var _state: DekState = DekState.Locked

    private val _lockState = MutableStateFlow(LockState.LOCKED)

    /** 锁定回调，由 DatabaseSessionManager 注册，用于在锁定前关闭数据库 */
    private var lockCallback: (suspend () -> Unit)? = null

    fun setLockCallback(callback: suspend () -> Unit) {
        lockCallback = callback
    }

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

    /**
     * 首次引导 DEK：创建 VerificationTag 并解锁（不创建信封）。
     *
     * 用于 AppPassword 首次设置的场景，此时尚无任何信封。
     * 验证标签在此创建，确保后续解锁可以通过 [unlockWithVerifiedDek] 校验。
     */
    suspend fun bootstrapDek(dek: ByteArray) {
        mutex.withLock {
            try {
                check(_state is DekState.Locked) { "DEK already loaded" }
                check(envelopeManager.hasAny().not()) { "Vault already initialized" }

                verificationTag.save(dek)
                SessionManager.deriveAndSet(dek)
                _state = DekState.Unlocked(dek.clone())
                onUnlocked()
                Logcat.i(TAG, "DEK bootstrapped with verification tag")
            } finally {
                MemoryCleaner.wipeByteArray(dek)
            }
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
                Logcat.logCryptoException(TAG, "DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                Logcat.logCryptoException(TAG, "Unlock via verified DEK", e)
                UnlockResult.Failed(UnlockError.UNKNOWN)
            } finally {
                MemoryCleaner.wipeByteArray(dek)
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
    //  信封管理委托
    // ─────────────────────────────────────────────────────────

    fun getEnvelope(type: EnvelopeType): Envelope? = envelopeManager.get(type)
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
                Logcat.logCryptoException(TAG, "Biometric unlock", e)
                UnlockResult.Failed(UnlockError.AUTH_FAILED)
            } catch (e: IllegalArgumentException) {
                Logcat.logCryptoException(TAG, "Biometric DEK verification", e)
                UnlockResult.Failed(UnlockError.DEK_VERIFY_FAILED)
            } catch (e: Exception) {
                Logcat.logCryptoException(TAG, "Biometric unlock", e)
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
                Logcat.logCryptoException(TAG, "Biometric bootstrap", e)
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

    /**
     * 锁定 Vault，执行以下步骤：
     * 1. 设置状态为 Locking，发出 LOCKED 信号（防止新操作进入）
     * 2. 在 mutex 外调用回调（关闭数据库），带异常保护
     * 3. 在 mutex 内完成密钥擦除（无论回调成功与否）
     */
    suspend fun lock() {
        // Step 1: 在 mutex 内设置状态为 Locking，发出 LOCKED
        mutex.withLock {
            _state = DekState.Locking
            onLocked()  // 立即发出 LOCKED，让 UI 响应
            Logcat.i(TAG, "Lock initiated, state set to Locking")
        }

        // Step 2: 在 mutex 外调用回调（关闭数据库）
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            Logcat.e(TAG, "Database close failed during lock, continuing with DEK wipe", e)
        }

        // Step 3: 在 mutex 内完成密钥擦除（无论回调成功与否）
        mutex.withLock {
            wipeCurrentDek()
            SessionManager.clearSessionKey()
            _state = DekState.Locked
            Logcat.i(TAG, "Lock completed, DEK cleared from memory")
        }
    }

    /**
     * 删除 Vault（不可逆）。
     * 执行步骤与 lock() 类似，但额外删除所有信封和验证标签。
     */
    suspend fun deleteVault() {
        // Step 1: 设置状态为 Deleting，发出 LOCKED
        mutex.withLock {
            _state = DekState.Deleting
            onLocked()
            Logcat.i(TAG, "Vault deletion initiated")
        }

        // Step 2: 在 mutex 外调用回调（关闭数据库）
        try {
            lockCallback?.invoke()
        } catch (e: Exception) {
            Logcat.e(TAG, "Database close failed during vault deletion, continuing", e)
        }

        // Step 3: 在 mutex 内完成清理
        mutex.withLock {
            wipeCurrentDek()
            SessionManager.clearSessionKey()
            envelopeManager.removeAll()
            verificationTag.delete()
            _state = DekState.Locked
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