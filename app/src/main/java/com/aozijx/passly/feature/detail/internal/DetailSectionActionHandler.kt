package com.aozijx.passly.feature.detail.internal

import android.content.Context
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailIntent

internal data class DetailSectionActionHandler(
    val onAuthenticate: DetailAuthenticate,
    val onEvent: (DetailIntent) -> Unit
) {
    fun record(field: String, type: ActivityType) {
        onEvent(DetailIntent.RecordAction(field, type))
    }
}

internal inline fun copySensitiveField(
    context: Context,
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: String?,
    sourceValue: String?,
    crossinline afterCopy: (String) -> Unit = {}
) {
    if (revealedValue != null) {
        ClipboardUtils.copy(context, revealedValue)
        afterCopy(revealedValue)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
        return
    }
    val source = sourceValue?.takeIf { it.isNotBlank() } ?: return
    val value = revealedValue ?: source
    handler.onAuthenticate.copy {
        ClipboardUtils.copy(context, value)
        afterCopy(value)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
    }
}

internal inline fun toggleRevealSensitiveField(
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: String?,
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
