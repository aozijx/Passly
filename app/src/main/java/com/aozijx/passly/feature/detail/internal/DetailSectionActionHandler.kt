package com.aozijx.passly.feature.detail.internal

import android.content.Context
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.feature.detail.contract.DetailEvent
import com.aozijx.passly.feature.detail.DetailAuthenticate

internal data class DetailSectionActionHandler(
    val onAuthenticate: DetailAuthenticate,
    val onEvent: (DetailEvent) -> Unit
) {
    fun record(field: String, type: ActivityType) {
        onEvent(DetailEvent.RecordAction(field, type))
    }
}

internal inline fun copySensitiveField(
    context: Context,
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: String?,
    sourceValue: String?,
    authTitle: String,
    authSubtitle: String,
    crossinline onReveal: (String) -> Unit = {},
    crossinline afterCopy: (String) -> Unit = {}
) {
    val source = sourceValue?.takeIf { it.isNotBlank() } ?: return
    if (revealedValue != null) {
        ClipboardUtils.copy(context, revealedValue)
        afterCopy(revealedValue)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
        return
    }

    handler.onAuthenticate {
        onReveal(source)
        ClipboardUtils.copy(context, source)
        afterCopy(source)
        handler.record(fieldName, ActivityType.COPY_PASSWORD)
    }
}

internal inline fun toggleRevealSensitiveField(
    handler: DetailSectionActionHandler,
    fieldName: String,
    revealedValue: String?,
    sourceValue: String?,
    authTitle: String,
    authSubtitle: String,
    crossinline onReveal: (String?) -> Unit
) {
    val source = sourceValue?.takeIf { it.isNotBlank() } ?: return
    if (revealedValue != null) {
        onReveal(null)
        return
    }

    handler.onAuthenticate {
        onReveal(source)
        handler.record(fieldName, ActivityType.VIEW)
    }
}
