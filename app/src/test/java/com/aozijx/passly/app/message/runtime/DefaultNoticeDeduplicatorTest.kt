package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.data.message.model.NoticeCode
import com.aozijx.passly.app.message.contract.DeduplicationClaim
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNoticeDeduplicatorTest {
    @Test
    fun exactClaimBlocksDuplicatesAndExpiresAfterCompletion() {
        var now = 1_000L
        val deduplicator = DefaultNoticeDeduplicator { now }
        val claim = deduplicator.begin("event-1", 2_000)
        assertTrue(claim is DeduplicationClaim.Acquired)
        assertTrue(deduplicator.begin("event-1", 2_000) is DeduplicationClaim.Duplicate)

        deduplicator.complete(claim as DeduplicationClaim.Acquired)
        now = 3_001L
        assertTrue(deduplicator.begin("event-1", 2_000) is DeduplicationClaim.Acquired)
    }

    @Test
    fun releasedClaimCanBeRetriedImmediately() {
        val deduplicator = DefaultNoticeDeduplicator { 1_000L }
        val claim = deduplicator.begin("event-2", 2_000) as DeduplicationClaim.Acquired
        deduplicator.release(claim)

        assertTrue(deduplicator.begin("event-2", 2_000) is DeduplicationClaim.Acquired)
    }

    @Test
    fun semanticWindowUsesNoticeCode() {
        var now = 5_000L
        val deduplicator = DefaultNoticeDeduplicator { now }

        assertFalse(
            deduplicator.claimSemantic(NoticeCode.CLIPBOARD_CLEARED, 5_000)
        )
        assertTrue(
            deduplicator.claimSemantic(NoticeCode.CLIPBOARD_CLEARED, 5_000)
        )
        now = 10_001L
        assertFalse(
            deduplicator.claimSemantic(NoticeCode.CLIPBOARD_CLEARED, 5_000)
        )
    }
}
