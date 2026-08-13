package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome

interface FaviconRepository {
    suspend fun download(input: String): FaviconOutcome
}
