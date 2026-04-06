package com.aozijx.passly.data.repository

import android.content.Context
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.repository.vault.FaviconRepository

class FaviconDataRepository(
    private val appContext: Context
) : FaviconRepository {
    override suspend fun downloadFavicon(input: String): FaviconOutcome {
        val outcome = FaviconUtils.downloadAndSaveFavicon(input, appContext)

        val mappedResult = when (outcome.result) {
            FaviconUtils.DownloadResult.SUCCESS -> FaviconResult.SUCCESS
            FaviconUtils.DownloadResult.NETWORK_ERROR -> FaviconResult.NETWORK_ERROR
            FaviconUtils.DownloadResult.DECODE_ERROR -> FaviconResult.DECODE_ERROR
            FaviconUtils.DownloadResult.SAVE_ERROR -> FaviconResult.SAVE_ERROR
            FaviconUtils.DownloadResult.EMPTY_INPUT -> FaviconResult.EMPTY_INPUT
        }

        return FaviconOutcome(
            result = mappedResult,
            filePath = outcome.filePath
        )
    }
}
