package com.aozijx.passly.app.clipboard

import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.core.platform.clipboard.SecureClipboard
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardCopyController @Inject constructor(
    private val settingsSource: SecuritySettingsSource,
    private val secureClipboard: SecureClipboard,
) {
    suspend fun copySensitive(text: String) {
        val policy = settingsSource.security.first().clipboardClearPolicy
        secureClipboard.copySensitive(
            text = text,
            clearAfterSeconds = policy.delaySeconds.takeIf { policy.enabled },
        )
    }

    fun clearOwned(): ClipboardClearResult = secureClipboard.clearOwned()
}
