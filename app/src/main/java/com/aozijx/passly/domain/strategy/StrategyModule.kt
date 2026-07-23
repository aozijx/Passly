package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.strategy.impl.BankCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.IdCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasskeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasswordEntryStrategy
import com.aozijx.passly.domain.strategy.impl.RecoveryCodeEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SeedPhraseEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SshKeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.TotpEntryStrategy
import com.aozijx.passly.domain.strategy.impl.WiFiEntryStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class StrategyModule {

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.LOGIN)
    abstract fun bindPasswordStrategy(impl: PasswordEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.TOTP)
    abstract fun bindTotpStrategy(impl: TotpEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.SEED_PHRASE)
    abstract fun bindSeedPhraseStrategy(impl: SeedPhraseEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.RECOVERY_CODE)
    abstract fun bindRecoveryCodeStrategy(impl: RecoveryCodeEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.PASSKEY)
    abstract fun bindPasskeyStrategy(impl: PasskeyEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.SSH_KEY)
    abstract fun bindSshKeyStrategy(impl: SshKeyEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.WIFI)
    abstract fun bindWifiStrategy(impl: WiFiEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.BANK_CARD)
    abstract fun bindBankCardStrategy(impl: BankCardEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.ID_CARD)
    abstract fun bindIdCardStrategy(impl: IdCardEntryStrategy): EntryTypeStrategy
}
