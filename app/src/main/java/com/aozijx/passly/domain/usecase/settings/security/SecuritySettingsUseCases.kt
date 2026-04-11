package com.aozijx.passly.domain.usecase.settings.security

import com.aozijx.passly.domain.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * 安全级设置用例：负责锁屏超时、生物识别、安全内容等用户敏感设置
 */
class SecuritySettingsUseCases(private val repository: SettingsRepository) {
    val lockTimeout: Flow<Long> = repository.lockTimeout
    val isBiometricEnabled: Flow<Boolean> = repository.isBiometricEnabled
    val isSecureContentEnabled: Flow<Boolean> = repository.isSecureContentEnabled
    val isFlipToLockEnabled: Flow<Boolean> = repository.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled: Flow<Boolean> = repository.isFlipExitAndClearStackEnabled

    suspend fun setLockTimeout(timeoutMs: Long) = repository.setLockTimeout(timeoutMs)
    suspend fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    suspend fun setSecureContentEnabled(enabled: Boolean) =
        repository.setSecureContentEnabled(enabled)
    suspend fun setFlipToLockEnabled(enabled: Boolean) = repository.setFlipToLockEnabled(enabled)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        repository.setFlipExitAndClearStackEnabled(enabled)
}