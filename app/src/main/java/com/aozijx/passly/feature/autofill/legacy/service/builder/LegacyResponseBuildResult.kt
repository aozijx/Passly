package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.service.autofill.FillResponse
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId

data class LegacyResponseBuildResult(
    val response: FillResponse?,
    val observation: LegacyDiscoveryObservation,
    val summary: Summary,
) {
    data class Summary(
        val datasetCount: Int,
        val hasSaveInfo: Boolean,
    )
}

sealed interface LegacyDiscoveryObservation {
    data class Completed(
        val interactionId: AutofillInteractionId,
    ) : LegacyDiscoveryObservation

    data class Unavailable(
        val reason: ReasonCode,
    ) : LegacyDiscoveryObservation
}
