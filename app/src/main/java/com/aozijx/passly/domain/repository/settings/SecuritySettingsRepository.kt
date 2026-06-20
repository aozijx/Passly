package com.aozijx.passly.domain.repository.settings

import kotlinx.coroutines.flow.Flow

interface SecuritySettingsRepository {
    val lockTimeout: Flow<Long>
    val isBiometricEnabled: Flow<Boolean>
    val isInvalidateKeyOnBioChange: Flow<Boolean>
    val isSecureContentEnabled: Flow<Boolean>
    val isFlipToLockEnabled: Flow<Boolean>
    val isFlipExitAndClearStackEnabled: Flow<Boolean>
    val isPasswordPreferredAuthFirst: Flow<Boolean>
    val isDeviceCredentialFallbackEnabled: Flow<Boolean>

    suspend fun setLockTimeout(timeoutMs: Long)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setInvalidateKeyOnBioChange(enabled: Boolean)
    suspend fun setSecureContentEnabled(enabled: Boolean)
    suspend fun setFlipToLockEnabled(enabled: Boolean)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean)
    suspend fun setPasswordPreferredAuthFirst(enabled: Boolean)
    suspend fun setDeviceCredentialFallbackEnabled(enabled: Boolean)
}