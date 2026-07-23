package com.aozijx.passly.data.repository.search

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.local.dao.buildEntryIdIntersectionQuery
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.lookup.LookupField
import com.aozijx.passly.domain.repository.search.SearchIndexRepository
import com.aozijx.passly.security.search.BlindIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlindIndexRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
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
                    searchTokenQueryDao().searchByTokenIntersection(sqlQuery)
                        .flowOn(Dispatchers.IO)
                }
            }
        }
}
