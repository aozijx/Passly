package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.settings.model.AutofillPresentation

internal enum class LegacyAutofillResultTarget(
    val wireValue: String,
) {
    RESPONSE("response"),
    DATASET("dataset"),
    ;

    companion object {
        fun fromWireValue(value: String?): LegacyAutofillResultTarget? = entries.firstOrNull { it.wireValue == value }

        fun forLockedPresentation(presentation: AutofillPresentation): LegacyAutofillResultTarget =
            when (presentation) {
                AutofillPresentation.BOTTOM_SHEET -> DATASET

                AutofillPresentation.FLOATING,
                AutofillPresentation.SYSTEM_INLINE,
                -> RESPONSE
            }
    }
}
