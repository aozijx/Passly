package com.aozijx.passly.app.entry.di

import com.aozijx.passly.domain.entry.policy.DefaultEntryDefaultsProvider
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.policy.DefaultEntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryDefaultsProvider
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EntryPolicyModule {

    @Binds
    @Singleton
    abstract fun bindEntryTypePolicy(impl: DefaultEntryTypePolicy): EntryTypePolicy

    @Binds
    @Singleton
    abstract fun bindEntryDefaultsProvider(impl: DefaultEntryDefaultsProvider): EntryDefaultsProvider

    @Binds
    @Singleton
    abstract fun bindEntryFieldReader(impl: DefaultEntryFieldReader): EntryFieldReader

}
