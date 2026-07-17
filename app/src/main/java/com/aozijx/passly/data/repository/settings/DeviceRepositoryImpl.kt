package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.repository.settings.DeviceRepository
import com.aozijx.passly.domain.repository.settings.DeviceSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) :
    DeviceRepository {
    private val dataStore = context.applicationContext.appSettingsDataStore

    // ── 安全 ──

    override val lockTimeout: Flow<Long> =
        dataStore.data.map { it.lockTimeoutMs }
    override val isBiometricEnabled: Flow<Boolean> =
        dataStore.data.map { it.biometricEnabled }
    override val isInvalidateKeyOnBioChange: Flow<Boolean> =
        dataStore.data.map { it.invalidateKeyOnBioChange }
    override val isSecureContentEnabled: Flow<Boolean> =
        dataStore.data.map { it.secureContent }
    override val isFlipToLockEnabled: Flow<Boolean> =
        dataStore.data.map { it.flipToLock }
    override val isFlipExitAndClearStackEnabled: Flow<Boolean> =
        dataStore.data.map { it.flipExitAndClearStack }
    override val isLockOnBackground: Flow<Boolean> =
        dataStore.data.map { it.lockOnBackground }

    // ── 备份 ──

    override val backupDirectoryUri: Flow<String?> =
        dataStore.data.map { s ->
            val uri = s.backupDirectoryUri
            uri.ifEmpty { null }
        }
    override val lastBackupExportFileName: Flow<String?> =
        dataStore.data.map { s ->
            val name = s.lastBackupExportFileName
            name.ifEmpty { null }
        }

    // ── 组合设置 ──

    override fun getSettingsFlow(): Flow<DeviceSettings> = combine(
        combine(
            lockTimeout, isBiometricEnabled, isInvalidateKeyOnBioChange,
            isSecureContentEnabled, isFlipToLockEnabled
        ) { timeout, bio, invalidate, secure, flip ->
            Group1(timeout, bio, invalidate, secure, flip)
        },
        isFlipExitAndClearStackEnabled, isLockOnBackground,
        backupDirectoryUri, lastBackupExportFileName
    ) { g1, flipExit, lockBg, backupUri, lastExport ->
        DeviceSettings(
            lockTimeout = g1.timeout,
            isBiometricEnabled = g1.bio,
            isInvalidateKeyOnBioChange = g1.invalidate,
            isSecureContentEnabled = g1.secure,
            isFlipToLockEnabled = g1.flip,
            isFlipExitAndClearStackEnabled = flipExit,
            isLockOnBackground = lockBg,
            backupDirectoryUri = backupUri,
            lastBackupExportFileName = lastExport
        )
    }

    private data class Group1(
        val timeout: Long,
        val bio: Boolean,
        val invalidate: Boolean,
        val secure: Boolean,
        val flip: Boolean
    )

    // ── 安全操作 ──

    override suspend fun setLockTimeout(timeoutMs: Long) {
        dataStore.updateData { it.toBuilder().setLockTimeoutMs(timeoutMs).build() }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setBiometricEnabled(enabled).build() }
    }

    override suspend fun setInvalidateKeyOnBioChange(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setInvalidateKeyOnBioChange(enabled).build() }
    }

    override suspend fun setSecureContentEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setSecureContent(enabled).build() }
    }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setFlipToLock(enabled).build() }
    }

    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setFlipExitAndClearStack(enabled).build() }
    }

    override suspend fun setLockOnBackground(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setLockOnBackground(enabled).build() }
    }

    // ── 备份操作 ──

    override suspend fun setBackupDirectoryUri(uri: String) {
        dataStore.updateData { it.toBuilder().setBackupDirectoryUri(uri).build() }
    }

    override suspend fun clearBackupDirectoryUri() {
        dataStore.updateData { it.toBuilder().setBackupDirectoryUri("").build() }
    }

    override suspend fun setLastBackupExportFileName(fileName: String) {
        dataStore.updateData { it.toBuilder().setLastBackupExportFileName(fileName).build() }
    }
}
