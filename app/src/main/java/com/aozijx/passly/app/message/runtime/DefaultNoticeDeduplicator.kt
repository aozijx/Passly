package com.aozijx.passly.app.message.runtime

import android.os.SystemClock
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.contract.DeduplicationClaim
import com.aozijx.passly.app.message.contract.NoticeDeduplicator
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNoticeDeduplicator internal constructor(
    private val nowMs: () -> Long
) : NoticeDeduplicator {
    @Inject
    constructor() : this(SystemClock::elapsedRealtime)

    private data class EventClaim(
        val token: Long,
        val deadlineMs: Long,
        val inFlight: Boolean
    )

    private val nextToken = AtomicLong(0)
    private val eventClaims = object : LinkedHashMap<String, EventClaim>(
        MAX_EVENT_IDS,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, EventClaim>
        ): Boolean = size > MAX_EVENT_IDS
    }
    private val semanticDeadlines = mutableMapOf<NoticeCode, Long>()

    @Synchronized
    override fun begin(eventId: String, ttlMs: Long): DeduplicationClaim {
        require(eventId.isNotBlank())
        require(ttlMs > 0)
        evictExpired()
        if (eventClaims.containsKey(eventId)) return DeduplicationClaim.Duplicate
        val token = nextToken.incrementAndGet()
        eventClaims[eventId] = EventClaim(
            token = token,
            deadlineMs = Long.MAX_VALUE,
            inFlight = true
        )
        return DeduplicationClaim.Acquired(eventId, token, ttlMs)
    }

    @Synchronized
    override fun complete(claim: DeduplicationClaim.Acquired) {
        val current = eventClaims[claim.eventId] ?: return
        if (current.token != claim.token) return
        eventClaims[claim.eventId] = current.copy(
            deadlineMs = safeDeadline(nowMs(), claim.ttlMs),
            inFlight = false
        )
    }

    @Synchronized
    override fun release(claim: DeduplicationClaim.Acquired) {
        val current = eventClaims[claim.eventId] ?: return
        if (current.token == claim.token && current.inFlight) {
            eventClaims.remove(claim.eventId)
        }
    }

    @Synchronized
    override fun claimSemantic(code: NoticeCode, windowMs: Long): Boolean {
        require(windowMs > 0)
        evictExpired()
        val now = nowMs()
        val current = semanticDeadlines[code]
        if (current != null && now < current) return true
        semanticDeadlines[code] = safeDeadline(now, windowMs)
        return false
    }

    private fun evictExpired() {
        val now = nowMs()
        eventClaims.entries.removeAll { (_, claim) ->
            !claim.inFlight && now >= claim.deadlineMs
        }
        semanticDeadlines.entries.removeAll { (_, deadline) -> now >= deadline }
    }

    private fun safeDeadline(now: Long, duration: Long): Long =
        if (Long.MAX_VALUE - now < duration) Long.MAX_VALUE else now + duration

    private companion object {
        const val MAX_EVENT_IDS = 1024
    }
}
