package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.inline.InlinePresentationSpec
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.aozijx.passly.R
import com.aozijx.passly.app.autofill.AndroidAutofillLaunchTarget
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
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyInlinePresentationFactory
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.EditableFieldInfo
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class LegacyResponseFactoryInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pendingIntentFactory = AutofillPendingIntentFactory(AndroidAutofillLaunchTarget())
    private val factory = LegacyResponseFactory(pendingIntentFactory)

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun inlinePresentationRequiresSupportedImeStyleAndCapacity() {
        val attribution = PendingIntent.getActivity(
            context,
            912,
            AndroidAutofillLaunchTarget().legacyFillIntent(context, "inline-test"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val size = Size(100, 48)
        val style = UiVersions.newStylesBuilder()
            .addStyle(InlineSuggestionUi.newStyleBuilder().build())
            .build()
        val spec = InlinePresentationSpec.Builder(size, Size(300, 80))
            .setStyle(style)
            .build()
        val request = InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(1)
            .build()

        assertEquals(true, LegacyInlinePresentationFactory.isAvailable(request))
        assertNotNull(LegacyInlinePresentationFactory.create(
            request, 0, "Account", null, attribution,
        ))
        assertNull(LegacyInlinePresentationFactory.create(
            request, 1, "Account", null, attribution,
        ))

        val unsupported = InlineSuggestionsRequest.Builder(listOf(
            InlinePresentationSpec.Builder(size, Size(300, 80)).setStyle(Bundle.EMPTY).build(),
        )).setMaxSuggestionCount(1).build()
        assertEquals(false, LegacyInlinePresentationFactory.isAvailable(unsupported))
        assertNull(LegacyInlinePresentationFactory.create(
            unsupported, 0, "Account", null, attribution,
        ))
        attribution.cancel()
    }

    @Test
    fun pickerAndUnlockIntentsDeclareTheirPlatformResultTargets() {
        val passwordId = View(context).autofillId
        val parsed = ParsedStructure(
            packageName = "com.example",
            editableFields = listOf(EditableFieldInfo(passwordId)),
        )
        val interactionId = AutofillInteractionId("interaction-1")
        val roleIds = mapOf(FieldRole.PASSWORD to listOf(passwordId))

        val pickerRequest = pendingIntentFactory.createPickerIntent(
            context = context,
            interactionId = interactionId,
            parsed = parsed,
            uiMode = AutofillPresentation.BOTTOM_SHEET,
            roleIds = roleIds,
        )
        val unlockRequest = pendingIntentFactory.createUnlockIntent(
            context = context,
            interactionId = interactionId,
            parsed = parsed,
            uiMode = AutofillPresentation.BOTTOM_SHEET,
            roleIds = roleIds,
        )

        assertEquals(
            LegacyAutofillResultTarget.DATASET,
            LegacyAutofillResultTarget.fromWireValue(
                pickerRequest.getStringExtra(AutofillLaunchExtras.RESULT_TARGET),
            ),
        )
        assertEquals(
            LegacyAutofillResultTarget.RESPONSE,
            LegacyAutofillResultTarget.fromWireValue(
                unlockRequest.getStringExtra(AutofillLaunchExtras.RESULT_TARGET),
            ),
        )
    }

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

    private fun discoveryContext(
        roleMap: Map<String, FieldRole>,
        presentation: AutofillPresentation = AutofillPresentation.SYSTEM_INLINE,
    ) = AutofillDiscoveryContext(
        roleMap = roleMap,
        presentation = presentation,
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
