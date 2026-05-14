package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetHistoryByEntryIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertHistoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase

class DetailUseCases(
    vaultRepository: VaultRepository,
    faviconRepository: FaviconRepository,
    historyRepository: HistoryRepository
) {
    val getEntryById = GetEntryByIdUseCase(vaultRepository)
    val updateEntry = UpdateEntryUseCase(vaultRepository)
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
    val getHistoryByEntryId = GetHistoryByEntryIdUseCase(historyRepository)
    val insertHistory = InsertHistoryUseCase(historyRepository)
}