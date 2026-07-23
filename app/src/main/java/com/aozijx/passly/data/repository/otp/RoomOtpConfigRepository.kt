package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.repository.otp.OtpConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomOtpConfigRepository @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val secretCodec: EntrySecretCodec
) : OtpConfigRepository {

    override suspend fun getConfig(entryId: String): OtpConfig? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val entity = entrySecretQueryDao().getByEntryId(entryId) ?: return@query null
            val secret = secretCodec.decrypt(entity.secretBlob, entity.entryId)
            when (secret) {
                is EntrySecret.Otp -> secret.data.config
                else -> null
            }
        }
    }
}
