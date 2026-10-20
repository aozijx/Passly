package com.aozijx.passly.feature.autofill.legacy

enum class LegacyAutofillResultTarget(val wireValue: String) {
    RESPONSE("response"),
    DATASET("dataset");

    companion object {
        fun fromWireValue(value: String?): LegacyAutofillResultTarget? =
            entries.firstOrNull { it.wireValue == value }
    }
}
