package com.aozijx.passly.feature.autofill.legacy.runtime

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JvmInline
value class LegacySaveToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Legacy save token cannot be blank" }
    }
}

data class PendingLegacySave(
    val draft: AutofillSaveDraft,
    val scope: AutofillGrantContext,
)

@Singleton
class LegacySaveDraftStore internal constructor(
    private val nowMillis: () -> Long,
    private val tokenFactory: () -> LegacySaveToken,
) {
    @Inject
    constructor() : this(
        nowMillis = System::currentTimeMillis,
        tokenFactory = { LegacySaveToken(UUID.randomUUID().toString()) },
    )

    private data class StoredDraft(
        val pending: PendingLegacySave,
        val stagedAtMillis: Long,
    )

    private val mutex = Mutex()
    private val entries = linkedMapOf<LegacySaveToken, StoredDraft>()

    suspend fun stage(
        draft: AutofillSaveDraft,
        scope: AutofillGrantContext,
    ): LegacySaveToken = mutex.withLock {
        val now = nowMillis()
        pruneExpired(now)
        while (entries.size >= MAX_ENTRIES) {
            entries.remove(entries.keys.first())
        }
        val token = tokenFactory()
        check(token !in entries) { "Legacy save token collision" }
        entries[token] = StoredDraft(
            pending = PendingLegacySave(draft, scope.normalized()),
            stagedAtMillis = now,
        )
        token
    }

    suspend fun consume(token: LegacySaveToken): PendingLegacySave? = mutex.withLock {
        val now = nowMillis()
        pruneExpired(now)
        entries.remove(token)?.pending
    }

    suspend fun remove(token: LegacySaveToken) {
        mutex.withLock { entries.remove(token) }
    }

    private fun pruneExpired(now: Long) {
        entries.entries.removeAll { (_, stored) ->
            now - stored.stagedAtMillis >= TTL_MILLIS
        }
    }

    companion object {
        const val MAX_ENTRIES = 8
        const val TTL_MILLIS = 60_000L
    }
}
