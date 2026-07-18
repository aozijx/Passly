package com.aozijx.passly.domain.usecase.settings

import com.aozijx.passly.domain.repository.settings.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备级设置用例：安全、锁定、备份（不可跨设备同步）。
 */
@Singleton
class DeviceSettingsUseCases @Inject constructor(private val repository: DeviceRepository) {
    // ── 安全 ──
    val lockTimeout: Flow<Long> = repository.lockTimeout
    val isInvalidateKeyOnBioChange: Flow<Boolean> = repository.isInvalidateKeyOnBioChange
    val isSecureContentEnabled: Flow<Boolean> = repository.isSecureContentEnabled
    val isFlipToLockEnabled: Flow<Boolean> = repository.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled: Flow<Boolean> = repository.isFlipExitAndClearStackEnabled
    val isLockOnBackground: Flow<Boolean> = repository.isLockOnBackground

    suspend fun setLockTimeout(timeoutMs: Long) = repository.setLockTimeout(timeoutMs)
    suspend fun setInvalidateKeyOnBioChange(enabled: Boolean) =
        repository.setInvalidateKeyOnBioChange(enabled)
    suspend fun setSecureContentEnabled(enabled: Boolean) =
        repository.setSecureContentEnabled(enabled)
    suspend fun setFlipToLockEnabled(enabled: Boolean) = repository.setFlipToLockEnabled(enabled)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        repository.setFlipExitAndClearStackEnabled(enabled)
    suspend fun setLockOnBackground(enabled: Boolean) =
        repository.setLockOnBackground(enabled)

    // ── 备份 ──
    val backupDirectoryUri: Flow<String?> = repository.backupDirectoryUri
    val lastBackupExportFileName: Flow<String?> = repository.lastBackupExportFileName

    suspend fun setBackupDirectoryUri(uri: String) = repository.setBackupDirectoryUri(uri)
    suspend fun clearBackupDirectoryUri() = repository.clearBackupDirectoryUri()
    suspend fun setLastBackupExportFileName(fileName: String) =
        repository.setLastBackupExportFileName(fileName)

}
