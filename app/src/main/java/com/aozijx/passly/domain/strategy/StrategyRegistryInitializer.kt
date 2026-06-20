package com.aozijx.passly.domain.strategy

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StrategyRegistryInitializer @Inject constructor(
    strategies: Map<Int, @JvmSuppressWildcards EntryTypeStrategy>
) {
    init {
        val mapped = strategies.mapKeys { (key, _) ->
            com.aozijx.passly.domain.model.EntryType.fromValue(key)
        }
        EntryTypeStrategyFactory.registerAll(mapped)
    }
}