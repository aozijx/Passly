package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.repository.otp.OtpRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpRepositoryImpl @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper
) : OtpRepository {

    override suspend fun getConfig(entryId: String): OtpConfig? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val entity = credentialDao().getByEntryId(entryId) ?: return@query null
            cryptoMapper.decryptCredential(entity).otp
        }
    }

    override fun generate(
        config: OtpConfig,
        overrideCounter: Long?,
        timestamp: Long
    ): OtpResult = OtpGenerator.generate(config, overrideCounter, timestamp)
}
