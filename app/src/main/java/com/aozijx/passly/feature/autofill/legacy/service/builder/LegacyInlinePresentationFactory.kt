package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.app.PendingIntent
import android.service.autofill.InlinePresentation
import android.view.inputmethod.InlineSuggestionsRequest
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.autofill.inline.UiVersions

internal object LegacyInlinePresentationFactory {
    fun isAvailable(request: InlineSuggestionsRequest?): Boolean = request != null &&
        request.maxSuggestionCount > 0 && request.inlinePresentationSpecs.firstOrNull()?.let { spec ->
            UiVersions.INLINE_UI_VERSION_1 in UiVersions.getVersions(spec.style)
        } == true

    fun create(
        request: InlineSuggestionsRequest?,
        suggestionIndex: Int,
        title: String,
        subtitle: String?,
        attributionIntent: PendingIntent,
    ): InlinePresentation? {
        if (request == null || InlineSuggestionSlots.candidateCount(
                suggestionIndex + 1,
                request.maxSuggestionCount,
                request.inlinePresentationSpecs.size,
            ) <= suggestionIndex
        ) return null

        val spec = request.inlinePresentationSpecs[
            InlineSuggestionSlots.specIndex(suggestionIndex, request.inlinePresentationSpecs.size)
        ]
        if (UiVersions.INLINE_UI_VERSION_1 !in UiVersions.getVersions(spec.style)) return null
        val content: UiVersions.Content = InlineSuggestionUi.newContentBuilder(attributionIntent)
            .setTitle(title)
            .setContentDescription(title)
            .apply { if (!subtitle.isNullOrBlank()) setSubtitle(subtitle) }
            .build()
        return InlinePresentation(content.slice, spec, false)
    }
}
