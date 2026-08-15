package com.aozijx.passly.app.entry.di

import com.aozijx.passly.app.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.app.entry.policy.DefaultEntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 领域策略接口的默认实现由 app 层提供（实现位于 app/entry/policy）。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EntryPolicyModule {

    @Binds
    @Singleton
    abstract fun bindEntryTypePolicy(impl: DefaultEntryTypePolicy): EntryTypePolicy

    @Binds
    @Singleton
    abstract fun bindEntryFieldReader(impl: DefaultEntryFieldReader): EntryFieldReader
}
