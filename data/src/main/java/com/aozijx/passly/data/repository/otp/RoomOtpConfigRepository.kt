package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomOtpConfigRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val secretFieldStore: SecretFieldStore
) : OtpConfigRepository {

    override suspend fun getConfig(entryId: String): OtpConfig? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return databaseSession.query {
            val secret = secretFieldStore.readAll(this, entryId)
            secret.otp?.config
        }
    }
}
