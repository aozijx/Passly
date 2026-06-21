package com.aozijx.passly.domain.usecase.settings.security

import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * 安全级设置用例：负责锁屏超时、生物识别、安全内容等用户敏感设置
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuritySettingsUseCases @Inject constructor(private val repository: SecuritySettingsRepository) {
    val lockTimeout: Flow<Long> = repository.lockTimeout
    val isBiometricEnabled: Flow<Boolean> = repository.isBiometricEnabled
    val isInvalidateKeyOnBioChange: Flow<Boolean> = repository.isInvalidateKeyOnBioChange
    val isSecureContentEnabled: Flow<Boolean> = repository.isSecureContentEnabled
    val isFlipToLockEnabled: Flow<Boolean> = repository.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled: Flow<Boolean> = repository.isFlipExitAndClearStackEnabled
    val isPasswordPreferredAuthFirst: Flow<Boolean> = repository.isPasswordPreferredAuthFirst
    val isDeviceCredentialFallbackEnabled: Flow<Boolean> =
        repository.isDeviceCredentialFallbackEnabled
    val isLockOnBackground: Flow<Boolean> = repository.isLockOnBackground

    suspend fun setLockTimeout(timeoutMs: Long) = repository.setLockTimeout(timeoutMs)
    suspend fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    suspend fun setInvalidateKeyOnBioChange(enabled: Boolean) =
        repository.setInvalidateKeyOnBioChange(enabled)
    suspend fun setSecureContentEnabled(enabled: Boolean) =
        repository.setSecureContentEnabled(enabled)
    suspend fun setFlipToLockEnabled(enabled: Boolean) = repository.setFlipToLockEnabled(enabled)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        repository.setFlipExitAndClearStackEnabled(enabled)
    suspend fun setPasswordPreferredAuthFirst(enabled: Boolean) =
        repository.setPasswordPreferredAuthFirst(enabled)
    suspend fun setDeviceCredentialFallbackEnabled(enabled: Boolean) =
        repository.setDeviceCredentialFallbackEnabled(enabled)
    suspend fun setLockOnBackground(enabled: Boolean) =
        repository.setLockOnBackground(enabled)
}