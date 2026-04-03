package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.strategy.impl.BankCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.IdCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasskeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasswordEntryStrategy
import com.aozijx.passly.domain.strategy.impl.RecoveryCodeEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SeedPhraseEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SshKeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.TotpEntryStrategy
import com.aozijx.passly.domain.strategy.impl.WiFiEntryStrategy

object EntryTypeStrategyRegistry {
    private var initialized = false

    @Synchronized
    fun ensureRegistered() {
        if (initialized) return

        val defaults = listOf(
            PasswordEntryStrategy(),
            TotpEntryStrategy(),
            PasskeyEntryStrategy(),
            RecoveryCodeEntryStrategy(),
            WiFiEntryStrategy(),
            BankCardEntryStrategy(),
            SeedPhraseEntryStrategy(),
            IdCardEntryStrategy(),
            SshKeyEntryStrategy()
        )

        defaults.forEach { strategy ->
            if (!EntryTypeStrategyFactory.hasStrategy(strategy.entryType)) {
                EntryTypeStrategyFactory.register(strategy)
            }
        }

        initialized = true
    }
}