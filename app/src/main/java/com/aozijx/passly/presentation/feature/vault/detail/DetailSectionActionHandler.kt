package com.aozijx.passly.presentation.feature.vault.detail

import android.content.Context
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.domain.sensitive.SensitiveValue

internal data class DetailSectionActionHandler(
    val onAuthenticate: DetailAuthenticate,
    val onAction: (DetailUiAction) -> Unit
) {
    fun record(field: String, type: ActivityType) {
        onAction(DetailUiAction.RecordAction(field, type))
    }
}

internal inline fun copySensitiveField(
    context: Context,
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: SensitiveValue?,
    sourceValue: String?,
    crossinline afterCopy: (String) -> Unit = {}
) {
    if (revealedValue != null) {
        val chars = revealedValue.toCharArray()
        val plain = String(chars)
        chars.fill('\u0000')
        ClipboardUtils.copy(context, plain)
        afterCopy(plain)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
        return
    }
    val source = sourceValue?.takeIf { it.isNotBlank() } ?: return
    handler.onAuthenticate.copy {
        ClipboardUtils.copy(context, source)
        afterCopy(source)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
    }
}

internal inline fun toggleRevealSensitiveField(
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: SensitiveValue?,
    sourceValue: String?,
    accessLevel: SensitiveAccessLevel = SensitiveAccessLevel.STANDARD,
    crossinline onReveal: (String?) -> Unit
) {
    val source = sourceValue?.takeIf { it.isNotBlank() } ?: return
    if (revealedValue != null) {
        onReveal(null)
        return
    }

    handler.onAuthenticate.reveal(accessLevel) {
        onReveal(source)
        handler.record(fieldName, ActivityType.VIEW)
    }
}
