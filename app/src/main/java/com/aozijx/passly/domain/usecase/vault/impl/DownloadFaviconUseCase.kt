package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.repository.vault.FaviconRepository

class DownloadFaviconUseCase(
    private val repository: FaviconRepository
) {
    suspend operator fun invoke(input: String): FaviconOutcome {
        return repository.downloadFavicon(input)
    }
}
