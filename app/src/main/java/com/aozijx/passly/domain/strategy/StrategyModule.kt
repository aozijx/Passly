package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.strategy.impl.BankCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.IdCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SshKeyEntryStrategy
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
    @EntryTypeKey("SSH_KEY")
    abstract fun bindSshKeyStrategy(impl: SshKeyEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey("WIFI")
    abstract fun bindWifiStrategy(impl: WiFiEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey("BANK_CARD")
    abstract fun bindBankCardStrategy(impl: BankCardEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @EntryTypeKey("ID_CARD")
    abstract fun bindIdCardStrategy(impl: IdCardEntryStrategy): EntryTypeStrategy
}