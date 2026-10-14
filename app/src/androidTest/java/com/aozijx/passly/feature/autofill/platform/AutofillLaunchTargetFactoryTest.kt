package com.aozijx.passly.feature.autofill.platform

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.aozijx.passly.app.autofill.AndroidAutofillLaunchTarget
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.credential.service.CredentialPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.presentation.feature.autofill.credential.CredentialResponseActivity
import com.aozijx.passly.presentation.feature.autofill.legacy.AutofillFillActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillLaunchTargetFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun factoriesForwardStableRequestIdsToLaunchTarget() {
        val target = RecordingLaunchTarget(context)
        val interactionId = AutofillInteractionId("interaction-1")

        AutofillPendingIntentFactory(target).createInteractionIntent(
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
        )

        assertEquals(
            listOf(
                "autofill-interaction:interaction-1",
                "password-get:interaction-1:entry-1",
            ),
            target.requestIds,
        )
    }

    @Test
    fun androidTargetUsesExplicitActivitiesInCurrentPackage() {
        val target = AndroidAutofillLaunchTarget()

        val legacy = target.legacyFillIntent(context, "legacy")
        val credential = target.credentialResponseIntent(context, "credential")

        assertEquals(context.packageName, legacy.`package`)
        assertEquals(AutofillFillActivity::class.java.name, legacy.component?.className)
        assertEquals(context.packageName, credential.`package`)
        assertEquals(CredentialResponseActivity::class.java.name, credential.component?.className)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun credentialPendingIntentIdentityAndMutabilityRemainStable() {
        val factory = CredentialPendingIntentFactory(AndroidAutofillLaunchTarget())
        val interactionId = AutofillInteractionId("interaction-1")

        val first = factory.createPasswordGetPendingIntent(
            context,
            interactionId,
            EntryId("entry-1"),
        )
        val same = factory.createPasswordGetPendingIntent(
            context,
            interactionId,
            EntryId("entry-1"),
        )
        val other = factory.createPasswordGetPendingIntent(
            context,
            interactionId,
            EntryId("entry-2"),
        )

        assertEquals(first, same)
        assertNotEquals(first, other)
        assertFalse(first.isImmutable)
    }

    private class RecordingLaunchTarget(private val context: Context) : AutofillLaunchTarget {
        val requestIds = mutableListOf<String>()

        override fun legacyFillIntent(context: Context, requestId: String): Intent {
            requestIds += requestId
            return Intent().setPackage(this.context.packageName)
        }

        override fun credentialResponseIntent(context: Context, requestId: String): Intent {
            requestIds += requestId
            return Intent().setPackage(this.context.packageName)
        }
    }
}
