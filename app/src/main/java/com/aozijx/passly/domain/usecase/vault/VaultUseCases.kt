package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.impl.DeleteAllEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DeleteEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetCategoriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntriesByCategoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntrySummariesByCategoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetHistoryByEntryIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetTotpCodeUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveAllEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveAllEntrySummariesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.SearchEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.SearchEntrySummariesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase

/**
 * 门面类，聚合所有仓库相关的用例实现
 */
class VaultUseCases(
    vaultRepository: VaultRepository,
    vaultSearchRepository: VaultSearchRepository,
    historyRepository: HistoryRepository,
    otpRepository: OtpRepository,
    faviconRepository: FaviconRepository
) {
    val observeAllEntries = ObserveAllEntriesUseCase(vaultRepository)
    val observeAllEntrySummaries = ObserveAllEntrySummariesUseCase(vaultSearchRepository)
    val getEntriesByCategory = GetEntriesByCategoryUseCase(vaultSearchRepository)
    val getEntrySummariesByCategory = GetEntrySummariesByCategoryUseCase(vaultSearchRepository)
    val searchEntries = SearchEntriesUseCase(vaultSearchRepository)
    val searchEntrySummaries = SearchEntrySummariesUseCase(vaultSearchRepository)
    val getCategories = GetCategoriesUseCase(vaultSearchRepository)
    val getHistoryByEntryId = GetHistoryByEntryIdUseCase(historyRepository)
    val getEntryById = GetEntryByIdUseCase(vaultRepository)
    val insertEntry = InsertEntryUseCase(vaultRepository)
    val updateEntry = UpdateEntryUseCase(vaultRepository)
    val deleteEntry = DeleteEntryUseCase(vaultRepository)
    val deleteAllEntries = DeleteAllEntriesUseCase(vaultRepository)
    val getTotpCode = GetTotpCodeUseCase(otpRepository)
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
}
