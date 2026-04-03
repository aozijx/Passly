package com.aozijx.passly.data.repository

import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.repository.FaviconRepository

class FaviconDataRepository() : FaviconRepository {
    override suspend fun downloadFavicon(input: String): FaviconOutcome {
        // TODO: Implement actual favicon download logic
        // This is a placeholder implementation that returns an error
        return if (input.isEmpty()) {
            FaviconOutcome(
                result = FaviconResult.EMPTY_INPUT,
                filePath = null
            )
        } else {
            FaviconOutcome(
                result = FaviconResult.NETWORK_ERROR,
                filePath = null
            )
        }
    }
}
