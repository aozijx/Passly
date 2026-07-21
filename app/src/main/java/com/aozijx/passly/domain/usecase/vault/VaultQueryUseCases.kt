package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.QueryRepository
import com.aozijx.passly.domain.repository.lookup.LookupRepository
import com.aozijx.passly.domain.repository.otp.OtpRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultQueryUseCases @Inject constructor(
    private val queryRepository: QueryRepository,
    private val lookupRepository: LookupRepository,
    private val otpRepository: OtpRepository
) {

    fun observe(
        query: String, category: String?, filter: LookupRepository.EntryFilter
    ): Flow<List<VaultEntry>> =
        lookupRepository.observe(query, category, filter)

    fun observeCategories(
        filter: LookupRepository.EntryFilter
    ): Flow<List<String>> = lookupRepository.observeCategories(filter)

    suspend fun getById(entryId: String): VaultEntry? = queryRepository.getById(entryId)

    fun getTotpCode(config: OtpConfig): String = otpRepository.generate(config)
}
