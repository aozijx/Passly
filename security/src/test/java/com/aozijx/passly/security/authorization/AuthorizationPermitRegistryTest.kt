package com.aozijx.passly.security.authorization

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.MonotonicClock
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationPermitRegistryTest {
    private var elapsedMs = 1_000L
    private val registry = AuthorizationPermitRegistry(MonotonicClock { elapsedMs })
    private val scope = AuthorizationScope.SensitiveFields(
        entryId = EntryId("entry-1"),
        fieldKeys = setOf(SensitiveFieldKey.CARD_CVV),
        action = SensitiveAccessAction.REVEAL,
    )

    @Test
    fun `permit is exact-scope bound and single use`() {
        val permit = registry.issue(scope, ttlMs = 1_000L)
        val wrongScope = scope.copy(entryId = EntryId("entry-2"))

        assertFalse(registry.consume(permit, wrongScope))
        assertFalse(registry.consume(permit, scope))

        val validPermit = registry.issue(scope, ttlMs = 1_000L)
        assertTrue(registry.consume(validPermit, scope))
        assertFalse(registry.consume(validPermit, scope))
        assertEquals(0, registry.activePermitCount())
    }

    @Test
    fun `external permit implementation cannot forge registry membership`() {
        val forged = object : AuthorizationPermit {}

        assertFalse(registry.consume(forged, scope))
    }

    @Test
    fun `expired and revoked permits are rejected`() {
        val expired = registry.issue(scope, ttlMs = 10L)
        elapsedMs += 10L
        assertFalse(registry.consume(expired, scope))

        val revoked = registry.issue(scope, ttlMs = 1_000L)
        registry.revokeAll()
        assertFalse(registry.consume(revoked, scope))
        assertEquals(0, registry.activePermitCount())
    }

    @Test
    fun `revision permit is bound to revision fields and action`() {
        val revisionScope = AuthorizationScope.SensitiveRevision(
            entryId = EntryId("entry-1"),
            revisionId = "revision-1",
            fieldKeys = setOf(SensitiveFieldKey.CARD_CVV),
            action = SensitiveRevisionAccessAction.REVEAL,
        )
        val wrongRevision = revisionScope.copy(revisionId = "revision-2")
        val permit = registry.issue(revisionScope, ttlMs = 1_000L)

        assertFalse(registry.consume(permit, wrongRevision))
        assertFalse(registry.consume(permit, revisionScope))

        val restorePermit = registry.issue(
            revisionScope.copy(action = SensitiveRevisionAccessAction.RESTORE),
            ttlMs = 1_000L,
        )
        assertFalse(registry.consume(restorePermit, revisionScope))
    }
}
