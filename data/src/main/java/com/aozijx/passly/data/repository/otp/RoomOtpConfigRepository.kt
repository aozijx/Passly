package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.repository.OtpConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomOtpConfigRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val secretCodec: EntrySecretCodec
) : OtpConfigRepository {

    override suspend fun getConfig(entryId: String): OtpConfig? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return databaseSession.query {
            val entity = entrySecretQueryDao().getByEntryId(entryId) ?: return@query null
            val secret = secretCodec.decrypt(entity.secretBlob, entity.entryId)
            secret.otp?.config
        }
    }
}
