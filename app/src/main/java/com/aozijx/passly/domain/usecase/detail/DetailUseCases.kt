package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase

/**
 * 详情页专用用例聚合
 */
class DetailUseCases(
    vaultRepository: VaultRepository,
    faviconRepository: FaviconRepository
) {
    val getEntryById = GetEntryByIdUseCase(vaultRepository)
    val updateEntry = UpdateEntryUseCase(vaultRepository)
    val downloadFavicon = DownloadFaviconUseCase(faviconRepository)
}