package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.impl.DeleteEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetTotpCodeUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveCategoriesByFilterUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveEntrySummariesByDemandUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase

/**
 * 保险箱主页专用用例聚合
 */
class VaultUseCases(
    vaultRepository: VaultRepository,
    vaultSearchRepository: VaultSearchRepository,
    otpRepository: OtpRepository,
    faviconRepository: FaviconRepository
) {
    val observeEntrySummariesByDemand = ObserveEntrySummariesByDemandUseCase(vaultSearchRepository)
    val getCategoriesByFilter = ObserveCategoriesByFilterUseCase(vaultSearchRepository)
    val getEntryById = GetEntryByIdUseCase(vaultRepository)
    val insertEntry = InsertEntryUseCase(vaultRepository)
    val updateEntry = UpdateEntryUseCase(vaultRepository)
    val deleteEntry = DeleteEntryUseCase(vaultRepository)
    val getTotpCode = GetTotpCodeUseCase(otpRepository)
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
}