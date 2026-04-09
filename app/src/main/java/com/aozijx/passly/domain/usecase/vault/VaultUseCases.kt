package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.impl.DeleteAllEntriesUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DeleteEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetHistoryByEntryIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetTotpCodeUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveCategoriesByFilterUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveEntrySummariesByDemandUseCase
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
    val observeEntrySummariesByDemand = ObserveEntrySummariesByDemandUseCase(vaultSearchRepository)
    val getCategoriesByFilter = ObserveCategoriesByFilterUseCase(vaultSearchRepository)
    val getHistoryByEntryId = GetHistoryByEntryIdUseCase(historyRepository)
    val getEntryById = GetEntryByIdUseCase(vaultRepository)
    val insertEntry = InsertEntryUseCase(vaultRepository)
    val updateEntry = UpdateEntryUseCase(vaultRepository)
    val deleteEntry = DeleteEntryUseCase(vaultRepository)
    val deleteAllEntries = DeleteAllEntriesUseCase(vaultRepository)
    val getTotpCode = GetTotpCodeUseCase(otpRepository)
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
}
