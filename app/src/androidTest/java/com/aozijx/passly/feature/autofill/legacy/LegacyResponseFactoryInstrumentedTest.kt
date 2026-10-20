package com.aozijx.passly.feature.autofill.legacy

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.aozijx.passly.R
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.builder.AutofillRemoteItem
import com.aozijx.passly.feature.autofill.legacy.service.builder.AutofillRemoteViewFactory
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyDiscoveryObservation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.EditableFieldInfo
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class LegacyResponseFactoryInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = LegacyResponseFactory(
        AutofillPendingIntentFactory(
            object : AutofillLaunchTarget {
                override fun legacyFillIntent(context: Context, requestId: String) = Intent()

                override fun legacySaveIntent(context: Context, requestId: String) = Intent()

                override fun credentialResponseIntent(context: Context, requestId: String) = Intent()
            },
        ),
    )

    @Test
    fun passwordFormWithoutCandidatesStillReturnsSaveInfoResponse() {
        val passwordId = View(context).autofillId
        val parsed = ParsedStructure(
            packageName = "com.example",
            editableFields = listOf(EditableFieldInfo(passwordId)),
        )

        val result = factory.buildResult(
            context,
            AutofillDiscoveryOutcome.NoCandidates(
                snapshot(),
                discoveryContext(mapOf(passwordId.toString() to FieldRole.PASSWORD)),
            ),
            parsed,
        )

        assertNotNull(result.response)
        assertEquals(
            LegacyDiscoveryObservation.Unavailable(Reasons.NoMatchingEntry),
            result.observation,
        )
    }

    @Test
    fun usernameOnlyFormWithoutCandidatesReturnsNoResponse() {
        val usernameId = View(context).autofillId
        val parsed = ParsedStructure(
            packageName = "com.example",
            editableFields = listOf(EditableFieldInfo(usernameId)),
        )

        val response = factory.buildFillResponse(
            context,
            AutofillDiscoveryOutcome.NoCandidates(
                snapshot(),
                discoveryContext(mapOf(usernameId.toString() to FieldRole.USERNAME)),
            ),
            parsed,
        )

        assertNull(response)
    }

    @Test
    fun disabledUnsupportedAndFailedOutcomesAdvertiseNothing() {
        val parsed = ParsedStructure()

        assertNull(factory.buildFillResponse(context, AutofillDiscoveryOutcome.Disabled, parsed))
        assertNull(
            factory.buildFillResponse(context, AutofillDiscoveryOutcome.UnsupportedFields, parsed),
        )
        assertNull(
            factory.buildFillResponse(
                context,
                AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.CANDIDATE_QUERY_FAILED),
                parsed,
            ),
        )
    }

    @Test
    fun remoteItemRendererUsesTheShapeDeclaredByItsPresentationModel() {
        val parent = FrameLayout(context)

        val single = AutofillRemoteViewFactory.render(
            context,
            AutofillRemoteItem.SingleLine("Locked"),
        ).apply(context, parent)
        val double = AutofillRemoteViewFactory.render(
            context,
            AutofillRemoteItem.DoubleLine("Passly", "2 accounts"),
        ).apply(context, parent)

        assertEquals("Locked", single.findViewById<TextView>(R.id.item_title).text)
        assertNull(single.findViewById<TextView?>(R.id.item_subtitle))
        assertEquals("Passly", double.findViewById<TextView>(R.id.item_title).text)
        assertEquals("2 accounts", double.findViewById<TextView>(R.id.item_subtitle).text)
    }

    @Test
    fun responseBuildResultCarriesOneSemanticDiscoveryObservation() {
        val parsed = ParsedStructure()

        val result = factory.buildResult(
            context,
            AutofillDiscoveryOutcome.Disabled,
            parsed,
        )

        assertNull(result.response)
        assertEquals(
            LegacyDiscoveryObservation.Unavailable(Reasons.SessionRestricted),
            result.observation,
        )
    }

    private fun discoveryContext(roleMap: Map<String, FieldRole>) = AutofillDiscoveryContext(
        roleMap = roleMap,
        presentation = AutofillPresentation.SYSTEM_INLINE,
        requireAuthentication = false,
        includeOtp = false,
        savePromptsEnabled = true,
    )

    private fun snapshot() = AutofillInteractionSnapshot(
        id = AutofillInteractionId("legacy-save-test"),
        source = AutofillInteractionSource.LEGACY,
        scope = AutofillGrantContext("com.example"),
        state = AutofillInteractionState.CandidatesReady,
    )
}
