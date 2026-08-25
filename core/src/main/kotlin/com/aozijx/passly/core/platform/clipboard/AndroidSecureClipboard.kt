package com.aozijx.passly.core.platform.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidSecureClipboard @Inject constructor(
    @ApplicationContext context: Context,
) : SecureClipboard {
    private val clipboard = context.applicationContext.getSystemService(ClipboardManager::class.java)
    private val scheduler = ClipboardClearScheduler(
        scope = CoroutineScope(SupervisorJob()),
        dispatcher = Dispatchers.Default,
    )

    override fun copySensitive(text: String, clearAfterSeconds: Int?) {
        val token = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(CLIP_LABEL, text).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(sensitiveExtraKey(), true)
                putString(OWNERSHIP_TOKEN_KEY, token)
            }
        }
        clipboard.setPrimaryClip(clip)

        scheduler.cancel()
        if (clearAfterSeconds != null) {
            scheduler.schedule(token, clearAfterSeconds * 1_000L, ::clearExact)
        }
    }

    override fun clearOwned(): ClipboardClearResult = try {
        if (!clipboard.hasPrimaryClip()) {
            ClipboardClearResult.Empty
        } else {
            val token = clipboard.primaryClipDescription
                ?.extras
                ?.getString(OWNERSHIP_TOKEN_KEY)
            if (token == null) ClipboardClearResult.NotOwned else clearExact(token)
        }
    } catch (error: RuntimeException) {
        ClipboardClearResult.Failed(error)
    }

    private fun clearExact(expectedToken: String): ClipboardClearResult = try {
        val description = clipboard.primaryClipDescription
            ?: return ClipboardClearResult.Empty
        val currentToken = description.extras?.getString(OWNERSHIP_TOKEN_KEY)
        if (description.label != CLIP_LABEL || currentToken != expectedToken) {
            ClipboardClearResult.NotOwned
        } else {
            clipboard.clearPrimaryClip()
            scheduler.cancel()
            ClipboardClearResult.Cleared
        }
    } catch (error: RuntimeException) {
        ClipboardClearResult.Failed(error)
    }

    private fun sensitiveExtraKey(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            LEGACY_SENSITIVE_EXTRA_KEY
        }

    private companion object {
        const val CLIP_LABEL = "passly.sensitive"
        const val OWNERSHIP_TOKEN_KEY = "com.aozijx.passly.clipboard.OWNER_TOKEN"
        const val LEGACY_SENSITIVE_EXTRA_KEY = "android.content.extra.IS_SENSITIVE"
    }
}
