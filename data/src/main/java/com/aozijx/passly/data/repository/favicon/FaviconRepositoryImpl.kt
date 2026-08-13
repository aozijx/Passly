package com.aozijx.passly.data.repository.favicon

import android.content.Context
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.repository.FaviconRepository
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaviconRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val settingsRepository: AppSettingsRepository
) : FaviconRepository {
    override suspend fun download(input: String): FaviconOutcome {
        val whitelist = settingsRepository.settings.first().interaction.faviconDownloadWhitelist
        val outcome = FaviconUtils.downloadAndSaveFavicon(input, appContext, whitelist)

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
