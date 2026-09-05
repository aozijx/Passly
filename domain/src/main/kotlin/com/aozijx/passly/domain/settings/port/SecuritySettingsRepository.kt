package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.SecuritySettings
import kotlinx.coroutines.flow.Flow

interface SecuritySettingsSource {
    val security: Flow<SecuritySettings>
}

interface SecuritySettingsRepository : SecuritySettingsSource {
    suspend fun setSecureContentEnabled(enabled: Boolean)
    suspend fun setFlipToLockEnabled(enabled: Boolean)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean)
    suspend fun setLockOnBackground(enabled: Boolean)
    suspend fun setLockTimeout(timeoutMs: Long)
    suspend fun setInvalidateBiometricKeyOnChange(enabled: Boolean)
    suspend fun setReauthenticateSensitiveCopies(enabled: Boolean)
    suspend fun setClipboardClearEnabled(enabled: Boolean)
    suspend fun setClipboardClearDelaySeconds(delaySeconds: Int)
}
