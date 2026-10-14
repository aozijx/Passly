package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacySaveInfoPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacySaveInfoPolicyTest {
    @Test
    fun passwordFieldsAreRequiredAndUsernameFieldsAreOptionalInStableOrder() {
        val plan = LegacySaveInfoPolicy.plan(
            linkedMapOf(
                "username-1" to FieldRole.USERNAME,
                "password-1" to FieldRole.PASSWORD,
                "otp" to FieldRole.OTP,
                "password-2" to FieldRole.PASSWORD,
                "username-2" to FieldRole.USERNAME,
            ),
        )

        requireNotNull(plan)
        assertEquals(listOf("password-1", "password-2"), plan.requiredPasswordFieldIds)
        assertEquals(listOf("username-1", "username-2"), plan.optionalUsernameFieldIds)
        assertEquals(linkedSetOf(FieldRole.PASSWORD, FieldRole.USERNAME), plan.dataTypes)
    }

    @Test
    fun passwordOnlyFormAdvertisesOnlyPasswordDataType() {
        val plan = LegacySaveInfoPolicy.plan(mapOf("password" to FieldRole.PASSWORD))

        requireNotNull(plan)
        assertEquals(setOf(FieldRole.PASSWORD), plan.dataTypes)
    }

    @Test
    fun usernameOnlyFormIsNotEligibleForLoginSave() {
        assertNull(LegacySaveInfoPolicy.plan(mapOf("username" to FieldRole.USERNAME)))
    }

    @Test
    fun unknownAndOtpOnlyFormIsNotEligibleForLoginSave() {
        assertNull(
            LegacySaveInfoPolicy.plan(
                linkedMapOf(
                    "unknown" to FieldRole.UNKNOWN,
                    "otp" to FieldRole.OTP,
                ),
            ),
        )
    }
}
