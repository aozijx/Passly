package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.icon.FaviconOutcome

interface FaviconRepository {
    suspend fun downloadFavicon(input: String): FaviconOutcome
}
