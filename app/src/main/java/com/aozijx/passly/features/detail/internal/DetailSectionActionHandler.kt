package com.aozijx.passly.features.detail.internal

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.features.detail.contract.DetailEvent

internal data class DetailSectionActionHandler(
    val activity: FragmentActivity,
    val onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    val onEvent: (DetailEvent) -> Unit
) {
    fun record(field: String, type: VaultHistory.HistoryType) {
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
        handler.record(fieldName, VaultHistory.HistoryType.COPY)
        return
    }

    handler.onAuthenticate(handler.activity, authTitle, authSubtitle) {
        onReveal(source)
        ClipboardUtils.copy(context, source)
        afterCopy(source)
        handler.record(fieldName, VaultHistory.HistoryType.COPY)
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

    handler.onAuthenticate(handler.activity, authTitle, authSubtitle) {
        onReveal(source)
        handler.record(fieldName, VaultHistory.HistoryType.ACCESS)
    }
}