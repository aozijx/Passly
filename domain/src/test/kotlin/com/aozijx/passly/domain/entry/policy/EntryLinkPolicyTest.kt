package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
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
            ),
        )
        assertFalse(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.MEMBER_OF_ACCOUNT,
                EntryType.ACCOUNT,
                EntryType.LOGIN,
            ),
        )
    }

    @Test
    fun `typed authentication factors must be relation sources`() {
        listOf(
            EntryRelationType.OTP_FOR to EntryType.OTP,
            EntryRelationType.RECOVERY_FOR to EntryType.RECOVERY_CODE,
        ).forEach { (relation, factorType) ->
            assertTrue(
                EntryLinkPolicy.isAllowed(relation, factorType, EntryType.LOGIN),
            )
            listOf(EntryType.ACCOUNT, EntryType.OTP, EntryType.RECOVERY_CODE).forEach { targetType ->
                assertFalse(
                    "$relation -> $targetType",
                    EntryLinkPolicy.isAllowed(relation, factorType, targetType),
                )
            }
        }
        assertFalse(
            EntryLinkPolicy.isAllowed(
                EntryRelationType.OTP_FOR,
                EntryType.LOGIN,
                EntryType.OTP,
            ),
        )
    }

    @Test
    fun `related links accept every endpoint type pair`() {
        EntryType.values().forEach { source ->
            EntryType.values().forEach { target ->
                assertTrue(
                    "$source -> $target",
                    EntryLinkPolicy.isAllowed(EntryRelationType.RELATED_TO, source, target),
                )
            }
        }
    }
}
