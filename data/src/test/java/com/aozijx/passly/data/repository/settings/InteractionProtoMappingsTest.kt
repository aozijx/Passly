package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.AutofillPreferences
import com.aozijx.passly.data.preferences.proto.AutofillPresentationPreference
import com.aozijx.passly.data.preferences.proto.InteractionPreferences
import com.aozijx.passly.data.preferences.proto.SwipeActionPreference
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.SwipeActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionProtoMappingsTest {
    @Test
    fun `proto interaction defaults map to safe directional actions`() {
        val settings = readInteraction(InteractionPreferences.getDefaultInstance())

        assertEquals(false, settings.isPullToSearchEnabled)
        assertEquals(SwipeActionType.COPY_PASSWORD, settings.swipeLeftAction)
        assertEquals(SwipeActionType.DELETE, settings.swipeRightAction)
    }

    @Test
    fun `disabled swipe direction round trips through proto enum`() {
        assertEquals(
            SwipeActionPreference.SWIPE_ACTION_DISABLED,
            SwipeActionType.DISABLED.toProto(),
        )
        assertEquals(
            SwipeActionType.DISABLED,
            SwipeActionPreference.SWIPE_ACTION_DISABLED.toDomain(),
        )
    }

    @Test
    fun `autofill strict domain matching defaults to enabled and can be disabled`() {
        assertTrue(AutofillPreferences.getDefaultInstance().strictDomainMatching)
        assertTrue(readAutofill(AutofillPreferences.getDefaultInstance()).strictDomainMatching)

        listOf(true, false).forEach { strict ->
            val settings = readAutofill(AutofillPreferences.newBuilder().setStrictDomainMatching(strict).build())
            assertEquals(strict, settings.strictDomainMatching)
        }
    }

    @Test
    fun `three autofill presentation modes round trip with stable proto values`() {
        val expected =
            mapOf(
                AutofillPresentation.SYSTEM_INLINE to
                    AutofillPresentationPreference.AUTOFILL_PRESENTATION_SYSTEM_INLINE,
                AutofillPresentation.BOTTOM_SHEET to
                    AutofillPresentationPreference.AUTOFILL_PRESENTATION_BOTTOM_SHEET,
                AutofillPresentation.FLOATING to
                    AutofillPresentationPreference.AUTOFILL_PRESENTATION_FLOATING,
            )
        expected.forEach { (domain, proto) ->
            assertEquals(proto, domain.toProto())
            assertEquals(domain, proto.toDomain())
        }
    }
}
