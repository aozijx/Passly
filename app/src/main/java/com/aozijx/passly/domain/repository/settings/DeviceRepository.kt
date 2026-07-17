package com.aozijx.passly.domain.repository.settings

import kotlinx.coroutines.flow.Flow

/**
 * 设备级设置（不可跨设备同步）：安全、锁定、备份。
 */
data class DeviceSettings(
    val lockTimeout: Long,
    val isBiometricEnabled: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val isLockOnBackground: Boolean,
    val backupDirectoryUri: String? = null,
    val lastBackupExportFileName: String? = null
)

interface DeviceRepository : IdleTimeoutSettings {
    fun getSettingsFlow(): Flow<DeviceSettings>

    // 安全
    override val lockTimeout: Flow<Long>
    val isBiometricEnabled: Flow<Boolean>
    val isInvalidateKeyOnBioChange: Flow<Boolean>
    val isSecureContentEnabled: Flow<Boolean>
    val isFlipToLockEnabled: Flow<Boolean>
    val isFlipExitAndClearStackEnabled: Flow<Boolean>
    override val isLockOnBackground: Flow<Boolean>

    suspend fun setLockTimeout(timeoutMs: Long)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setInvalidateKeyOnBioChange(enabled: Boolean)
    suspend fun setSecureContentEnabled(enabled: Boolean)
    suspend fun setFlipToLockEnabled(enabled: Boolean)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean)
    suspend fun setLockOnBackground(enabled: Boolean)

    // 备份
    val backupDirectoryUri: Flow<String?>
    val lastBackupExportFileName: Flow<String?>

    suspend fun setBackupDirectoryUri(uri: String)
    suspend fun clearBackupDirectoryUri()
    suspend fun setLastBackupExportFileName(fileName: String)
}
