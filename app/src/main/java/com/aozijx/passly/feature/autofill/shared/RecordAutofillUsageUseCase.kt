package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordAutofillUsageUseCase @Inject constructor(
    private val activityRecorder: ActivityRecorder,
) {
    suspend operator fun invoke(candidateId: String): AppResult<Unit> =
        activityRecorder.recordUsage(candidateId, ActivityType.AUTOFILL)
}
