package com.aozijx.passly.domain.repository.favicon

import com.aozijx.passly.domain.model.favicon.FaviconOutcome

interface FaviconRepository {
    suspend fun download(input: String): FaviconOutcome
}
