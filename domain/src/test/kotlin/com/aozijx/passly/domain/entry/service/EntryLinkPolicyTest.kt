package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryLinkPolicyTest {
    @Test
    fun `account membership is directed toward account hub`() {
        assertTrue(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.MEMBER_OF_ACCOUNT,
                EntryType.LOGIN,
                EntryType.ACCOUNT,
            )
        )
        assertFalse(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.MEMBER_OF_ACCOUNT,
                EntryType.ACCOUNT,
                EntryType.LOGIN,
            )
        )
    }

    @Test
    fun `typed authentication factors must be relation sources`() {
        assertTrue(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.OTP_FOR,
                EntryType.OTP,
                EntryType.LOGIN,
            )
        )
        assertTrue(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.RECOVERY_FOR,
                EntryType.RECOVERY_CODE,
                EntryType.LOGIN,
            )
        )
        assertFalse(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.OTP_FOR,
                EntryType.LOGIN,
                EntryType.OTP,
            )
        )
        assertFalse(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.RECOVERY_FOR,
                EntryType.RECOVERY_CODE,
                EntryType.ACCOUNT,
            )
        )
    }

    @Test
    fun `related links accept any distinct endpoint types`() {
        assertTrue(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.RELATED_TO,
                EntryType.ACCOUNT,
                EntryType.OTP,
            )
        )
    }
}
