package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLinkKind
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryLinkPolicyTest {
    @Test
    fun `account membership is inferred independent of endpoint order`() {
        assertEquals(EntryLinkKind.ACCOUNT_MEMBERSHIP, EntryLinkPolicy.classify(EntryType.LOGIN, EntryType.ACCOUNT))
        assertEquals(EntryLinkKind.ACCOUNT_MEMBERSHIP, EntryLinkPolicy.classify(EntryType.ACCOUNT, EntryType.LOGIN))
    }

    @Test
    fun `factor associations are inferred from either endpoint order`() {
        assertEquals(EntryLinkKind.OTP_ASSOCIATION, EntryLinkPolicy.classify(EntryType.OTP, EntryType.LOGIN))
        assertEquals(EntryLinkKind.OTP_ASSOCIATION, EntryLinkPolicy.classify(EntryType.LOGIN, EntryType.OTP))
        assertEquals(EntryLinkKind.RECOVERY_ASSOCIATION, EntryLinkPolicy.classify(EntryType.RECOVERY_CODE, EntryType.LOGIN))
        assertEquals(EntryLinkKind.RECOVERY_ASSOCIATION, EntryLinkPolicy.classify(EntryType.LOGIN, EntryType.RECOVERY_CODE))
    }
}
