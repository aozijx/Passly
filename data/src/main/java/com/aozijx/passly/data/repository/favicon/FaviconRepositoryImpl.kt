package com.aozijx.passly.data.repository.favicon

import android.content.Context
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.port.FaviconRepository
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FaviconRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val settingsRepository: AppSettingsRepository,
    private val downloader: FaviconDownloader
) : FaviconRepository {
    override suspend fun download(input: String): FaviconOutcome {
        val whitelist = settingsRepository.settings.first().interaction.faviconDownloadWhitelist
        val outcome = downloader.downloadAndSaveFavicon(input, appContext, whitelist)

        val mappedResult = when (outcome.result) {
            FaviconDownloader.DownloadResult.SUCCESS -> FaviconResult.SUCCESS
            FaviconDownloader.DownloadResult.NETWORK_ERROR -> FaviconResult.NETWORK_ERROR
            FaviconDownloader.DownloadResult.DECODE_ERROR -> FaviconResult.DECODE_ERROR
            FaviconDownloader.DownloadResult.SAVE_ERROR -> FaviconResult.SAVE_ERROR
            FaviconDownloader.DownloadResult.EMPTY_INPUT -> FaviconResult.EMPTY_INPUT
        }

        return FaviconOutcome(
            result = mappedResult,
            filePath = outcome.filePath
        )
    }
}
