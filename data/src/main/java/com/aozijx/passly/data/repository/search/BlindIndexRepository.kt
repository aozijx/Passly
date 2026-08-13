package com.aozijx.passly.data.repository.search

import com.aozijx.passly.data.local.database.session.UnifiedSessionManager
import com.aozijx.passly.data.local.dao.buildEntryIdIntersectionQuery
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.lookup.LookupField
import com.aozijx.passly.domain.entry.repository.SearchIndexRepository
import com.aozijx.passly.security.search.BlindIndexer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BlindIndexRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val blindIndexer: BlindIndexer
) : SearchIndexRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(query: String, fields: List<LookupField>): Flow<List<String>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized || query.isBlank()) flowOf(emptyList())
            else sessionManager.observeFlow {
                val searchTokens = blindIndexer.searchTokens(query)
                if (searchTokens.isEmpty()) flowOf(emptyList())
                else {
                    val sqlQuery = buildEntryIdIntersectionQuery(searchTokens, fields)
                    val result = searchTokenQueryDao().searchByTokenIntersection(sqlQuery)
                    flowOf(result)
                }
            }
        }
}
