package com.aozijx.passly.data.repository.access

import android.content.Context
import androidx.datastore.core.DataStore
import com.aozijx.passly.data.local.datastore.securityPreferencesDataStore
import com.aozijx.passly.data.preferences.proto.AuthenticationAttemptRecord
import com.aozijx.passly.data.preferences.proto.SecurityPreferences
import com.aozijx.passly.domain.access.model.AuthenticationAttemptState
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoAuthenticationAttemptStore internal constructor(
    private val dataStore: DataStore<SecurityPreferences>,
) : AuthenticationAttemptStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.applicationContext.securityPreferencesDataStore)

    override suspend fun update(
        method: AuthenticationMethod,
        transform: (AuthenticationAttemptState) -> AuthenticationAttemptState,
    ): AuthenticationAttemptState {
        val methodKey = method.stableKey()
        var updated: AuthenticationAttemptState? = null
        dataStore.updateData { preferences ->
            val index = preferences.credentialAttemptsList.indexOfFirst { it.methodKey == methodKey }
            val previous =
                if (index >= 0) {
                    preferences.getCredentialAttempts(index).toDomain()
                } else {
                    AuthenticationAttemptState()
                }
            val next = transform(previous)
            updated = next
            val builder = preferences.toBuilder()
            val record = next.toProto(methodKey)
            if (index >= 0) {
                builder.setCredentialAttempts(index, record)
            } else {
                builder.addCredentialAttempts(record)
            }
            builder.build()
        }
        return checkNotNull(updated)
    }

    private fun AuthenticationAttemptRecord.toDomain() =
        AuthenticationAttemptState(
            failedAttempts = failedAttempts,
            lockedUntilEpochMs = lockedUntilEpochMs,
            lockedUntilElapsedRealtimeMs = lockedUntilElapsedRealtimeMs,
            bootCount = bootCount,
        )

    private fun AuthenticationAttemptState.toProto(methodKey: String) =
        AuthenticationAttemptRecord
            .newBuilder()
            .setMethodKey(methodKey)
            .setFailedAttempts(failedAttempts)
            .setLockedUntilEpochMs(lockedUntilEpochMs)
            .setLockedUntilElapsedRealtimeMs(lockedUntilElapsedRealtimeMs)
            .setBootCount(bootCount)
            .build()

    private fun AuthenticationMethod.stableKey(): String =
        when (this) {
            AuthenticationMethod.APP_PASSWORD -> "app_password"
            AuthenticationMethod.RECOVERY_CODE -> "recovery_code"
            AuthenticationMethod.BIOMETRIC -> error("Biometric attempts are enforced by the platform")
        }
}
