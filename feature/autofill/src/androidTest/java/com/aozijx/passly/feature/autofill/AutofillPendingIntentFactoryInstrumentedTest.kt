package com.aozijx.passly.feature.autofill

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.credential.service.CredentialPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillPendingIntentFactoryInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun factoriesForwardStableRequestIdsToLaunchTarget() {
        val target = RecordingLaunchTarget()
        val interactionId = AutofillInteractionId("interaction-1")

        AutofillPendingIntentFactory(target).createPickerIntent(
            context = context,
            interactionId = interactionId,
            parsed = ParsedStructure(packageName = "com.example"),
            uiMode = AutofillPresentation.SYSTEM_INLINE,
            roleIds = emptyMap(),
        )
        CredentialPendingIntentFactory(target).createPasswordGetPendingIntent(
            context = context,
            interactionId = interactionId,
            entryId = EntryId("entry-1"),
        ).cancel()

        assertEquals(
            listOf(
                "autofill-interaction:interaction-1",
                "password-get:interaction-1:entry-1",
            ),
            target.requestIds,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun credentialPendingIntentIdentityAndMutabilityRemainStable() {
        val factory = CredentialPendingIntentFactory(RecordingLaunchTarget())
        val interactionId = AutofillInteractionId("interaction-1")

        val first = factory.createPasswordGetPendingIntent(context, interactionId, EntryId("entry-1"))
        val same = factory.createPasswordGetPendingIntent(context, interactionId, EntryId("entry-1"))
        val other = factory.createPasswordGetPendingIntent(context, interactionId, EntryId("entry-2"))

        assertEquals(first, same)
        assertNotEquals(first, other)
        assertFalse(first.isImmutable)

        first.cancel()
        other.cancel()
    }

    private class RecordingLaunchTarget : AutofillLaunchTarget {
        val requestIds = mutableListOf<String>()

        override fun legacyFillIntent(
            context: Context,
            requestId: String,
        ): Intent = intent(context, requestId)

        override fun accessibilityFillIntent(context: Context): Intent =
            Intent(context, PendingIntentTargetActivity::class.java)

        override fun credentialResponseIntent(
            context: Context,
            requestId: String,
        ): Intent = intent(context, requestId)

        private fun intent(
            context: Context,
            requestId: String,
        ): Intent {
            requestIds += requestId
            return Intent(context, PendingIntentTargetActivity::class.java)
        }
    }

    private class PendingIntentTargetActivity : Activity()
}
