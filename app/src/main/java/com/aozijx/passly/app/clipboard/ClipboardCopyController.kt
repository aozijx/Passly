package com.aozijx.passly.app.clipboard

import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.core.platform.clipboard.SecureClipboard
import com.aozijx.passly.domain.clipboard.port.OwnedClipboardCleaner
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardCopyController @Inject constructor(
    private val settingsSource: SecuritySettingsSource,
    private val secureClipboard: SecureClipboard,
) : SensitiveClipboardWriter, OwnedClipboardCleaner {
    override suspend fun writeSensitive(text: String) {
        writeSensitiveIf(text) { true }
    }

    override suspend fun writeSensitiveIf(text: String, canWrite: () -> Boolean): Boolean {
        val policy = settingsSource.security.first().clipboardClearPolicy
        if (!canWrite()) return false
        secureClipboard.copySensitive(
            text = text,
            clearAfterSeconds = policy.delaySeconds.takeIf { policy.enabled },
        )
        return true
    }

    override fun clearOwned(): Boolean =
        secureClipboard.clearOwned() == ClipboardClearResult.Cleared
}
