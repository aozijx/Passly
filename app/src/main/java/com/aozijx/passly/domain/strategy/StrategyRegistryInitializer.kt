package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntryType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StrategyRegistryInitializer @Inject constructor(
    strategies: Map<String, @JvmSuppressWildcards EntryTypeStrategy>
) {
    init {
        val mapped = strategies.mapKeys { (key, _) ->
            EntryType.fromName(key)
        }
        EntryTypeStrategyFactory.registerAll(mapped)
    }
}