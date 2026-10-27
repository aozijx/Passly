package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.securityPreferencesDataStore
import com.aozijx.passly.data.preferences.proto.SecurityPreferences
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoSecurityPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) : SecuritySettingsRepository, IdleTimeoutSettings {

    private val dataStore = context.applicationContext.securityPreferencesDataStore

    override val security = dataStore.data.map(::readSecurity)
    override val lockTimeout = dataStore.data.map { it.lockTimeoutMs }
    override val isLockOnBackground = dataStore.data.map { it.lockOnBackground }

    override suspend fun setSecureContentEnabled(enabled: Boolean) =
        update { secureContentEnabled = enabled }
    override suspend fun setFlipToLockEnabled(enabled: Boolean) =
        update { flipToLockEnabled = enabled }
    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        update { flipExitAndClearStack = enabled }
    override suspend fun setLockOnBackground(enabled: Boolean) = update { lockOnBackground = enabled }
    override suspend fun setLockTimeout(timeoutMs: Long) = update { lockTimeoutMs = timeoutMs }
    override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) =
        update { reauthenticateSensitiveCopies = enabled }
    override suspend fun setClipboardClearEnabled(enabled: Boolean) =
        update { clipboardClearEnabled = enabled }
    override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = update {
        clipboardClearDelaySeconds = ClipboardClearPolicy.normalizeDelaySeconds(delaySeconds)
    }

    private suspend fun update(block: SecurityPreferences.Builder.() -> Unit) {
        dataStore.updateData { current -> current.toBuilder().apply(block).build() }
    }
}
