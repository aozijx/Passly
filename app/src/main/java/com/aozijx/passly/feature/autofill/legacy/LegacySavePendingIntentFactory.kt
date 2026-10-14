package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacySavePendingIntentFactory @Inject constructor(
    private val launchTarget: AutofillLaunchTarget,
) {
    fun create(context: Context, token: LegacySaveToken): PendingIntent {
        val requestId = AutofillPendingIntentPolicy.legacySaveRequestId(token.value)
        val intent = launchTarget.legacySaveIntent(context, requestId).apply {
            identifier = requestId
            putExtra(AutofillLaunchExtras.LEGACY_SAVE_TOKEN, token.value)
        }
        return PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }
}
