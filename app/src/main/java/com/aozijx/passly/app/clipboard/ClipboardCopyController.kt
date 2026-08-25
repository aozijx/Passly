package com.aozijx.passly.app.clipboard

import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.core.platform.clipboard.SecureClipboard
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardCopyController @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val secureClipboard: SecureClipboard,
) {
    suspend fun copySensitive(text: String) {
        val policy = settingsRepository.settings.first().security.clipboardClearPolicy
        secureClipboard.copySensitive(
            text = text,
            clearAfterSeconds = policy.delaySeconds.takeIf { policy.enabled },
        )
    }

    fun clearOwned(): ClipboardClearResult = secureClipboard.clearOwned()
}
