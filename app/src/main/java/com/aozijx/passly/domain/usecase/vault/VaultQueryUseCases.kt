package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.EntryFilter
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.repository.entry.EntryListQueryRepository
import com.aozijx.passly.domain.repository.entry.EntryQueryRepository
import com.aozijx.passly.domain.repository.otp.OtpConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultQueryUseCases @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val otpConfigRepository: OtpConfigRepository
) {

    fun observe(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<EntryListItem>> =
        entryListQueryRepository.observe(query, category, filter)

    fun observeCategories(
        filter: EntryFilter
    ): Flow<List<String>> = entryListQueryRepository.observeCategories(filter)

    suspend fun getById(entryId: String): VaultEntry? = entryQueryRepository.getById(entryId)

    suspend fun getOtpConfig(entryId: String): OtpConfig? = otpConfigRepository.getConfig(entryId)

    suspend fun getEntriesForIconResync(): List<VaultEntry> =
        entryQueryRepository.getEntriesForIconResync()

    fun getTotpCode(
        config: OtpConfig,
        overrideCounter: Long? = null,
        timestamp: Long = System.currentTimeMillis() / 1000
    ): OtpResult = OtpGenerator.generate(config, overrideCounter, timestamp)
}
