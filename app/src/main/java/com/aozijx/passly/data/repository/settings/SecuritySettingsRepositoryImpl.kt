package com.aozijx.passly.data.repository.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.aozijx.passly.core.auth.authconstants.AuthLockConstants
import com.aozijx.passly.data.repository.settings.internal.BIOMETRIC_AUTH_KEY
import com.aozijx.passly.data.repository.settings.internal.DEVICE_CREDENTIAL_FALLBACK_KEY
import com.aozijx.passly.data.repository.settings.internal.FLIP_EXIT_AND_CLEAR_STACK_KEY
import com.aozijx.passly.data.repository.settings.internal.FLIP_TO_LOCK_KEY
import com.aozijx.passly.data.repository.settings.internal.INVALIDATE_KEY_ON_BIO_CHANGE_KEY
import com.aozijx.passly.data.repository.settings.internal.LOCK_ON_BACKGROUND_KEY
import com.aozijx.passly.data.repository.settings.internal.LOCK_TIMEOUT_KEY
import com.aozijx.passly.data.repository.settings.internal.PASSWORD_PREFERRED_AUTH_FIRST_KEY
import com.aozijx.passly.data.repository.settings.internal.SECURE_CONTENT_KEY
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecuritySettingsRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    SecuritySettingsRepository {
    private val appContext = context.applicationContext

    override val lockTimeout: Flow<Long> =
        appContext.settingsDataStore.data.map {
            it[LOCK_TIMEOUT_KEY] ?: AuthLockConstants.DEFAULT_LOCK_TIMEOUT_MS
        }
    override val isBiometricEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[BIOMETRIC_AUTH_KEY] ?: AppDefaults.SECURITY_BIOMETRIC_ENABLED
        }
    override val isInvalidateKeyOnBioChange: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[INVALIDATE_KEY_ON_BIO_CHANGE_KEY]
                ?: AppDefaults.SECURITY_INVALIDATE_KEY_ON_BIO_CHANGE
        }
    override val isSecureContentEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[SECURE_CONTENT_KEY] ?: AppDefaults.SECURITY_SECURE_CONTENT_ENABLED
        }
    override val isFlipToLockEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[FLIP_TO_LOCK_KEY] ?: AppDefaults.SECURITY_FLIP_TO_LOCK_ENABLED
        }
    override val isFlipExitAndClearStackEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[FLIP_EXIT_AND_CLEAR_STACK_KEY]
                ?: AppDefaults.SECURITY_FLIP_EXIT_AND_CLEAR_STACK_ENABLED
        }
    override val isPasswordPreferredAuthFirst: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[PASSWORD_PREFERRED_AUTH_FIRST_KEY]
                ?: AppDefaults.SECURITY_PASSWORD_PREFERRED_AUTH_FIRST
        }
    override val isDeviceCredentialFallbackEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[DEVICE_CREDENTIAL_FALLBACK_KEY]
                ?: AppDefaults.SECURITY_DEVICE_CREDENTIAL_FALLBACK_ENABLED
        }
    override val isLockOnBackground: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[LOCK_ON_BACKGROUND_KEY] ?: AppDefaults.SECURITY_LOCK_ON_BACKGROUND
        }

    override suspend fun setLockTimeout(timeoutMs: Long) {
        appContext.settingsDataStore.edit { it[LOCK_TIMEOUT_KEY] = timeoutMs }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[BIOMETRIC_AUTH_KEY] = enabled }
    }

    override suspend fun setInvalidateKeyOnBioChange(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[INVALIDATE_KEY_ON_BIO_CHANGE_KEY] = enabled }
    }

    override suspend fun setSecureContentEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[SECURE_CONTENT_KEY] = enabled }
    }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[FLIP_TO_LOCK_KEY] = enabled }
    }

    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[FLIP_EXIT_AND_CLEAR_STACK_KEY] = enabled }
    }

    override suspend fun setPasswordPreferredAuthFirst(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[PASSWORD_PREFERRED_AUTH_FIRST_KEY] = enabled }
    }

    override suspend fun setDeviceCredentialFallbackEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[DEVICE_CREDENTIAL_FALLBACK_KEY] = enabled }
    }

    override suspend fun setLockOnBackground(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[LOCK_ON_BACKGROUND_KEY] = enabled }
    }
}