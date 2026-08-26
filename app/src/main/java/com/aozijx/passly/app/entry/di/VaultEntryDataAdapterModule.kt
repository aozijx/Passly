package com.aozijx.passly.app.entry.di

import com.aozijx.passly.app.entry.paging.DataVaultDataChangeSignal
import com.aozijx.passly.app.entry.paging.DataVaultEntryPageSource
import com.aozijx.passly.feature.vault.entry.VaultDataChangeSignal
import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultEntryDataAdapterModule {
    @Binds
    @Singleton
    abstract fun bindVaultEntryPageSource(
        implementation: DataVaultEntryPageSource,
    ): VaultEntryPageSource

    @Binds
    @Singleton
    abstract fun bindVaultDataChangeSignal(
        implementation: DataVaultDataChangeSignal,
    ): VaultDataChangeSignal
}
